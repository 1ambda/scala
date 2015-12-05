package free

import scalaz.Free._
import scalaz.Scalaz._
import scalaz._

object MutualRecursion{

  def isOdd(n: Int): Boolean = {
    if (0 == n) false
    else isEven(n -1)
  }

  def isEven(n: Int): Boolean = {
    if (0 == n) true
    else isOdd(n -1)
  }

def isOddT(n: Int): Trampoline[Boolean] =
  if (0 == n) return_(false)
  else suspend(isEvenT(n - 1))

def isEvenT(n: Int): Trampoline[Boolean] =
  if (0 == n) return_(true)
  else suspend(isOddT(n - 1))

}


