package chapter6

trait RNG {
  def nextInt: (Int, RNG)

  def unit[A](a: A): Rand[A] = rng => (a, rng)
  def _map[A, B](s: Rand[A])(f: A => B): Rand[B] = rng => {
    val (a, rng2) = s(rng)
      (f(a), rng2)
  }

  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (n, nextRNG) = rng.nextInt
    val pos = math.abs(n)

    (pos, nextRNG)
  }

  type Rand[+A] = RNG => (A, RNG)
  val int: Rand[Int] = _.nextInt

  // 0 <= d < 1
  def double2(rng: RNG): (Double, RNG) = {
    val (n, nextRNG) = nonNegativeInt(rng)

    if (n == Int.MaxValue) (((n - 1).toDouble / Int.MaxValue), nextRNG)
    else (n.toDouble / Int.MaxValue, nextRNG)
  }

  def double: Rand[Double] = map(nonNegativeInt)(n => {
    if (n == Int.MaxValue) (n - 1).toDouble / Int.MaxValue
    else n.toDouble / Int.MaxValue
  })

  def nonNegativeEven: Rand[Int] = map(nonNegativeEven)(x => x - x % 2)

  def _map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] = { rng =>
    val (a, nextRngA) = ra(rng)
    val (b, nextRngB) = rb(nextRngA)

    (f(a, b), rng)
  }

  def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2(ra, rb)((_, _))

  def randIntDouble = both(int, double)
  def randDoubleInt = both(double, int)

  def ints1(count: Int)(rng: RNG): (List[Int], RNG) =
    if (count == 0) (Nil, rng)
    else {
      val (n, r1) = rng.nextInt
      val (ns, r2) = ints1(count - 1)(rng)

      (n :: ns, r2)
    }

  // tail-recursive
  def ints2(count: Int)(rng: RNG): (List[Int], RNG) = {

    def recur(count: Int, rng: RNG, xs: List[Int]): (List[Int], RNG) = {
      if (count == 0) (xs, rng)
      else {
        val (n, r) = rng.nextInt
        recur(count - 1, r, n :: xs)
      }
    }

    recur(count, rng, Nil)
  }

  // ref: https://github.com/fpinscala/fpinscala/blob/master/answers/src/main/scala/fpinscala/state/State.scala

  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs.foldRight(unit(List[A]()))((f, acc) => map2(f, acc)(_ :: _))

  def ints(n: Int)(rng: RNG): Rand[List[Int]] =
    sequence(List.fill(n)(int))

  def nonNegativeLessThan1(n: Int): Rand[Int] =
    map(nonNegativeInt) { _ % n}


  def flatMap[A, B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, rng1) = f(rng)
      g(a)(rng1)
    }

  def nonNegativeLessThan(n: Int): Rand[Int] = flatMap(nonNegativeInt) { i =>
    val mod = i % n
    if (i + (n - 1) - mod >= 0) unit(mod) else nonNegativeLessThan(n)
  }

  def map[A, B](ra: Rand[A])(f: A => B): Rand[B] =
    flatMap(ra)(a => unit(f(a)))

  def map2[A, B, C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => map(rb)(b => f(a, b)))
}

case class SimpleRNG(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE660DL + 0xBL) & 0xFFFFFFFFFFFFL 
    val nextRNG = SimpleRNG(newSeed)

    val n = (newSeed >>> 16).toInt
    (n, nextRNG)
  }
}

