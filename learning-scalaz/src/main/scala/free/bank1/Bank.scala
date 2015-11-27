package free.bank1

import scalaz._
import scalaz.Scalaz._

/** ref
  *
  * Composable application architecture with reasonably priced monad
  *
  * code - https://gist.github.com/runarorama/a8fab38e473fafa0921d
  * slide - https://dl.dropboxusercontent.com/u/4588997/ReasonablyPriced.pdf
  * video - https://www.parleys.com/tutorial/53a7d2c3e4b0543940d9e538/
  *
  * gist
  *
  * - https://github.com/turtlecoder/reasonably-priced/blob/master/src/main/scala/reasonablypriced/Reasonablypriced.scala
  * - https://github.com/petomat/reasonably-priced/blob/master/src/main/scala/reasonablypriced/Reasonablypriced.scala
  * - https://github.com/stew/reasonably-priced/blob/master/src/main/scala/reasonable/App.scala
  */

/**
 * Idea - Return a description of what we want to do
 *
 * Where `Instruction` is the *coproduct* of
 * def transfer(amount: Long, from: Account, to: Account, user: User): List[Instruction]
 *
 * - Logging
 * - Fail with an error
 * - Authorize
 * - Read from storage, Write to storage
 * ...
 */

object Bank {
  import Free._
  import Interact._
  import Tester._

  def run[A](program: Interact[A]): A = runFC(program)(Console)

  def test[A](program: Interact[A]): Tester[A] = runFC(program)(Test)
}





/**
 * TODO
 *
 * file example - https://www.chrisstucchio.com/blog/2015/free_monads_in_scalaz.html
 *
 * branch example - https://gist.github.com/EECOLOR/c312bdf54039a42a3058
 *
 * underscore io free
 * 1. http://underscore.io/blog/posts/2015/04/28/monadic-io-laziness-makes-you-free.html
 * 2. http://underscore.io/blog/posts/2015/04/14/free-monads-are-simple.html (tweet)
 *
 * FreeR - http://mandubian.com/2015/04/09/freer/
 * FreeAp - http://d.hatena.ne.jp/xuwei/20150127/1422322757
 *
 * Free Monad Tank Game Example - https://github.com/kenbot/free
 *
 * Stackless Scala with Free
 * 1. SO - http://stackoverflow.com/questions/18427790/stackless-scala-with-free-monads-complete-example
 *
 *
 * Yonead lemma - http://blog.higher-order.com/blog/2013/11/01/free-and-yoneda/
 */
