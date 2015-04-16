package coursera.chapter4

abstract class cBoolean {

  def IfThenElse[T](t: T, e: T): T

  def &&(other: cBoolean) = IfThenElse(other, False)
  def ||(other: cBoolean) = IfThenElse(True, other)
  def unary_! : cBoolean = IfThenElse(False, True)

  def ==(other: cBoolean) = IfThenElse(other, other.unary_!)
  def !=(other: cBoolean) = IfThenElse(other.unary_!, other)

  def <(other: cBoolean) = IfThenElse(False, other)
  def >(other: cBoolean) = IfThenElse(other.unary_!, False)
}

object True extends cBoolean {
  def IfThenElse[T](t: T, e:T) = t
}

object False extends cBoolean {
  def IfThenElse[T](t: T, e:T) = e
}

object Assert {
  def apply(b: cBoolean) = if (b == True) true else false
}

abstract class Nat {
  def isZero: Boolean
  def predecessor: Nat
  def successor = new Succ(this)
  def + (that: Nat): Nat
  def - (that: Nat): Nat
  def number = {
    def count(n: Int, succ: Nat): Int = {
      if (succ == Zero) n
      else count(n + 1, succ.predecessor)
    }

    count(0, this)
  }
}

object Zero extends Nat {
  def isZero = true
  def predecessor = throw new RuntimeException("Zero.predecessor");
  def + (that: Nat) = that
  def - (that: Nat) = {
    if (that.isZero) this
    else throw new RuntimeException("Zero.-")
  }
}

class Succ(n: Nat) extends Nat {
  def isZero = false
  def predecessor: Nat = n
  def + (that: Nat) = new Succ(n + that)
  def - (that: Nat) = if(that.isZero) this else n - that.predecessor
  // def - (that: nat) = new Succ(n - that)
  // will not work
  // think about one - one,
  // in - function it will be expanded like new Succ(Zero - one)
  // which makes run-time exception
}

// val one = new Succ(Zero)
// Zero + one == one
// one + one

