package io.github.lambda.week2

import org.scalatest.{FunSuite, Matchers}

class ParMergeSortSpec extends FunSuite with Matchers {

  import ParMergeSort._

  test("") {
    val arr1 = Array[Int](4, 3, 2, -1, 5, 6, 0)
    parMergeSort(arr1, 1)

    Thread.sleep(3000)
    println(arr1.toList)
  }

}
