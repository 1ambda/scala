## learn-scala

### 1. SBT

Based on http://www.scala-sbt.org/0.13/tutorial/

#### Basics

```
$ sbt
> compile
> run
> clean
> package


$ sbt compile run
```

`build.sbt` : **Scala** version, application info
`project/build.properties` : specify **SBT** version

Also, you can use `build.scala` instead of `build.sbt`which are suitable for most cases. The .scala files are typically used for sharing code across .sbt files and for more complex build definitions.

#### Keys

```scala
// build.sbt

name := "learn-scala"

version := "1.0"

scalaVersion := "2.11.2"
```

Each of these expressions is called `Key`. **Key** is an instance of `SettingKey[T]`, `InputKey[T]`, `TaskKey[T]` and should be seperated by blank lines. Using keys, You can get values for example `SettingKey["name"]` will return `"learn-scala"` string.

There are three flavors of key

- **SettingKey[T]** : a key for a value computed once when loading the project, and kept around  
- **SettingKey[T]** : a key for a value, called a task that has to be re computed each time, potentially with side effects  
- **SettingKey[T]** : a key for a task that has command line arg as input  

#### Defining Tasks

```scala
// build.sbt

example := { println("Example Task") }
```

#### Imports in build.sbt

You can place import statement at the top of `build.sbt`. But They need not to be seperated by blank lines.

```scala
// build.sbt

import sbt._
import Process._
import Keys._
```

#### Adding library dependencies

To depend on thrid-party libraries, there are two options.

1. Drop jars in **lib/**. in case of unmanaged dependencies
2. Add managed dependencies in `build.sbt`. It will look like this

```scala
// build.sbt

// groupID %% artifactID % revision % configuration
// configuration is optional
libraryDependencies += "org.scala-tools.testing" %% "scalacheck" % "1.8" % "test"
```

You can see more complex example [Here](http://www.scala-sbt.org/0.13/docs/Basic-Def-Examples.html)

<br/>
Here is another example of `build.scala`

```scala
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "My first application"
    val appVersion      = "1.0"

    val appDependencies = Seq(
        
      "org.scala-tools" %% "scala-stm" % "0.3",
      "org.apache.derby" % "derby" % "10.4.1.3" % "test"
      
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(defaultScalaSettings:_*).settings(
      
      resolvers += "JBoss repository" at "https://repository.jboss.org/nexus/content/repositories/",
      resolvers += "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
            
    )

}
```
