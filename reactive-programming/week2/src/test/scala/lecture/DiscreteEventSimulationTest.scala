package lecture

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSuite}
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class DiscreteEventSimulation extends FunSuite with ShouldMatchers {
  object easySimulation extends Curcuit with Parameters
  import easySimulation._

  test("run simulation") {
    val in1, in2, sum, carry = new Wire

    halfAdder(in1, in2, sum, carry)
    probe("sum", sum)
    probe("carry", carry)

    in1 setSignal(true)
    easySimulation.run()

    in2 setSignal(true)
    easySimulation.run()
  }

}

abstract class Simulation {
  type Action = () => Unit
  case class Event(time: Int, action: Action)

  private type Agenda = List[Event]
  private var agenda: Agenda = List()

  private var curtime = 0
  def currentTime: Int = curtime

  def afterDelay(delay: Int)(block: => Unit): Unit = {
    val item = Event(currentTime + delay, () => block)
    agenda = insert(agenda, item)
  }

  private def insert(ag: List[Event], item: Event): List[Event] = ag match {
    case e :: es if e.time <= item.time => e :: insert(es, item)
    case _ => item :: ag
  }

  private def loop(): Unit = agenda match {
    case e :: es =>
      agenda = es
      curtime = e.time
      e.action()
      loop()

    case Nil =>
  }

  def run(): Unit = {
    afterDelay(0) {
      println(s"*** simulation started, time = $curtime ***")
    }

    loop()
  }
}

trait Parameters {
  val InverterDelay: Int = 2
  val AndGateDelay: Int = 3
  val OrGateDelay: Int = 5
}

abstract class Gate extends Simulation {
  def InverterDelay: Int
  def AndGateDelay: Int
  def OrGateDelay: Int

  class Wire {

    private var sigVal = false
    private var actions: List[Action] = List()

    def getSignal: Boolean = sigVal
    def setSignal(s: Boolean): Unit = {
      if (s != sigVal) {
        sigVal = s
        actions foreach (_())
      }
    }

    def addAction(a: Action): Unit = {
      actions = a :: actions
      a()
    }
  }

  def orGate(in1: Wire, in2: Wire, output: Wire) = {
    def orAction(): Unit = {
      val in1Sig = in1.getSignal
      val in2Sig = in2.getSignal

      afterDelay(OrGateDelay) { output setSignal (in1Sig | in2Sig)}
    }
    in1 addAction orAction
    in2 addAction orAction
  }

  def andGate(in1: Wire, in2: Wire, output: Wire) = {
    def andAction(): Unit = {
      val in1Sig = in1.getSignal
      val in2Sig = in2.getSignal
      afterDelay(AndGateDelay) { output setSignal (in1Sig & in2Sig) }
    }

    in1 addAction andAction
    in2 addAction andAction
  }

  def inverter(input: Wire, output: Wire) = {
    def invertAction(): Unit = {
      val inputSig = input.getSignal
      afterDelay(InverterDelay) { output setSignal !inputSig }
    }

    input addAction invertAction
  }

  // for debugging
  def probe(name: String, wire: Wire): Unit = {
    def probeAction(): Unit = {
      println(s"$name $currentTime value = ${wire.getSignal}")
    }

    wire addAction probeAction
  }
}

abstract class Curcuit extends Gate {

  def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit = {
    val d = new Wire
    val e = new Wire

    orGate(a, b, d)
    andGate(a, b, c)
    inverter(c, e)
    andGate(d, e, s)
  }

  def fullAdder(a: Wire, b: Wire, cIn: Wire, sum: Wire, cOut: Wire): Unit = {
    val s, c1, c2 = new Wire

    halfAdder(b, cIn, s, c1)
    halfAdder(a, s, sum, c2)
    orGate(c1, c2, cOut)
  }
}