package formulaide.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.flowOf
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.request
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

@Composable
fun <T> rememberRef(ref: Ref<T>?) = rememberState(ref) { ref?.request() ?: flowOf() }
