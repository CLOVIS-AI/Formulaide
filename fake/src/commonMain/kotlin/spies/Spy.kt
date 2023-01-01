package opensavvy.formulaide.fake.spies

import arrow.core.Either
import opensavvy.logger.Logger
import opensavvy.logger.Logger.Companion.trace
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
	val (result, duration) = measureTimedValue {
		try {
			block()
		} catch (e: Throwable) {
			log.trace { "$functionName(${arguments.joinToString(separator = ", ")})  #[FAILED]#>  $e" }
			throw e
		}
	}

	log.trace(
		when (result) {
			is Either.Right<*> -> result.value
			is Either.Left<*> -> result.value
			else -> result
		}
	) { "$functionName(${arguments.joinToString(separator = ", ")})  #[${duration.toString(DurationUnit.MILLISECONDS)}]#> " }

	return result
}
