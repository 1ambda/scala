import org.scalatest._

class EtaExpansionTEst extends FlatSpec with Matchers {


  // http://stackoverflow.com/questions/2529184/difference-between-method-and-function-in-scala

  /*
   * A Function Type is a type of the form (T1, ..., Tn) => U
   * Anonymous Functions and Method Values have function types
   * ETA expansion converts methods into function types.
   * an Anonymous Function is instance of trait 'FunctinoN'
   */


  // http://stackoverflow.com/questions/2363013/in-scala-why-cant-i-partially-apply-a-function-without-explicitly-specifying-i


  behavior of "Eta-expansion"

  // Rule A
  it can "be invoked when _ is used in place of the argument list" in {
    def method(a: Int, b: Int): Int = a + b
    val func = method _
  }

  // Rule B
  it can """be invoked when the argument list is ommitted
            and expected type of expression is a function type""" in {
    def method(a: Int, b: Int): Int = a + b
    val func: (Int, Int) => Int = method
  }

  // Rule C
  it can "be invoked when each of the arguments is _" in {
    def method(a: Int, b: Int): Int = a + b
    val func = method(_, _)

    // method(_, _) expands to (a, b) => foo(a, b)
  }

  "Rule B" can "be used with partial application" in {
    def plus(x: Int)(y: Int) = x + y

    val plus10 = plus(10) _
    val result = plus10(20)
    val expected = 30
    assert(result == expected)
  }
}
