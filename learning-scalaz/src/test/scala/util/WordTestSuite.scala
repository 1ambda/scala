package util

import org.scalatest._

trait WordTestSuite
  extends WordSpec
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with TestImplicits

    trait FunTestSuite
      extends FunSuite
      with Matchers
      with BeforeAndAfterEach
      with BeforeAndAfterAll
      with TestImplicits

    trait TestImplicits {
      final case class StrictEqualOps[A](val a: A) {
        def =:=(aa: A) = assert(a == aa)
        def =/=(aa: A) = assert(!(a == aa))
      }

      implicit def toStrictEqualOps[A](a: A) = StrictEqualOps(a)
    }
