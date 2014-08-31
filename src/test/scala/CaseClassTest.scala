import org.scalatest._

class CaseClassTest extends FlatSpec with Matchers {
  "Case class" can "be created like function call" in {
    case class Car(model: String, price: Double)

    val car1 = Car("K5", 2127.0)
    val car2 = Car("K7", 3070.5)
    val car3 = Car("K9", 4300.27)

    def discount(car: Car): Double = {
      car match {
        case car: Car if car.model == "K5" => 1900.0
        case car@Car(_, _) => car.price
      }
    }

    assert(discount(car1) == 1900.0)
    assert(discount(car2) == car2.price)
    assert(discount(car3) == car3.price)
  }
}
