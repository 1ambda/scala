package reader_writer

trait HttpRequest {
  def url: String
}

case class GET(url: String) extends HttpRequest
case class POST(url: String, body: Map[String, String]) extends HttpRequest


