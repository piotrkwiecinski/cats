package cats

import cats.instances.list._
import simulacrum.typeclass

/**
 * Applicative functor.
 *
 * Allows application of a function in an Applicative context to a value in an Applicative context
 *
 * See: [[https://www.cs.ox.ac.uk/jeremy.gibbons/publications/iterator.pdf The Essence of the Iterator Pattern]]
 * Also: [[http://staff.city.ac.uk/~ross/papers/Applicative.pdf Applicative programming with effects]]
 *
 * Must obey the laws defined in cats.laws.ApplicativeLaws.
 */
@typeclass trait Applicative[F[_]] extends Apply[F] { self =>

  /**
   * `pure` lifts any value into the Applicative Functor.
   *
   * Applicative[Option].pure(10) = Some(10)
   */
  def pure[A](x: A): F[A]

  /**
   * Returns an `F[Unit]` value, equivalent with `pure(())`.
   *
   * A useful shorthand, also allowing implementations to optimize the
   * returned reference (e.g. it can be a `val`).
   */
  def unit: F[Unit] = pure(())

  override def map[A, B](fa: F[A])(f: A => B): F[B] =
    ap(pure(f))(fa)

  /**
   * Given `fa` and `n`, apply `fa` `n` times to construct an `F[List[A]]` value.
   */
  def replicateA[A](n: Int, fa: F[A]): F[List[A]] =
    sequence(List.fill(n)(fa))

  def traverse[A, G[_], B](value: G[A])(f: A => F[B])(implicit G: Traverse[G]): F[G[B]] =
    G.traverse(value)(f)(this)

  def sequence[G[_], A](as: G[F[A]])(implicit G: Traverse[G]): F[G[A]] =
    G.sequence(as)(this)

  def compose[G[_]: Applicative]: Applicative[λ[α => F[G[α]]]] =
    new ComposedApplicative[F, G] {
      val F = self
      val G = Applicative[G]
    }

  /**
   * Returns the given argument if `cond` is `false`, otherwise, unit lifted into F.
   */
  def unlessA[A](cond: Boolean)(f: => F[A]): F[Unit] =
    if (cond) pure(()) else void(f)

  /**
   * Returns the given argument if `cond` is `true`, otherwise, unit lifted into F.
   */
  def whenA[A](cond: Boolean)(f: => F[A]): F[Unit] =
    if (cond) void(f) else pure(())
}

object Applicative {
  def monoid[F[_], A](implicit f: Applicative[F], monoid: Monoid[A]): Monoid[F[A]] =
    new ApplicativeMonoid[F, A](f, monoid)
}

private[cats] class ApplicativeMonoid[F[_], A](f: Applicative[F], monoid: Monoid[A]) extends ApplySemigroup(f, monoid) with Monoid[F[A]] {
  def empty: F[A] = f.pure(monoid.empty)
}
