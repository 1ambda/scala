# Parallel Collections

## Basics 

```scala
trait ParUtils {
  @volatile var dummy: Any = _

  def timed[T](body: => T): Double = {
    val start = System.nanoTime
    dummy = body
    val end   = System.nanoTime
    ((end - start) / 1000) / 1000.0
  }
}
```

Certain runtime optimizations in the JVM, such as the deadcode eliminations, can potentially remove 
the invocation of the `body` block, causing us to measure an incorrect running time. To prevent this, 
we assign the return value of the `body` block to a volatile field named `dummy` 

```scala
object ParBasic extends App with ParUtils with ThreadUtils {
  val numbers = Random.shuffle(Vector.tabulate(50000000)(i => i))

  val seqTime = timed { numbers.max }
  log(s"seqTime $seqTime ms")
  val parTime = timed { numbers.par.max }
  log(s"parTime $parTime ms")
}

// output
[info] Running parallel.ParBasic 
[info] main: seqTime 1547.562 ms
[info] main: parTime 451.546 ms
```

> Always validate assumptions about the performance by measuring the execution time

The `max` method is particularly well-suited for parallelization. We say that the `max` method is 
trivially parallelizable.

In general, data-parallel operations require more inter-processor communication then the `max` method. 

```scala
object ParUid extends App with ParUtils with ThreadUtils {
  private val uid = new AtomicLong(0L)

  val seqTime = timed { for (i <- (0 until 10000000)) uid.incrementAndGet() }
  log(s"seqTime = $seqTime ms")

  val parTime = timed { for (i <- (0 until 10000000).par) uid.incrementAndGet() }
  log(s"parTime = $parTime ms")
}

// output
[info] Running parallel.ParUid 
[info] main: seqTime = 153.328 ms
[info] main: parTime = 355.869 ms
```

Recall that every occurrence of a `for` loop is desugared into the `foreach` call by the compiler. 

```scala
(0 until 100000000).par.foreach(i => uid.incrementAndGet())
```

This means that separate worker threads simultaneously invoke the specified function, 
so proper synchronization must be applied.

As you can see, the parallel version of the program is even slower because of the atomic variable `uid`. 
We can't write to the same memory location at once.

> Memory writes do not go directly to RAM in modern architectures, as this would be too slow. 
instead, modern computer architectures separate the CPU from the RAM with multiple levels of caches. 
The cache level closest to the CPU is called the L1 cache. The L1 cache is divided into short contiguous 
parts called **cache lines**. 

> Typically, a cache line size is 64 byres. Although multiple cores can read the same cache line simultaneously, 
in standard multicore processors, the cache line need to be in exclusive ownership when a core writes to it.

> When another core requests to write the the same cache line, the cache line need to be copied the that core's L1 cache. 
The cache coherence protocol that enables that is called **Modified Exclusive Shared Invalid (MESI)**. Exchanging the cache-line 
ownership can be relatively expensive on the processors' time scale.

Since the `uid` variable is atomic, the JVM need to ensure a happens-before relationship between 
the writes and reads of `uid`. To ensure the happens-before relationship, memory writes have to be visible to other processors. 

The only way to ensure this is to **obtain the cache line** in exclusive mode before writing to it resulting program becomes much slower than 
its sequential version.

> Writing to the same memory location **with proper synchronization leads to performance bottlenecks and contention**. avoid this in data-parallel operations.   

Parallel programs share other resource in addition to computing power. When different parallel computations request 
more resources han are currently available, an effect known as **resource contention** occurs.

## Parallel Collection Class Hierarchy 
