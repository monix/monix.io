package io.monix.website

import org.yaml.snakeyaml.Yaml
import java.io.File

object Main extends App {
  def listFilesRecursively(dir: File, files: List[File], rest: List[File]): List[File] = {
    val list = Option(dir.listFiles()).map(_.toList).toList.flatten
    val moreDirs = list.filter(_.isDirectory()) ::: rest
    val moreFiles = list.filter(f => f.isFile() && f.getName.endsWith(".md")) ::: files

    moreDirs match {
      case h :: remaining => listFilesRecursively(h, moreFiles, remaining)
      case Nil => moreFiles
    }
  }

  val posts = listFilesRecursively(BuildInfo.tutInput, Nil, Nil).map(Post(_))
  posts.foreach(_.process())
}
