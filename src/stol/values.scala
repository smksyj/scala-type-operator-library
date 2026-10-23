package stol

import scala.compiletime.{constValue, constValueTuple, erasedValue}
import scala.deriving.Mirror
import scala.NamedTuple.{AnyNamedTuple, Names, From}

// ---------------------------------------------------------------------------
// Value-level counterparts
//
// TS erases types, so `Pick` only exists in the type world.  Scala named
// tuples exist at runtime, so every type operator on objects gets a matching
// value operator: `p.pick["name" | "age"]` has type `Pick[Person, "name" | "age"]`.
//
// Everything a type operator already knows is reused here at compile time:
// the result's field names come from the type operator, each field's source
// position comes from `IndexOf` via `constValue`, and the inline recursion
// over the result names unrolls into straight-line `arr(i) = src.productElement(j)`
// code.  No name lookup, hashing or collection building happens at runtime.
// ---------------------------------------------------------------------------

extension [T <: AnyNamedTuple](t: T)
  inline def pick[K <: String]: Pick[T, K] = select[T, Pick[T, K]](t)
  inline def omit[K <: String]: Omit[T, K] = select[T, Omit[T, K]](t)
  inline def field[K]: T Field K =
    product(t).productElement(constValue[IndexOf[keysOf[T], K, 0]]).asInstanceOf[T Field K]
  inline def keys: List[String] = constValueTuple[keysOf[T]].toList.asInstanceOf[List[String]]
  inline def partial: Partial[T] = mapAll[Tuple.Size[keysOf[T]]](product(t), Some(_)).asInstanceOf[Partial[T]]
  /** Only unwraps `Some`; a `None` field is passed through as is. */
  inline def required: Required[T] =
    mapAll[Tuple.Size[keysOf[T]]](product(t), { case Some(v) => v; case other => other }).asInstanceOf[Required[T]]
  inline def merge[U <: AnyNamedTuple](u: U): T Merge U =
    val arr = new Array[Object](constValue[Tuple.Size[Names[T Merge U]]])
    fillMerge[keysOf[T], keysOf[U], Names[T Merge U]](product(t), product(u), arr, 0)
    Tuple.fromArray(arr).asInstanceOf[T Merge U]

/** The same operations directly on case classes (no `toNamedTuple` needed). Named tuples are not
 *  `Product`s, so this never overlaps with the block above; results are always named tuples. */
extension [P <: Product](p: P)(using m: Mirror.ProductOf[P])
  /** Case class → named tuple. */
  inline def toNamedTuple: From[P] = Tuple.fromProductTyped(p).asInstanceOf[From[P]]
  inline def pick[K <: String]: Pick[P, K] = select[P, Pick[P, K]](p)
  inline def omit[K <: String]: Omit[P, K] = select[P, Omit[P, K]](p)
  inline def field[K]: P Field K =
    p.productElement(constValue[IndexOf[keysOf[P], K, 0]]).asInstanceOf[P Field K]
  inline def keys: List[String] = constValueTuple[keysOf[P]].toList.asInstanceOf[List[String]]
  inline def partial: Partial[P] = mapAll[Tuple.Size[keysOf[P]]](p, Some(_)).asInstanceOf[Partial[P]]
  inline def merge[U <: AnyNamedTuple](u: U): P Merge U =
    val arr = new Array[Object](constValue[Tuple.Size[Names[P Merge U]]])
    fillMerge[keysOf[P], keysOf[U], Names[P Merge U]](p, product(u), arr, 0)
    Tuple.fromArray(arr).asInstanceOf[P Merge U]

// --- internals --------------------------------------------------------------

/** Named tuples erase to plain tuples, which are `Product`s. */
private inline def product(t: Any): Product = t.asInstanceOf[Product]

/** Builds `R` by copying each of its fields out of `src` at a position computed at compile time. */
private inline def select[S, R <: AnyNamedTuple](src: S): R =
  val arr = new Array[Object](constValue[Tuple.Size[Names[R]]])
  fill[keysOf[S], Names[R]](product(src), arr, 0)
  Tuple.fromArray(arr).asInstanceOf[R]

/** Unrolls to one `arr(pos) = src.productElement(IndexOf[k])` per wanted name. */
private inline def fill[SrcKeys <: Tuple, Wanted <: Tuple](src: Product, arr: Array[Object], inline pos: Int): Unit =
  inline erasedValue[Wanted] match
    case _: EmptyTuple => ()
    case _: (k *: ks) =>
      arr(pos) = src.productElement(constValue[IndexOf[SrcKeys, k, 0]]).asInstanceOf[Object]
      fill[SrcKeys, ks](src, arr, pos + 1)

/** Like `fill`, but each name is taken from `u` when `u` has it, otherwise from `t`. */
private inline def fillMerge[TK <: Tuple, UK <: Tuple, Wanted <: Tuple](
    t: Product, u: Product, arr: Array[Object], inline pos: Int): Unit =
  inline erasedValue[Wanted] match
    case _: EmptyTuple => ()
    case _: (k *: ks) =>
      inline if constValue[Tuple.Contains[UK, k]] then
        arr(pos) = u.productElement(constValue[IndexOf[UK, k, 0]]).asInstanceOf[Object]
      else
        arr(pos) = t.productElement(constValue[IndexOf[TK, k, 0]]).asInstanceOf[Object]
      fillMerge[TK, UK, ks](t, u, arr, pos + 1)

private inline def mapAll[N <: Int](src: Product, f: Any => Any): Tuple =
  val n = constValue[N]
  val arr = new Array[Object](n)
  var i = 0
  while i < n do
    arr(i) = f(src.productElement(i)).asInstanceOf[Object]
    i += 1
  Tuple.fromArray(arr)
