package lecture

import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class AdventureSpec extends FunSuite with Matchers {

  import Adventure._

  test("#Try") {
    val game = Adventure

    val result : Try[Treasure] = for {
      coins <- game.collectCoins()
      diamond <- game.buyTreasure(coins)
    } yield diamond

    result should be (Success(Diamond))
  }
}

object Adventure {

  case class GameOverException(message: String) extends RuntimeException
  case class Coin(value: Int)
  trait Treasure
  case object Diamond extends Treasure

  val treasureCost = 500

  def eatenByMonster = false

  def collectCoins(): Try[List[Coin]] = {
    if (eatenByMonster)
      Failure(GameOverException("Ooops"))
    else
      Success(List(Coin(100), Coin(400), Coin(99)))
  }

  def buyTreasure(coins: List[Coin]): Try[Treasure] = {
    if (coins.map(_.value).sum < treasureCost)
      Failure(GameOverException("Nice Try!"))
    else
      Success(Diamond)
  }
}
