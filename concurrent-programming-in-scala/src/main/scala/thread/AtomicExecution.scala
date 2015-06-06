package thread

class AtomicExecution { }

object ThreadCommunicate extends App with ThreadUtils {
  var result: String = null

  val t = thread { result = "\nTitle\n" + "=" * 5 }
  t.join
  log(result)
}

object ThreadProtectedUid extends App with ThreadUtils {
  var uidCount = 0L

  def genUniqueUid() = this.synchronized {
    val freshUid = uidCount + 1
    uidCount = freshUid
    freshUid
  }

  def printUniqueIds(n: Int): Unit = {
    val uids = for (i <- 0 until n) yield genUniqueUid()
    log(s"Generated uids: $uids")
  }

  val t = thread { printUniqueIds(5) }
  printUniqueIds(5)

  t.join()
  log("Completed")
}

object ThreadUnProtectedUid extends App with ThreadUtils {
  var uidCount = 0L

  def genUniqueUid() = {
    val freshUid = uidCount + 1
    uidCount = freshUid
    freshUid
  }

  def printUniqueIds(n: Int): Unit = {
    val uids = for (i <- 0 until n) yield genUniqueUid()
    log(s"Generated uids: $uids")
  }

  val t = thread { printUniqueIds(5) }
  printUniqueIds(5)

  t.join()
  log("Completed")
}