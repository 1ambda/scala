package lambda

object Main extends App {

  // class
  val user1 = new User("hoon")
  assert(user1.name == "hoon")

  // anonymous function
  val func2 = { x: Int =>
    println("Hello World")
    x * 13
  }

  // partial application
  def adder(x: Int, y: Int) = x + y
  def add2  = adder(2, _: Int)
  assert(4 == add2(2))

  // constructor test
  val user2 = new User("Hoon");
  assert(user2.age == 26)

  // method test
  user2.incAge
  assert(user2.age == 27)

  // funciton test
  val incAgeFunc = user2.incAgeFunc
  incAgeFunc();
  assert(user2.age == 28)

  // apply : class behaves like functions 
  val af = new functionAlike
  assert(af() == 0)

  // apply test 2 : Companion Object
  // function is just an object implements FUnction trait
  println(Person("Hoon", 26))
  println(Person("Hoon"))

  // function trait test
  val plusOne1 = new AddOne1()
  val plusOne2 = new AddOne2()
  assert(plusOne1(1) == plusOne2(1))
}

class User(val name: String) {

  var age: Int = if (name == "Hoon") {
    26
  } else {
    1
  }

  def greet() = println(name)

  def incAge() = { age += 1 }

  val incAgeFunc = { () => age += 1}

}

// class for apply test
class functionAlike {
  def apply() = 0
}

class AddOne1 extends Function[Int, Int] {
  def apply(x: Int) : Int = x + 1
}

class AddOne2 extends (Int => Int) {
  def apply(x: Int) : Int = x + 1
}
