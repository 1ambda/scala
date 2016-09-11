import util.TestSuite

class HListSpec extends TestSuite {
  import shapeless._
  import shapeless.poly._

  test("choose") {

    object choose extends (Set ~> Option) {
      def apply[T](s: Set[T]): Option[T] = s.headOption
    }

    val sets = Set(1) :: Set("foo") :: HNil

    sets map choose shouldBe (Some(1) :: Some("foo") :: HNil)
  }

  test("addSize") {

    object size extends Poly1 {
      implicit def caseInt = at[Int](x => 1)
      implicit def caseString = at[String](x => x.length)
      implicit def caseTuple[T, U](implicit
                                   st: Case.Aux[T, Int],
                                   su: Case.Aux[U, Int]) =
        at[(T, U)](t => size(t._1) + size(t._2))
    }

    object addSize extends Poly2 {
      implicit def default[T](implicit st: size.Case.Aux[T, Int]) =
        at[Int, T] { (acc, t) => acc + size(t) }
    }

    val l = 23 :: "foo" :: (13, "wibble") :: HNil
    l.foldLeft(0)(addSize) shouldBe 11
  }

  test("zipper") {
    import syntax.zipper._

    val l = 1 :: "foo" :: 3.0 :: HNil
    l.toZipper.right.put(("wibble", 45)).reify shouldBe 1 :: ("wibble", 45) :: 3.0 :: HNil
    l.toZipper.right.delete.reify shouldBe 1 :: 3.0 :: HNil
  }

  test("HList is covariant") {
    trait Fruit
    case class Apple() extends Fruit
    case class Pear() extends Fruit

    type FFFF = Fruit :: Fruit :: Fruit :: Fruit :: HNil
    type APAP = Apple :: Pear :: Apple :: Pear :: HNil

    val a: Apple = Apple()
    val p: Pear = Pear()

    val apap: APAP = a :: p :: a :: p :: HNil

    import scala.reflect.runtime.universe._

    // implicitly[TypeTag[APAP]].tpe.typeConstructor <:< typeOf[FFFF] should be(true)

    apap.isInstanceOf[FFFF] shouldBe true
    apap.unify.isInstanceOf[FFFF] shouldBe true

    apap.toList shouldBe List(Apple(), Pear(), Apple(), Pear())

    import syntax.typeable._

    val ffff: FFFF = apap.unify
    val precise: Option[APAP] = ffff.cast[APAP]

    precise shouldBe Some(Apple() :: Pear() :: Apple() :: Pear() :: HNil)
  }


}
