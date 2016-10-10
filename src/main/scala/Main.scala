package io.monix.website

import org.yaml.snakeyaml.Yaml
import java.io.{File, FileInputStream}
import scala.collection.JavaConverters._
import scala.util.Try

object Main extends App {
  lazy val configFile: ConfigFile = {
    val yaml = new Yaml()
    val stream = new FileInputStream(BuildInfo.configFile)
    val map = yaml.load(stream).asInstanceOf[java.util.Map[String,Any]].asScala
    stream.close()

    ConfigFile(
      version1x = map("version1x").asInstanceOf[String],
      version2x = map("version2x").asInstanceOf[String]
    )
  }

  println(configFile)
  
  def listFilesRecursively(dir: File, files: List[File], rest: List[File]): List[File] = {
    val list = Option(dir.listFiles()).map(_.toList).toList.flatten
    val moreDirs = list.filter(_.isDirectory()) ::: rest
    val moreFiles = list.filter(f => f.isFile() && f.getName.endsWith(".md")) ::: files

    moreDirs match {
      case h :: remaining => listFilesRecursively(h, moreFiles, remaining)
      case Nil => moreFiles
    }
  }

  val posts = listFilesRecursively(BuildInfo.tutInput, Nil, Nil).map(Post(_, configFile))
  posts.foreach(_.process())
}
