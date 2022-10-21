package formulaide.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import opensavvy.state.Slice
import opensavvy.state.Slice.Companion.pending
import opensavvy.state.Slice.Companion.successful
import androidx.compose.runtime.State as ComposeState
import opensavvy.state.State as PedestalState

@Composable
fun <T> rememberState(vararg dependencies: Any?, generator: () -> PedestalState<T>): ComposeState<Slice<T>> {
	return remember(*dependencies) { generator() }.collectAsState(pending())
}

@Composable
fun rememberEmptyState() = remember { mutableStateOf(successful(Unit)) }
