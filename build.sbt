scalaVersion := "2.11.8"

name := "monix-website"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "1.0.0-M15",
  "io.get-coursier" %% "coursier-cache" % "1.0.0-M15",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.yaml" % "snakeyaml" % "1.17"
)

lazy val configFile = SettingKey[File]("configFile")
lazy val tutInput = SettingKey[File]("tutInput")
lazy val tutOutput = SettingKey[File]("tutOutput")
lazy val tutVersion = SettingKey[String]("tutVersion")

configFile := (baseDirectory in ThisBuild).value / "_config.yml"
tutInput := (baseDirectory in ThisBuild).value / "_tut"
tutOutput := (baseDirectory in ThisBuild).value
tutVersion := "0.4.8"

watchSources ++= (tutInput.value ** "*.md").get

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](tutInput, tutOutput, tutVersion, configFile)

buildInfoPackage := "io.monix.website"
