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

Parallel collections use all the processors by default. Their underlying executor has as many workes as there are processors. 
We can change this default behavior by changing the `TaskSupport` object of the parallel collection. 
To change the parallelism level of a parallel collection, we instantiated a `ForkJoinPool` collection with desired parallelism level.

```scala
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
```

## Measuring the Performance on the JVM

When bytecode from the Scala compiler gets run inside the JVM, at first it executes in so-called interpreted mode. The JVM 
interpreters each bytecode instruction and simulates the execution of the program. 

Only when the JVM decides that the bytecode in a certain method was run often enough does it compile the bytecode to machine code, 
which can be executed directly on the processor. This process is called just-in-time compilation.

However, the entire bytecode of a program cannot be instantiated to the machine code as soon as the program runs. This would be too slow. 
Instead the JVM translates parts of the program, such as specific method, incrementally, in short compiler runs.

In addition, the JVM can decide to additionally optimize certain parts of the program that execute very frequently. 
As a result, programs running on the JVM are usually slow immediately after they start, and eventually reach 
their optimal performance. Once this happens, we say that the JVM reached its **steady state**.

When evaluating the performance on the JVM, we are usually interested in the **steady state**.

<br/>

There are other reasons why measuring performance on the JVM is hard. 

- Even if the JVM reached a steady state for the part of the program we measure, JIT compiler can at any point pause the execution and translate some other part of the program, effectively slowing down our measurement. 
- Periodically, the JVM stops the execution, scans the heap for all objects no longer used in the program, and free the memory they occupy.

> To get really reliable running time values, we need to run the code many times by starting separate JVM processes, making sure that the 
JVM reached a steady state, and taking the average of all the measurements.

## Non-Parallelizable Collections

Parallel collections use **splitters**, represented with the `Splitter[T]` type, in order to provide parallel operations. 
A splitter is a more advanced form of an iterator, defining the `split` method that divides the splitter `s` into a sequence of splitters that traverse part of `s`

```scala
def split: Seq[Splitter[T]]
```

This method allows separate processors to traverse separate parts of the input collection. 

Splitters can be implemented for flat data structures such as arrays and hash tables, and tree-like data structures such as immutable hash maps and vectors. Linear data structure sch as the Scala `List` and `Stream` collection cannot efficiently implement the `split` method. 

Operations on Scala collections such as `Array`, `ArrayBuffer`, mutable `HashMap` and `HashSet`, `Range`, `Vector`, immutable `HashMap` and `HashSet`, and concurrent `TrieMap` can be parallelized. These collections are **parallelizable**. 
Calling `.par` on these collections creates a parallel collection that shares the same underlying dataset as the original collection. No element are copied and the conversion is fast. 

- `Array`
- `ArrayBuffer`
- mutable `HashMap`, `HashSet`, `Range`, `Vector`
- immutable `HashMap`, `HashSet`, `TrieMap`

Other scala collection need to be converted to their parallel counterparts upon calling `par`. These are **non-parallelizable** collections. 
For example, the `List` collection need to be copied to a `Vector `collection when `par` is called.

```scala
object ParNonParallelizableCollections extends App with ParUtils with ThreadUtils {
  val l = List.fill(10000000)("")
  val v = Vector.fill(10000000)("")
  
  log(s"list conversion time: ${timed(l.par)} ms")
  log(s"vector conversion time: ${timed(v.par)} ms")
}

// output
> runMain parallel.ParNonParallelizableCollections
[info] Running parallel.ParNonParallelizableCollections 
[info] main: list conversion time: 180.609 ms
[info] main: vector conversion time: 0.015 ms
```

> Converting a non-parallelizable sequential collection to a parallel collection is **not a parallel operation**. 
It executes on he caller thread.

## Non-Parallelizable Operations

```scala
def foldLeft[S](z: S)(f: (S, T) => S): S
```

