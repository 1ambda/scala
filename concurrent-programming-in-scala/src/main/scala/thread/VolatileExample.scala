package thread

object VolatileExample extends App with ThreadUtils {
  class Page(val text: String, var position: Int)

  val pages = for(i <- 1 to 10) yield
    new Page("Na" * (10000000 - 20 * i) + " Batman!", -1)

  @volatile var found = false

  for(p <- pages) yield thread {
    var i = 0

    while(i < p.text.length && !found) {
      if (p.text(i) == '!') {
        p.position = i
        found = true
      } else i += 1
    }
  }

  while (!found) {}
  log(s"position: ${pages map (_.position)}}")
}
