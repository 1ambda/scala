package filesystem

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}
import java.util.concurrent.atomic.AtomicReference
import java.io.File
import forkjoin.ExecutorUtils
import org.apache.commons.io.FileUtils
import thread.ThreadUtils

import scala.collection.concurrent.TrieMap
import scala.collection.convert.decorateAsScala._
import scala.annotation.tailrec
import scala.collection._

class FileSystem(root: String) extends ThreadUtils with ExecutorUtils {
  private val messages = new LinkedBlockingQueue[String]
  val rootDir = new File(root)
  val files: concurrent.Map[String, Entry] = new TrieMap()

  def allFiles(): Iterable[String] = for((name, state) <- files) yield name

  @tailrec
  private def prepareForDelete(entry: Entry): Boolean = {
    val s0: State = entry.state.get()

    s0 match {
      case i: Idle =>
        if (entry.state.compareAndSet(s0, new Deleting)) true
        else prepareForDelete(entry)

      case c: Creating =>
        logMessage("File currently created, cannot delete."); false

      case c: Copying =>
        logMessage("File currently copied, cannot delete."); false

      case c: Deleting=>
        logMessage("File currently deleted, cannot delete."); false
    }
  }

  def deleteFile(filename: String): Unit = {
    files.get(filename) match {
      case None =>
        logMessage(s"Path '$filename' does no exist!")

      case Some(entry) if entry.isDir =>
        logMessage(s"Path '$filename' is a directory!")

      case Some(entry) /* not dir */ =>
        if (prepareForDelete(entry))
          if (FileUtils.deleteQuietly(new File(filename)))
            files.remove(filename)
    }
  }

  @tailrec
  private def acquire(entry: Entry): Boolean = {
    val s0 = entry.state.get

    s0 match {
      case _: Creating | _: Deleting =>
        logMessage("File inaccessible, cannot copy."); false

      case i: Idle =>
        if (entry.state.compareAndSet(s0, new Copying(1))) true
        else acquire(entry)

      case c: Copying =>
        if (entry.state.compareAndSet(s0, new Copying(c.n + 1))) true
        else acquire(entry)
    }
  }

  @tailrec
  private def release(entry: Entry): Unit = {
    val s0 = entry.state.get

    s0 match {
      case c: Creating =>
        if (!entry.state.compareAndSet(s0, new Idle)) release(entry)

      case c: Copying =>
        val nstate = if (c.n == 1) new Idle else new Copying(c.n - 1)

        if (!entry.state.compareAndSet(s0, nstate)) release(entry)
    }
  }

  def copyFile(src: String, dest: String): Unit = {
    files.get(src) match {
      case Some(srcEntry) if !srcEntry.isDir => execute {
        if (acquire(srcEntry)) try {
          val destEntry = new Entry(isDir = false)

          if (files.putIfAbsent(dest, destEntry) == None)
            try {
              FileUtils.copyFile(new File(src), new File(dest))
            } finally release(destEntry)

        } finally release(srcEntry)
      }
    }
  }

  def logMessage(message: String) = messages.offer(message)

  // initialize logger
  val logger = new Thread {
    setDaemon(true)
    override def run() = while (true) log(messages.take())
  }

  logger.start()

  // get all files
  for (f <- FileUtils.iterateFiles(rootDir, null, false).asScala)
    files.put(f.getName, new Entry(false))
}

sealed trait State
class Idle extends State
class Creating extends State
class Copying(val n: Int) extends State
class Deleting extends State

class Entry(val isDir: Boolean) {
  val state = new AtomicReference[State](new Idle)
}
