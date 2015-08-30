package chapter9

trait JSON

object JSON {
  case object JNull extends JSON
  case class JNumber(get: Double) extends JSON
  case class JString(get: String) extends JSON
  case class JBool(get: Boolean) extends JSON
  case class JArray(get: IndexedSeq[JSON]) extends JSON
  case class JObject(get: Map[String, JSON]) extends JSON

  // string, regex, slice, succeed, flatMap, or, + letter, digit, whitespace
  import Parser._ /* we need the implicit function regex */

  def jNumber: Parser[JNumber] = double map (JNumber(_))
  def jString: Parser[JString] = escapedQuote map (JString(_))
  def jBool: Parser[JBool] = boolean map (JBool(_))
  def jNull: Parser[JSON] = regex("null".r) map(_ => JNull)

  def json: Parser[JSON] = root(jObject | jArray)

  def jObject: Parser[JSON] = surround("{", "}") {
    jKeyValue sep "," map (elems => JObject(elems.toMap))
  }

  def jArray: Parser[JSON] = surround("[", "]") {
    jValue sep "," map (elems => JArray(elems.toIndexedSeq))
  }

  def jLiteral: Parser[JSON] =
    jNull | jNumber | jString | jBool

  def jValue: Parser[JSON] = jLiteral | jArray | jObject
  def jKeyValue: Parser[(String, JSON)] = escapedQuote ** (":" *> jValue)
}
