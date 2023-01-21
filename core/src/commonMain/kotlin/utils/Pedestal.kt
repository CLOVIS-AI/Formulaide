package opensavvy.formulaide.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import opensavvy.state.progressive.ProgressiveOutcome

//TODO: upstream to Pedestal, see https://gitlab.com/opensavvy/pedestal/-/issues/56
fun <T, O> Flow<ProgressiveOutcome<T>>.mapSuccess(transform: (T) -> O) = map {
	when (it) {
		is ProgressiveOutcome.Success -> ProgressiveOutcome.Success(transform(it.value), it.progress)
		is ProgressiveOutcome.Failure -> it
		is ProgressiveOutcome.Empty -> it
	}
}
