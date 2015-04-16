package IO

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import java.io._

// ref: http://www.casualmiracles.com/2012/01/03/a-small-example-of-the-scalaz-io-monad/
object ListDir extends App {
  val dirIO: IO[File] = IO { new File(System.getProperty("user.dir")) }
  val filesIO: IO[Array[File]] = for(dir <- dirIO) yield dir.listFiles()
  val namesIO: IO[Array[String]] = for(files <- filesIO) yield files.map(_.getName())
  val unitsIO: IO[Array[Unit]] = for(names <- namesIO) yield names.map(println(_))

  //unitsIO.unsafePerformIO()

  // same as

  val result = for {
    dir <- dirIO
    files <- IO { dir.listFiles() }
    names <- IO { files.map(_.getName())}
  } yield names.map(println(_))

  result.unsafePerformIO()
}
