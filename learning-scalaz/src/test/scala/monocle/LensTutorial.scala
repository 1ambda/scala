package monocle

import org.scalatest._

/* ref - http://julien-truffaut.github.io/Monocle//tut/lens.html */
class LensTutorial extends WordSpec with Matchers {

  case class Address(streetNumber: Int, streetName: String)
  case class Person(name: String, age: Int, address: Address)

  val _streetNumLens =
    Lens[Address, Int](_.streetNumber)(num => a => a.copy(streetNumber = num))
  val _streetNameLens =
    Lens[Address, String](_.streetName)(name => a => a.copy(streetName = name))

  val a1 = Address(10, "High Street")
  val p1 = Person("John", 20, a1)

  val _addressLens = Lens[Person, Address](_.address)(a => p => p.copy(address = a))

  "create Lens" in {
    /**
     *  get: Address => Int
     *  set: Int => Address => Address
     */
    _streetNumLens.get(a1) shouldBe a1.streetNumber

    val a2 = _streetNumLens.set(5)(a1)
    a2 shouldBe a1.copy(streetNumber = 5)

    /* calling `modify` equivalent to  call `get` and `set` */
    val a3 = _streetNameLens.modify(_ + "2")(a2)
    a3 shouldBe a2.copy(streetName = a2.streetName + "2")
  }

  "Lens.modifyF" in {
    def neighbors(n: Int): List[Int] = if (n > 0) List(n - 1, n + 1) else List(n + 1)

    import scalaz.std.list._
    val as =_streetNumLens.modifyF(neighbors)(a1)
    as shouldBe List(_streetNumLens.modify(_ - 1)(a1), _streetNumLens.modify(_ + 1)(a1))
  }

  "Lens.composeLens" in {
    val _personStreetNum  = (_addressLens composeLens _streetNumLens)
    val _personStreetName = (_addressLens composeLens _streetNameLens)

    val p2 = _personStreetName.modify(_ + " 2")(p1)
    p2 shouldBe p1.copy(address = p1.address.copy(streetName = p1.address.streetName + " 2"))
  }

  "Generate Lens using Macro" in {
    import monocle.macros.GenLens

    val _age = GenLens[Person](_.age)
    val _ageLens = Lens[Person, Int](_.age)(a => p => p.copy(age = a))

    val p2 = _age.modify(_ + 1)(p1)
    val p3 = _ageLens.modify(_ + 1)(p1)

    p2 shouldBe p3

    /* generate a nested Lens */
    val _personStreetName = GenLens[Person](_.address.streetName)
    val p4 = _personStreetName.set("Iffley Road")(p1)
    p4 shouldBe p1.copy(address = p1.address.copy(streetName = "Iffley Road"))
  }

  "Generate Lens using Annotation" in {
    import monocle.macros.Lenses
    @Lenses case class Point(x: Int, y: Int)

    val p = Point(5, 3)
    Point.x.get(p) shouldBe 5
    Point.y.set(0)(p) shouldBe Point(5, 0)
  }
}
