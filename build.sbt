import sbt._
import Process._
import Keys._

name := "learn-scala"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

testOptions in Test += Tests.Argument("-oI")