The crucial property of the `foldLeft` operations is that it traverse the elements of the list by going from left to right. 
As a consequence, computing the accumulator cannot be implemented in parallel. The `foldLeft` method can not merge two accumulators from 
two different processors. 

```scala
object ParNonParallelizableOperations extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    def allMatches(d: GenSeq[String]) = warnedTimed() {
      val result = d.foldLeft("") { (acc, line) =>
        if (line.matches("quiescent")) s"$acc\nline" else acc
      }
    }

    val seqTime = allMatchs(specDoc)
    log(s"seqTime $seqTime ms")

    val parTime = allMatchs(specDoc)
    log(s"parTime $parTime ms")
  }

  Thread.sleep(5000)
}

// output
// !?!?


> runMain parallel.ParNonParallelizableOperations
[info] Running parallel.ParNonParallelizableOperations 
[info] ForkJoinPool-1-worker-13: seqTime 0.122 ms
[info] ForkJoinPool-1-worker-13: parTime 0.042 ms

object ParNonParallelizableOperations2 extends App with ParUtils with ThreadUtils {
  import scala.concurrent.ExecutionContext.Implicits.global

  ParHtmlSearch.getHtmlSpec() foreach { case specDoc =>
    def allMatches(d: GenSeq[String]) = warnedTimed() {
      val result = d.aggregate("")(
        (acc, line) => if (line.matches("quiescent")) s"$acc\nline" else acc,
        (acc1, acc2) => acc1 + acc2
      )
    }

    val seqTime = allMatchs(specDoc)
    log(s"seqTime $seqTime ms")

    val parTime = allMatchs(specDoc)
    log(s"parTime $parTime ms")
  }

  Thread.sleep(5000)
}

// output
> runMain parallel.ParNonParallelizableOperations2
[info] Running parallel.ParNonParallelizableOperations2 
[info] ForkJoinPool-1-worker-13: seqTime 0.119 ms
[info] ForkJoinPool-1-worker-13: parTime 0.043 ms
```

> Use the `aggregate` method to execute parallel reduction operations

Other inherently sequential operations include 

- `foldLeft`
- `reduceLeft`, `reduceRight`
- `reduceLeftOption`, `reduceRightOption`
- `scanLeft`, `scanRight`
- and method that produce non-parallelizable collections such as `toList`

## Side-Effect in Parallel Operations

Assigning to a mutable variable from a parallel collection operation may be temping, but it is almost certainly incorrect.

```scala
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

> runMain parallel.ParSideEffectsIncorrect
[info] Running parallel.ParSideEffectsIncorrect 
[info] main: seqTotal 250
[info] main: parTotal 245
```

To ensure that parallel version returns the correct results, we can use atomic variable and its `incrementAndGet`. 
However, this leads to the same scalability problems we had before. A better alternative is to used the parallel `count` method. 

```scala
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

// output

> runMain parallel.ParSideEffectsCorrect
[info] Running parallel.ParSideEffectsCorrect 
[info] main: seqTotal 250
[info] main: parTotal 250
```

If the amount of work executed per element is low and the matches are frequent, the parallel `count` method will result in better performance than the `foreach` method with an atomic variable.

> To avoid the need for synchronization and ensure better scalability, favor declarative-style parallel operations instead of the side effects in parallel `for` loop.

## Nondeterministic Parallel Operations

```scala
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

// output
> runMain parallel.ParNonDeterministicOperation
[info] Running parallel.ParNonDeterministicOperation 
[info] ForkJoinPool-1-worker-13: seqResult Some(    <title>Akka Documentation | Akka</title>)
[info] ForkJoinPool-1-worker-13: parResult Some(    <p>Akka Documentation</p>)
```

If we want to retrieve the first occurrence, we need to use `indexWhere` instead

```scala
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
```

> Parallel collection operations other than `find` are deterministic as long as their operators are **pure functions**.

Even if a function does not modify any memory locations, it is not pure if it reads memory location that might change. For example,

