# Week2

[Ref: Reactive Programming, Coursera](https://class.coursera.org/reactive-002/)

### Functions and States

Rewriting can be done anywhere in a term, and all rewritings which terminate lead to the same solution. 
This is an important result of the **lambda-calculus**, the theory behind functional programming.

An object **has a state** if its behavior is influenced by its history.   
For example, a bank account has a state because the answer to the question  

**"can I withdraw 100 CHF?"**
 
may vary over the course of the lifetime of the account

```scala
class BankAccount {
  private var balance = 0
  def deposit(amount: Int): Unit = {
    if (amount > 0) balance += amount
  }
  def withdraw(amount: Int): Int = {
    if (0 < amount && amount <= balance) {
      balance -= amount
      balance
    } else throw new Error("insufficient funds")
  }
}

val acc = new BankAccount
acc deposit 50
acc withdraw 20
acc withdraw 20
acc withdraw 15
```

even though `BankAccountProxy` doesn't contain any variable, its behavior is clearly stateful because it depends on the history
 
<br/>

### Referential Transparency

> An expression is said to be referentially transparent if it can be replaced with its value without changing the behaviour of a program (in other words, yielding a program that has the same effects and output on the same input). The opposite term is referential opaqueness.

In short, `val x = E; val y = E`, then `val y = x`
 
The expression below is not referential transparent.

```scala
// x and y are not the same

val x = new BankAccount
val y = new BankAccount
```

### Operational Equivalence

> `x` and `y` are operationally equivalent if **no possible test** can distinguish between them

To test if `x` and `y` are the same, we must 

- execute the definitions followed by an arbitrary sequence of operations that involves `x` and `y`, observing the possible outcomes.

```scala
// test 1
val x = new BankAccount
val y = new BankAccount
f(x, y)

// test 2
val x = new BankAccount
val y = new BankAccount
f(x, x)

def f(x: BankAccount, y: BankAccount) {
  x deposit 30
  y withdraw 20
}
```

In this case, we can't use **the substitution model**

On the other hand, If we define 

```scala
val x = new BankAccount
val y = x
```

Then, no sequence of operations can distinguish between `x` and `y`

### Loops

```scala
class LoopsTest extends FunSuite with ShouldMatchers {

  import Loops._

  test("WHILE test") {
    var i = 0
    WHILE(i <= 3) { i += 1 }
    assert(i == 4)
  }

  test("REPEAT UNTIL test") {
    var i = 0

    REPEAT {
      i += 1
    } UNTIL (i > 3)

    assert(i == 4)
  }

  test("for loop") {
    for (i <- 1 until 3) println(i)
    (1 until 3) foreach println
  }
}

object Loops {
  def WHILE(condition: => Boolean)(command: => Unit): Unit = {
    if (condition) {
      command
      WHILE(condition)(command)
    } else ()
  }

  def REPEAT(command: => Unit) =  new {
    def UNTIL(condition: => Boolean): Unit = {
      command

      if (condition) ()
      else UNTIL(condition)
    }
  }

}
```

### Discrete Event Simulation

As you can see, **State** and **Assignments** make our metal model of computatin more complicated. 
In particular We lose referential transparency. But, assignment allow us to formulate certain program in an elegant way (e.g discrete event simulator) 

- A system is represented by a **mutable** list of *actions*.
- The effect of actions change the state of objects and can also install other actions to be executed in the future

```scala
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
```

### The Observer Pattern

The Observer Pattern is widely used when views need to react to changes in a model. Variants of it are also called

- public / subscribe
- model / view / controller

a.k.a **Imperative Event Handling**

(1) The Good Parts

- Decouples view (Subscriber) from state (Publisher)
- Allow to have a varying number of views of a given state (1:N)
- Simple to set up

(2) The Bad Parts

- Forces imperative style, since handlers are **Unit-typed**
- Many moving parts that need to be co-ordinated
- Concurrency makes things more complicated
- Views are still tightly bound to one state. View update happens immediately

(3) Then, How to Improve?

- Functional Reactive Programming
- Abstracting over events and eventstreams with `Future` and `Observable`
- Handling concurrency with `Actor`


```scala
@RunWith(classOf[JUnitRunner])
class ObserverPatternTest extends FunSuite with ShouldMatchers {
  test("imperative event hanlding") {
    val a = new BankAccount
    val b = new BankAccount
    val c = new Consolidator(List(a, b))

    c.totalBalance
    a deposit 20
    assert(c.totalBalance == 20)
  }
}

trait Subscriber {
  def handler(pub: Publisher)
}

trait Publisher {

  private var subscribers: Set[Subscriber] = Set()

  def subscribe(subscriber: Subscriber): Unit = {
    subscribers += subscriber
  }

  def unsubscribe(subscriber: Subscriber): Unit = {
    subscribers -= subscriber
  }

  def publish(): Unit = {
    subscribers foreach { _.handler(this) }
  }
}

class BankAccount extends Publisher {
  private var balance = 0
  def currentBalance = balance

  def deposit(amount: Int): Unit = {
    if (amount > 0) balance += amount
    publish()
  }

  def withdraw(amount: Int): Unit = {
    if (0 < amount && amount <= balance) {
      balance -= balance
      publish()
    } else throw new Error("insufficient funds")
  }
}

class Consolidator(observed: List[BankAccount]) extends Subscriber {
  private var total: Int = _ // initialize as uninitialized. (!!)
  def totalBalance = total

  private def compute(): Unit =  total = observed.map(_.currentBalance).sum
  override def handler(pub: Publisher): Unit =  compute()

  observed foreach { _.subscribe(this) }
  compute()
}
```

### Functional Reactive Programming















