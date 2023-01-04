package opensavvy.formulaide.fake.spies

import arrow.core.Either
import opensavvy.logger.Logger
import opensavvy.logger.Logger.Companion.debug
import opensavvy.logger.Logger.Companion.trace
import opensavvy.logger.Logger.Companion.warn
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
inline fun <T> spy(
	log: Logger,
	functionName: String,
	vararg arguments: Any?,
	block: () -> T,
): T {
	val call = "$functionName(${arguments.joinToString(separator = ", ")})"
	log.trace { "$call…" }

	val (result, duration) = measureTimedValue {
		try {
			block()
		} catch (e: Throwable) {
			log.warn { "$call  #[FAILED]#>  $e" }
			throw e
		}
	}

	log.debug(
		when (result) {
			is Either.Right<*> -> result.value
			is Either.Left<*> -> result.value
			else -> result
		}
	) { "$call  #[${duration.toString(DurationUnit.MILLISECONDS)}]#> " }

	return result
}
