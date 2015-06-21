package chapter4

object Statistics {
  def variance(xs: Seq[Double]): scala.Option[Double] = {
    mean(xs).flatMap((m: Double) => mean(xs.map(x => math.pow(x - m, 2))))
  }

  def mean(xs: Seq[Double]): scala.Option[Double] =
    if (xs.isEmpty) scala.None else scala.Option(xs.sum / xs.length)
}


