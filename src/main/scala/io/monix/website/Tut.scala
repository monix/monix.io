package io.monix.website

import coursier._
import coursier.util.Parse
import java.io.File
import java.net.URLClassLoader

import scala.util.control.NonFatal

case class FrontMatter(tut: Tut)

case class ConfigFile(
  version1x: String,
  version2x: String,
  version3x: String
)

case class Tut(
  scala: String,
  binaryScala: String,
  dependencies: List[String]) {

  val tutResolution: Resolution = Resolution(Set(
    Dependency(Module("org.tpolecat", s"tut-core_$binaryScala"), BuildInfo.tutVersion)
  ))

  def parsedDependencies(config: ConfigFile): List[String] =
    dependencies.map { uri =>
      uri.replaceAll("version1x", config.version1x)
        .replaceAll("version2x", config.version2x)
        .replaceAll("version3x", config.version3x)
    }

  def libResolution(config: ConfigFile): Resolution =
    Resolution(parsedDependencies(config).map { dep =>
      val (mod, v) = Parse.moduleVersion(dep, BuildInfo.scalaVersion).right.get
      Dependency(mod, v)
    }.toSet)

  def invoke(config: ConfigFile, in: File, out: File): Unit = {
    val tutClasspath = resolve(tutResolution).get
    val libClasspath = resolve(libResolution(config)).get

    val classLoader = new URLClassLoader(tutClasspath.map(_.toURI.toURL).toArray, null)
    val tutClass = classLoader.loadClass("tut.TutMain")
    val tutMain = tutClass.getDeclaredMethod("main", classOf[Array[String]])

    val commandLine = Array(
      in.getAbsolutePath,
      out.getParentFile.getAbsolutePath,
      ".*",
      "-classpath",
      libClasspath.mkString(File.pathSeparator)
    )

    try {
      try tutMain.invoke(null, commandLine)
      finally classLoader.close()
    } catch {
      case NonFatal(e) =>
        out.delete()
        throw e
    }
  }
}
