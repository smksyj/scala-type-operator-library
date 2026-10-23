package stol

// ---------------------------------------------------------------------------
// Set operators (TS `Exclude`, `Extract`, ...)
//
// TS distributes conditional types over unions; Scala match types do not, so
// `(A | B) match { case B => ... }` is stuck.  These operators therefore work
// on *tuples* of types (e.g. `keysOf[T]`) and accept a union as the filter.
// Convert back with `Tuple.Union` (or use the `*U` aliases).
// ---------------------------------------------------------------------------

/** TS `Exclude<T, U>` on a tuple.  `Exclude[("a", "b", "c"), "b"]  =:=  ("a", "c")` */
type Exclude[T <: Tuple, U] <: Tuple = T match
  case EmptyTuple => EmptyTuple
  case h *: t => h match
    case U => Exclude[t, U]
    case _ => h *: Exclude[t, U]

/** TS `Extract<T, U>` on a tuple.  `Extract[("a", "b", "c"), "a" | "c"]  =:=  ("a", "c")` */
type Extract[T <: Tuple, U] <: Tuple = T match
  case EmptyTuple => EmptyTuple
  case h *: t => h match
    case U => h *: Extract[t, U]
    case _ => Extract[t, U]

/** `Exclude` returning a union, for use where TS would give one: `ExcludeU[keysOf[T], "id"]` */
type ExcludeU[T <: Tuple, U] = Tuple.Union[Exclude[T, U]]

/** `Extract` returning a union. */
type ExtractU[T <: Tuple, U] = Tuple.Union[Extract[T, U]]

/** TS `NonNullable<T>` on a tuple: drops `Null` and `None.type` members. */
type NonNullable[T <: Tuple] = Exclude[T, Null | None.type]
