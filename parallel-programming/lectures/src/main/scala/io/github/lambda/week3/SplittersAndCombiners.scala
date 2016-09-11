package io.github.lambda.week3

import scala.collection.parallel.Splitter

object SplittersAndCombiners {
  /**
    * the `Splitter` contract
    *
    * - after calling `split`, the original splitter is left in an undefined state
    * - the resulting splitters traverse disjoint subsets of the original splitter
    * - `remaining` is an estimate on the number of remaining elements
    * - `split` is an efficient method `O(log n)`
    *
    * we can easily implement `fold` on a splitter
    *
    * def fold(z: A)(f: (A, A) => A): A = {
    *   if (remaining < threadhold) foldLeft(z)(f)
    *   else {
    *     val children = for (child <- split) yield task { child.fold(z)(f) }
    *     children.map(_.join()).foldLeft(z)(f)
    *   }
    * }
    *
    */

  /**
    *
    * trait Builder[A, Repr] {
    *   def +=(elem: A): Builder[A, Repr]
    *   def result: Repr
    * }
    *
    * The `Builder` contract
    *
    * - calling `result` returns a collection of type `Repr`,
    *   containing the elemts that were previously added with +=
    * - calling `result`  leaves the `Builder` in an undefined state
    *
    * we can easily implement `filter` using `newBuilder`
    *
    * def filter(p: T => Boolean): Repr = {
    *   val b = newBuilder
    *   for (x <- this) if (p(x)) b += x
    *   b.result
    * }
    *
    */

  /**
    * trait Combiner[A, Repr] extends Builder[A, Repr] {
    *   def combine(that: Combiner[A, Repr]): Combiner[A, Repr]
    * }
    *
    * The `Combiner` contract
    *
    * - calling `combine` returns a new combiner that contains elements of input combiners
    * - calling `combine` leaves both original `Combiners` in an undefined state
    * - `combine` is an efficient method `O(log n)`
    *
    * when `Repr` is a set or a map, `combine` represents union
    *
    * - Set
    *   - hash table - expected `O(1)`
    *   - balanced trees `O(log n)`
    *   - linked lists `O(n)`
    *
    * when `Repr` is a sequence, `combine` represents concatenation
    * - Array cannot be concatenated efficiently
    *
    * - Sequence
    *   - mutable linked lists - O(1) prepend and append, O(n) insertion
    *   - functional (cons) lists - O(1) prepend, everything else O(n)
    *   - array lists - amortized O(1) append, O(1) random access, otherwise O(n)
    *
    *
    */

}
