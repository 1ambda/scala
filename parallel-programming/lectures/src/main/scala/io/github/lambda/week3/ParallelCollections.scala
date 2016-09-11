package io.github.lambda.week3

import scala.collection.GenSet
import scala.collection.concurrent.TrieMap

object ParallelCollections extends App {

  def intersection(a: GenSet[Int], b: GenSet[Int]): GenSet[Int] = {
    if (a.size < b.size) a.filter(b(_))
    else b.filter(a(_))
  }

//  println(intersection((0 until 10000).toSet, (0 until 10000 by 4).toSet).size)
//  println(intersection((0 until 10000).par.toSet, (0 until 10000 by 4).par.toSet).size)

  /**
    * Never write to a collection that is concurrently traversed
    * Never read from a collection that is concurrently modified
    */

  def findConcurrentModifiedElems(): Unit = {
    // val graph = collection.mutable.Map[Int, Int]() ++= (0 until 100000).map(i => (i, i + 1))
    val graph = TrieMap[Int, Int]() ++= (0 until 100000).map(i => (i, i + 1))
    graph(graph.size - 1) = 0
    val previous = graph.snapshot()

    for ((k, v) <- graph.par) graph(k) = previous(v)
    val violation = graph.find({
      case (i, v) => v != (i + 2) % graph.size
    })

    println(violation)
  }

  findConcurrentModifiedElems()
}
