package tutorial

import org.scalatest._

// ref - http://www.slideshare.net/mpilquist/scalaz-13068563?related=1
class ScalazTutorial extends WordSpec with Matchers {

  "Option" in {
    // import functions & typeclass instances for option
    import scalaz.std.option._

    some(42) shouldBe Some(42)
    none[Int] shouldBe None

    val os = List(Some(42), None, Some(20))
    os.foldLeft(None: Option[Int]) { (acc, o) => acc orElse o } shouldBe some(42)
    os.foldLeft(none[Int]) { (acc, o) => acc orElse o } shouldBe some(42)
  }

  "Option conditional" in {
    // import syntax extensions for option
    import scalaz.syntax.std.option._

    def xget(opt: Option[Int]) = opt | Int.MaxValue

    xget(Some(42)) shouldBe 42
    xget(None) shouldBe Int.MaxValue
  }
}
