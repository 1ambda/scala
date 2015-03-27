// https://github.com/MarkusJais/scala-futures-examples

package future

import scala.util.Random

case class Book(name: String, price: Double)

object Book {

  def sleepRandom() = Thread.sleep(Random.nextInt(500))

  def getBookListPrice(books: List[Book]): Double = {
    sleepRandom()

    if (Random.nextInt(5) == 1) throw new RuntimeException("error01")

    books.foldLeft(0.0)(_ + _.price)
  }

}
