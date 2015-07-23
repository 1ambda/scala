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

> A parallel collection cannot be a subtype of a sequential collection. If it were, then the **Liskov substitution** principle would be violated. 
The Liskov substitution principle states that if the type `S` is a subtype of `T`, then the object of type `T` can be replaced with objects of type `S` 
without affecting the correctness of the program.

If parallel collections are subtypes of sequential collections, then some method can return a sequential collection with the static type `Seq[Int]`, where 
the sequence object is a parallel sequence collection at runtime. Clients can cal method such as `foreach` on the collection without knowing that the 
body of the `foreach` method need synchronization, and their programs would not work correctly. For these reasons, 
parallel collections form a hierarchy that is separate from the sequential collections.

![](https://encrypted-tbn1.gstatic.com/images?q=tbn:ANd9GcShIAiLxr6w3dJ9AawBFqDn1t_vc1ywK4AjkL04DFU55KBUFH0R)
(Ref - lamp.epfl.ch)

The most general collection type is called `Traversable` which provides `find`, `map`, `filter` or `reduceLeft` that are implemented in terms of its abstract `foreach` method. 
Its `Iterable[T]` subtype offers additional operations such as `zip`, `grouped`, `sliding` and `sameElements` that are implemented using its `iterator` method. 
`Seq`, `Map` and `Set` are iterable collections thar represent Scala sequences, maps, and sets, respectively. 

Parallel collections form a separate hierarchy. The most general parallel collection type is called `ParIterable`. 
Methods such as `foreach`, `map` or `reduce` on a `ParIterable` object execute in parallel. 
 
The `ParSeq`, `ParMap`, `ParSet` collection are parallel collections that correspond to `Set`, `Map`, `Seq`, but not their subtypes. 

```scala
def nonNull(xs: Seq[T]): Seq[T] = xs.filter(_ != null)
def nonNull(xs: ParSeq[T]): ParSeq[T] = xs.filter(_ != null)
def nonNull(xs: GenSeq[T]): GenParSeq[T] = xs.filter(_ != null)
```

## Configuring the parallelism level







