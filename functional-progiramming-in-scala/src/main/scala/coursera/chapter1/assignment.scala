package coursera.chapter1

object Main {
  def main(args: Array[String]) {
    println("Pascal's Triangle")
    for (row <- 0 to 10) {
      for (col <- 0 to row)
        print(pascal(col, row) + " ")
      println()
    }
  }

  /**
   * Exercise 1
   */
  def pascal(c: Int, r: Int): Int = {

    if (c == 0) 1
    else if (c == r) 1
    else {
      // case: row => 2, col != 0 && col != row
      pascal(c-1, r-1) + pascal(c, r-1)
    }
  }

  /**
   * Exercise 2
   */
  def balance(chars: List[Char]): Boolean = {
    // list is empty
    if (chars.isEmpty)
      return true

    // filter '(' and ')'
    val parens = for(char <- chars if (char == '(' || char == ')')) yield char
    if (parens.isEmpty) 
      return true

    // startWith )
    if (parens.head == ')') 
      return false

    // counting
    if (parens.size % 2 != 0) {
      return false
    }

    // matching
    matchParens(parens, 0);
  }

  def matchParens(parens: List[Char], openedParensCount: Int): Boolean = {
    if (openedParensCount < 0) false
    else if (parens.isEmpty && openedParensCount != 0) false
    else if (parens.isEmpty && openedParensCount == 0) true
    else {
      parens.head match {
        case '(' => matchParens(parens.tail, openedParensCount + 1)
        case ')' => matchParens(parens.tail, openedParensCount - 1)
      }
    }
  }

  /**
   * Exercise 3
   */

  // http://stackoverflow.com/questions/12629721/coin-change-algorithm-in-scala-using-recursion
  def countChange(money: Int, coins: List[Int]): Int = {
    if (money <= 0 || coins.isEmpty) 0
    else {
      count(money, coins.sortWith(_.compareTo(_) < 0))
      count2(money, coins.sortWith(_.compareTo(_) < 0))
    }
  }

  def count(capacity: Int, changes: List[Int]): Int = {
    if (capacity == 0) 1
    else if (capacity < 0 ) 0
    else if (changes.isEmpty && capacity >=1) 0
    else
      count(capacity, changes.tail) + count(capacity - changes.head, changes)
  }

  def count2(money:Int, coins: List[Int]): Int = {
    if (money < 0) return 0
    else if (money == 0) return 1

    val small = coins.head;

    if (small > money) 0
    else {
      // case: small <= money
      var sum = 0;
      for(coin <- coins if coin <= money) {
        sum += count2(money - coin, coins.filter { _ >= coin })
      }
      sum
    }
  }
}
