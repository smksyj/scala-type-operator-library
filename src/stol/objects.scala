package stol

import scala.NamedTuple.{NamedTuple, Names, DropNames, From}
import scala.compiletime.ops.any.==
import scala.compiletime.ops.int.+

// ---------------------------------------------------------------------------
// Object (named tuple) operators
//
// TS object types map onto Scala named tuples. Any `T` accepted below may be a
// case class or a named tuple; `NamedTuple.From[T]` normalises both to
// `NamedTuple[Names, Values]`, and every result is a named tuple.
// ---------------------------------------------------------------------------

/** TS `keyof T`: union of field names.  `keyof[Person]  =:=  "name" | "age"`
 *
 *  `& String` keeps `keyof[T] <: String` visible even when `T` is abstract, so that a mapped-type
 *  lambda may optionally be bounded like TS's `K in keyof T`:  `[K <: keyof[T]] =>> ...`
 */
type keyof[T] = Tuple.Union[Names[From[T]]] & String

/** Tuple form of `keyof`, for operators that need to iterate the keys (`Record`, `Exclude`, ...). */
type keysOf[T] = Names[From[T]]

/** Values of `T` as a plain tuple, in declaration order. */
type valuesOf[T] = DropNames[From[T]]

/** TS `T[K]` (indexed access): the type of field `K`.  `Person Field "age"  =:=  Int`
 *  `K` is deliberately unbounded so that an unbounded mapped-type lambda `[K] =>> T Field K` type-checks. */
infix type Field[T, K] = Tuple.Elem[valuesOf[T], IndexOf[keysOf[T], K, 0]]

/** TS `Pick<T, K>`.  `Pick[Person, "name" | "age"]  =:=  (name: String, age: Int)` */
type Pick[T, K] = KnownKeys[T, K, FromPairs[FilterPairs[keysOf[T], valuesOf[T], K, true]]]

/** TS `Omit<T, K>`.  `Omit[Person, "email"]`  Unlike TS, an unknown key is an error here as well. */
type Omit[T, K] = KnownKeys[T, K, FromPairs[FilterPairs[keysOf[T], valuesOf[T], K, false]]]

/** TS `Partial<T>`: every value wrapped in `Option`. */
type Partial[T] = MapValues[T, Option]

/** TS `Required<T>`: inverse of `Partial`, unwraps `Option` values. */
type Required[T] = MapValues[T, Unopt]

/** TS `Readonly<T>`: named tuples are already immutable, so this is the identity. */
type Readonly[T] = From[T]

/** TS `Record<K, V>`.  Keys are a tuple of literals: `Record[("a", "b"), Int]  =:=  (a: Int, b: Int)` */
type Record[K <: Tuple, V] = NamedTuple[K, Tuple.Map[K, [_] =>> V]]

/** TS mapped type `{ [K in keyof T]: ... }`; the key is the lambda parameter, and `in keyof T`
 *  is implied by `T`:
 *
 *    Mapped[Person, [K] =>> Option[Person Field K]]   // { [K in keyof Person]: Option<Person[K]> }
 *
 *  Returning [[Skip]] drops the field (TS `never`).  A bounded lambda (`[K <: keyof[Person]]`,
 *  `[K <: String]`) is accepted too; it is only needed when `K` is passed straight to a
 *  `scala.compiletime.ops` operator such as `"get" + K`.
 */
type Mapped[T, F[_ <: keyof[T]]] = FromPairs[MapPairs[T, keysOf[T], [k] =>> k, F]]

/** TS `{ [K in keyof T as N<K>]: F<K> }`: `N` renames the key, `F` gives the value, both see `K`.
 *
 *    MappedAs[Person, [K] =>> "get" + Capitalize[K], [K] =>> () => Person Field K]
 *
 *  Returning [[Skip]] from either lambda drops the field.
 */
type MappedAs[T, N[_ <: keyof[T]], F[_ <: keyof[T]]] = FromPairs[MapPairs[T, keysOf[T], N, F]]

/** Returned from a `Mapped`/`MappedAs` lambda to drop that field — the role TS gives to `never`.
 *  (`Nothing` cannot be used: Scala refuses to reduce a match type whose scrutinee is `Nothing`.)
 */
final class Skip private ()

/** Shorthand for `Mapped` when only the value changes: `MapValues[Person, List]`. */
type MapValues[T, F[_]] = NamedTuple[keysOf[T], Tuple.Map[valuesOf[T], F]]

/** Shorthand for `MappedAs` when only the key changes: `MapKeys[Person, [k] =>> "get" + Capitalize[k]]`. */
type MapKeys[T, F[_] <: String] = NamedTuple[MapNames[keysOf[T], F], valuesOf[T]]

