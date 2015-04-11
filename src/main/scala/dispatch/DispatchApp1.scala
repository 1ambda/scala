package dispatch

import dispatch._, Defaults._
import org.json4s._
import scala.util.{Success, Failure}

// ref: http://dispatch.databinder.net/Dispatch.html
// ref: https://gist.github.com/travisbrown/6105462
object DispatchApp1 extends App {
  val svc = url("https://api.coursera.org/api/catalog.v1/categories")
  val courses = Http(svc OK as.json4s.Json)

  for(c <- courses) println(c)
  // for(length <- lengthFuture) println(length)
  readLine()
}
