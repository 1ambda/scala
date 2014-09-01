import org.scalatest._

class OptionTest extends FlatSpec with Matchers {
  // based on http://twitter.github.io/scala_school/ko/collections.html

  "null" should "be avoided. Use Option[T] instead" in {
    // base on http://twitter.github.io/effectivescala/#Functional programming-Options

    trait Preemptible[T] {
      def isAvailable(): Boolean
      def isNotAvailable(): Boolean = !isAvailable
      def set(resource: T): Boolean
      def release(): Boolean
    }

    case class Thread(name: String, priorit: Int)
    case class CPU(thread: Option[Thread]) extends Preemptible[Thread] {
      def isAvailable = false
      def set(_thread: Thread): Boolean = true
      def release = false
    }

    def getCurrentThread(): Option[Thread] = {
      return None
    }
  }

}
