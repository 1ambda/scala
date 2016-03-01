name := "macro"

version := "0.0.1"

scalaVersion := "2.11.7"

lazy val settingsCommon = Seq(
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-all" % "1.9.5",
    "org.scalatest" %% "scalatest" % "3.0.0-M15"
  )
)

lazy val projectInterpolator = Project(
  "interpolator",
  file("interpolator"),
  settings = settingsCommon ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.1"
    )
  )
)

lazy val projectApplication = Project(
  "application",
  file("application"),
  settings = settingsCommon
).dependsOn(projectInterpolator)

