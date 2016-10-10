package io.monix.website

import coursier._
import coursier.util.Parse
import java.io.{File, FileInputStream}
import java.net.URLClassLoader
import java.nio.file.Files
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
    out.getParentFile()

  lazy val out: File = {
    val source = file.getCanonicalPath.toString
    val inputDir = BuildInfo.tutInput.getCanonicalPath.toString
    val outputFile = new File(BuildInfo.tutOutput, source.replaceFirst(inputDir, "."))
    outputFile.getParentFile().mkdirs()
    outputFile
  }

  def outdated(): Boolean =
    !(out.exists() && out.isFile() && file.lastModified() <= out.lastModified())

  def process(): Unit = 
    if (outdated()) {
      println(s"[blog] Processing ${file.getName} ...")
      BuildInfo.tutOutput.mkdirs()

      frontMatter match {
        case Some(FrontMatter(tut)) =>
          invoke(tut, file, outDir)
        case None =>
          println("[blog] No tut header, copying.")
          Files.copy(file.toPath, out.toPath)
      }
    }
    else {
      println(s"[blog] Skipping ${file.getName} (up to date).")
    }

  def tutResolution(tut: Tut): Resolution = 
    Resolution(Set(
      Dependency(Module("org.tpolecat", s"tut-core_${tut.binaryScala}"), BuildInfo.tutVersion)
    ))

  def parsedDependencies(tut: Tut): List[String] =
    tut.dependencies.map { uri =>
      uri.replaceAll("version1x", config.version1x)
         .replaceAll("version2x", config.version2x)
    }


  def libResolution(tut: Tut): Resolution =
    Resolution(parsedDependencies(tut).map { dep =>
      val (mod, v) = Parse.moduleVersion(dep, tut.binaryScala).right.get
      Dependency(mod, v)
    }.toSet)

  def invoke(tut: Tut, file: File, outputDir: File): Unit = {
    import tut._
    val tutClasspath = resolve(tutResolution(tut)).get
    val libClasspath = resolve(libResolution(tut)).get

    val classLoader = new URLClassLoader(tutClasspath.map(_.toURI.toURL).toArray, null)
    val tutClass = classLoader.loadClass("tut.TutMain")
    val tutMain = tutClass.getDeclaredMethod("main", classOf[Array[String]])

    val commandLine = Array(
      file.toString,
      outputDir.toString,
      ".*",
      "-classpath",
      libClasspath.mkString(File.pathSeparator)
    )

    tutMain.invoke(null, commandLine)
  }  
}
