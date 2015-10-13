package monad_transformer

import scalaz._, Scalaz._

case class User(name: String, repositories: List[Repository])
case class Repository(name: String, languages: List[Language])
case class Language(name: String, line: Long)


object GithubService {
  def findLanguage1(users: List[User],
                    userName: String,
                    repoName: String, 
                    langName: String): Option[Language] =
    for {
      u <- users          find { _.name === userName }
      r <- u.repositories find { _.name === repoName }
      l <- r.languages    find { _.name === langName }
    } yield l

  type LangState[A] = State[List[Language], A]

def findLanguage2(users: List[User],
                  userName: String,
                  repoName: String,
                  langName: String): LangState[Option[Language]] =
  for {
    optUser <- (users.find { _.name === userName }).point[LangState]
    optRepository <- (
      optUser match {
        case Some(u) => u.repositories.find(_.name === repoName)
        case None => none[Repository] // same as Option.empty[Repository]
      }).point[LangState]
    optLanguage <- (optRepository match {
      case Some(r) => r.languages.find(_.name === langName)
      case None    => none[Language]
    }).point[LangState]
    _ <- modify { langs: List[Language] => optLanguage match {
      case Some(l) => l :: langs
      case None    => langs
    }}
  } yield optLanguage


}
