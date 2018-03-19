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
    val yaml = new Yaml()
    val stream = new FileInputStream(BuildInfo.configFile)
    val map = yaml.load(stream).asInstanceOf[java.util.Map[String,Any]].asScala
    stream.close()

    ConfigFile(
      version1x = map("version1x").asInstanceOf[String],
      version2x = map("version2x").asInstanceOf[String],
      version3x = map("version3x").asInstanceOf[String]
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
