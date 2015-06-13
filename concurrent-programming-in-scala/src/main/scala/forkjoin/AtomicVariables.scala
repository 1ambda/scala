package forkjoin

import java.util.concurrent.atomic.AtomicLong

import thread.ThreadUtils

import scala.annotation.tailrec

object AtomicUid extends App with ThreadUtils with ExecutorUtils {
  private val uid = new AtomicLong(0L)

  def genUniqueId(): Long = uid.incrementAndGet()

  execute { log(s"Uid asynchronously: ${genUniqueId()}")}

  log(s"Got a unique: id ${genUniqueId()}")
}

object InvalidUid extends App with ThreadUtils with ExecutorUtils {
  @volatile private var uid: Long = 0L

  def genUniqueId(): Long = {
    // volatile field is visible to other thread,
    // but entire operations in genUniqueId method is not atomic
    val fresh = uid + 1
    uid = fresh
    fresh
  }

  for(i <- 0 until 20) yield thread {
    log(s"uid: ${genUniqueId()}")
    Thread.sleep(20);
  }

  Thread.sleep(1000);
}

object UidImplementUsingCAS extends App with ThreadUtils with ExecutorUtils {
  private var uid = 0L

  def compareAndSet(currentValue: Long, newValue: Long): Boolean = this.synchronized {
    if (currentValue != this.uid) false else {
      this.uid = newValue
      true
    }
  }

  @tailrec def genUniqueId(): Long = {
    val oldUid = uid
    val newUid = uid + 1
    if (compareAndSet(oldUid, newUid)) newUid else genUniqueId()
  }

  for(i <- 0 until 1000) yield thread {
    log(s"uid: ${genUniqueId()}")
    Thread.sleep(20);
  }

  Thread.sleep(1000);
}

object UidImplUsingAtomicRefAndCAS extends App with ThreadUtils with ExecutorUtils {
  private val uid = new AtomicLong(0L)

  @tailrec def genUniqueId(): Long = {
    val oldUid = uid.get
    val newUid = oldUid + 1
    if (uid.compareAndSet(oldUid, newUid)) newUid else genUniqueId()
  }

  for(i <- 0 until 1000) yield thread {
    log(s"uid: ${genUniqueId()}")
    Thread.sleep(20);
  }

  Thread.sleep(1000);
}