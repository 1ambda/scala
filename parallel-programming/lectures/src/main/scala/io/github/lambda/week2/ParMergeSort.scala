package io.github.lambda.week2

object ParMergeSort {

  import io.github.lambda.util.Parallelism._

  def partition(arr: Array[Int], start: Int, end: Int): Int = {
    val pivot = arr(end)
    var pIndex = start
    var i = start

    while (i < end) {
      if (arr(i) <= pivot) {
        swap(arr, i, pIndex)
        pIndex += 1
      }

      i += 1
    }
    swap(arr, pIndex, end)
    pIndex
  }

  def swap(arr: Array[Int], x: Int, y: Int) {
    val temp = arr(x)
    arr(x) = arr(y)
    arr(y) = temp
  }

  def quickSort(arr: Array[Int], start: Int, until: Int) {
    val end = until
    if (start < end) {
      val pIndex = partition(arr, start, end)
      quickSort(arr, start, pIndex - 1)
      quickSort(arr, pIndex + 1, end)
    }
  }

  def merge(src: Array[Int], dst: Array[Int], from: Int, mid: Int, until: Int): Unit = {
    var left = from
    var right = mid
    var i = from

    while (left < mid && right < until) {
      while (left < mid && src(left) <= src(right)) {
        dst(i) = src(left)
        i += 1
        left += 1
      }
      while (right < until && src(right) <= src(left)) {
        dst(i) = src(right)
        i += 1
        right += 1
      }
    }

    while (left < mid) {
      dst(i) = src(left)
      i += 1
      left += 1
    }

    while (right < mid) {
      dst(i) = src(right)
      i += 1
      right += 1
    }
  }


  def parMergeSort(xs: Array[Int], maxDepth: Int): Unit = {
    val ys = new Array[Int](xs.length)

    def sort(from: Int, until: Int, depth: Int): Unit = {
      if (depth == maxDepth) quickSort(xs, from, until - from - 1)
      else {
        val mid = from + ((until - from) / 2)
        parallel(
          sort(mid, until, depth + 1),
          sort(from, mid, depth + 1)
        )

        val flip = (maxDepth - depth) % 2 == 0
        val src = if (flip) ys else xs
        val dst = if (flip) xs else ys

        merge(src, dst, from, mid, until)
      }
    }

    sort(0, xs.length, 0)

    def copy(src: Array[Int], target: Array[Int],
             from: Int, until: Int, depth: Int): Unit = {

      if (depth == maxDepth) Array.copy(src, from, target, from, until - from)
      else {
        val mid = from + ((until - from) / 2)

        parallel(
          copy(src, target, mid, until, depth + 1),
          copy(src, target, from, mid, depth + 1)
        )
      }

    }

    if (maxDepth % 2 == 0) copy(ys, xs, 0, xs.length, 0)
  }

}
