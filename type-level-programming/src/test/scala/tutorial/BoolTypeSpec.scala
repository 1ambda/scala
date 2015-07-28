package tutorial

// ref: http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/T4_Barnes_Typelevel_Prog.pdf
object BoolTypeSpec {
  import shapeless.test.illTyped

  implicitly[TrueType   =:= TrueType]
  implicitly[FalseType  =:= FalseType]

  // Not
  implicitly[TrueType#Not   =:= FalseType]
  implicitly[FalseType#Not  =:= TrueType]

  // Or
  implicitly[TrueType#Or[TrueType]    =:= TrueType]
  implicitly[TrueType#Or[FalseType]   =:= TrueType]
  implicitly[FalseType#Or[TrueType]   =:= TrueType]
  implicitly[FalseType#Or[FalseType]  =:= FalseType]

  // compiles only if string DOESN'T compile
  illTyped("implicitly[TrueType   =:= FalseType]")
  illTyped("implicitly[FalseType  =:= TrueType]")
}


