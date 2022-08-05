package formulaide.ui.utils

import androidx.compose.runtime.*
import formulaide.ui.theme.CustomColor
import formulaide.ui.theme.Shade
import kotlinx.coroutines.delay

private const val defaultRate = 0.05
private const val colorRate = 20.0

@Composable
fun animateDouble(target: Double, rate: Double = defaultRate): Double {
	var state by remember { mutableStateOf(target) }

	LaunchedEffect(target) {
		while (state !in target..(target + rate)) {
			if (state < target) {
				state += rate
			} else {
				state -= rate
			}

			delay(1)
		}

		state = target
	}

	return state
}

@Composable
fun animateColor(target: CustomColor): CustomColor {
	val red = animateDouble(target.red.toDouble(), rate = colorRate)
	val green = animateDouble(target.green.toDouble(), rate = colorRate)
	val blue = animateDouble(target.blue.toDouble(), rate = colorRate)
	val alpha = animateDouble(target.alpha.toDouble(), rate = colorRate)

	return CustomColor(red, green, blue, alpha)
}

@Composable
fun animateShade(target: Shade): Shade {
	val content = animateColor(target.content)
	val background = animateColor(target.background)

	return Shade(content, background)
}
