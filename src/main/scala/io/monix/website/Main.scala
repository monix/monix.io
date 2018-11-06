package io.monix.website

import java.io.{File, FileInputStream}
import org.yaml.snakeyaml.Yaml
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.collection.immutable.Queue
import java.util.concurrent.atomic.AtomicReference

object Main extends App {
  def worker(queue: AtomicReference[Queue[Post]]): Future[Unit] = {
    val current = queue.get
    if (current.nonEmpty) {
      val (post, update) = current.dequeue
      if (!queue.compareAndSet(current, update))
        worker(queue)
      else
        Future(post.process()).flatMap(_ => worker(queue))
    } else {
      Future.successful(())
    }
  }

  lazy val configFile: ConfigFile = {
    import java.util.LinkedHashMap

    val yaml = new Yaml()
    val stream = new FileInputStream(BuildInfo.configFile)
    val map = yaml.load(stream).asInstanceOf[java.util.Map[String,Any]].asScala   
    stream.close()

    def getValue(key1: String, key2: String): String =
      Option(map(key1).asInstanceOf[LinkedHashMap[String, String]].get(key2).asInstanceOf[String])
        .getOrElse("")

    ConfigFile(
      code = VersionsSet(
        version1x = getValue("code", "version1x"),
        version2x = getValue("code", "version2x"),
        version3x = getValue("code", "version3x")
      ),
      promoted = VersionsSet(
        version1x = getValue("promoted", "version1x"),
        version2x = getValue("promoted", "version2x"),
        version3x = getValue("promoted", "version3x")
      )
    )
  }

  def listFilesRecursively(dir: File, files: List[File], rest: List[File]): List[File] = {
    val list = Option(dir.listFiles()).map(_.toList).toList.flatten
    val moreDirs = list.filter(_.isDirectory()) ::: rest
    val moreFiles = list.filter(f => f.isFile && f.getName.endsWith(".md")) ::: files

    moreDirs match {
      case h :: remaining => listFilesRecursively(h, moreFiles, remaining)
      case Nil => moreFiles
    }
  }

  val posts = listFilesRecursively(BuildInfo.tutInput, Nil, Nil).map(Post(_, configFile))
  val queue = new AtomicReference(Queue(posts:_*))
  
  val parallelism = 
    Option(System.getenv("TUT_PARALLELISM")).filterNot(_.isEmpty) match {
      case Some(value) if value.matches("^\\d+$") =>
        val nr = value.toInt
        val procs = Runtime.getRuntime().availableProcessors()
        if (nr < 1) 1
        else if (procs > 1 && nr > procs) procs
        else nr
      case _ =>
        1
    }

  val f = Future.sequence((0 until parallelism).map(_ => worker(queue)))
  Await.result(f, Duration.Inf)
}
