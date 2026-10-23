package stol

class ValuesSuite extends munit.FunSuite:
  case class Person(name: String, age: Int, email: String)
  val kim = Person("kim", 30, "kim@x.io")
  val nt = kim.toNamedTuple

  test("toNamedTuple"):
    val typed: (name: String, age: Int, email: String) = nt
    assertEquals(typed.name, "kim")

  test("pick / omit produce values of the computed type"):
    val slim: (name: String, age: Int) = nt.pick["name" | "age"]
    assertEquals(slim, (name = "kim", age = 30))
    val noEmail: (name: String, age: Int) = nt.omit["email"]
    assertEquals(noEmail, slim)
    // Declaration order is kept regardless of the order of K, as in TS.
    val chained: (name: String, email: String) = nt.pick["email" | "name"].omit["age"]
    assertEquals(chained.email, "kim@x.io")

  test("field / keys"):
    val age: Int = nt.field["age"]
    assertEquals(age, 30)
    assertEquals(nt.keys, List("name", "age", "email"))

  test("partial / required round-trip"):
    val p: (name: Option[String], age: Option[Int], email: Option[String]) = nt.partial
    assertEquals(p.age, Some(30))
    val back: (name: String, age: Int, email: String) = p.required
    assertEquals(back, nt)

  test("merge"):
    val m: (name: String, age: Int, email: String, id: Long) = nt.merge((id = 7L))
    assertEquals(m.id, 7L)
    val shadowed: (name: String, age: Int, email: String) = nt.merge((age = 31))
    assertEquals(shadowed.age, 31)

  test("destructuring the result"):
    val (name = n, age = a) = nt.pick["name" | "age"]
    assertEquals((n, a), ("kim", 30))
