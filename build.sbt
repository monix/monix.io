scalaVersion := "2.12.4"

name := "monix-website"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  "io.get-coursier" %% "coursier" % "1.0.0",
  "io.get-coursier" %% "coursier-cache" % "1.0.0",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "org.yaml" % "snakeyaml" % "1.19"
)

lazy val configFile = SettingKey[File]("configFile")
lazy val tutInput = SettingKey[File]("tutInput")
lazy val tutOutput = SettingKey[File]("tutOutput")
lazy val tutVersion = SettingKey[String]("tutVersion")

configFile := (baseDirectory in ThisBuild).value / "_config.yml"
tutInput := (baseDirectory in ThisBuild).value / "_tut"
tutOutput := (baseDirectory in ThisBuild).value
tutVersion := "0.5.6"

watchSources ++= (tutInput.value ** "*.md").get

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](tutInput, tutOutput, tutVersion, configFile)

buildInfoPackage := "io.monix.website"
