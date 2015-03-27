import sbt._
import Process._
import Keys._

name := "scala-note"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

testOptions in Test += Tests.Argument("-oI")
