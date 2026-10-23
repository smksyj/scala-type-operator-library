package stol

import scala.concurrent.Future
import scala.compiletime.ops.string.+
import scala.compiletime.testing.typeCheckErrors

class SetsSuite extends munit.FunSuite:
  case class Person(name: String, age: Int, email: String)

  test("Exclude / Extract on tuples"):
    summon[Exclude[("a", "b", "c"), "b"] =:= ("a", "c")]
    summon[Exclude[("a", "b", "c"), "b" | "c"] =:= Tuple1["a"]]
    summon[Extract[("a", "b", "c"), "a" | "c"] =:= ("a", "c")]
    summon[Exclude[(Int, String, Boolean), String] =:= (Int, Boolean)]

  test("ExcludeU / ExtractU give unions, composable with Pick"):
    summon[ExcludeU[keysOf[Person], "email"] =:= ("name" | "age")]
    summon[Pick[Person, ExcludeU[keysOf[Person], "email"]] =:= (name: String, age: Int)]

  test("NonNullable"):
    summon[NonNullable[(Int, Null, String, None.type)] =:= (Int, String)]

  test("documented limitation: match types do not distribute over unions"):
    type NaiveExclude[T, U] = T match { case U => Nothing; case _ => T }
    val errors = typeCheckErrors("""summon[NaiveExclude["a" | "b", "b"] =:= "a"]""")
    assert(errors.nonEmpty)

class StringsSuite extends munit.FunSuite:
  test("Uppercase / Lowercase"):
    summon[Uppercase["hello"] =:= "HELLO"]
    summon[Uppercase["Hello World 1"] =:= "HELLO WORLD 1"]
    summon[Lowercase["HeLLo"] =:= "hello"]
    summon[Uppercase[""] =:= ""]

  test("Capitalize / Uncapitalize"):
    summon[Capitalize["name"] =:= "Name"]
    summon[Uncapitalize["Name"] =:= "name"]
    summon[Capitalize["ñ"] =:= "ñ"]

  test("template literal via +"):
    summon[("get" + Capitalize["name"]) =:= "getName"]

class FunctionsSuite extends munit.FunSuite:
  // `final` lets the compiler prove Person is disjoint from the Future pattern in Awaited.
  final case class Person(name: String, age: Int)

  test("ReturnType / Parameters (Function1 and tupled)"):
    summon[ReturnType[Int => String] =:= String]
    summon[Parameters[Int => String] =:= Tuple1[Int]]
    summon[ReturnType[((Int, String)) => Boolean] =:= Boolean]
    summon[Parameters[((Int, String)) => Boolean] =:= (Int, String)]

  test("Fn for any arity"):
    val f0 = summon[Fn[() => Int]]
    summon[f0.Return =:= Int]
    summon[f0.Params =:= EmptyTuple]
    val f2 = summon[Fn[(Int, String) => Boolean]]
    summon[f2.Return =:= Boolean]
    summon[f2.Params =:= (Int, String)]
    val f3 = summon[Fn[(Int, Int, Int) => Long]]
    summon[f3.Params =:= (Int, Int, Int)]

  test("Fn as a dependent result type"):
    def returnOf[F](f: F)(using fn: Fn[F]): Option[fn.Return] = None
    val r: Option[Boolean] = returnOf((a: Int, b: String) => a > 0)
    assertEquals(r, None)

  test("ConstructorParameters / Awaited"):
    summon[ConstructorParameters[Person] =:= (name: String, age: Int)]
    summon[Awaited[Future[Future[Int]]] =:= Int]
    summon[Awaited[Int] =:= Int]
    summon[Awaited[Future[Person]] =:= Person]
