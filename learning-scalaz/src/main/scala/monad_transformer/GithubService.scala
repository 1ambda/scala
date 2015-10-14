package monad_transformer

import scalaz._, Scalaz._

case class User(name: String, repositories: List[Repository])
case class Repository(name: String, languages: List[Language])
case class Language(name: String, line: Long)

case class LanguageLookup(userName: String, repoName: String, langName: String)


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
        case Some(l) if (l.line >= 1000) =>  l :: langs
        case _                           => langs
      }}
    } yield optLanguage

  case class LangStateOption[A](run: LangState[Option[A]])

  implicit val LangStateOptionMonad = new Monad[LangStateOption] {
    override def point[A](a: => A): LangStateOption[A] =
      LangStateOption(a.point[Option].point[LangState])

    override def bind[A, B](fa: LangStateOption[A])(f: (A) => LangStateOption[B]): LangStateOption[B] =
      LangStateOption(fa.run.flatMap { (o: Option[A]) => o match {
        case Some(a) => f(a).run
        case None    => (none[B]).point[LangState] /* same as `(None: Option[B]).point[LangState]` */
      }})
  }

  def findLanguage3(users: List[User],
                    userName: String,
                    repoName: String,
                    langName: String): LangStateOption[Language] =
    for {
      u <- LangStateOption((users.find { _.name === userName }).point[LangState])
      r <- LangStateOption((u.repositories.find { _.name === repoName }).point[LangState])
      l <- LangStateOption((r.languages.find { _.name === langName }).point[LangState])
      _ <- LangStateOption((modify { langs: List[Language] =>
        if (l.line >= 1000) l :: langs else langs
      }) map (_ => none[Language]))
    } yield l

  def findLanguage(users: List[User],
                   userName: String,
                   repoName: String,
                   langName: String): OptionT[LangState, Language] =
    for {
      u <- OptionT((users.find { _.name === userName }).point[LangState])
      r <- OptionT((u.repositories.find { _.name === repoName }).point[LangState])
      l <- OptionT((r.languages.find { _.name === langName }).point[LangState])
      _ <- modify { langs: List[Language] =>
        if (l.line >= 1000) l :: langs else langs
      }.liftM[OptionT]
    } yield l

  def findLanguages1(users: List[User],
                     lookups: List[LanguageLookup]): OptionT[LangState, List[Language]] =
    lookups.traverseU { lookup =>
      findLanguage(users, lookup.userName, lookup.repoName, lookup.langName)
    }

  def findLanguages2(users: List[User],
                     lookups: List[LanguageLookup]): LangState[List[Option[Language]]] =
    lookups.traverseS { lookup =>
      findLanguage(users, lookup.userName, lookup.repoName, lookup.langName).run
    }
}
