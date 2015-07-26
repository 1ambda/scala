package parallel

import java.util.concurrent.atomic.AtomicLong

import thread.ThreadUtils

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.WrappedString
import scala.collection.mutable.ArrayBuffer
import scala.collection.{GenIterable, GenSet, GenSeq}
import scala.concurrent.Future
import scala.concurrent.forkjoin.ForkJoinPool
import scala.collection.parallel._
import scala.io.Source
import scala.util.Random

object ParallelCollectionExamples

trait ParUtils {
  @volatile var dummy: Any = _

  def timed[T](body: => T): Double = {
    val start = System.nanoTime
    dummy = body
    val end   = System.nanoTime
    ((end - start) / 1000) / 1000.0
  }

  def warmedTimed[T](n: Int = 200)(body: => T): Double = {
    for (_ <- 0 until n ) body
    timed(body)
  }
}

object ParBasic extends App with ParUtils with ThreadUtils {
  val numbers = Random.shuffle(Vector.tabulate(50000000)(i => i))

  val seqTime = timed { numbers.max }
  log(s"seqTime $seqTime ms")
  val parTime = timed { numbers.par.max }
  log(s"parTime $parTime ms")
}

object ParUid extends App with ParUtils with ThreadUtils {
  private val uid = new AtomicLong(0L)

  val seqTime = timed { for (i <- (0 until 10000000)) uid.incrementAndGet() }
  log(s"seqTime = $seqTime ms")

  val parTime = timed { for (i <- (0 until 10000000).par) uid.incrementAndGet() }
  log(s"parTime = $parTime ms")
}

object ParConfig extends App with ParUtils with ThreadUtils {
  val pool = new ForkJoinPool(2)
  val customTaskSupport = new ForkJoinTaskSupport(pool)

  val numbers = Random.shuffle(Vector.tabulate(5000000)(i => i))

  val parTime = timed {
    val parNumbers = numbers.par

    parNumbers.tasksupport = customTaskSupport

    val n = parNumbers.max

    println(s"largest number $n")
  }

  log(s"parTime $parTime ms")
}

object ParHtmlSearch extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  def getHtmlSpec() = Future {
    val url = "http://akka.io/docs/"

    val specSrc = Source.fromURL(url)
    try specSrc.getLines.toArray finally specSrc.close()
  }

  getHtmlSpec() foreach { case specDoc =>
    def search(d: GenSeq[String]): Double = warmedTimed() {
      d.indexWhere(line => line.matches("quiescent"))
    }

    val seqTime = search(specDoc)
    log(s"seqTime $seqTime ms")

    val parTime = search(specDoc)
    log(s"parTime $parTime ms")
  }

  Thread.sleep(3000);
}

object ParNonParallelizableCollections extends App with ParUtils with ThreadUtils {
  val l = List.fill(10000000)("")
  val v = Vector.fill(10000000)("")

  log(s"list conversion time: ${timed(l.par)} ms")
  log(s"vector conversion time: ${timed(v.par)} ms")
}

object ParNonParallelizableOperations extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    def allMatches(d: GenSeq[String]) = warmedTimed() {
      val result = d.foldLeft("") { (acc, line) =>
        if (line.matches("quiescent")) s"$acc\nline" else acc
      }
    }

    val seqTime = allMatches(specDoc)
    log(s"seqTime $seqTime ms")

    val parTime = allMatches(specDoc)
    log(s"parTime $parTime ms")
  }

  Thread.sleep(5000)
}


object ParNonParallelizableOperations2 extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    def allMatches(d: GenSeq[String]) = warmedTimed() {
      val result = d.aggregate("")(
        (acc, line) => if (line.matches("quiescent")) s"$acc\nline" else acc,
        (acc1, acc2) => acc1 + acc2
      )
    }

    val seqTime = allMatches(specDoc)
    log(s"seqTime $seqTime ms")

    val parTime = allMatches(specDoc)
    log(s"parTime $parTime ms")
  }

  Thread.sleep(5000)
}

object ParSideEffectsIncorrect extends App with ParUtils with ThreadUtils {
  def intersectionSize(a: GenSet[Int], b: GenSet[Int]): Int = {
    var total = 0 /* mutable variable */
    for (x <- a) if (b contains x) total += 1
    total
  }

  val a = (0 until 1000).toSet
  val b = (0 until 1000 by 4).toSet

  var seqTotal = intersectionSize(a, b)
  var parTotal = intersectionSize(a.par, b.par)

  log(s"seqTotal $seqTotal")
  log(s"parTotal $parTotal")
}

object ParSideEffectsCorrect extends App with ParUtils with ThreadUtils {
  def intersectionSize(a: GenSet[Int], b: GenSet[Int]): Int = {
    a.count(x => b contains x)
  }

  val a = (0 until 1000).toSet
  val b = (0 until 1000 by 4).toSet

  var seqTotal = intersectionSize(a, b)
  var parTotal = intersectionSize(a.par, b.par)

  log(s"seqTotal $seqTotal")
  log(s"parTotal $parTotal")
}

object ParNonDeterministicOperation extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    val pattern = ".*Akka.*"
    val seqResult = specDoc.find(_.matches(pattern))
    val parResult = specDoc.par.find(_.matches(pattern))
    log(s"seqResult $seqResult")
    log(s"parResult $parResult")
  }

  Thread.sleep(3000)
}

