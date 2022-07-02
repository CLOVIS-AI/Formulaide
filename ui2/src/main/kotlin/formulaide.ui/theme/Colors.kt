package formulaide.ui.theme

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgba

data class CustomColor(
	val red: Number,
	val green: Number,
	val blue: Number,
	val alpha: Number = 1.0,
) {

	val css: CSSColorValue
		get() = rgba(red, green, blue, alpha)

	companion object {
		// Colors from the light mode at https://m3.material.io/styles/color/the-color-system/tokens

		val primary = CustomColor(103, 80, 164)
		val primaryContainer = CustomColor(207, 193, 230)

		val secondary = CustomColor(87, 114, 181)
		val secondaryContainer = CustomColor(204, 210, 255)

		val tertiary = CustomColor(222, 109, 146)
		val tertiaryContainer = CustomColor(242, 191, 207)
	}
}