```scala
val g (x: Int) => (x, uid.get)
```

When used with a non-pure function, any parallel operation can become **non-deterministic**.

<br/>

## Commutative and Associative Operators

A binary operator `op` is **commutative** if changing the order of its arguments return the same result.

```scala
op(a, b) == op(b a)
```

Binary operators for the parallel `reduce`, `fold`, `aggregate`, and `scan` operations never need to be commutative. 
Parallel collection operations always respect the relative order of the elements when applying binary operators, provided that 
underlying collections have any ordering.

```scala
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

// output
[info] Running parallel.ParNonCommutativeOperator 
[info] main: seqText Page 0, Page 1, Page 2, Page 3, Page 4, Page 5, Page 6, Page 7, Page 8, Page 9, Page 10, Page 11, Page 12, Page 13, Page 14, Page 15, Page 16, Page 17, Page 18, Page 19, 
[info] 
[info] main: parText Page 0, Page 1, Page 2, Page 3, Page 4, Page 5, Page 6, Page 7, Page 8, Page 9, Page 10, Page 11, Page 12, Page 13, Page 14, Page 15, Page 16, Page 17, Page 18, Page 19, 
[info] 
[info] main: seqText Page 9, Page 15, Page 5, Page 2, Page 1, Page 10, Page 8, Page 17, Page 19, Page 6, Page 14, Page 0, Page 7, Page 18, Page 3, Page 4, Page 11, Page 13, Page 16, Page 12, 
[info] 
[info] main: parText Page 12, Page 16, Page 13, Page 11, Page 4, Page 3, Page 18, Page 7, Page 0, Page 14, Page 6, Page 19, Page 17, Page 8, Page 10, Page 1, Page 2, Page 5, Page 15, Page 9, 
```


<br/>

A binary operator `op` is **associative** if applying `op` consecutively to a sequence of values `a`, `b`, and `c` gives the same result regardless of 
the order in which the operator is applied.

```scala
op(a, (op(b, c)) == op(op(a, b), c)
```

Parallel collection operations usually require associative binary operators. While using subtraction with `reduceLeft` means that 
all the numbers in the collection should be subtracted from the first number, using subtraction in `reduce`, `fold`, or `scan` gives 
nondeterministic and incorrect results.

```scala
object ParNonAssociativeOperator extends App with ParUtils with ThreadUtils {
  def test(doc: GenIterable[Int]): Unit = {
    val seqText = doc.seq.reduceLeft(_ - _)
    val parText = doc.par.reduce(_ - _)

    log(s"seqText $seqText\n")
    log(s"parText $parText\n")
  }

  test(0 until 30)
}

// output

[info] Running parallel.ParNonAssociativeOperator 
[info] main: seqText -435
[info] 
[info] main: parText -57
[info] 

> runMain parallel.ParNonAssociativeOperator
[info] Running parallel.ParNonAssociativeOperator 
[info] main: seqText -435
[info] 
[info] main: parText -15
[info] 

```

> Binary operators used in parallel operations do not need to be commutative

> Make sure that binary operators used in parallel operations are associative
 
<br/>

Parallel operations such as `aggregate` require multiple binary operators, `sop` and `cop`

```scala
def aggregate[S](z: S)(sop: (S, T) => S, cop: (S, S) => S): S
```

The `sop` operator is used to fold elements within a subset as signed to a specific processor. 
The `cop` operator is used to merge the subsets together, and is of the same type as the operators for `reduce` and `fold`.

The `aggregate` operation requires that 

- `cop` is associative
- `z` is the **zero element** for the accumulator (`cop(z, a) == a`) 
- `sop` and `cop` must give the same result irrespective of the order in which element subsets are assigned to processors

```scala
cop(sop(z, a), sop(z, b)) == cop(z, sop(sop(z, a), b))
```

## Using Parallel and Concurrent Collection Together






