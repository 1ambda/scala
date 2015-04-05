package Implicit 

import org.scalatest._

class ImplicitTest extends FlatSpec with Matchers {

  "implicit function" should "make a applicable type" in  {
    implicit def str2Int(x :String) = x.toInt

    val x: Int = "3"

    x should be (3)
  }

  "view bound" should "make a applicable type using implicit function" in  {
    implicit def str2Int(x :String) = x.toInt

    class Container[A <% Int] { def addTen(x: A) = 10 + x }

    val result = (new Container[String]).addTen("20");

    result should be (30)
  }

  "implicit parameters" should "be used when conversion is available" in  {
    def bar(z: Int)(implicit x: Int, y: Double) = x + y + 2 * z
    def foo(z: Int) = {
      implicit val x: Int = 3
      implicit val y: Double = 3.5
      bar(z)
    }

    val result = foo(2)
    result should be (2 * 2 + 3 + 3.5)
  }
}
