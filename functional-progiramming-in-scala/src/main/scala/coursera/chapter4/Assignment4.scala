package coursera.chapter4.Assignment

object Assignment4 {

  abstract class CodeTree
  case class Fork(left: CodeTree, right: CodeTree, chars: List[Char], weight: Int) extends CodeTree
  case class Leaf(char: Char, weight: Int) extends CodeTree

  def weight(tree: CodeTree): Int = tree match {
    case Leaf(_, weight) => weight
    case Fork(_, _, _, weight) => weight
  }

  def chars(tree: CodeTree): List[Char] = tree match {
    case Leaf(char, _) => List(char)
    case Fork(_, _, chars, _) => chars
  }

  def makeCodeTree(left: CodeTree, right: CodeTree) =
    Fork(left, right, chars(left) ::: chars(right), weight(left) + weight(right))

  def string2Chars(str: String): List[Char] = str.toList

  // def times(chars: List[Char]): List[(Char, Int)] = chars match {
  //     case List() => Nil
  //     case xs: List[Char] => 
  //       val char = xs.head
  //       val (first, rest) = xs partition { _ == char }
  //       val length = first.length
  //       List((char, length)) ++ times(rest)
  // }
  def times(chars: List[Char]): List[(Char, Int)] = chars.groupBy(c => c).mapValues(_.size).toList

  // def makeOrderedLeafList(freqs: List[(Char, Int)]): List[Leaf] = freqs match {
  //     case Nil => Nil
  //     case (c, w) :: xs => insert(Leaf(c, w), makeOrderedLeafList(xs))
  // }

  // def insert(x: Leaf, xs: List[Leaf]): List[Leaf] = xs match {
  //   case Nil => List(x)
  //   case y :: ys => {
  //     if (weight(x) <= weight(y)) x :: xs
  //     else y :: insert(x, ys)
  //   }
  // }

  def makeOrderedLeafList(freqs: List[(Char, Int)]): List[Leaf] =
    freqs.sortBy(_._2).map { x => Leaf(x._1, x._2) }

  def singleton(trees: List[CodeTree]): Boolean = trees match {
    case Nil => false
    case List(tree) => true
    case tree :: tress => false
  }

  def combine(trees: List[CodeTree]): List[CodeTree] = (singleton(trees) match {
    case true => trees
    case false => trees match {
      case Nil => Nil
      case x :: xs =>
        makeCodeTree(x, xs.head) :: xs.tail
    }
  }).sortBy(weight(_))

  def until(p: List[CodeTree] => Boolean, reduce: List[CodeTree] => List[CodeTree])
    (xs: List[CodeTree]): List[CodeTree] = {

    if (p(xs)) xs 
    else until(p, reduce)(reduce(xs))
  }

  def createCodeTree(chars: List[Char]): CodeTree = {
    until(singleton, combine)(makeOrderedLeafList(times(chars))).head
  }

  // Part 3: Decoding

  type Bit = Int

  def decode(tree: CodeTree, bits: List[Bit]): List[Char] = bits match {
    case List() => Nil
    case _ => {
      val (chars, restBits) = decodeChar(tree, bits)
      chars :: decode(tree, restBits)
    }
  }

  def decodeChar(tree: CodeTree, bits: List[Bit]): (Char, List[Bit]) = tree match {
    case Leaf(c, w) => (c, bits)
    case Fork(left, right, cs, w) => {
      if (bits.head == 0) decodeChar(left, bits.tail)
      else decodeChar(right, bits.tail)
    }
  }

