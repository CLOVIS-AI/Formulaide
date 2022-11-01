package formulaide.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import arrow.core.continuations.EffectScope
import arrow.core.getOrHandle
import opensavvy.state.Failure
import opensavvy.state.slice.Slice
import opensavvy.state.slice.slice

@Composable
fun rememberPossibleFailure() = remember { mutableStateOf<Failure?>(null) }

fun <T> Slice<T>.orReport(failure: MutableState<Failure?>): T? = getOrHandle { failure.value = it; null }

suspend fun runOrReport(failure: MutableState<Failure?>, block: suspend EffectScope<Failure>.() -> Unit) {
	slice {
		block()
	}.orReport(failure)
}
