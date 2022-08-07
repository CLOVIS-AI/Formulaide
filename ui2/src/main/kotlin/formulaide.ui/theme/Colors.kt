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
		val transparent = CustomColor(0, 0, 0, alpha = 0.0)
	}
}
