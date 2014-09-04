import org.scalatest._

class ComposeTest extends FlatSpec with Matchers {
  // http://twitter.github.io/scala_school/ko/pattern-matching-and-functional-composition.html

  def addUmm(x: String) = x + " umm"
  def addAhem(x: String) = x + " ahem"

  "Compose" can "used to combine two functions" in {

    // http://stackoverflow.com/questions/7505304/compose-and-andthen-methods
    // eta expansion
    
    val ummAndAhem = (addAhem _).compose(addUmm _)
    // equal to 'addAhem _ compose addUmm'

    val result = ummAndAhem("Hello,")
    val expected = "Hello, umm ahem"
    assert(result == expected)
  }

  "addThen" can "also combine two functions" in {
    val ahemAndUmm = addAhem _ andThen addUmm

    val result = ahemAndUmm("Hello,")
    val expected = "Hello, ahem umm"

    assert(result == expected)
  }
}
