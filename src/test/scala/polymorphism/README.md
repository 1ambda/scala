## Polymorphism

### F-bounded Polymorphism

[F-bounded Polymorphism](http://twitter.github.io/scala_school/advanced-types.html#fbounded)

```scala

// won't compile.
// since we are specifying Ordered for Container
//  not the particular subtype.
class MyContainer extends Container {
  def compare(that: MyContainer): Int
}

// working example.
"F-bounded polymorphism example" should "be compiled" in  {
  trait Container[A <: Container[A]] extends Ordered[A]
  class MyContainer extends Container[MyContainer] {
    def compare(that: MyContainer) = 0
  }

  val first = new MyContainer
  val cs = List(first, new MyContainer, new MyContainer)
  cs.min should be (first)
}
```

### Subtype Polymorphism

[Learning Scalaz Day0](http://eed3si9n.com/learning-scalaz/polymorphism.html)

```scala
// sub-type polymorphism using f-bounded polymorphism
scala> trait Plus[A] {
         def plus(a2: A): A
       }
defined trait Plus

scala> def plus[A <: Plus[A]](a1: A, a2: A): A = a1.plus(a2)
plus: [A <: Plus[A]](a1: A, a2: A)A


// exmaple
case class FooPlus(i: Int) extends Plus[FooPlus] {
def plus(a2: FooPlus): FooPlus = FooPlus(i + a2.i)
}

plus(FooPlus(1), FooPlus(2)) // FooPlus(3)
```

We can at least provide different definitions of `plus` for `A`. But this is not flexible since trait `Plus` needs to be mixed in at the time of defining the datatype. So it can't work for `Int` and `String`


### Ad-hoc Polymorphism

[Learning Scalaz Day0](http://eed3si9n.com/learning-scalaz/polymorphism.html)

```scala
"ad-hoc polymorphism" should "be compiled" in {
  trait Plus[A] {
    def plus(a1: A, a2: A): A
  }

  // use context bound
  def plus[A: Plus](a1: A, a2: A): A = implicitly[Plus[A]].plus(a1, a2)

  implicit val StringPlus = new Plus[String] {
    def plus(a1: String, a2: String) = a1 + "+" + a2
  }

  val expected = "1+2"
  val result = plus("1", "2")

  result should be (expected)
}
```

- we can provide seperate function definition for different types of `A`
- we can provide function definition to types (like `Int`) without access to its source code
- the function definitions can be enabled or disabled in different scopes
