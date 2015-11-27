package free.withoutScalaz

import Common._

sealed trait Free[F[_], A] extends {
  def flatMap[B](f: A => Free[F, B]): Free[F, B]
}

