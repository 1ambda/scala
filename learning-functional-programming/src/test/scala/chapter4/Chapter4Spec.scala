package chapter4

import org.scalatest.{FunSuite, Matchers}

class Chapter4Spec extends FunSuite with Matchers {

  case class User(id: Int,
                  firstName: String,
                  lastName: String,
                  age: Int,
                  gender: Option[String])

  object UserRepository {
    def findById(id: Int): Option[User] = Some(User(1, "John", "Doe", 32, Some("male")))
  }


  test("Some(3).map(_ + 5) == Some(7)") {
    Some(3).map(_ + 5) should be (Some(8))
  }

  test("None.map(_ + 7481480) == None") {
    val a: Option[Int] = None
    a.map(_ * 124091409) should be (None)
  }

  test("Some(3).filter(_ > 1) == Some(3)") {
    Some(3).filter(_ > 1) should be (Some(3))
  }

  test("Some(3).filter(_ > 5) == None") {
    Some(3).filter(_ > 5) should be (None)
  }

  test("None.filter(_ > 129319 == None") {
    val a: Option[Int] = None
    a.map(_ > 12391239) should be(None)
  }

  test("UserRepository.findById(1).map(_.gender) == Some(Some(\"male\")") {
    UserRepository.findById(1).map(_.gender) should be (Some(Some("male")))
  }

  test("UserRepository.findById(1).flatMap(_.gender) == Some(\"male\")") {
    UserRepository.findById(1).flatMap(_.gender) should be (Some("male"))
  }

  test("Some(3).getOrElse(4) == 3") {
    Some(3).getOrElse(4) should be (3)
  }

  test("None.getOrElse(4) == 4") {
    val a: Option[Int] = None
    a.getOrElse(4) should be (4)
  }

}
