package chapter8


trait Prop {
  import Prop._

  def &&(p: Prop): Prop = new Prop {
    override def check: Either[(FailedCase, SuccessCount), SuccessCount] =
      Prop.this.check match {
        case Left(failedCase: FailedCase, successCount: SuccessCount) => p.check match {
          case Left(otherFailedCase: FailedCase, otherSuccessCount: SuccessCount) =>
            Left(failedCase + otherFailedCase, successCount + otherSuccessCount)
          case Right(otherSuccessCount) =>
            Left(failedCase, successCount + otherSuccessCount)
        }

        case Right(successCount: SuccessCount) => p.check match {
          case Left(otherFailedCase: FailedCase, otherSuccessCount: SuccessCount) =>
            Left(otherFailedCase, successCount + otherSuccessCount)
          case Right(otherSuccessCount) =>
            Right(successCount + otherSuccessCount)
        }
      }

  }

  def check: Either[(String, Int), Int]
}

object Prop {
  type FailedCase = String
  type SuccessCount = Int
}
