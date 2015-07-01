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
 
 




