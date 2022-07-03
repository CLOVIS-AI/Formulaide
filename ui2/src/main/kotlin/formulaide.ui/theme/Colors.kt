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
		val secondary = CustomColor(98, 91, 113)
		val tertiary = CustomColor(125, 82, 96)
	}
}
