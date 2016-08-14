name := "cats"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.6.1"
  , "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test"
)