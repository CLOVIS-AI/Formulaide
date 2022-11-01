package formulaide.ui.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.request
import opensavvy.state.slice.Slice

@Composable
fun <T> rememberSlice(vararg dependencies: Any, generator: suspend () -> Slice<T>): Pair<Slice<T>?, () -> Unit> {
	var result by remember { mutableStateOf<Slice<T>?>(null) }
	var forceRefresh by remember { mutableStateOf(0) }

	LaunchedEffect(forceRefresh, *dependencies) {
		result = generator()
	}

	return result to { forceRefresh++ }
}

@Composable
fun <T> rememberRef(ref: Ref<T>?) = remember(ref) { ref?.request() ?: flow {} }
	.collectAsState(null)
