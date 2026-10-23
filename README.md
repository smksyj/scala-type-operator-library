# stol — Scala Type Operator Library

TypeScript's type operators and utility types, ported to Scala 3 with **as close to the same ergonomics as possible**.
Built on Scala 3.7+ named tuples and match types only — no macros — so every operator can be used
**directly in type position** (`def f(p: Pick[Person, "name"])`).

[한국어 README](README_ko.md)

```scala
import stol.*

case class Person(name: String, age: Int, email: String)

type Keys = keyof[Person]                    // "name" | "age" | "email"
type Age  = Person Field "age"               // Int
type Slim = Pick[Person, "name" | "age"]     // (name: String, age: Int)
type Edit = Partial[Omit[Person, "email"]]   // (name: Option[String], age: Option[Int])

val p = Person("kim", 30, "kim@x.io").toNamedTuple
val slim: Slim = p.pick["name" | "age"]      // the value level mirrors the type level
val (name = n, age = a) = slim               // destructuring
```

## Requirements

- Scala **3.7 or later** (the version in which named tuples became stable)
- Build/test: [scala-cli](https://scala-cli.virtuslab.org) — `scala-cli test .`

## TS ↔ stol mapping

A TS object type corresponds to a Scala **named tuple**. The input `T` may be a case class or a named tuple
(both are normalised through `NamedTuple.From`); the result is always a named tuple.

### Object operators (`objects.scala`)

| TypeScript | stol | Notes |
|---|---|---|
| `keyof T` | `keyof[T]` | Scala has no prefix type operators, hence `[ ]` |
| — | `keysOf[T]`, `valuesOf[T]` | keys / values as tuples, for composing with `Record`, `Exclude`, ... |
| `T["k"]` | `T Field "k"` | `infix type` |
| `Pick<T, K>` | `Pick[T, K]` | `K` is a union of literals |
| `Omit<T, K>` | `Omit[T, K]` | |
| `Partial<T>` | `Partial[T]` | wraps every value in `Option` |
| `Required<T>` | `Required[T]` | unwraps `Option` |
| `Readonly<T>` | `Readonly[T]` | named tuples are already immutable → identity |
| `Record<K, V>` | `Record[K, V]` | **`K` is a tuple** `("a", "b")` (see limitations) |
| `{ [K in keyof T]: F<K, T[K]> }` | `Mapped[T, [K] =>> ...]` | `in keyof T` is implied by `T`; use `T Field K` in the body |
| `{ [K in keyof T as N<K>]: F<K> }` | `MappedAs[T, [K] =>> ..., [K] =>> ...]` | key lambda + value lambda |
| `as never` (drop a field) | return `Skip` from the lambda | `Nothing` does not work → see "Reading a mapped type" |
| values only | `MapValues[T, F]` | `MapValues[Person, List]` (shorthand) |
| keys only | `MapKeys[T, F]` | `MapKeys[Person, [k] =>> "get" + Capitalize[k]]` (shorthand) |
| `A & B` (objects) | `A Merge B` | keeps `A`'s order; `B` overrides duplicate keys |
| `A extends B ? X : Y` | `A match { case B => X; case _ => Y }` | built into Scala |
| `infer U` | lower-case type variable in a match type | built into Scala |

### Reading a mapped type

A TS mapped type has three parts, each with a stol counterpart.

| TS part | stol | Meaning |
|---|---|---|
| `[K in keyof T]` | `[K] =>>` | bind each key to `K` (`keyof T` is the `T` of `Mapped[T, …]`) |
| `T[K]` | `T Field K` | the value type of that key |
| `as NewKey` | first lambda of `MappedAs` | rename the key |
| `never` | `Skip` | leave this field out |

**1. Change values only** — wrap every field in `Option`

```scala
// TS
type Opt = { [K in keyof Person]: Option<Person[K]> }
// stol
type Opt = Mapped[Person, [K] =>> Option[Person Field K]]
//                ^^^^^^  ^^^         ^^^^^^^^^^^^^^
//                keyof   K in        Person[K]
// result: (name: Option[String], age: Option[Int], email: Option[String])
```

**2. Rename keys** — derive getter signatures

```scala
// TS
type Getters = { [K in keyof Person as `get${Capitalize<K>}`]: () => Person[K] }
// stol: first lambda = new key, second lambda = value
type Getters = MappedAs[Person,
  [K] =>> "get" + Capitalize[K],   // as `get${Capitalize<K>}`
  [K] =>> () => Person Field K]    // () => Person[K]
// result: (getName: () => String, getAge: () => Int, getEmail: () => String)
```

**3. Filter fields by a condition** — keep only `String`-valued fields. Works for a generic `T` too.

```scala
// TS
type StringFields<T> = { [K in keyof T as T[K] extends string ? K : never]: T[K] }
// stol: a field whose value lambda returns Skip is dropped
type StringFields[T] = Mapped[T, [K] =>> (T Field K) match
  case String => T Field K   // keep
  case _      => Skip]       // drop  (TS never)
// StringFields[Person] = (name: String, email: String)
```

The key lambda can filter the same way: `[K] =>> K match { case "email" => Skip; case _ => K }`.
For why it is `Skip` rather than `never`, see limitation 7 below.

### Set operators (`sets.scala`)

| TypeScript | stol | Notes |
|---|---|---|
| `Exclude<T, U>` | `Exclude[T, U]` / `ExcludeU[T, U]` | **`T` is a tuple**, `U` may be a union; the `*U` form returns a union |
| `Extract<T, U>` | `Extract[T, U]` / `ExtractU[T, U]` | |
| `NonNullable<T>` | `NonNullable[T]` | removes `Null` and `None.type` from a tuple |

### String operators (`strings.scala`)

| TypeScript | stol |
|---|---|
| `` `${A}${B}` `` | `A + B` (`scala.compiletime.ops.string.+`) |
| `Uppercase<S>` / `Lowercase<S>` | `Uppercase[S]` / `Lowercase[S]` |
| `Capitalize<S>` / `Uncapitalize<S>` | `Capitalize[S]` / `Uncapitalize[S]` |

### Function operators (`functions.scala`)

| TypeScript | stol | Notes |
|---|---|---|
| `ReturnType<F>` | `ReturnType[F]` | reduces for `Function1` (including tupled arguments) |
| `Parameters<F>` | `Parameters[F]` | same |
| any arity | `Fn[F]` | `(using fn: Fn[F]): fn.Return` — arities 0..22 |
| `ConstructorParameters<C>` | `ConstructorParameters[C]` | case class → **named** tuple |
| `Awaited<T>` | `Awaited[T]` | unwraps nested `Future` |

### Value level (`values.scala`)

Named tuples exist at runtime, so every object operator has a value-level twin with the same name.
The result type is computed by the type operator, and field positions are resolved at compile time,
so there is no name lookup at runtime.

| Value operation | Type |
|---|---|
| `caseClass.toNamedTuple` | `From[P]` |
| `t.pick["a" \| "b"]`, `t.omit["a"]` | `Pick[T, K]`, `Omit[T, K]` |
| `t.field["a"]`, `t.keys` | `T Field "a"`, `List[String]` |
| `t.partial`, `t.required` | `Partial[T]`, `Required[T]` |
| `t.merge(u)` | `T Merge U` |

## `Record<string, V>` is out of scope

TS's `Record<K, V>` is two different kinds of type depending on `K`.

```ts
Record<"a" | "b", number>   // closed object { a: number; b: number }
Record<string, number>      // open dictionary { [key: string]: number }
```

The closed form is what `stol.Record` covers. The open form (index signatures, `ParsedUrlQuery`, ...) declares a
dictionary whose keys are not known in advance; that is not a type operation, and in Scala it is simply a `Map`.

```scala
type AB = Record[("a", "b"), Int]                      // (a: Int, b: Int)
type ParsedUrlQuery = Map[String, String | Seq[String]] // { [key: string]: string | string[] }
```

## Known limitations (language level)

1. **Match types do not distribute over unions.** TS's `Exclude<A | B, B>` works because conditional types
   distribute; in Scala `(A | B) match { case B => ... }` does not reduce. `Exclude`/`Extract`/`Record` therefore
   take **tuples**. For the common case of filtering `keyof` results, use `keysOf[T]` instead:
   `Pick[T, ExcludeU[keysOf[T], "id"]]`.
2. **Refinement types `{ val name: String }` are not accepted as input.** `NamedTuple.From` does not support them;
   use a case class or a named tuple.
3. **Function types cannot be dispatched on arity.** `FunctionN` are traits, so the compiler cannot prove a
   `Function2` scrutinee disjoint from a `Function0` pattern. `ReturnType` is therefore for `Function1`;
   use `Fn[F]` for other arities.
4. **`Awaited[T]` does not reduce when `T` is a non-`final` class** (same reason as 3). A `final case class` works.
5. There is no prefix type operator syntax, so it is `keyof[T]` rather than `keyof T`, and `T Field "k"` rather than `T["k"]`.
6. **Type lambda parameter bounds are not inferred.** stol's `Field` and string operators accept an unbounded `K`,
   so `[K] =>>` is normally enough; a bound such as `[K <: String] =>>` is needed only when `K` is passed straight
   to a standard `compiletime.ops` operator (e.g. `"get" + K`). The TS-flavoured `[K <: keyof[T]] =>>` is accepted too.
7. **A match type whose scrutinee is `Nothing` does not reduce**, because `Nothing` matches every case.
   TS's `as never` (dropping a field) is therefore expressed with the `Skip` marker (see "Reading a mapped type").

## License

MIT — see [LICENSE](LICENSE).

## Layout

```
project.scala            scala-cli configuration (Scala 3.7.4, munit)
src/stol/objects.scala   keyof, Field, Pick, Omit, Partial, Required, Record, Mapped, MappedAs, Skip, Merge
src/stol/sets.scala      Exclude, Extract, NonNullable
src/stol/strings.scala   Uppercase, Lowercase, Capitalize, Uncapitalize
src/stol/functions.scala ReturnType, Parameters, Fn, ConstructorParameters, Awaited
src/stol/values.scala    pick / omit / field / partial / required / merge / toNamedTuple
test/stol/               munit tests (mostly compile-time checks via `summon[A =:= B]`)
```
