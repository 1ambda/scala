package selftype

import org.scalatest._

// http://marcus-christie.blogspot.kr/2014/03/scala-understanding-self-type.html
// https://coderwall.com/p/t_rapw/cake-pattern-in-scala-self-type-annotations-explicitly-typed-self-references-explained
// http://www.slideshare.net/LappleApple/scala-self-types-by-gregor-hein
class Self extends FlatSpec with Matchers {

  trait Connector {
    def getConnection: String 
  }

  trait MysqlConnector extends Connector {
    def getConnection = "Mysql Connection"
  }

  trait MongoConnector extends Connector {
    def getConnection = "Mongo Connection"
  }

  class UserService {
    self: Connector =>

    def getAllUsers() = {
      val con = getConnection
      // do something using the connection
      "users returned using " + con
    }
  }

  "UserServiceMongo" should "return users using mongo connector" in  {
    val UserServiceMongo = new UserService with MongoConnector
    UserServiceMongo.getAllUsers should be ("users returned using Mongo Connection")
  }

  "UserServiceMysql" should "return users using mysql connector" in  {
    val UserServiceMysql = new UserService with MysqlConnector
    UserServiceMysql.getAllUsers should be ("users returned using Mysql Connection")
  }
}
