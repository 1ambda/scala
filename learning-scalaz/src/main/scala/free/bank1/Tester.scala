package free.bank1

import Interact._

import scalaz._, Scalaz._

object Tester {
  type Tester[A] = Map[String, String] => (List[String], A)

  object Test extends (InteractOp ~> Tester) {
    override def apply[A](i: InteractOp[A]): Tester[A] = i match {
      case Ask(prompt) =>
        m => (List(), m(prompt))

      case Tell(msg)   =>
        _ => (List(msg), ())
    }
  }

  implicit val testerMonad = new Monad[Tester] {
    override def bind[A, B](fa: Tester[A])(f: (A) => Tester[B]): Tester[B] = m => {
      val (o1, a) = fa(m)
      val (o2, b) = f(a)(m)
      (o1 ++ o2, b)
    }

    override def point[A](a: => A): Tester[A] = _ => (List(), a)
  }

}