/** TS `A & B` on object types. Keeps `A`'s field order; `B` overrides duplicate keys in place and appends new ones. */
infix type Merge[A, B] = FromPairs[MergePairs[Pairs[keysOf[A], valuesOf[A]], Pairs[keysOf[B], valuesOf[B]]]]

// --- internals --------------------------------------------------------------

/** `R` when every key in `K` is a field of `T`. A misspelt key leaves this unreduced, which is a
 *  compile error wherever the type is used. Checked at reduction rather than as a `K <: keyof[T]`
 *  bound, so generic aliases such as `Pick[T, ExcludeU[keysOf[T], "id"]]` still type-check. */
type KnownKeys[T, K, R] = Ensure[K, Tuple.Union[keysOf[T]], R]

type Ensure[K, U, R] = K match
  case U => R

type Unopt[X] = X match
  case Option[a] => a
  case _ => X

/** Position of `K` in `Ks`. Compares with `==` rather than a pattern so that `K` may itself be a
 *  computed (match type) key; a missing key leaves the match type unreduced, i.e. a compile error. */
type IndexOf[Ks <: Tuple, K, I <: Int] <: Int = Ks match
  case k *: ks => (k == K) match
    case true => I
    case false => IndexOf[ks, K, I + 1]

/** The lambdas are bounded by `keyof[T]` so that `[K]`, `[K <: String]` and `[K <: keyof[T]]`
 *  all conform (a caller's lambda may be bounded more loosely than the parameter, never more tightly). */
type MapPairs[T, Ks <: Tuple, N[_ <: keyof[T]], F[_ <: keyof[T]]] <: Tuple = Ks match
  case EmptyTuple => EmptyTuple
  case k *: ks => N[k] match
    case Skip => MapPairs[T, ks, N, F]
    case _ => F[k] match
      case Skip => MapPairs[T, ks, N, F]
      case _ => (N[k], F[k]) *: MapPairs[T, ks, N, F]

/** Keeps `(name, value)` pairs whose name matches `K` when `Keep = true`, drops them when `false`. */
type FilterPairs[N <: Tuple, V <: Tuple, K, Keep <: Boolean] <: Tuple = (N, V) match
  case (EmptyTuple, EmptyTuple) => EmptyTuple
  case (n *: ns, v *: vs) => n match
    case K => Keep match
      case true => (n, v) *: FilterPairs[ns, vs, K, Keep]
      case false => FilterPairs[ns, vs, K, Keep]
    case _ => Keep match
      case true => FilterPairs[ns, vs, K, Keep]
      case false => (n, v) *: FilterPairs[ns, vs, K, Keep]

type Pairs[N <: Tuple, V <: Tuple] <: Tuple = (N, V) match
  case (EmptyTuple, EmptyTuple) => EmptyTuple
  case (n *: ns, v *: vs) => (n, v) *: Pairs[ns, vs]

type FromPairs[P <: Tuple] = NamedTuple[PairNames[P], PairValues[P]]

type PairNames[P <: Tuple] <: Tuple = P match
  case EmptyTuple => EmptyTuple
  case (n, _) *: ps => n *: PairNames[ps]

type PairValues[P <: Tuple] <: Tuple = P match
  case EmptyTuple => EmptyTuple
  case (_, v) *: ps => v *: PairValues[ps]

type MapNames[N <: Tuple, F[_] <: String] <: Tuple = N match
  case EmptyTuple => EmptyTuple
  case n *: ns => F[n] *: MapNames[ns, F]

type MergePairs[A <: Tuple, B <: Tuple] = Tuple.Concat[Override[A, B], Without[B, A]]

/** `A` with values replaced by `B`'s where the names coincide. */
type Override[A <: Tuple, B <: Tuple] <: Tuple = A match
  case EmptyTuple => EmptyTuple
  case (n, v) *: as => HasName[B, n] match
    case true => (n, PairLookup[B, n]) *: Override[as, B]
    case false => (n, v) *: Override[as, B]

/** Pairs of `B` whose names do not occur in `A`. */
type Without[B <: Tuple, A <: Tuple] <: Tuple = B match
  case EmptyTuple => EmptyTuple
  case (n, v) *: bs => HasName[A, n] match
    case true => Without[bs, A]
    case false => (n, v) *: Without[bs, A]

type PairLookup[P <: Tuple, K] = P match
  case (K, v) *: _ => v
  case _ *: ps => PairLookup[ps, K]

type HasName[P <: Tuple, K] <: Boolean = P match
  case EmptyTuple => false
  case (K, _) *: _ => true
  case _ *: ps => HasName[ps, K]
