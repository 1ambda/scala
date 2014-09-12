import org.scalatest._

class OperatorSplatTest extends FlatSpec with Matchers {

  // email split @
  // domain split .

  object Email {
    def unapply(str: String): Option[(String, String)] = {
      val parts = str split '@'
      if (parts.length == 2) Some(parts(0), parts(1)) else None
    }
  }

  object Domain {
    def unapply(str: String): Option[(String, String)] = {
      val parts = str split '.'
      if (parts.length == 2) Some(parts(0), parts(1)) else None
    }
  }

  def reverseDomain(domain: String): String = {
    domain match {
      case Email(user, Domain(first, last)) => reverseStrings(first, last)
      case _ => ""
    }
  }

  def reverseStrings(strings: String*): String = {
    strings.reverse.mkString(".")
  }

  "'domain.com'" should "be reversed like 'com.domain'" in {

    val (domain1, expected1) = ("user@acm.org", "org.acm")

    assert(reverseDomain(domain1) == expected1)
  }

}
