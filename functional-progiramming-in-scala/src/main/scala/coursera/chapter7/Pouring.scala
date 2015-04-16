package coursera.chapter7

class Pouring(capacity: Vector[Int]) {

  // State
  type State = Vector[Int]
  val initialState = capacity map { _ => 0 }

  // Move
  trait Move {
    def change(s: State): State
  }
  case class Empty(glass: Int) extends Move {
    def change(s: State) = s updated(glass, 0)
  }
  case class Fill(glass: Int) extends Move {
    def change(s: State) = s updated(glass, capacity(glass))
  }
  case class Pour(from: Int, to: Int) extends Move {
    def change(s: State) = {
      val amount = s(from) min (capacity(to) - s(to))
      s updated (from, s(from) - amount) updated (to, s(to) + amount)
    }
  }

  val glasses = 0 until capacity.length

  val moves =
    (for (glass <- glasses) yield Empty(glass)) ++
    (for (glass <- glasses) yield Fill(glass)) ++
    (for (from <- glasses; to <- glasses if from != to) yield Pour(from, to))

  // Path

  class Path(history: List[Move], val endState: State) {
    def extend(move: Move): Path = new Path(move :: history, move change endState)
    override def toString = "\n" + (history.reverse mkString " ") + "-->" + endState
  }

  val initialPath = new Path(Nil, initialState)

  def from(paths: Set[Path], explored: Set[State]): Stream[Set[Path]] = {
    if (paths.isEmpty) Stream.empty
    else {
      val more = for {
        path <- paths
        next <- moves map path.extend
      } yield next

      paths #:: from(more, explored ++ (more map (_.endState)))
    }
  }

  val pathSets = from(Set(initialPath), Set(initialState))

  def solutions(target: Int): Stream[Path] = {
    for {
      pathSet <- pathSets
      path <- pathSet
      if path.endState contains target
    } yield path
  }
}

