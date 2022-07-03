package formulaide.ui.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

private const val EPSILON = 0.05

@Composable
fun animateDouble(target: Double): Double {
	var state by remember { mutableStateOf(target) }

	LaunchedEffect(target) {
		while (state !in target..(target + EPSILON)) {
			if (state < target) {
				state += EPSILON
			} else {
				state -= EPSILON
			}

			delay(1)
		}

		state = target
	}

	return state
}
