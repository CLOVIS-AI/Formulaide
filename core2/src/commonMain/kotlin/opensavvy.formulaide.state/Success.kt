package opensavvy.formulaide.state

import kotlinx.coroutines.flow.*
import opensavvy.state.*

inline fun <I, O> State<I>.mapSuccess(crossinline transform: suspend (I) -> O): State<O> = map { slice ->
	slice.mapSuccess { transform(it) }
}

fun <I> State<I>.onEachSuccess(action: suspend (I) -> Unit): State<I> = onEach { slice ->
	(slice.status as? Status.Successful)
		?.let { action(it.value) }
}

fun <I> State<I>.onEachNotSuccess(action: suspend (Slice<Nothing>) -> Unit): State<I> = onEach { slice ->
	val (status, progression) = slice

	when (status) {
		is Status.Failed -> action(Slice(status, progression))
		is Status.Pending -> action(Slice(status, progression))
		is Status.Successful -> {}
	}
}

fun <I, O> State<I>.flatMapSuccess(transform: suspend StateBuilder<O>.(I) -> Unit): State<O> = transform {
	val (status, progression) = it

	when (status) {
		is Status.Failed -> emit(Slice(status, progression))
		is Status.Pending -> emit(Slice(status, progression))
		is Status.Successful -> emitAll(state { transform(status.value) })
	}
}

inline fun <I, O> Slice<I>.mapSuccess(transform: (I) -> O): Slice<O> {
	val (status, progression) = this

	val newStatus: Status<O> = when (status) {
		is Status.Failed -> status
		is Status.Pending -> status
		is Status.Successful -> Status.Successful(transform(status.value))
	}

	return Slice(newStatus, progression)
}

fun <T> List<Slice<T>>.flatten(): Slice<List<T>> {
	val result = map {
		val (status, progression) = it

		when (status) {
			is Status.Failed -> return Slice(status, progression)
			is Status.Pending -> return Slice(status, progression)
			is Status.Successful -> status.value
		}
	}

	return Slice.successful(result)
}

fun <T> Flow<Slice<List<Slice<T>>>>.flatten(): Flow<Slice<List<T>>> = flatMapSuccess { emit(it.flatten()) }

suspend fun <T, O> StateBuilder<O>.bind(slice: Slice<T>): T {
	val (status, progression) = slice

	return when (status) {
		is Status.Successful -> status.value
		is Status.Failed -> {
			emit(Slice(status, progression))
			shortCircuit()
		}

		is Status.Pending -> {
			emit(Slice(status, progression))
			shortCircuit()
		}
	}
}