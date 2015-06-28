# Learning FP

[Ref: Functinal Programming in Scala](http://www.amazon.com/Functional-Programming-Scala-Paul-Chiusano/dp/1617290653/ref=sr_1_3?ie=UTF8&qid=1432526049&sr=8-3&keywords=scala)

## Chapter 1

> 함수형 프로그래밍은 프로그램을 작성하는 **방식**에 대한 제약이지,  
표현 가능한 프로그램의 **종류** 에 대한 제약이 아니다.

참조 투명성을 지키면, 치환모형이 사용 가능해지며 이로 인해 컴퓨터가 수행하는 계산뿐만 아니라 
개발자의 머릿속에서 진행되는 추론도 간단해진다. 

모듈적인 프로그램은 독립적으로 이해하고, 재사용할 수 있는 컴포넌트로 이루어진다. 
프로그램 전체의 의미는 오직 컴포넌트의 의미와 합성에 관한 규칙들로 정의 된다. 
이는 함수형 프로그래밍의 개념과 동일하다. **순수함수는 재사용 가능하고, 독립적인, 그리고 합성 가능한 컴포넌트다.** 

## Chapter 2

`compose` 같은 고차함수는 자신이 수백만줄의 코드로 이루어진 거대한 함수를 다루는지, 
아니면 간단한 한 줄 짜리 함수를 다루는지 신경쓰지 않는다. 이는 다형적 고차함수가 공통적인 패턴을 추상화하여 제공하기 때문인
데 이로인해 **큰 규모의 프로그래밍도 작은 규모의 프로그래밍과 아주 비슷한 느낌으로 진행할 수 있다.**

## Chapter 3

함수적 자료구조란 오직 순수 함수만으로 조작되는 자료구조이다. 따라서 함수적 자료구조는 정의에 의해 **immutable** 이다.

대규모 프로그램에서는 방어적인 복사가 문제가 될 수 있다. 변이 자료가 일련의 느슨하게 결합된 구성 요소들을 거쳐간다면, 
각 구성요소는 자신의 복사본을 만들어야 한다. 다른 구성요소가 그 자료를 변경할 수도 있기 때문이다. 

반면 **immutable** 자료구조는 항상 안전하게 공유할 수 있으므로 복사본을 만들 필요가 없으며, 부수효과에 의존하는 
프로그래밍 방식보다 FP 가 더 효율적인 결과를 산출하는 경우가 많다.

## Chapter 4

`map2` 함수는, 인자가 2개인 어떤 함수라도 수정 없이 `Option` 에 대응하도록 만들 수 있다.

```scala
def map2[A, B, C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = {
  for {
    aValue <- a
    bValue <- b
  } yield f(aValue, bValue)
}

// sequence using traverse
def sequence[A](a: List[Option[A]]): Option[List[A]] = {
  traverse(a)(x => x)
}

// simply,
def traverse[A, B](a: List[A])(f: A => Option[B]): Option[List[B]] = {
  a.foldRight[Option[List[B]]](Some(Nil))((x, y) => map2(f(x), y)(_ :: _))
}
```

`map`, `lift`, `sequence`, `map2`, `map3` 같은 함수들이 있으면, 생략적 값을 다루기 위해 기존 함수를 수정해야 할일이 **전혀** 없어야 하는 것이 정상이다.


4장의 핵심은 실패와 예오를 보통의 값으로 표현할 수 있다는 점과 오류 처리 및 복구에 대한 공통의 패턴을 추상화 하는 함수를 작성할 수 있다는 점이다. 
`Option` 이 그러한 목적으로 많이 쓰이긴 하지만, 에러에 대한 정보를 주지 않는다. 

실패에 대한 원인을 얻기 위해, `Either` 를 사용할 수 있다.  

## Chapter 5

Non-strictness (혹은 laziness) 를 이용하면 컬렉션을 변환하고 탐색하는 `map`, `filter` 등을 하나의 패스로 융합해서 더 효율적인 계산을 해 낼 수 있다.
  
> **Strictness**
> 어떤 표현식의 평가가 무한히 실행되면, 또는 한정돈 값을 돌려주지 않고 오류를 던진다면, 그러한 표현식을 일컬어 **bottom** 으로 평가되는 표현식이라고 부른다. 
> 만약 **bottom** 으로 평가되는 식 모든 `x` 에 대해 `f(x)` 가 *bottom** 이면 `f` 는 **strict** 하다. 
