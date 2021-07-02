package no.nav.rekrutteringsbistand.api.option

import arrow.core.Either
import arrow.core.getOrElse

/**
 * Hvorfor implementere Option på denne måten: https://github.com/arrow-kt/arrow-core/issues/114
 */
typealias Option<A> = Either<Unit, A>
typealias None = Either.Left<Unit>
typealias Some<A> = Either.Right<A>

/**
 * Hvorfor implementere Option på denne måten: https://github.com/arrow-kt/arrow-core/issues/114
 */
fun <A> optionOf(nullable: A?): Option<A> =
    if (nullable == null) Either.left(Unit) else Either.right(nullable)

/**
 * Appliser en prosedyre/kommando på verdien hvis den finnes
 */
fun <A> Option<A>.forEach(action: (A) -> Unit) {
    this.map(action)
}

fun <A> Some<A>.get(): A =
    this.getOrElse { throw NullPointerException("Should never reach this point") }
