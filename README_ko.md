# stol — Scala Type Operator Library

TypeScript의 타입 연산자/유틸리티 타입을 Scala 3에 **가능한 한 같은 사용성으로** 옮긴 라이브러리.
매크로 없이 Scala 3.7+의 Named Tuple과 match type만으로 구현되어 있으며,
모든 연산자는 **타입 위치에 그대로** 쓸 수 있다 (`def f(p: Pick[Person, "name"])`).

[English README](README.md)

```scala
import stol.*

case class Person(name: String, age: Int, email: String)

type Keys = keyof[Person]                    // "name" | "age" | "email"
type Age  = Person Field "age"               // Int
type Slim = Pick[Person, "name" | "age"]     // (name: String, age: Int)
type Edit = Partial[Omit[Person, "email"]]   // (name: Option[String], age: Option[Int])

val p = Person("kim", 30, "kim@x.io").toNamedTuple
val slim: Slim = p.pick["name" | "age"]      // 값 레벨도 같은 모양
val (name = n, age = a) = slim               // destructuring
```

## 요구 사항

- Scala **3.7 이상** (Named Tuple이 stable이 된 버전)
- 빌드/테스트: [scala-cli](https://scala-cli.virtuslab.org) — `scala-cli test .`

## TS ↔ stol 대응표

TS의 object type은 Scala **named tuple**에 대응한다. 입력 `T`에는 case class와 named tuple을
모두 쓸 수 있고(`NamedTuple.From`으로 정규화), 결과는 항상 named tuple이다.

### Object 연산자 (`objects.scala`)

| TypeScript | stol | 비고 |
|---|---|---|
| `keyof T` | `keyof[T]` | 전위 타입 연산자가 없어 `[ ]` 사용 |
| — | `keysOf[T]`, `valuesOf[T]` | 키/값을 튜플로. `Record`, `Exclude` 등과 조합용 |
| `T["k"]` | `T Field "k"` | `infix type` |
| `Pick<T, K>` | `Pick[T, K]` | K는 리터럴 union |
| `Omit<T, K>` | `Omit[T, K]` | |
| `Partial<T>` | `Partial[T]` | 값을 `Option`으로 감쌈 |
| `Required<T>` | `Required[T]` | `Option` 벗김 |
| `Readonly<T>` | `Readonly[T]` | named tuple은 이미 불변 → 항등 |
| `Record<K, V>` | `Record[K, V]` | **K는 튜플** `("a", "b")` (아래 한계 참고) |
| `{ [K in keyof T]: F<K, T[K]> }` | `Mapped[T, [K] =>> ...]` | `in keyof T`는 `T`에 담김. 본문에서 `T Field K` 사용 |
| `{ [K in keyof T as N<K>]: F<K> }` | `MappedAs[T, [K] =>> ..., [K] =>> ...]` | 키 람다 + 값 람다 |
| `as never` (필드 제거) | 람다에서 `Skip` 반환 | `Nothing`으로는 안 됨 → 아래 "Mapped type 읽는 법" 참고 |
| 값만 바꿀 때 | `MapValues[T, F]` | `MapValues[Person, List]` (축약형) |
| 키만 바꿀 때 | `MapKeys[T, F]` | `MapKeys[Person, [k] =>> "get" + Capitalize[k]]` (축약형) |
| `A & B` (object) | `A Merge B` | A의 순서 유지, 중복 키는 B가 덮어씀 |
| `A extends B ? X : Y` | `A match { case B => X; case _ => Y }` | Scala 내장 |
| `infer U` | match type의 소문자 타입 변수 | Scala 내장 |

### Mapped type 읽는 법

TS의 mapped type은 세 조각으로 되어 있고, stol에서 각각 이렇게 대응된다.

| TS 조각 | stol | 뜻 |
|---|---|---|
| `[K in keyof T]` | `[K] =>>` | 키를 하나씩 `K`에 바인딩 (`keyof T`는 `Mapped[T, …]`의 `T`) |
| `T[K]` | `T Field K` | 그 키의 값 타입 |
| `as NewKey` | `MappedAs`의 첫 번째 람다 | 키 이름 바꾸기 |
| `never` | `Skip` | 이 필드는 결과에서 제외 |

**1. 값만 바꾸기** — 모든 필드를 `Option`으로

```scala
// TS
type Opt = { [K in keyof Person]: Option<Person[K]> }
// stol
type Opt = Mapped[Person, [K] =>> Option[Person Field K]]
//                ^^^^^^  ^^^         ^^^^^^^^^^^^^^
//                keyof   K in        Person[K]
// 결과: (name: Option[String], age: Option[Int], email: Option[String])
```

**2. 키 이름 바꾸기** — getter 시그니처 만들기

```scala
// TS
type Getters = { [K in keyof Person as `get${Capitalize<K>}`]: () => Person[K] }
// stol: 첫 람다 = 새 키, 둘째 람다 = 값
type Getters = MappedAs[Person,
  [K] =>> "get" + Capitalize[K],   // as `get${Capitalize<K>}`
  [K] =>> () => Person Field K]    // () => Person[K]
// 결과: (getName: () => String, getAge: () => Int, getEmail: () => String)
```

**3. 조건으로 필드 거르기** — 값이 `String`인 필드만 남기기. 제네릭 `T`에도 그대로 쓸 수 있다.

```scala
// TS
type StringFields<T> = { [K in keyof T as T[K] extends string ? K : never]: T[K] }
// stol: 값 람다가 Skip을 돌려주면 그 필드는 빠진다
type StringFields[T] = Mapped[T, [K] =>> (T Field K) match
  case String => T Field K   // 유지
  case _      => Skip]       // 제외  (TS의 never)
// StringFields[Person] = (name: String, email: String)
```

키 람다에서도 같은 방식으로 거를 수 있다: `[K] =>> K match { case "email" => Skip; case _ => K }`.
`never` 대신 `Skip`인 이유는 아래 "알아둘 한계" 7번 참고.

### 집합 연산자 (`sets.scala`)

| TypeScript | stol | 비고 |
|---|---|---|
| `Exclude<T, U>` | `Exclude[T, U]` / `ExcludeU[T, U]` | **T는 튜플**, U는 union 가능. `*U`는 결과를 union으로 |
| `Extract<T, U>` | `Extract[T, U]` / `ExtractU[T, U]` | |
| `NonNullable<T>` | `NonNullable[T]` | 튜플에서 `Null`, `None.type` 제거 |

### 문자열 연산자 (`strings.scala`)

| TypeScript | stol |
|---|---|
| `` `${A}${B}` `` | `A + B` (`scala.compiletime.ops.string.+`) |
| `Uppercase<S>` / `Lowercase<S>` | `Uppercase[S]` / `Lowercase[S]` |
| `Capitalize<S>` / `Uncapitalize<S>` | `Capitalize[S]` / `Uncapitalize[S]` |

### 함수 연산자 (`functions.scala`)

| TypeScript | stol | 비고 |
|---|---|---|
| `ReturnType<F>` | `ReturnType[F]` | `Function1`(튜플 인자 포함)에서 환원 |
| `Parameters<F>` | `Parameters[F]` | 〃 |
| 임의 arity | `Fn[F]` | `(using fn: Fn[F]): fn.Return` — 0..22 arity |
| `ConstructorParameters<C>` | `ConstructorParameters[C]` | case class → **named** tuple |
| `Awaited<T>` | `Awaited[T]` | 중첩 `Future` 벗김 |

### 값 레벨 (`values.scala`)

named tuple은 런타임에도 존재하므로 object 연산자마다 같은 이름의 값 연산이 있다.
결과 타입은 타입 연산자가 계산하고, 필드 위치는 컴파일 타임에 결정되므로 런타임 이름 탐색은 없다.

| 값 연산 | 타입 |
|---|---|
| `caseClass.toNamedTuple` | `From[P]` |
| `t.pick["a" \| "b"]`, `t.omit["a"]` | `Pick[T, K]`, `Omit[T, K]` |
| `t.field["a"]`, `t.keys` | `T Field "a"`, `List[String]` |
| `t.partial`, `t.required` | `Partial[T]`, `Required[T]` |
| `t.merge(u)` | `T Merge U` |

## `Record<string, V>` 는 범위 밖

TS의 `Record<K, V>`는 `K`에 따라 두 가지 다른 타입이 된다.

```ts
Record<"a" | "b", number>   // 닫힌 객체 { a: number; b: number }
Record<string, number>      // 열린 딕셔너리 { [key: string]: number }
```

닫힌 쪽은 `stol.Record`가 대응한다. 열린 쪽(index signature, `ParsedUrlQuery` 등)은 키를 미리 알 수 없는
딕셔너리 선언이라 타입 연산 대상이 아니며, Scala에서는 `Map`을 쓴다.

```scala
type AB = Record[("a", "b"), Int]                      // (a: Int, b: Int)
type ParsedUrlQuery = Map[String, String | Seq[String]] // { [key: string]: string | string[] }
```

## 알아둘 한계 (언어 수준)

1. **match type은 union 위에 분배되지 않는다.** TS의 `Exclude<A | B, B>`는 조건부 타입의 분배 덕에 동작하지만,
   Scala에서 `(A | B) match { case B => ... }`는 환원되지 않는다. 그래서 `Exclude`/`Extract`/`Record`는
   **튜플**을 받는다. `keyof` 결과에 쓰는 흔한 경우는 `keysOf[T]`로 대체하면 된다:
   `Pick[T, ExcludeU[keysOf[T], "id"]]`.
2. **refinement 타입 `{ val name: String }`은 입력으로 받을 수 없다.** `NamedTuple.From`이 지원하지 않는다.
   case class 또는 named tuple을 쓴다.
3. **함수 타입의 arity 분기가 안 된다.** `FunctionN`은 trait라 `Function0` 패턴과 `Function2` scrutinee의
   disjoint를 컴파일러가 증명하지 못한다. 그래서 `ReturnType`은 `Function1`용이고, 다중 arity는 `Fn[F]`를 쓴다.
4. **`Awaited[T]`에서 `T`가 `final`이 아닌 클래스면 환원되지 않는다** (3과 같은 이유). `final case class`는 된다.
5. 전위 타입 연산자 문법이 없어 `keyof T`가 아니라 `keyof[T]`이고, `T["k"]`는 `T Field "k"`이다.
6. **타입 람다 파라미터의 바운드는 추론되지 않는다.** stol의 `Field`와 문자열 연산자는 바운드 없는 `K`를 받도록
   되어 있어 보통 `[K] =>>`면 충분하지만, `K`를 표준 `compiletime.ops`에 직접 넘길 때(`"get" + K` 등)는
   `[K <: String] =>>`처럼 바운드가 필요하다. TS처럼 읽히는 `[K <: keyof[T]] =>>`도 허용된다.
7. **`Nothing`이 match type의 검사 대상(scrutinee)이 되면 환원되지 않는다.** 모든 case에 매치되기 때문이다.
   그래서 TS의 `as never`(필드 제거)는 `Skip` 마커로 표현한다 ("Mapped type 읽는 법" 참고).

## 라이선스

MIT — [LICENSE](LICENSE) 참고.

## 레이아웃

```
project.scala            scala-cli 설정 (Scala 3.7.4, munit)
src/stol/objects.scala   keyof, Field, Pick, Omit, Partial, Required, Record, Mapped, MappedAs, Skip, Merge
src/stol/sets.scala      Exclude, Extract, NonNullable
src/stol/strings.scala   Uppercase, Lowercase, Capitalize, Uncapitalize
src/stol/functions.scala ReturnType, Parameters, Fn, ConstructorParameters, Awaited
src/stol/values.scala    pick / omit / field / partial / required / merge / toNamedTuple
test/stol/               munit 테스트 (대부분 `summon[A =:= B]` 컴파일 타임 검증)
```
