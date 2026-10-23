package stol

import scala.compiletime.ops.string.{+, Length, Substring, CharAt}
import scala.compiletime.ops.any.ToString

// ---------------------------------------------------------------------------
// String literal operators (TS intrinsic string manipulation types)
//
// Template literals: TS `${A}${B}` is `A + B` (from scala.compiletime.ops.string).
//
// The input `S` is unbounded so that these can be applied to an unbounded
// mapped-type lambda parameter (`[K] =>> Capitalize[K]`).  `Str[S]` coerces it
// to the `<: String` the standard ops require; on a string literal it is the identity.
// ---------------------------------------------------------------------------

/** TS `Uppercase<S>`.  `Uppercase["hello"]  =:=  "HELLO"` */
type Uppercase[S] <: String = Length[Str[S]] match
  case 0 => ""
  case _ => ToString[UpChar[CharAt[Str[S], 0]]] + Uppercase[Substring[Str[S], 1, Length[Str[S]]]]

/** TS `Lowercase<S>`. */
type Lowercase[S] <: String = Length[Str[S]] match
  case 0 => ""
  case _ => ToString[LowChar[CharAt[Str[S], 0]]] + Lowercase[Substring[Str[S], 1, Length[Str[S]]]]

/** TS `Capitalize<S>`.  `Capitalize["name"]  =:=  "Name"` */
type Capitalize[S] <: String = Length[Str[S]] match
  case 0 => ""
  case _ => ToString[UpChar[CharAt[Str[S], 0]]] + Substring[Str[S], 1, Length[Str[S]]]

/** TS `Uncapitalize<S>`. */
type Uncapitalize[S] <: String = Length[Str[S]] match
  case 0 => ""
  case _ => ToString[LowChar[CharAt[Str[S], 0]]] + Substring[Str[S], 1, Length[Str[S]]]

// --- internals --------------------------------------------------------------

/** Bound-free coercion to a `String` constant type (identity on string literals). */
type Str[S] = ToString[S]

type UpChar[C <: Char] <: Char = C match
  case 'a' => 'A' case 'b' => 'B' case 'c' => 'C' case 'd' => 'D' case 'e' => 'E'
  case 'f' => 'F' case 'g' => 'G' case 'h' => 'H' case 'i' => 'I' case 'j' => 'J'
  case 'k' => 'K' case 'l' => 'L' case 'm' => 'M' case 'n' => 'N' case 'o' => 'O'
  case 'p' => 'P' case 'q' => 'Q' case 'r' => 'R' case 's' => 'S' case 't' => 'T'
  case 'u' => 'U' case 'v' => 'V' case 'w' => 'W' case 'x' => 'X' case 'y' => 'Y'
  case 'z' => 'Z' case _ => C

type LowChar[C <: Char] <: Char = C match
  case 'A' => 'a' case 'B' => 'b' case 'C' => 'c' case 'D' => 'd' case 'E' => 'e'
  case 'F' => 'f' case 'G' => 'g' case 'H' => 'h' case 'I' => 'i' case 'J' => 'j'
  case 'K' => 'k' case 'L' => 'l' case 'M' => 'm' case 'N' => 'n' case 'O' => 'o'
  case 'P' => 'p' case 'Q' => 'q' case 'R' => 'r' case 'S' => 's' case 'T' => 't'
  case 'U' => 'u' case 'V' => 'v' case 'W' => 'w' case 'X' => 'x' case 'Y' => 'y'
  case 'Z' => 'z' case _ => C