  val frenchCode: CodeTree = Fork(Fork(Fork(Leaf('s',121895),Fork(Leaf('d',56269),Fork(Fork(Fork(Leaf('x',5928),Leaf('j',8351),List('x','j'),14279),Leaf('f',16351),List('x','j','f'),30630),Fork(Fork(Fork(Fork(Leaf('z',2093),Fork(Leaf('k',745),Leaf('w',1747),List('k','w'),2492),List('z','k','w'),4585),Leaf('y',4725),List('z','k','w','y'),9310),Leaf('h',11298),List('z','k','w','y','h'),20608),Leaf('q',20889),List('z','k','w','y','h','q'),41497),List('x','j','f','z','k','w','y','h','q'),72127),List('d','x','j','f','z','k','w','y','h','q'),128396),List('s','d','x','j','f','z','k','w','y','h','q'),250291),Fork(Fork(Leaf('o',82762),Leaf('l',83668),List('o','l'),166430),Fork(Fork(Leaf('m',45521),Leaf('p',46335),List('m','p'),91856),Leaf('u',96785),List('m','p','u'),188641),List('o','l','m','p','u'),355071),List('s','d','x','j','f','z','k','w','y','h','q','o','l','m','p','u'),605362),Fork(Fork(Fork(Leaf('r',100500),Fork(Leaf('c',50003),Fork(Leaf('v',24975),Fork(Leaf('g',13288),Leaf('b',13822),List('g','b'),27110),List('v','g','b'),52085),List('c','v','g','b'),102088),List('r','c','v','g','b'),202588),Fork(Leaf('n',108812),Leaf('t',111103),List('n','t'),219915),List('r','c','v','g','b','n','t'),422503),Fork(Leaf('e',225947),Fork(Leaf('i',115465),Leaf('a',117110),List('i','a'),232575),List('e','i','a'),458522),List('r','c','v','g','b','n','t','e','i','a'),881025),List('s','d','x','j','f','z','k','w','y','h','q','o','l','m','p','u','r','c','v','g','b','n','t','e','i','a'),1486387)

  val secret: List[Bit] = List(0,0,1,1,1,0,1,0,1,1,1,0,0,1,1,0,1,0,0,1,1,0,1,0,1,1,0,0,1,1,1,1,1,0,1,0,1,1,0,0,0,0,1,0,1,1,1,0,0,1,0,0,1,0,0,0,1,0,0,0,1,0,1)

  def decodedSecret: List[Char] = decode(frenchCode, secret)

  def encode(tree: CodeTree)(text: List[Char]): List[Bit] = text match {
    case List() => Nil
    case t :: ts => encodeChar(tree, t) ++ encode(tree)(ts)
   }

  def encodeChar(tree: CodeTree, t: Char): List[Bit] = tree match {
    case Leaf(c, w) => Nil
    case Fork(l, r, cs, w) => {
      if (chars(r).contains(t)) 1 :: encodeChar(r, t)
      else 0 :: encodeChar(l, t)
    }
  }

  type CodeTable = List[(Char, List[Bit])]

  def codeBits(table: CodeTable)(char: Char): List[Bit] = table match {
    case Nil => throw new Error("Non-Exist Symbol")
    case x :: xs => if (x._1 == char) x._2 else codeBits(table.tail)(char)
  }

  def convert(tree: CodeTree): CodeTable = tree match {
    case Leaf(c, w) => List((c, Nil))
    case Fork(l, r, cs, w) => {
      val leftTable = convert(l)
      val rightTable = convert(r)

      mergeCodeTables(addBit(leftTable, 0), addBit(rightTable, 1))
    }
  }

  // helper function for `convert`
  def addBit(codeTable: CodeTable, bit: Bit): CodeTable = {
    codeTable match {
      case Nil => Nil
      case (char, bits) :: xs => (char, bit :: bits) :: addBit(xs, bit)
    }
  }

  def mergeCodeTables(a: CodeTable, b: CodeTable): CodeTable = a ++ b

  def quickEncode(tree: CodeTree)(text: List[Char]): List[Bit] = {
    def quickEncodeUsingTable(table: CodeTable, text: List[Char]): List[Bit] = text match {
      case List() => Nil
      case t :: ts => codeBits(table)(t) ++ quickEncodeUsingTable(table, ts)
    }

    quickEncodeUsingTable(convert(tree), text)
  }
}
