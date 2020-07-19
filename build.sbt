lazy val sharedSettings = Seq(
  scalaVersion := "2.12.12"
)

lazy val root = project
  .in(file("."))
  .aggregate(docs2x)
  .settings(sharedSettings)
  .settings(
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )

lazy val docs2x = project       // new documentation project
  .in(file(".mdoc-projects/2x")) // important: it must not be docs/  
  .enablePlugins(MdocPlugin)
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "2.3.3",
      "io.monix" %% "monix-scalaz-72" % "2.3.3",
      "io.monix" %% "monix-cats" % "2.3.3",
      "org.slf4j" % "slf4j-api" % "1.7.30",
    ),
    mdocIn := file("_docs/2x"),
    mdocOut := file("docs/2x"),
  )
