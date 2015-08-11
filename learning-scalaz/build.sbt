name := "learning-scalaz"

version := "1.0"

scalaVersion := "2.11.7"

val scalazVersion = "7.2.0-M2"

resolvers ++= Seq(
  "tpolecat" at "http://dl.bintray.com/tpolecat/maven",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)

lazy val doobieVersion = "0.2.2"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion,
  "org.scalaz" %% "scalaz-effect" % scalazVersion,
  "org.scalaz" %% "scalaz-typelevel" % "7.1.3",
  "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion,
  "org.scalatest" % "scalatest_2.11" % "3.0.0-M5",
  "org.tpolecat"   %% "doobie-core"               % doobieVersion,
  "org.tpolecat"   %% "doobie-contrib-h2" % doobieVersion,
  "org.tpolecat"   %% "doobie-contrib-specs2"     % doobieVersion
)