package lecture

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner
import rx.lang.scala.Subscription
import rx.lang.scala.subscriptions._

@RunWith(classOf[JUnitRunner])
class SubscriptionSpec extends FunSuite with Matchers {

  object fixture {
    def a = Subscription { println("A") }
    def b = Subscription { println("B") }
    def c = Subscription { println("C") }
  }

  test ("SerialSubscription") {
    val a = fixture.a
    val b = fixture.b
    val c = fixture.c

    val serial: SerialSubscription = SerialSubscription()
    serial.subscription = a

    a.isUnsubscribed should be (false)
    serial.subscription = b
    a.isUnsubscribed should be (true)
  }

  ignore ("MultiAssignment") {
    val a = fixture.a
    val b = fixture.b
    val c = fixture.c

    val multi = MultipleAssignmentSubscription()

    multi.isUnsubscribed should be (false)

    multi.subscription = a
    multi.subscription = b

    multi.unsubscribe()

    a.isUnsubscribed should be (false)
    b.isUnsubscribed should be (true)

    // already unsubscribed
    multi.subscription = c
    multi.isUnsubscribed should be (true)
  }


  ignore ("unsubscribe CompositeSubscription") {

    val a = fixture.a
    val b = fixture.b

    val composite = CompositeSubscription(a, b)

    composite.isUnsubscribed should be (false)

    composite.unsubscribe()

    composite.isUnsubscribed should be (true)
    a.isUnsubscribed should be (true)
    b.isUnsubscribed should be (true)

    val c = fixture.c

    composite += c
    c.isUnsubscribed should be (true)
  }
}
