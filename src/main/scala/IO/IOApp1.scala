package IO

import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._

// ref: http://blog.higher-order.com/assets/scalaio.pdf
object IOApp1 extends App {
  val helloWorld: IO[Unit] = for {
    _ <- putStrLn("Hello World")
  } yield ()

  helloWorld.unsafePerformIO()

  val ask: IO[String] = for {
    _ <- putStrLn("What is your name?: ")
    name <- readLn
    _ <- putStrLn("Hello, " ++ name)
  } yield name 

  ask.unsafePerformIO()
}
