name := "lectures"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/releases"

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "3.0.0-RC2",
  "com.storm-enroute" %% "scalameter" % "0.7"
)
