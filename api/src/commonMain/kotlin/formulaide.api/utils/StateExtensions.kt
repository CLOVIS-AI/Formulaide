package formulaide.api.utils

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import opensavvy.state.*

fun <I, O> State<I>.mapSuccesses(transform: suspend (I) -> O): State<O> = map {
	val (status, progression) = it

	val newStatus: Status<O> = when (status) {
		is Status.Failed -> status
		is Status.Pending -> status
		is Status.Successful -> Status.Successful(transform(status.value))
	}

	Slice(newStatus, progression)
}

fun <I, O> State<I>.flatMapSuccesses(transform: suspend StateBuilder<O>.(I) -> Unit): State<O> = transform {
	val (status, progression) = it

	when (status) {
		is Status.Failed -> emit(Slice(status, progression))
		is Status.Pending -> emit(Slice(status, progression))
		is Status.Successful -> emitAll(state { transform(status.value) })
	}
}

fun <I> State<I>.onEachSuccess(action: suspend (I) -> Unit) = onEach { slice ->
	(slice.status as? Status.Successful)
		?.let { action(it.value) }
}
