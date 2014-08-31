package lambda

// Companion Class
class Person(val name: String, val age: Int = 1) {
  override def toString = s"name : $name, age: $age"
}

// Companion Object
// COmpanion Object and Class should be declared in the same file.
object Person {
  def apply(name: String, age: Int) = {
    val p = new Person(name, age)
    p
  }

  def apply(name: String) = {
    val p = new Person(name)
    p
  }
}
