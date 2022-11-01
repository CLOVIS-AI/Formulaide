package formulaide.api.utils

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.flow.*
import opensavvy.state.slice.Slice
import opensavvy.state.slice.slice

fun <I, O> Flow<Slice<I>>.mapSuccesses(transform: suspend (I) -> O): Flow<Slice<O>> = map {
	slice {
		transform(it.bind())
	}
}

fun <I, O> Flow<Slice<I>>.flatMapSuccesses(transform: suspend FlowCollector<Slice<O>>.(I) -> Unit): Flow<Slice<O>> =
	transform { slice ->
		slice.fold(
			ifLeft = {
				emit(it.left())
			},
			ifRight = {
				transform(it)
			}
		)
	}

fun <I> Flow<Slice<I>>.onEachSuccess(action: suspend (I) -> Unit) = onEach { slice ->
	if (slice is Either.Right)
		action(slice.value)
}
