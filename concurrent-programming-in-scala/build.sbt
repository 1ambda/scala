name := "concurrent-programming-in-scala"

version := "1.0"

scalaVersion := "2.11.6"

fork := true

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.scala-lang.modules" % "scala-async_2.11" % "0.9.4",
  "org.scalaz" % "scalaz-concurrent_2.11" % "7.2.0-M2",
  "com.github.scala-blitz" % "scala-blitz_2.11" % "1.2"
)

