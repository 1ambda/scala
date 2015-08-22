package chapter8

case class Gen[A](sample: State[RNG, A]) {
  def map[B](f: A => B): Gen[B] =
    Gen(sample.map(a => f(a)))

  def flatMap[B](f: A => Gen[B]): Gen[B] =
    Gen(sample.flatMap(a => f(a).sample))

  def unsized: SGen[A] =
    SGen(_ => this) // SGen(n => Gen(sample))

  def listOf(size: Int): Gen[List[A]] =
    Gen.listOfN(size, this)

  def listOfN(size: Gen[Int]): Gen[List[A]] =
    size.flatMap(n => this.listOf(n))

  def listOf1: SGen[List[A]] = Gen.listOf1(this)
}

object Gen {
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

  def weighted[A](g1: (Gen[A], Double), g2: (Gen[A], Double)): Gen[A] = {
    val g1SelectedProb: Double = g1._2.abs / g1._2.abs + g2._2.abs

    Gen(State(RNG.double).flatMap(d =>
      if (d < g1SelectedProb) g1._1.sample else g2._1.sample
    ))
  }

  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] =
    boolean.flatMap(b => if(b) g1 else g2)

  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(g.sample)))

  def listOf[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOf(n))

  def listOf1[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOf(n max 1))
}

/*
  val intList = Gen.listOf(Gen.choose(1, 100))
  val prop = forAll(intList)(ns => ns.reverse.reverse == ns) &&
 */
