## State Monad

[Learning Scalaz: Tasteful Stateful Computation](http://eed3si9n.com/learning-scalaz/State.html)

### Id

[Learning Scalaz: Id](http://eed3si9n.com/learning-scalaz/Id.html)

```scala
type Id[+X] = X

scala > (5: Id[Int])
res0: scalaz.Scalaz.Id[Int] = 0
```

### State, StateT

[Scalaz: StateT.scala](https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/StateT.scala)

[Scalaz: State.scala](https://github.com/scalaz/scalaz/blob/series/7.1.x/core/src/main/scala/scalaz/State.scala)

```scala
object State extends StateFunction {
  def apply[S, A](f: S => (S, A)): State[S, A] = new StateT[Id, S, A] {
    def apply(s: S) = f(s)
  }
}
```

위 코드에서 알 수 있듯이 `State[S, +A]` 는 단지 `StateT` 의 *type alias* 다.

다시 말해서, `State` 는 `f` 를 받아 `StateT` 를 만들고 이 `StateT` 는 상태 `s` 를 받아, `(s', A)` 를 만들어 낸다.

`StateT` 는 다음처럼 정의되는데 (간략한 버전)

```scala
trait StateT[F[+_], S, +A] { self =>
  /** Run and return the final value and state in the context of `F` */
  def apply(initial: S): F[(S, A)]

  /** An alias for `apply` */
  def run(initial: S): F[(S, A)] = apply(initial)

  /** Calls `run` using `Monoid[S].zero` as the initial state */
  def runZero(implicit S: Monoid[S]): F[(S, A)] =
    run(S.zero)
}
```

만약 `S` 가 모노이드면, `runZero` 는 `mzero` 값을 이용해서 `apply` 함수를 호출. 즉 초기 상태에 대해 `apply` 를 호출하는 것.

위에서 보았듯이 `State` 는 *State Transition* 을 나타낸다. 그리고 이것이 모나드라면 우리는 *State Transition* 을 엮을 수 있다는 뜻이 된다.

아래 예를 보면

```scala
class StateMonadTest1 extends FlatSpec with Matchers {

  type Stack = List[Int]

  def pop = State[Stack, Int] {
    case x :: xs => (xs, x)
  }

  def push(a: Int) = State[Stack, Unit] {
    case xs => (a :: xs, ())
  }

  "push(3) pop pop on List(5, 1, 2, 4)" should "return (List(1, 2, 4), 5)" in  {
    val s: Stack = List(5, 1, 2, 4)

    val ops: State[Stack, Int] =
      for {
        _ <- push(3)
        a <- pop
        b <- pop
      } yield b

    val result = ops(s)
    result should be (List(1, 2, 4), 5)
  }
}
```

`ops` 변수에 일어나지 않은 연산, *state transition* 을 엮어서 저장했다.

### StateFunctions

```scala
object State extends StateFunction {
  ...
  ...
}


trait StateFunctions {
  def constantState[S, A](a: A, s: => S): State[S, A] =
    State((_: S) => (s, a))
  def state[S, A](a: A): State[S, A] =
    State((_ : S, a))
  def init[S]: State[S, S] = State(s => (s, s))
  def get[S]: State[S, S] = init
  def gets[S, T](f: S => T): State[S, T] = State(s => (s, f(s)))
  def put[S](s: S): State[S, Unit] = State(_ => (s, ()))
  def modify[S](f: S => S): State[S, Unit] = State(s => {
    val r = f(s);
    (r, ())
  })
  /**
   * Computes the difference between the current and previous values of `a`
   */
  def delta[A](a: A)(implicit A: Group[A]): State[A, A] = State{
    (prevA) =>
      val diff = A.minus(a, prevA)
      (diff, a)
  }
}
```

`put`, `get`, `init` 메소드를 다시 보면

```scala
def init[S]: State[S, S] = State(s => (s, s))
def get[S]: State[S, S] = init

def put[S](s: S): State[S, Unit] = State(_ => (s, ()))
```

즉 `get` 은 `State` 에 들어가는 `f` 가 `s => (s, s)` 이므로 현재 상태 `s` 를 얻기 위해 쓰이고

`put` 은 이전 상태가 무엇이든 인자로 받은 `s` 를 `State` 로 만든다. 예제를 보면

```scala
val ops = for {
  now <- get[Stack]
  unit <- put(List(8, 3, 1))
} yield unit

ops(List(1, 2, 3)) should be ((List(8, 3, 1), ()))
```

따라서 이를 이용하면

```scala
def pop2: State[Stack, Int] = for {
  now <- get[Stack]
  val (x :: xs) = now
  unit <- put(xs) // unit == (), since put return [Stack, Unit]
} yield x

def push2(a: Int): State[Stack, Unit] = for {
  now <- get[Stack]
  unit <- put(a :: now)
} yield unit
```
