import Resolvers._
import Dependencies._
import sbtassembly.AssemblyKeys

lazy val commonSettings = Seq(
  name := "week7",
  organization := "com.github.lambda",
  version := "1.0",
  scalaVersion := "2.11.6",
  scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
  fork := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  parallelExecution := true,
  parallelExecution in Test := false
)

lazy val akkaDeps = Seq(
  akkaActor,
  akkaTestkit,
  akkaCluster,
  akkaRemote,
  akkaPersistence
)

lazy val testDeps = Seq(
  scalaTest % Test
)

lazy val crawlerDeps = Seq(
  json4s,
  jsoup,
  dispatch,
  asyncHttpClient
)

lazy val root = (project in file("."))
  .settings(commonSettings: _ *)
  .settings(
    resolvers += mavenCentral,
    libraryDependencies ++= testDeps,
    libraryDependencies ++= akkaDeps,
    libraryDependencies ++= crawlerDeps
  )

Revolver.settings