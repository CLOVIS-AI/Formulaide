package formulaide.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.ui.utils.LocalStorageState.Companion.localStorageOf
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.Div

class Shade(
	/**
	 * The color of the main content of this shade.
	 *
	 * In material jargon, this is the "on color" for this shade.
	 */
	val content: CustomColor,

	/**
	 * The color of the background of this shade.
	 */
	val background: CustomColor,
)

/**
 * Create a fake CSS 'shade' attribute for convenience.
 */
fun StyleScope.shade(shade: Shade) {
	color(shade.content.css)
	backgroundColor(shade.background.css)
}

interface AbstractTheme {
	val primary: Shade
	val primaryContainer: Shade

	val secondary: Shade
	val secondaryContainer: Shade

	val tertiary: Shade
	val tertiaryContainer: Shade

	val default: Shade
	val neutral1: Shade
	val neutral2: Shade

	val error: Shade
	val errorContainer: Shade
}

@Serializable
enum class Theme : AbstractTheme {
	LIGHT {
		override val icon: String get() = "ri-contrast-2-line"

		override val primary = Shade(
			CustomColor(255, 255, 255),
			CustomColor(103, 80, 164),
		)
		override val primaryContainer = Shade(
			CustomColor(33, 0, 94),
			CustomColor(234, 221, 255),
		)

		override val secondary = Shade(
			CustomColor(255, 255, 255),
			CustomColor(98, 91, 113),
		)
		override val secondaryContainer = Shade(
			CustomColor(30, 25, 43),
			CustomColor(232, 222, 248),
		)

		override val tertiary = Shade(
			CustomColor(255, 255, 255),
			CustomColor(125, 82, 96),
		)
		override val tertiaryContainer = Shade(
			CustomColor(55, 11, 30),
			CustomColor(255, 216, 228),
		)

		override val default = Shade(
			CustomColor(28, 27, 31),
			CustomColor(255, 251, 254),
		)
		override val neutral1 = Shade(
			CustomColor(28, 27, 31),
			CustomColor(255, 251, 254),
		)
		override val neutral2 = Shade(
			CustomColor(73, 69, 78),
			CustomColor(231, 224, 236),
		)

		override val error = Shade(
			CustomColor(179, 38, 30),
			CustomColor.transparent,
		)
		override val errorContainer = Shade(
			CustomColor(55, 11, 30),
			CustomColor(249, 222, 220),
		)
	},
	DARK {
		override val icon: String get() = "ri-contrast-2-fill"

		override val primary = Shade(
			CustomColor(55, 30, 115),
			CustomColor(208, 188, 255),
		)
		override val primaryContainer = Shade(
			CustomColor(234, 221, 255),
			CustomColor(79, 55, 139),
		)

		override val secondary = Shade(
			CustomColor(51, 45, 65),
			CustomColor(204, 194, 220),
		)
		override val secondaryContainer = Shade(
			CustomColor(232, 222, 248),
			CustomColor(74, 68, 88),
		)

		override val tertiary = Shade(
			CustomColor(73, 37, 50),
			CustomColor(239, 184, 200),
		)
		override val tertiaryContainer = Shade(
			CustomColor(255, 216, 228),
			CustomColor(99, 59, 72),
		)

		override val default = Shade(
			CustomColor(230, 225, 229),
			CustomColor(28, 27, 31),
		)
		override val neutral1 = Shade(
			CustomColor(230, 225, 229),
			CustomColor(28, 27, 31),
		)
		override val neutral2 = Shade(
			CustomColor(202, 196, 208),
			CustomColor(73, 69, 79),
		)

		override val error = Shade(
			CustomColor(242, 184, 181),
			CustomColor.transparent,
		)
		override val errorContainer = Shade(
			CustomColor(249, 222, 220),
			CustomColor(140, 29, 24),
		)
	},
	;

	abstract val icon: String

	companion object {
		var current by localStorageOf("theme", LIGHT)
	}
}

@Composable
fun ThemeSelector() {
	RailButton(
		Theme.current.icon,
		Theme.current.icon,
		selected = false,
		text = "Changer de thème visuel",
		action = {
			Theme.current = when (Theme.current) {
				Theme.LIGHT -> Theme.DARK
				Theme.DARK -> Theme.LIGHT
			}
			console.log("Switched to theme ${Theme.current}")
		}
	)
}

@Composable
fun ApplyTheme(block: @Composable () -> Unit) {
	Div(
		{
			id("theme")

			style {
				backgroundColor(Theme.current.default.background.css)
				color(Theme.current.default.content.css)
			}
		}
	) {
		block()
	}
}
