package io.monix.website

import coursier._
import java.io.{File, FileInputStream}
import java.nio.file.Files
import org.yaml.snakeyaml.Yaml
import scala.util.Try

case class Post(file: File) {

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
          tut.invoke(file, outDir)
        case None =>
          println("[blog] No tut header, copying.")
          Files.copy(file.toPath, out.toPath)
      }
    }
    else {
      println(s"[blog] Skipping ${file.getName} (up to date).")
    }

}
