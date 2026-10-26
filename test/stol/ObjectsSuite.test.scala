package stol

import scala.compiletime.ops.string.+
import scala.compiletime.testing.typeCheckErrors

class ObjectsSuite extends munit.FunSuite:
  case class Person(name: String, age: Int, email: String)
  type PersonNT = (name: String, age: Int, email: String)

  test("keyof / keysOf / valuesOf"):
    summon[keyof[Person] =:= ("name" | "age" | "email")]
    summon[keyof[PersonNT] =:= ("name" | "age" | "email")]
    summon[keysOf[Person] =:= ("name", "age", "email")]
    summon[valuesOf[Person] =:= (String, Int, String)]

  test("Field (indexed access)"):
    summon[(Person Field "age") =:= Int]
    summon[(PersonNT Field "name") =:= String]

  test("Pick / Omit"):
    summon[Pick[Person, "name" | "age"] =:= (name: String, age: Int)]
    summon[Pick[Person, "email"] =:= (email: String)]
    summon[Omit[Person, "email"] =:= (name: String, age: Int)]
    summon[Omit[Person, "name" | "email"] =:= (age: Int)]
    summon[Omit[Person, keyof[Person]] =:= NamedTuple.NamedTuple[EmptyTuple, EmptyTuple]]

  test("Pick / Omit reject unknown keys"):
    assert(typeCheckErrors("""val x: Pick[Person, "nmae"] = (nmae = "kim")""").nonEmpty)
    assert(typeCheckErrors("""val x: Pick[Person, "name" | "nmae"] = (name = "kim")""").nonEmpty)
    assert(typeCheckErrors("""val x: Omit[Person, "emial"] = (name = "kim", age = 1, email = "e")""").nonEmpty)

  test("Pick / Omit in generic aliases"):
    type WithoutId[T] = Pick[T, ExcludeU[keysOf[T], "id"]]
    type DropId[T] = Omit[T, "id"]
    case class Row(id: Long, v: Int)
    summon[WithoutId[Row] =:= (v: Int)]
    summon[DropId[Row] =:= (v: Int)]

  test("Partial / Required / Readonly"):
    summon[Partial[Person] =:= (name: Option[String], age: Option[Int], email: Option[String])]
    summon[Required[Partial[Person]] =:= PersonNT]
    summon[Required[(a: Option[Int], b: String)] =:= (a: Int, b: String)]
    summon[Readonly[Person] =:= PersonNT]

  test("Record"):
    summon[Record[("a", "b"), Int] =:= (a: Int, b: Int)]
    summon[Record[keysOf[Person], Boolean] =:= (name: Boolean, age: Boolean, email: Boolean)]

  test("Field with a computed key"):
    type Id[K <: String] = K match { case _ => K }
    summon[(Person Field Id["age"]) =:= Int]
    summon[(Person Field ("a" + "ge")) =:= Int]
    summon[Pick[Person, Id["name"]] =:= (name: String)]
    assert(typeCheckErrors("summon[(Person Field \"nope\") =:= Int]").nonEmpty)

  test("Mapped: K is bound, TS `{ [K in keyof T]: ... }`"):
    summon[Mapped[Person, [K] =>> Option[Person Field K]] =:= (name: Option[String], age: Option[Int], email: Option[String])]
    summon[Mapped[Person, [K] =>> K] =:= (name: "name", age: "age", email: "email")]
    type Nullable[T] = Mapped[T, [K] =>> (T Field K) | Null]
    summon[Nullable[Person] =:= (name: String | Null, age: Int | Null, email: String | Null)]

  test("Mapped: bounded lambdas are accepted too"):
    summon[Mapped[Person, [K <: keyof[Person]] =>> Option[Person Field K]] =:= (name: Option[String], age: Option[Int], email: Option[String])]
    summon[Mapped[Person, [K <: String] =>> "f_" + K] =:= (name: "f_name", age: "f_age", email: "f_email")]
    type Nullable[T] = Mapped[T, [K <: keyof[T]] =>> (T Field K) | Null]
    summon[Nullable[Person] =:= (name: String | Null, age: Int | Null, email: String | Null)]

  test("MappedAs: key remapping, TS `as`"):
    summon[MappedAs[Person, [K] =>> "get" + Capitalize[K], [K] =>> () => Person Field K]
           =:= (getName: () => String, getAge: () => Int, getEmail: () => String)]
    summon[MappedAs[Person, Uppercase, [K] =>> Option[Person Field K]]
           =:= (NAME: Option[String], AGE: Option[Int], EMAIL: Option[String])]

  test("Skip drops a field, TS `as never`"):
    // by key:   { [K in keyof T as K extends "email" ? never : K]: T[K] }
    summon[MappedAs[Person, [K] =>> K match { case "email" => Skip; case _ => K }, [K] =>> Person Field K]
           =:= (name: String, age: Int)]
    // by value: { [K in keyof T as T[K] extends string ? K : never]: T[K] }
    type StringFields[T] = Mapped[T, [K] =>> (T Field K) match { case String => T Field K; case _ => Skip }]
    summon[StringFields[Person] =:= (name: String, email: String)]

  test("MapValues / MapKeys shorthands"):
    summon[MapValues[Person, List] =:= (name: List[String], age: List[Int], email: List[String])]
    summon[MapKeys[Person, [k] =>> "get" + Capitalize[k]] =:= (getName: String, getAge: Int, getEmail: String)]

  test("Merge"):
    summon[((a: Int, b: String) Merge (c: Boolean)) =:= (a: Int, b: String, c: Boolean)]
    summon[((a: Int, b: String) Merge (b: Long)) =:= (a: Int, b: Long)]
    summon[(Person Merge (id: Long)) =:= (name: String, age: Int, email: String, id: Long)]

  test("composition"):
    type Patch = Partial[Omit[Person, "email"]]
    summon[Patch =:= (name: Option[String], age: Option[Int])]
    summon[Pick[Partial[Person], "age"] =:= (age: Option[Int])]
