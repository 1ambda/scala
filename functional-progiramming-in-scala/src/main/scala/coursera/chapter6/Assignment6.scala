package coursera.chapter6

object Anagrams {

  type Word = String

  type Sentence = List[Word]

  type Occurrences = List[(Char, Int)]

  val dictionaryPath = List("forcomp", "linuxwords.txt")

  def loadDictionary = {
    val wordstream = Option {
      getClass.getClassLoader.getResourceAsStream(dictionaryPath.mkString("/"))
    } getOrElse {
      sys.error("Could not load word list, dictionary file not found")
    }
    try {
      val s = io.Source.fromInputStream(wordstream)
      s.getLines.toList
    } catch {
      case e: Exception =>
        println("Could not load word list: " + e)
        throw e
    } finally {
      wordstream.close()
    }
  }

  val dictionary: List[Word] = loadDictionary

  def wordOccurrences(w: Word): Occurrences = {
    w.toLowerCase.toList.sorted.groupBy(c => c).
      values.map { cs => (cs.head, cs.length )}.toList.
      sortBy { case(c, n) => c }
  }

  def sentenceOccurrences(s: Sentence): Occurrences = wordOccurrences(s.mkString)

  lazy val dictionaryByOccurrences: Map[Occurrences, List[Word]] = {
    (dictionary groupBy { w => wordOccurrences(w) }).withDefaultValue(Nil)
  }

  /** Returns all the anagrams of a given word. */
  def wordAnagrams(word: Word): List[Word] =
    dictionaryByOccurrences(wordOccurrences(word))

  def combinations(occurrences: Occurrences): List[Occurrences] = occurrences match {
    case Nil => List(List())
    case (c, n) :: occrs => {
      // combinatorial search is analoguos to the n-queens problem
      val nextSet = combinations(occrs) // caching the result of recursive call
      (for {
        subset <- nextSet
        i <- 1 to n
      } yield (c, i) :: subset) ++ nextSet
    }
  }

  def subtract(xs: Occurrences, ys: Occurrences): Occurrences = {
    def subOccurs(xs: Map[Char, Int], y: (Char, Int)): Map[Char, Int] = {
      val (c, n) = y
      val v = xs(c) - n
      if (v == 0) xs - c else xs + (c ->  v)
    }

    ((ys.toMap foldLeft xs.toMap)(subOccurs)).toList.sortBy {
      case (c, n) => c
    }
  }

  def sentenceAnagrams(sentence: Sentence): List[Sentence] = {

    def recur(occurs: Occurrences): List[Sentence] = occurs match {
      case Nil => List(List())
      case _ => for {
        comb <- combinations(occurs)
        dicWord <- dictionaryByOccurrences.getOrElse(comb, Nil)
        prev <- recur(subtract(occurs, wordOccurrences(dicWord)))
      } yield dicWord :: prev
    }

    recur(sentenceOccurrences(sentence))
  }

  def sentenceAnagramsMemo(st: Sentence): List[Sentence] = ???
}
