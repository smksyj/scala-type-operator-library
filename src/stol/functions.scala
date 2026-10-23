package stol

import scala.concurrent.Future
import scala.NamedTuple.From

// ---------------------------------------------------------------------------
// Function operators (TS `ReturnType`, `Parameters`, `ConstructorParameters`, `Awaited`)
//
// Scala's FunctionN types are traits, so a match type cannot prove that a
// `Function2` scrutinee is disjoint from a `Function0` pattern and gets stuck
// on the first non-matching arity.  Two forms are therefore offered:
//
//   * `ReturnType[F]` / `Parameters[F]`  -- pure match types, reduce for
//     `Function1` (so also for the tupled convention `((A, B)) => R`).
//   * `Fn[F]`                             -- a given-based shape for arities
//     0..22, usable as a dependent type: `(using fn: Fn[F]): fn.Return`.
// ---------------------------------------------------------------------------

/** TS `ReturnType<F>` for single-argument / tupled functions. */
type ReturnType[F] = F match
  case Function1[_, r] => r

/** TS `Parameters<F>` for single-argument / tupled functions; always a tuple. */
type Parameters[F] <: Tuple = F match
  case Function1[a, _] => a match
    case Tuple => a & Tuple
    case _ => a *: EmptyTuple

/** TS `ConstructorParameters<C>` for a case class, as a *named* tuple. */
type ConstructorParameters[C] = From[C]

/** TS `Awaited<T>`: unwraps nested `Future`s. */
type Awaited[T] = T match
  case Future[a] => Awaited[a]
  case _ => T

/** Shape of a function type of any arity.  `summon[Fn[(Int, String) => Boolean]]` */
trait Fn[F]:
  type Params <: Tuple
  type Return

object Fn:
  type Aux[F, P <: Tuple, R] = Fn[F] { type Params = P; type Return = R }

  private def mk[F, P <: Tuple, R]: Aux[F, P, R] = new Fn[F]:
    type Params = P
    type Return = R
  given fn0[R]: Aux[() => R, EmptyTuple, R] = mk
  given fn1[A1, R]: Aux[A1 => R, A1 *: EmptyTuple, R] = mk
  given fn2[A1, A2, R]: Aux[(A1, A2) => R, A1 *: A2 *: EmptyTuple, R] = mk
  given fn3[A1, A2, A3, R]: Aux[(A1, A2, A3) => R, A1 *: A2 *: A3 *: EmptyTuple, R] = mk
  given fn4[A1, A2, A3, A4, R]: Aux[(A1, A2, A3, A4) => R, A1 *: A2 *: A3 *: A4 *: EmptyTuple, R] = mk
  given fn5[A1, A2, A3, A4, A5, R]: Aux[(A1, A2, A3, A4, A5) => R, A1 *: A2 *: A3 *: A4 *: A5 *: EmptyTuple, R] = mk
  given fn6[A1, A2, A3, A4, A5, A6, R]: Aux[(A1, A2, A3, A4, A5, A6) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: EmptyTuple, R] = mk
  given fn7[A1, A2, A3, A4, A5, A6, A7, R]: Aux[(A1, A2, A3, A4, A5, A6, A7) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: EmptyTuple, R] = mk
  given fn8[A1, A2, A3, A4, A5, A6, A7, A8, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: EmptyTuple, R] = mk
  given fn9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: EmptyTuple, R] = mk
  given fn10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: EmptyTuple, R] = mk
  given fn11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: EmptyTuple, R] = mk
  given fn12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: EmptyTuple, R] = mk
  given fn13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: EmptyTuple, R] = mk
  given fn14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: EmptyTuple, R] = mk
  given fn15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: EmptyTuple, R] = mk
  given fn16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: EmptyTuple, R] = mk
  given fn17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: EmptyTuple, R] = mk
  given fn18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: A18 *: EmptyTuple, R] = mk
  given fn19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: A18 *: A19 *: EmptyTuple, R] = mk
  given fn20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: A18 *: A19 *: A20 *: EmptyTuple, R] = mk
  given fn21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: A18 *: A19 *: A20 *: A21 *: EmptyTuple, R] = mk
  given fn22[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, R]: Aux[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) => R, A1 *: A2 *: A3 *: A4 *: A5 *: A6 *: A7 *: A8 *: A9 *: A10 *: A11 *: A12 *: A13 *: A14 *: A15 *: A16 *: A17 *: A18 *: A19 *: A20 *: A21 *: A22 *: EmptyTuple, R] = mk
