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
  "org.yaml" % "snakeyaml" % "1.19",
  "org.tpolecat" %% "tut-core" % "0.6.2"
)

lazy val configFile = SettingKey[File]("configFile")
lazy val tutInput = SettingKey[File]("tutInput")
lazy val tutOutput = SettingKey[File]("tutOutput")

configFile := (baseDirectory in ThisBuild).value / "_config.yml"
tutInput := (baseDirectory in ThisBuild).value / "_tut"
tutOutput := (baseDirectory in ThisBuild).value

watchSources ++= (tutInput.value ** "*.md").get

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](tutInput, tutOutput, configFile, scalaVersion)

buildInfoPackage := "io.monix.website"
