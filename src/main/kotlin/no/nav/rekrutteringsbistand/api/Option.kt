package no.nav.rekrutteringsbistand.api

import arrow.core.Either

typealias Option<A> = Either<Unit, A>
typealias None = Either.Left<Unit>
typealias Some<A> = Either.Right<A>

/**
 * Inspirasjon hentet fra https://github.com/arrow-kt/arrow-core/issues/114
 */
fun <A> Option(a: A?): Option<A> = if (a == null) Either.Left(Unit) else Either.right(a)
