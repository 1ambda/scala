package free.github

/**
 * ref - http://underscore.io/blog/posts/2015/04/14/free-monads-are-simple.html
 */

import scalaz._, Scalaz._, Free._


object Github {
  type User = String
  type Repository = String
  type Language = String

  trait GithubServiceOp[A]
  final case class GetRepository(user: User) extends GithubServiceOp[List[Repository]]
  final case class GetLanguageSet(repositories: List[Repository]) extends GithubServiceOp[Set[Language]]
  final case class CreateRepository(user: User,
                                    repository: Repository) extends GithubServiceOp[Boolean]

  final case class GetLanguage(user: User,
                               repository: Repository) extends GithubServiceOp[Set[Language]]

  type GithubServiceCoyo[A] = Coyoneda[GithubServiceOp, A]
  type GithubService[A] = Free[GithubServiceCoyo, A]

  /** primitives */
  def getRepository(user: User): FreeC[GithubServiceOp, List[Repository]] =
    Free.liftFC(GetRepository(user))

  def getLanguageSet(repositories: List[Repository]): FreeC[GithubServiceOp, Set[Language]] =
    Free.liftFC(GetLanguageSet(repositories))

  def createRepository(user: User,
                       repository: Repository): FreeC[GithubServiceOp, Boolean] =
    Free.liftFC(CreateRepository(user, repository))

  def getLanguage(user: User, repository: Repository) =
    Free.liftFC(GetLanguage(user, repository))
}

object Application {
  import Github._

  val user = "1ambda"

  val program = for {
    repos <- getRepository(user)
    langs <- getLanguageSet(repos)
  } yield langs
}


