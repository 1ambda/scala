package chapter8

trait Gen[A] {
  def choose[A](arr: A*): Gen[A]
  def listOf[A](a: Gen[A]): Gen[List[A]]
  def listOfN[A](n: A, a: Gen[A]): Gen[List[A]]

  def forAll[A](a: Gen[A])(f: A => Boolean): Prop
}

/*
  val intList = Gen.listOf(Gen.choose(1, 100))
  val prop = forAll(intList)(ns => ns.reverse.reverse == ns) &&
 */
