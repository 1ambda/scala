# scalaz Either `\/`

[scalaz source - either](https://github.com/scalaz/scalaz/blob/series/7.2.x/core/src/main/scala/scalaz/Either.scala)

```scala
scala> 1.right
res2: scalaz.\/[Nothing,Int] = \/-(1)

scala> 1.right[String]
res3: scalaz.\/[String,Int] = \/-(1)

scala> "error".left[Int]
res4: scalaz.\/[String,Int] = -\/(error)
```

The `Either` in scala standard library is not a monad.

```scala
```