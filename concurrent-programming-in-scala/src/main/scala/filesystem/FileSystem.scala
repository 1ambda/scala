package forkjoin

import java.util.concurrent.atomic.AtomicReference

import thread.ThreadUtils

import scala.annotation.tailrec

//object FileSystem extends ThreadUtils {
//  @tailrec
//  private def prepareForDelete(entry: Entry): Boolean = {
//    val s0: State = entry.state.get()
//
//    s0 match {
//      case i: Idle =>
//        if (entry.state.compareAndSet(s0, new Deleting)) true
//        else prepareForDelete(entry)
//
//      case c: Creating =>
//        logMessage("File currently created, cannot delete."); false
//
//      case c: Copying =>
//        logMessage("File currently copied, cannot delete."); false
//
//      case c: Deleting=>
//        logMessage("File currently deleted, cannot delete."); false
//    }
//  }
//
//  @tailrec
//  def releaseCopy(e: Entry): Copying = e.state.get match {
//    case c: Copying =>
//      val newState = if (1 == c.n) new Idle else new Copying(c.n -1)
//
//      if (e.state.compareAndSet(c, newState)) c
//      else releaseCopy(e)
//  }
//
//  @tailrec
//  def acquireCopy(e: Entry, c: Copying) = e.state.get match {
//    case i: Idle =>
//      c.n = 1
//      if (!e.state.compareAndSet(i ,c)) acquireCopy(e, c)
//
//    case oc: Copying =>
//      c.n = oc.n + 1
//      if (!e.state.compareAndSet(oc, c)) acquireCopy(e, c)
//  }
//
//  def logMessage(message: String) = log(message)
//}
//
//sealed trait State
//class Idle extends State
//class Creating extends State
//class Copying(val n: Int) extends State
//class Deleting extends State
//
//class Entry(val isDir: Boolean) {
//  val state = new AtomicReference[State](new Idle)
//}
