package st

object st {

  /** https://apocalisp.wordpress.com/2011/03/20/towards-an-effect-system-in-scala-part-1/ */

  case class World[A]()

  case class ST[S, A](f: World[S] => (World[S], A)) {
    def run(s: World[S]): (World[S], A) = f(s)

    def flatMap[B](g: A => ST[S, B]): ST[S, B] = ST(s => {
      val (s1, a) = run(s)
      g(a).run(s1)
    })

    def map[B](g: A => B): ST[S, B] = ST(s => {
      val (s1, a) = run(s)
      (s1, g(a))
    })
  }

  def returnST[S, A](a: => A): ST[S, A] = ST(s => (s, a))
}
