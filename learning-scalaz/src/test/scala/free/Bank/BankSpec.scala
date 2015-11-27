package free.bank

import free.bank1.{Bank, Interact}
import org.scalatest._

class BankSpec extends FunSuite with Matchers {

  import Bank._
  import Interact._

  test("Console") {
    val program = for {
      first <- ask("What is your first name: ")
      last  <- ask("What is your last name: ")
      _     <- tell(s"Hello, $first $last")
    } yield ()

    // Bank.run(program)
  }

  test("Tester") {
    val m: Map[String, String] = Map(
      "What is your first name: " -> "Harry",
      "What is your last name: " -> "Potter"
    )

    val program = for {
      first <- ask("What is your first name: ")
      last  <- ask("What is your last name: ")
      _     <- tell(s"Hello, $first $last")
    } yield ()

    val r = Bank.test(program)(m)
    r shouldBe (List("Hello, Harry Potter"), ())
  }
}
