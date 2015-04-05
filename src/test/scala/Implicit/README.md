## Implicit

### Implicit Parameter

```scala
def bar(z: Int)(implicit x: Int, y: Double) = x + y + 2 * z
def foo(z: Int) = {
  implicit val x: Int = 3
  implicit val y: Double = 3.5
  bar(z)
}

val result = foo(2)
result should be (2 * 2 + 3 + 3.5)
}
```

### View Bound ("type classes")

[SO: Implicit View](http://stackoverflow.com/questions/3855595/what-is-the-scala-identifier-implicitly)

An Implicit View can be triggered when the prefix of a selection (e.g `the.prefix.selection(args)`) doesn't contain a member `selection` that is applicable to `args` even after trying to convert `args` with Implicit Views. In this case, the compiler looks for implicit members, locally defined in the current or enclosing scopes, inherited, or imported, that are either Functions from the type of that `the.prefix` to a type with `selection` defined, or equivalent implicit methods.

```scala
scala> 1.min(2) // Int doesn't have min defined, where did that come from?
res21: Int = 1

scala> implicitly[Int => { def min(i: Int): Any }]
res22: (Int) => AnyRef{def min(i: Int): Any} = <function1>

scala> res22(1) //
res23: AnyRef{def min(i: Int): Int} = 1

scala> .getClass
res24: java.lang.Class[_] = class scala.runtime.RichInt
```

[Scala School: View Bound](https://twitter.github.io/scala_school/advanced-types.html#viewbounds)

**Implicit** functions allow automatic conversions. More precisely, they allow on-demand function application when this can help satisfy type inferences.

```scala
scala> implicit def strToInt(x: String) = x.toInt
strToInt: (x: String)Int

scala> "123"
res0: java.lang.String = 123

scala> val y: Int = "123"
y: Int = 123

scala> math.max("123", 111)
res1: Int = 123
```

**View bounds**, like type bounds demand such a function exists for the given type. You specify a view bound with `A <% Int` . This says that `A` has to be **viewable** as `Int`.

```scala
implicit def str2Int(x :String) = x.toInt

class Container[A <% Int] { def addTen(x: A) = 10 + x }

val result = (new Container[String]).addTen("20");

result should be (30)

```

### Context Bound

[Scala Doc: Context Bound](http://docs.scala-lang.org/tutorials/FAQ/context-and-view-bounds.html)

### Implicit

[SO: What is the Scala identifier "Implicitly"?](http://stackoverflow.com/questions/3855595/what-is-the-scala-identifier-implicitly)

`Implicitly` is available in Scala 2.8 and is defined in `Predef` as:

```scala
def implicitly[T](implicit e:T): T = e
```

It is commonly used to **check if an implicit value of type** `T` **is available and return it** if so is the case.

```scala
scala> implicit val a = "test" // define an implicit value of type String
a: java.lang.String = test

scala> val b = implicitly[String] // search for an implicit value of type String and assign it to b
b: String = test

scala> val c = implicitly[Int] // search for an implicit value of type Int and assign it to c
<console>:6: error: could not find implicit value for parameter e: Int
       val c = implicitly[Int]
```

Scala 2.8 allow a shorthand syntax for implicit parameters, called **Context Bounds**. Briefly, a method with a type parameter `A` that requires an implicit parameter of type `M[A]`

```scala
def foo[A](implicit ma: M[A])

// can be rewritten as:
def foo[A: M]

// you can use implicitly as follow
def foo[A: M] = {
  val ma = implicitly[M[A]]
}
```

### `=:=, <:<, <%<`

[=:=, <:<, <%<](https://apocalisp.wordpress.com/2010/06/10/type-level-programming-in-scala-part-2-implicitly-and/)

- `A =:= B`  will only be found when `A` is the same type as `B`
- `A <:< B` is for type conformance. `A` must be a subtype of `B`
- `A <%< B` is for type conversion. `A` must be viewable as `B`

```scala
scala> implicitly[Int =:= Int]
res0: =:=[Int,Int] = <function1>

scala> implicitly[Int =:= AnyVal]
error: could not find implicit value for parameter e: =:=[Int,AnyVal]

scala> implicitly[Int <:< AnyVal]
res1: <:<[Int,AnyVal] = <function1>

scala> implicitly[Int <:< Long]
error: could not find implicit value for parameter e: (Int <:< Long)

scala> implicitly[Int <%< Long]
res1: (Int <%< Long) = <function1>
```

### Summary

[SO Question: Good example of implicit parameter in Scala](http://stackoverflow.com/questions/9530893/good-example-of-implicit-parameter-in-scala)

Note that there are two concepts. Implicit conversions and implicit parameters that very close, but do not completely overlap.

```scala
// view bound
def max[T <% Ordered[T]](a: T, b: T): T = if (a < b) b else a

// implicit parameter
def max[T](a: T, b: T)(implicit $ev1: Function1[T, Ordered[T]]): T = if ($ev1(a) < b) b else a


def max[T](a: T, b: T)(implicit $ev1: Ordering[T]): T = if ($ev1.lt(a, b)) b else a
// latter followed by the syntactic sugar. Context Bound
def max[T: Ordering](a: T, b: T): T = if (implicitly[Ordering[T]].lt(a, b)) b else a

```

Some examples are as follow

```scala
// Array initialization uses a context bound of a class manifests.
def f[T](size: Int) = new Array[T](size) // won't compile
def f[T: ClassManifest](size: Int) = new Array[T](size)

// Another common usage is to decrease boiler-plate on operations that muse share a common parameter.

def withTransaction(f: Transaction => Unit) = {
  val txn = new Transaction

  try { f(txn); txn.commit() }
  catch { case ex => txn.rollback(); throw ex }
}

withTransaction { txn =>
  op1(data)(txn)
  op2(data)(txn)
  op3(data)(txn)
}

// Which is then simplified like this
withTransaction { implicit txn =>
  op1(data)
  op2(data)
  op3(data)
}
```
