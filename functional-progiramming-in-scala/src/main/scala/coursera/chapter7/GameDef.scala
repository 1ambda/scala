package coursera.chapter7

trait GameDef {

  case class Pos(x: Int, y: Int) {
    def dx(d: Int) = copy(x = x + d)
    def dy(d: Int) = copy(y = y + d)
  }

  val startPos: Pos
  val goal: Pos

  type Terrain = Pos => Boolean
  val terrain: Terrain

  sealed abstract class Move
  case object Left  extends Move
  case object Right extends Move
  case object Up    extends Move
  case object Down  extends Move

  def startBlock: Block = Block(startPos, startPos)

  /**
   * A block is represented by the position of the two cubes that
   * it consists of. We make sure that `b1` is lexicographically

   */
  case class Block(b1: Pos, b2: Pos) {

    // checks the requirement mentioned above
    require(b1.x <= b2.x && b1.y <= b2.y, "Invalid block position: b1=" + b1 + ", b2=" + b2)

    def dx(d1: Int, d2: Int) = Block(b1.dx(d1), b2.dx(d2))
    def dy(d1: Int, d2: Int) = Block(b1.dy(d1), b2.dy(d2))

    def left = if (isStanding)         dy(-2, -1)
               else if (b1.x == b2.x)  dy(-1, -2)
               else                    dy(-1, -1)

    def right = if (isStanding)        dy(1, 2)
                else if (b1.x == b2.x) dy(2, 1)
                else                   dy(1, 1)

    def up = if (isStanding)           dx(-2, -1)
             else if (b1.x == b2.x)    dx(-1, -1)
             else                      dx(-1, -2)

    def down = if (isStanding)         dx(1, 2)
               else if (b1.x == b2.x)  dx(1, 1)
               else                    dx(2, 1)


    def neighbors: List[(Block, Move)] =
      List((left, Left), (right, Right), (up, Up), (down, Down))

    def legalNeighbors: List[(Block, Move)] =
      neighbors.filter(_._1.isLegal)

    def isStanding: Boolean = b1.x == b2.x && b1.y == b2.y

    def isLegal: Boolean = terrain(b1) && terrain(b2)
  }
}
