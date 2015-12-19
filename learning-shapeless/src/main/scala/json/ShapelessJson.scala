package json

import shapeless._, labelled._, syntax.singleton._
import spray.json.DeserializationException

object ShapelessJson {

  sealed trait JsValue
  case class JsString(s: String) extends JsValue
  case class JsObject() extends JsValue

  trait JsonFormat[T] {
    def read(json: JsValue): T
    def write(obj: T): JsValue
  }

  implicit object StringJsonFormat extends JsonFormat[String] {
    override def read(value: JsValue): String = value match {
      case JsString(s) => s
      case other => throw new DeserializationException(other.toString)
    }

    override def write(s: String): JsValue = JsString(s)
  }

  implicit class EnrichedAny[T](val any: T) extends AnyVal {
    def toJson(implicit f: JsonFormat[T]): JsValue = f.write(any)
  }

  implicit class EnrichedJsValue(val v: JsValue) extends AnyVal {
    def convertTo[T](implicit f: JsonFormat[T]): T = f.read(v)
  }

  implicit object HNilFormat extends JsonFormat[HNil] {
    override def read(json: JsValue): HNil = HNil
    override def write(obj: HNil): JsValue = JsObject()
  }

  implicit def hListFormat[Key <: Symbol, Value, Rest <: HList]
  (implicit
   key: Witness.Aux[Key],
   jfh: JsonFormat[Value],
   jft: JsonFormat[Rest]): JsonFormat[FieldType[Key, Value] :: Rest] =
    new JsonFormat[FieldType[Key, Value] :: Rest] {
      override def read(json: JsValue): ::[FieldType[Key, Value], Rest] = ???
      override def write(obj: ::[FieldType[Key, Value], Rest]): JsValue = ???
    }
}