object ParDeterministicOperation extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    val pattern = ".*Akka.*"
    val seqIndex = specDoc.indexWhere(_.matches(pattern))
    val parIndex = specDoc.par.indexWhere(_.matches(pattern))
    val seqResult = if (seqIndex != -1) Some(specDoc(seqIndex)) else None
    val parResult = if (parIndex != -1) Some(specDoc(parIndex)) else None
    log(s"seqResult $seqResult")
    log(s"parResult $parResult")
  }

  Thread.sleep(3000)
}

object ParNonCommutativeOperator extends App with ParUtils with ThreadUtils {
  val doc = collection.mutable.ArrayBuffer.tabulate(20)(i => s"Page $i, ")

  def test(doc: GenIterable[String]): Unit = {
    val seqText = doc.seq.reduceLeft(_ + _)
    val parText = doc.par.reduce(_ + _)

    log(s"seqText $seqText\n")
    log(s"parText $parText\n")
  }

  test(doc)
  test(doc.toSet)
}

object ParNonAssociativeOperator extends App with ParUtils with ThreadUtils {
  def test(doc: GenIterable[Int]): Unit = {
    val seqText = doc.seq.reduceLeft(_ - _)
    val parText = doc.par.reduce(_ - _)

    log(s"seqText $seqText\n")
    log(s"parText $parText\n")
  }

  test(0 until 30)
}

object ConcurrentTrieMap extends App with ParUtils with ThreadUtils {
  val cache = new TrieMap[Int, String]()

  for (i <- 0 until 100) cache(i) = i.toString
  for ((index, string) <- cache.par) cache(-index) = s"-$string"

  log(s"cache - ${cache.keys.toList.sorted}")
}

class ParStringSplitter(val s: String,
                        var i: Int,
                        val limit: Int) extends SeqSplitter[Char] {

  final def next(): Char = {
    val r = s.charAt(i)
    i += 1
    r
  }

  final def hasNext: Boolean = i < limit

  def remaining: Int = limit - i

  def dup = new ParStringSplitter(s, i, limit)

  def split = {
    val rem = remaining
    if (rem >= 2) psplit(rem / 2, rem - rem / 2) else Seq(this)
  }

  def psplit(sizes: Int*) = {
    val ss = for (sz <- sizes) yield {
      val nlimit = (i + sz) min limit
      val ps = new ParStringSplitter(s, i, nlimit)
      i = nlimit
      ps
    }
    if (i == limit) ss
    else ss :+ new ParStringSplitter(s, i, limit)
  }
}

class ParString(val str: String) extends immutable.ParSeq[Char] {
  def apply(i: Int) = str.charAt(i)
  def length = str.length
  def splitter = new ParStringSplitter(str, 0, str.length)
  def seq = new collection.immutable.WrappedString(str)
}

object CustomCharCount extends App with ParUtils with ThreadUtils {
  val txt = "A custom text " * 250000
  val parText = new ParString(txt)

  val seqTime = warmedTimed(50) {
    txt.foldLeft(0) { (n, c) =>
      if (Character.isUpperCase(c)) n + 1 else n
    }
  }

  log(s"seqTime $seqTime ms")

  val parTime = warmedTimed(50) {
    parText.aggregate(0) (
      (n, c) => if (Character.isUpperCase(c)) n + 1 else n,
      _ + _
    )
  }

  log(s"parTime $parTime ms")
}

class ParStringCombiner extends Combiner[Char, ParString] {
  private val chunks = new ArrayBuffer += new StringBuilder
  private var lastc = chunks.last

  var size = 0
  def +=(elem: Char) = {
    lastc += elem
    size += 1
    this
  }

  override def combine[N <: Char, NewTo >: ParString](other: Combiner[N, NewTo]): Combiner[N, NewTo] = {
    if (this eq other) this else other match {
      case other: ParStringCombiner =>
        size += other.size
        chunks ++= other.chunks
        lastc = chunks.last
        this
    }
  }

  override def result(): ParString = {
    val rsb = new StringBuilder
    for (sb <- chunks) rsb.append(sb)

    new ParString(rsb.toString)
  }

  override def clear(): Unit = ???
}

object ParCombinerPerformance extends App with ParUtils with ThreadUtils {
  val text = "A custom text" * 25000
  val parText = new ParString(text)

  val seqTime = warmedTimed(250) { text.filter(_ != ' ')}
  log(s"seqTime $seqTime ms")

  val parTime = warmedTimed(250) { parText.filter(_ != ' ')}
  log(s"parTime $parTime ms")
}

object BlitzCmparison extends App with ParUtils with ThreadUtils {
  import scala.collection.par._
  import scala.collection.par.Scheduler.Implicits.global

  val array = (0 until 100000).toArray
  val seqtime = warmedTimed(1000) {
    array.reduce(_ + _)
  }
  val partime = warmedTimed(1000) {
    array.par.reduce(_ + _)
  }
  val blitztime = warmedTimed(1000) {
    array.toPar.reduce(_ + _)
  }
  log(s"sequential time - $seqtime")
  log(s"parallel time   - $partime")
  log(s"ScalaBlitz time - $blitztime")
}