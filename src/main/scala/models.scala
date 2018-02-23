package io.monix.website

case class TutConfig(dependencies: List[String])
case class FrontMatter(tut: TutConfig)

case class ConfigFile(
  version1x: String, 
  version2x: String, 
  version3x: String
)
