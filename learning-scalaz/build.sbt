name := "learning-scalaz"

version := "1.0"

scalaVersion := "2.11.7"

val scalazVersion = "7.2.0-M2"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
  "org.scalatest" % "scalatest_2.11" % "3.0.0-M5"
)