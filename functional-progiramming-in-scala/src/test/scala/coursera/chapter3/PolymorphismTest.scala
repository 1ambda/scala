package coursera.chapter3

/*
   This is a stub test class. To learn how to customize it,
see the documentation for `ensime-goto-test-configs'
*/


import org.scalatest._

class PolymorphismTest extends FlatSpec with Matchers {
  "new Nil().tail" should "throw NoSuchElementException" in {
    intercept[NoSuchElementException] {
      new Nil().tail
    }
  }

  "nth" should "return n'th element" in {
    val l1 = new Cons(1, new Cons(2, new Cons(3, new Nil)))
    assert(Polymorphism.nth(0, l1) == 1)
    assert(Polymorphism.nth(1, l1) == 2)
    assert(Polymorphism.nth(2, l1) == 3)
  }

  "nth" should "throw OutOfBoundsException" in {
    val l1 = new Cons(1, new Cons(2, new Cons(3, new Nil)))

    intercept[IndexOutOfBoundsException] {
      val a = Polymorphism.nth(-1, l1)
    }

    intercept[IndexOutOfBoundsException] {
      Polymorphism.nth(3, l1)
    }

    intercept[IndexOutOfBoundsException] {
      Polymorphism.nth(5, l1)
    }

    intercept[IndexOutOfBoundsException] {
      Polymorphism.nth(11, l1)
    }

  }
}
