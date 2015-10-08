package monocles

import org.scalatest._

/* ref - http://www.slideshare.net/JulienTruffaut/beyond-scala-lens */
class IsoTutorial extends WordSpec with Matchers {

  import monocle.Iso

  /**
   * case class Iso[S, A](
   *  get       : S => A,
   *  reverseGet: A => S
   * )
   *
   * For all s:S, reverseGet(get(s)) == s
   * For all a:A, get(reverseGet(a)) == a
   */

  sealed trait Volume
  case class Bytes(size: Long) extends Volume
  case class KiloBytes(size: Long) extends Volume
  case class Megabytes(size: Long) extends Volume

  def addBytes(b1: Bytes, b2: Bytes): Bytes = Bytes(b1.size +b2.size)
  def byteToKilo =
    Iso[Bytes, KiloBytes](b => KiloBytes(b.size / 1024))(k => Bytes(k.size * 1024))

  def kiloToMega =
    Iso[KiloBytes, Megabytes](k => Megabytes(k.size / 1024))(m => KiloBytes(m.size * 1024))

  def kiloToByte: Iso[KiloBytes, Bytes] = byteToKilo.reverse
  def megaToByte: Iso[Megabytes, Bytes] = kiloToMega.reverse composeIso kiloToByte

  case class Storage(bytes: Bytes) {
    def store(v: Volume): Storage = v match {
      case b: Bytes     => Storage(addBytes(bytes, b))
      case k: KiloBytes => Storage(addBytes(bytes, kiloToByte.get(k)))
      case m: Megabytes => Storage(addBytes(bytes, megaToByte.get(m)))
    }
  }

  "Storage Spec" in {
    val s1 = Storage(Bytes(2048))

    s1.store(KiloBytes(2)) shouldBe Storage(Bytes(4096))
  }

  "Iso examples" in {
    def stringToList = Iso[String, List[Char]](_.toList)(_.mkString(""))
    def listToVector[A] = Iso[List[A], Vector[A]](_.toVector)(_.toList)
    def vectorToList[A]: Iso[Vector[A], List[A]] = listToVector.reverse

    val l1 = List(1, 2, 3, 4)
    val v1 = Vector(1, 2, 3, 4)
    listToVector.get(l1) shouldBe v1
    vectorToList.get(v1) shouldBe l1
    listToVector.reverseGet(listToVector.get(l1)) shouldBe l1
  }

  "Compose with Lens" in {
    import monocle.Monocle._
    import monocle.macros._

    case class Person(name: String, age: Int)

    val p1 = Person("1ambda", 27)

    val personToTuple = Iso{p: Person => (p.name, p.age)}((Person.apply _).tupled)


//    personToTuple.get(p1) shouldBe ("1ambda", 27)

//    def personToTuple1 = Iso[Person, (String, Int)](p => (p.name, p.age)) {
//      case (name, age) => Person(name, age)
//    }


//    val p2 = (personToTuple composeLens second).set(30, p1)
//    p2 shouldBe Person("1ambda", 30)
  }
}
