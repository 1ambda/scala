package chapter1

class Cafe {
  def buyCoffee(c: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(c, cup.price))
  }

  def buyCoffee(c: CreditCard, n: Int): (List[Coffee], Charge) = {
    val purchases: List[(Coffee, Charge)] = List.fill(n)(buyCoffee(c))
    val (coffees, charges) = purchases.unzip
    (coffees, charges.reduce { (c1, c2) => c1.combine(c2) })
  }
}

case class Coffee() {
  def price: Int = 15
}

object Coffee {
  def price: Int = 15
}

class CreditCard

case class Charge(c: CreditCard, price: Int) {
  def combine(other: Charge): Charge = {
    if (other.c != this.c) throw new RuntimeException("")
    Charge(c, this.price + other.price)
  }
}





