package io.monix.website

case class Tut(scala: String, binaryScala: String, dependencies: List[String])
case class FrontMatter(tut: Tut)

case class ConfigFile(version1x: String, version2x: String)
