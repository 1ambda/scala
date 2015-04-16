package validation 

import org.scalatest._
import scalaz._
import Scalaz._


class ValidationTest extends FlatSpec with Matchers {

  "Validation" should "report all failure" in  {
    val event1: Validation[String, String] = "event1 Failed".failure[String]
    val event2: Validation[String, String] = "event2 Done".success[String]
    val event3: Validation[String, String] = "event3 Failed".failure[String]

    val result: Validation[String, String] =
      (event1 |@| event2 |@| event3) { _ + _ + _ }

    result should be (Failure("event1 Failedevent3 Failed"))
  }

  "Validationnel" can "be used to prevent mushed error mesasges" in {
    val nel1: NonEmptyList[Int] = 1.wrapNel
    nel1 should be (NonEmptyList(1))

    val failed1: ValidationNel[String, String] = "failed1".failureNel[String]
    failed1 should be (Failure(NonEmptyList("failed1")))

    val event1: ValidationNel[String, String] = "event1 Failed".failureNel[String]
    val event2: ValidationNel[String, String] = "event2 Done".successNel[String]
    val event3: ValidationNel[String, String] = "event3 Failed".failureNel[String]

    val result: ValidationNel[String, String] =
      (event1 |@| event2 |@| event3) { _ + _ + _ }

    result should be (Failure(NonEmptyList("event1 Failed", "event3 Failed")))
  }
}
