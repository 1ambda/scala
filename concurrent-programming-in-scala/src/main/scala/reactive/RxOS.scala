package reactive

import rx.lang.scala._
import rx.lang.scala.subjects.ReplaySubject
import thread.ThreadUtils

object OldRxOS extends ThreadUtils {
  val messageBus = Observable.just(
    TimeModule.systemClock,
    FileSystemModule.fileModifications
  ).flatten.subscribe(log _)
}

object RxOS extends ThreadUtils {
  val messageBus = Subject[String]()
  val messageLog = subjects.ReplaySubject[String]()
  messageBus.subscribe(log _)
  messageBus.subscribe(messageLog)
}

object RxOSRuntime extends App with ThreadUtils {
  log(s"RxOS botting...")

  val loadedModules = List(
    TimeModule.systemClock,
    FileSystemModule.fileModifications
  ).map(_.subscribe(RxOS.messageBus))

  log(s"RxOS booted")

  Thread.sleep(2000)

  for (mod <- loadedModules) mod.unsubscribe()
  log(s"RxOS going for shutdown")
}

object FileSystemModule {
  val fileModifications = FileSystemMonitor.modified(".")
}

object TimeModule {
  import Observable._
  import scala.concurrent.duration._

  val systemClock = interval(1 second).map(t => s"system time $t")
}