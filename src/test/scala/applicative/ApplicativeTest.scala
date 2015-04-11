package applicative

import org.scalatest._
import scalaz._
import Scalaz._

class ApplicativeTest extends FlatSpec with Matchers {

  "curried" should "handle binary functions" in  {
    val functions = List(1, 2, 3, 4) map {(_: Int) * (_: Int)}.curried
    val result = functions map {_(9)}

    result should be (List(9, 18, 27, 36))
  }

  "point" can "be used instead of pure" in  {
    1.point[List] should be (List(1))
    1.pure[Option].map(_ * 3) should be (Some(3))
  }

  "applicative" can "be used to handle a functor including a function in it" in {
    val result1 =
      3.some <*> { 9.some <*> {(_: Int) + (_: Int)}.curried.some }

    result1 should be (Some(12))

    val result2 = (3.some |@| 5.some) {_ + _}

    result2 should be (Some(8))
  }

  "Apply.liftA2" can "be used to lift a function" in {
    val lifted = Apply[Option].lift2((_: Int) :: (_: List[Int]))
    val result = lifted(1.some, 2.pure[List].some)

    result should be (Some(List(1, 2)))
  }

  "sequenceA" can "be implemented using functor and applicative" in {
    def sequenceA[F[_]: Applicative, A](list: List[F[A]]): F[List[A]] = list match {
         case Nil     => (Nil: List[A]).point[F]
         case x :: xs => (x |@| sequenceA(xs)) {_ :: _} 
    }

    val result1 = sequenceA(List(1.some, 2.some, 3.some))
    result1 should be (Some(List(1, 2, 3)))

    val result2 = sequenceA(List(1.some, none, 3.some))
    result2 should be (None)

    // consider function as an applicative e.g
    // ([r - > a]) -> (r -> [a])
    type Function1Int[A] = ({type l[A]=Function1[Int, A]})#l[A]
    val sequenced =
      sequenceA(List((_: Int) + 3, (_: Int) + 2, (_: Int) + 1): List[Function1Int[Int]])

    val result3 = sequenced(3)
    result3 should be (List(6, 5, 4))
    
  }
}
