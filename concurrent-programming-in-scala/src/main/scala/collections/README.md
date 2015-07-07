# Concurrent Collections

Predicting how multiple threads affecting collection state in the absence of synchronization is 
neither recommended nor possible.

```scala
object CollectionsBad extends App with ExecutorUtils with ThreadUtils {
  val buffer = mutable.ArrayBuffer[Int]()

  def asyncAdd(numbers: Seq[Int]) = execute {
    buffer ++= numbers
    log(s"buffer = $buffer")
  }

  asyncAdd(0 until 10)
  asyncAdd(10 until 20)
  Thread.sleep(500)
}

// output
> runMain collections.CollectionsBad
[info] Running collections.CollectionsBad 
[info] ForkJoinPool-1-worker-11: buffer = ArrayBuffer(10, 11, 12, 13, 14, 15, 16, 7, 8, 9)
[info] ForkJoinPool-1-worker-13: buffer = ArrayBuffer(10, 11, 12, 13, 14, 15, 16, 7, 8, 9)
[success] Total time: 1 s, completed Jul 2, 2015 12:17:37 AM
> runMain collections.CollectionsBad
[info] Running collections.CollectionsBad 
[info] ForkJoinPool-1-worker-11: buffer = ArrayBuffer(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
[info] ForkJoinPool-1-worker-13: buffer = ArrayBuffer(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
```

> Never use mutable collections from several different threads without applying proper synchronization

We can restore synchronization in several ways

- immutable collections
- synchronized

```scala
// immutable collections

class AtomicBuffer[T] {
  private val buffer = new AtomicReference[List[T]](Nil) 
  
  @tailrec
  def +=[T](x: T): Unit = {
    val xs = bugger.get
    val nxs = x :: xs
    
    if (!buffer.compareAndSet(xs nxs)) this += x
  }
}
```

While using atomic variables or synchronized statements with immutable collections is simple, 
it can lead to scalability problems when many threads access an atomic variable at once.

Conceptually, the same operations can be achieved using atomic primitives, synchronized statements, and guarded blocks, 
but concurrent collections ensure far better performance and scalability.
 
<br/>
 
## Concurrent Queues

Concurrent queues can be **bounded**, or the can be **unbounded**

- bounded queues can only contain maximum number of elements
- unbounded queues can grow indefinitely

JDK represents multiple efficient concurrent queue implementations in the `java.util.concurrent` package with 
the `BlockingQueue` 

![](https://lh3.googleusercontent.com/-MdH82AtUrfs/VSq6eJIWwYI/AAAAAAAABiY/FvCA2Bx0HpA/w800-h800/java.util.BlockingQueue%2Bin%2Bjava%2B-%2BCrunchify.png)

(Ref - https://plus.google.com/+CrunchifyDotCom/posts/bvbbEdv9XoV)

- Methods such as `poll` and `offer` return special values such as `null` and `false`
- Timed versions of these method block the caller for a specified duration before returning an element or a special value

If producers can potentially create elements faster than the consumers can process them, 
we need to used bounded queue. Otherwise, the queue size can potentially grow to the point where 
it consumes all the available memory in the program.

`LinkedBlockingQueue` is the unbounded queue. We can use it when we are sure that the consumer work much faster than 
the producers. This queue is an ideal candidate for the logging component of our filesystem's API. 

<br/>

An important difference between sequential queues and concurrent queues is that concurrent queues have 
**weakly consistent iterators**. An iterator created with the `iterator` method traverses the elements that 
**were in the queue at the moment the iterator was created**.

```scala
val queue = new LinkedBlockingQueue[String]

for (i <- 1 to 5500) queue.offer(i.toString)

execute {
  val it = queue.iterator
  while (it.hasNext) log(it.next())
}

for (i <- 1 to 5500) queue.poll()
Thread.sleep(1000)
```

It is never corrupt and does not throw exceptions, but it fails to return a consistent set of elements.

> Use iterators on concurrent data structures only when you can ensure that no other thread will 
modify the data structure from the point where the iterator was created until the point where 
the iterator's hasNext method returns false

The `CopyOnWriteArrayList` and `CopyOnWriteArraySet` in JDK are exceptions to this rule, 
but the copy the underlying data when ever the collection mutated and can be slow.

<br/>

## Concurrent Sets and Maps

As the main use case of concurrent queues is the producer-consumer pattern, the `BlockingQueue` interface 
additionally provides blocking versions of methods that are already known from sequential queues.

Concurrent maps and concurrent sets are map and set collections, respectively, that can be safely accessed and modified by 
multiple threads.
 
Unlike concurrent queues, the do not have blocking operations. The reason is that their principal use case is 
**not the producer-consumer pattern** but **encoding the program state**

<br/>

The `concurrent.Map` trait also defines several complex linearizable methods which involve multiple 
reads and writes. In the context of concurrent map, methods are complex linearizable operations 
if they involve multiple instances of `get` and `put` but appear to get executed at a single point in time.

Unlike the CAS instruction, the `concurrent.Map`'s complex linearizable methods use **structural equality** to 
compare keys and values, and they call the `equal` method.

- `putIfAbsent(k: K, v: V): Option[V]`
- `remove(k: K, v: V): Boolean`
- `replace(k: K, ov: V, nv: V): Boolean`
- `replace(k: K, v: V): Option[V]`

```scala
class FileSystem(root: String) extends ThreadUtils {
  private val messages = new LinkedBlockingQueue[String]
  val rootDir = new File(root)
  val files: concurrent.Map[String, Entry] =
    new ConcurrentHashMap[String, Entry]().asScala

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
```

```scala
object FileSystemWatcher extends App with ExecutorUtils {

  val fs = new FileSystem("/Users/1002471/Desktop")

  execute {
    fs.deleteFile("sy-2.png")
    fs.logMessage("Testing Log!")
  }

  execute {
    fs.deleteFile("sy-2.png")
  }

  Thread.sleep(2000)
}

// output
[info] Thread-0: File currently deleted, cannot delete.
[info] Thread-0: Testing Log!
```

Using `concurrent.Map`'s complex linearizable methods, we can implement `acquire`, `release` `copyFile` 
 
```scala
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
```

The `copyFile` method would be incorrect if it fist called `get` to check whether `dest` is in 
the map and then called `put` to place` dest` in the map. This would allow another thread's `get` and `put` steps 
to interleave and potentially overwrite an entry in the files `map`. This demonstrates the importance of the `putIfAbsent` method.

There are some methods the `concurrent.map` trait inherits from the `mutable.Map` trait that are not atomic. 
An example is `getOrElseUpdate`, which retrieves an element if it is present in the map and updates it with a different element otherwise.

This method is **not atomic**, while its individual steps are atomic. They can be interleaved arbitrarily with concurrent calls to `getOrElseUpdate`.

Another example is `clear`, which does not have to be atomic on concurrent collections in general and can behave like he concurrent data 
structure iterators wi studies before.

TODO: File remove check




















