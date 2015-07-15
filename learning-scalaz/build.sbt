name := "learning-scalaz"

version := "1.0"

scalaVersion := "2.11.7"

val scalazVersion = "7.2.0-M2"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
  "org.scalaz" %% "scalaz-effect" % scalazVersion,
  "org.scalaz" %% "scalaz-typelevel" % "7.1.3",
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion,
  "org.scalatest" % "scalatest_2.11" % "3.0.0-M5"
)