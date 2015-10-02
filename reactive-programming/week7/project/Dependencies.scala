import sbt._
import Keys._

object Dependencies {
  val akkaVersion = "2.4-M1"

  val akkaActor       = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaRemote      = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  val akkaCluster     = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  val akkaTestkit     = "com.typesafe.akka" %% "akka-testkit"% akkaVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion

  val scalaTestVersion = "3.0.0-M3"
  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion

  val json4s = "org.json4s" %% "json4s-native" % "3.2.11"
  val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
  val jsoup = "org.jsoup" % "jsoup" % "1.8.2"
  val asyncHttpClient = "com.ning" % "async-http-client" % "1.9.28"
}