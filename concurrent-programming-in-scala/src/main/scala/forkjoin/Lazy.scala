package forkjoin

import thread.ThreadUtils

object Lazy

object LazyValsCreate extends App with ExecutorUtils with ThreadUtils {
  lazy val obj = new AnyRef
  lazy val non = s"made by ${Thread.currentThread.getName}"

  execute {
    log(s"EC sees obj = $obj")
    log(s"EC sees non = $non")
  }

  log(s"Main sees obj = $obj")
  log(s"Main sees non = $non")
}

object LazyValsObject extends App with ExecutorUtils with ThreadUtils {
  object Lazy { log("Running Lazy constructor")}

  log("Main thread is about to reference Lazy.")
  Lazy
  log("Main thread completed")
}

object LazyValsUnderTheHood extends App with ExecutorUtils with ThreadUtils {
  @volatile private var _bitmap = false
  private var _obj: AnyRef = _

  def obj = if (_bitmap) _obj else this.synchronized {
    if (!_bitmap) {
      _obj = new AnyRef
      _bitmap = true
    }

    _obj
  }

  log(s"$obj")
  log(s"$obj")
}

object LazyValsDeadLock extends App with ExecutorUtils with ThreadUtils {
  object A { lazy val x: Int = B.y }
  object B { lazy val y: Int = A.x }

  execute { B.y }
}

object LazyValsAndBlocking extends App with ExecutorUtils with ThreadUtils {
  lazy val x: Int = {
    val t = thread { println(s"Initializing $x")}

    t.join
    1
  }

  x
}

object LazyValsAndMonitors extends App with ExecutorUtils with ThreadUtils {
  lazy val x = 1

  this.synchronized {
    val t = thread { x }
    t.join()
  }
}
