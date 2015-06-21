package chapter4

object Insurance {
  import Option._
  import InsuranceSecret._

  def parseInsuranceRateQuote(age: String,
                              numberOfSpeedingTickets: String): Option[Double] = {

    val optAge: Option[Int] = Try { age.toInt }
    val optTickets: Option[Int] = Try { numberOfSpeedingTickets.toInt }

    val rate: Option[Double] = map2(optAge, optTickets)(insuranceRateQuote)
    rate
  }
}

object InsuranceSecret {
  def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double = {
    (1.0 / age) * 30 * numberOfSpeedingTickets
  }
}
