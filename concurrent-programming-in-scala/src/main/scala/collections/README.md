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







