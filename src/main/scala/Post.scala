package io.monix.website

import coursier._
import coursier.util.Parse
import java.io.{File, FileInputStream}
import java.nio.file.{Files, StandardCopyOption}
import org.yaml.snakeyaml.Yaml
import scala.util.Try

case class Post(file: File, config: ConfigFile) {
  lazy val frontMatter: Option[FrontMatter] = Try {
    val yaml = new Yaml()
    val stream = new FileInputStream(file)
    val any = yaml.loadAll(stream).iterator.next()
    stream.close()
    any
  }.flatMap(YAML.decodeTo[FrontMatter]).toOption

  lazy val outDir: File =
    out.getParentFile

  lazy val out: File = {
    val source = file.getCanonicalPath
    val inputDir = BuildInfo.tutInput.getCanonicalPath
    val outputFile = new File(BuildInfo.tutOutput, source.replaceFirst(inputDir, "."))
    outputFile.getParentFile.mkdirs()
    outputFile
  }

  def outdated(): Boolean =
    !(out.exists() && out.isFile && file.lastModified() <= out.lastModified())

  def process(): Unit = 
    if (outdated()) {
      println(s"[blog] Processing ${file.getName} ...")
      BuildInfo.tutOutput.mkdirs()

      frontMatter match {
        case Some(FrontMatter(tut)) =>
          invoke(tut, file, outDir)
        case None =>
          println("[blog] No tut header, copying.")
          Files.copy(file.toPath, out.toPath, StandardCopyOption.REPLACE_EXISTING)
      }
    }
    else {
      println(s"[blog] Skipping ${file.getName} (up to date).")
    }

  def parsedDependencies(tut: TutConfig): List[String] =
    tut.dependencies.map { uri =>
      uri.replaceAll("version1x", config.version1x)
         .replaceAll("version2x", config.version2x)
         .replaceAll("version3x", config.version3x)
    }

  def libResolution(tut: TutConfig): Resolution =
    Resolution(parsedDependencies(tut).map { dep =>
      val (mod, v) = Parse.moduleVersion(dep, BuildInfo.scalaVersion).right.get
      Dependency(mod, v)
    }.toSet)

  def invoke(tutConfig: TutConfig, file: File, outputDir: File): Unit = {
    val libClasspath = resolve(libResolution(tutConfig)).get
    val commandLine = Array(
      file.toString,
      outputDir.toString,
      ".*",
      "-classpath",
      libClasspath.mkString(File.pathSeparator)
    )

    tut.TutMain.main(commandLine)
  }
}
