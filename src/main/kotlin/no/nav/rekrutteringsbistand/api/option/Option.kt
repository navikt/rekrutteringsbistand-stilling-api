package no.nav.rekrutteringsbistand.api.option

import arrow.core.Either

/**
 * Hvorfor implementere Option på denne måten: https://github.com/arrow-kt/arrow-core/issues/114
 */
typealias Option<A> = Either<Unit, A>
typealias None = Either.Left<Unit>
typealias Some<A> = Either.Right<A>

/**
 * Hvorfor implementere Option på denne måten: https://github.com/arrow-kt/arrow-core/issues/114
 */
fun <A> Option(nullable: A?): Option<A> =
    if (nullable == null) Either.left(Unit) else Either.right(nullable)
