package chapter8

case class Gen[A](sample: State[RNG, A]) {
  def unit[A](a: => A): Gen[A] = // State.unit(a)
    Gen(State(rng => (a, rng)))

  def boolean: Gen[Boolean] = // State(RNG.boolean)
    Gen(State(rng => rng.nextInt match {
      case (n, rng2) => (n % 2 == 0, rng2)
    }))

  def choose2(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(State(rng => RNG.nonNegativeInt(rng) match {
      case (n, rng2) => (start + n % (stopExclusive - start), rng2)
    }))

  def choose(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(State(RNG.nonNegativeInt).map(n => start + n % (stopExclusive - start)))

  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(g.sample)))

  def flatMap[B](f: A => Gen[B]): Gen[B] =
    Gen(sample.flatMap(a => f(a).sample))

  def listOfN(size: Gen[Int]): Gen[List[A]] =
    size.flatMap(n => listOfN(n, this))

  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] =
  boolean.flatMap(b => if(b) g1 else g2)

  def weighted[A](g1: (Gen[A], Double), g2: (Gen[A], Double)): Gen[A] = {
    val g1SelectedProb: Double = g1._2.abs / g1._2.abs + g2._2.abs

    Gen(State(RNG.double).flatMap(d =>
      if (d < g1SelectedProb) g1._1.sample else g2._1.sample
    ))
  }
}

/*
  val intList = Gen.listOf(Gen.choose(1, 100))
  val prop = forAll(intList)(ns => ns.reverse.reverse == ns) &&
 */
