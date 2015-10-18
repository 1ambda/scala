package reader_writer

import org.scalatest.{Matchers, FunSuite}
import scalaz._, Scalaz._

class RWSSpec extends FunSuite with Matchers {

  /**
   * ref
   *
   * RWS
   * http://stackoverflow.com/questions/11619433/reader-writer-state-monad-how-to-run-this-scala-code
   * https://gist.github.com/mpilquist/2364137
   * https://github.com/scalaz/scalaz/blob/series/7.2.x/example/src/main/scala/scalaz/example/ReaderWriterStateTUsage.scala
   * http://underscore.io/blog/posts/2014/07/27/readerwriterstate.html
   */



  /**
   * IO ST
   * http://underscore.io/blog/posts/2015/04/28/monadic-io-laziness-makes-you-free.html
   * http://stackoverflow.com/questions/19687470/scala-io-monad-whats-the-point
   * https://apocalisp.wordpress.com/2011/03/20/towards-an-effect-system-in-scala-part-1/
   * https://apocalisp.wordpress.com/2011/12/19/towards-an-effect-system-in-scala-part-2-io-monad/
   *
   * https://github.com/fpinscala/fpinscala/wiki/Chapter-14:-Local-effects-and-mutable-state
   * https://github.com/fpinscala/fpinscala/wiki/Chapter-13:-External-effects-and-IO
   * https://github.com/fpinscala/fpinscala/wiki/Chapter-15:-Stream-processing-and-incremental-IO
   *
   * FREE, TRAMPOLINE
   * https://apocalisp.wordpress.com/2011/10/26/tail-call-elimination-in-scala-monads/
   * http://blog.higher-order.com/blog/2015/06/18/easy-performance-wins-with-scalaz/
   *
   * ADVANCED MONAD
   * https://github.com/fpinscala/fpinscala/wiki/Chapter-11:-Monads
   */
}
