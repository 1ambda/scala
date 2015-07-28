package tutorial

// ref: http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/T4_Barnes_Typelevel_Prog.pdf
object IntTypeSpec {
  import shapeless.test.illTyped

  type Int1 = IntN[Int0]
  type Int2 = IntN[Int1]
  type Int3 = IntN[Int2]

  implicitly[Int0 =:= Int0]
  illTyped("implicitly[Int0 =:= Int1]")

  // plus
  implicitly[Int0#plus[Int1] =:= Int1]
  implicitly[Int1#plus[Int1] =:= Int2]
  implicitly[Int1#plus[Int2] =:= Int3]
}

object IntListTypeSpec {
  val sum = ((1 :: 2 :: IntNil) ++ (3 :: IntNil))
  val expected = (6 :: IntNil)

  assert(sum == expected)
}