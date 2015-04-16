## Either

[Reference: Neophyte's Scala Guide Either](http://danielwestheide.com/blog/2013/01/02/the-neophytes-guide-to-scala-part-7-the-either-type.html)

Either is designed to be unbiased. So, you can't use an `Either` instance like `Try`, `Option` (`Try` is success-biased)

```scala
// ref: http://alvinalexander.com/
def divide(x: Int, y:Int): Either[String, Int] = {
  if (y == 0) Left("Dude, can't divide by 0")
  else Right(x / y)
}

"divide by 0" can "not be permitted" in {
  val result: Either[String, Int] = divide(3, 0)
  result should be (Left("Dude, can't divide by 0"))
}
```

Calling `left` or `right` will return  `LeftProjection` or `RightProjection` which are basically left or right-biased wrappers for the `Either`

```scala
// ref: http://danielwestheide.com/
def getContent(url: URL): Either[String, Source] = {
  if (url.getHost.contains("google"))
    Left("Requested URL is blocked")
  else
    Right(Source.fromURL(url))
}

// Content is the Right
val content1: Either[Iterator[String], Source] =
  getContent(new URL("http://1ambda.github.io")).left.map(Iterator(_))

// content is the Left
val moreContent: Either[Iterator[String], Source] =
  getContent(new URL("http://www.google.com")).left.map(Iterator(_))

// you can use `Projection` in for-comprehensions
def avgLineNumber(url1: URL, url2: URL): Either[String, Int] = {
  for {
    src1 <- getContent(url1).right
    src2 <- getContent(url2).right
    lines1 <- Right(src1.getLines().size).right
    lines2 <- Right(src2.getLines().size).right
  } yield (lines1 + lines2) / 2
}
```

### Option and Either

```scala
// Either provides useful methods
"Right(3).right.toOption" should "Some(3)" in {
  Right(3).right.toOption should be (Some(3))
}

"Right(3).left.toOption" should "None" in {
  Right(3).left.toOption should be (None)
}
```

### When to use Either

Either has one advantage over `Try` on error handling. You can have more specific error types at compile time. These means `Either` can be a good choice for expected errors.

```scala
// Either is useful for handling a expected exception
import scala.util.control.Exception.catching
def hanlding[Ex <: Throwable, T](exType: Class[Ex])(block: => T): Either[Ex, T] =
  catching(exType).either(block).asInstanceOf[Either[Ex, T]]

import java.net.MalformedURLException
def parseURL(url: String): Either[MalformedURLException, URL] =
  hanlding(classOf[MalformedURLException])(new URL(url))

"parseURL(\"asdasd\")" should "give an Left(MalformedUrlException)" in {
  val result:Either[MalformedURLException, URL] = parseURL("asdasd")

  result.left.toOption.get.getClass.getName should be ("java.net.MalformedURLException")
}

// custom exception using case class
case class Customer(age: Int)
class Cigarettes
case class UnderAgeFailure(age: Int, required: Int)
def buyCigarettes(customer: Customer): Either[UnderAgeFailure, Cigarettes] =
  if (customer.age < 16) Left(UnderAgeFailure(customer.age, 16))
  else Right(new Cigarettes)

```

### Collection

```scala
// collection hanlding
type Citizen = String
case class BlackListedResource(url: URL, visitors: Set[Citizen])

val blacklist = List(
  BlackListedResource(new URL("https://google.com"), Set("John Doe", "Johanna Doe")),
  BlackListedResource(new URL("http://yahoo.com"), Set.empty),
  BlackListedResource(new URL("https://maps.google.com"), Set("John Doe")),
  BlackListedResource(new URL("http://plus.google.com"), Set.empty)
)

val checkedBlacklist: List[Either[URL, Set[Citizen]]] =
  blacklist.map(resource =>
    if (resource.visitors.isEmpty) Left(resource.url)
    else Right(resource.visitors)
  )

it should "check suspicious URL, problematic citizens using Either" in {
  val suspicious: List[URL] =
    checkedBlacklist.flatMap(_.left.toOption)
  val problematic: Set[Citizen] =
    checkedBlacklist.flatMap(_.right.toOption).flatten.toSet

  suspicious.length should be (2)
  problematic.size should be (2)
```
