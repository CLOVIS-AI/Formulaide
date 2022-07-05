package formulaide.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.ui.utils.LocalStorageState.Companion.localStorageOf
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.dom.Div

interface AbstractTheme {
	val primary: CustomColor
	val primaryContainer: CustomColor

	val secondary: CustomColor
	val secondaryContainer: CustomColor

	val tertiary: CustomColor
	val tertiaryContainer: CustomColor

	val text: CustomColor
	val background: CustomColor

	val textVariant: CustomColor
	val backgroundVariant: CustomColor
}

@Serializable
enum class Theme : AbstractTheme {
	LIGHT {
		override val icon: String get() = "ri-contrast-2-line"

		override val primary = CustomColor(103, 80, 164)
		override val primaryContainer = CustomColor(207, 193, 230)

		override val secondary = CustomColor(87, 114, 181)
		override val secondaryContainer = CustomColor(204, 210, 255)

		override val tertiary = CustomColor(222, 109, 146)
		override val tertiaryContainer = CustomColor(242, 191, 207)

		override val text = CustomColor(28, 27, 31)
		override val background = CustomColor(255, 251, 254)

		override val textVariant = CustomColor(73, 69, 78)
		override val backgroundVariant = CustomColor(231, 224, 236)
	},
	DARK {
		override val icon: String get() = "ri-contrast-2-fill"

		override val primary = CustomColor(208, 188, 255)
		override val primaryContainer = CustomColor(79, 55, 139)

		override val secondary = CustomColor(204, 194, 220)
		override val secondaryContainer = CustomColor(74, 68, 88)

		override val tertiary = CustomColor(239, 184, 200)
		override val tertiaryContainer = CustomColor(99, 59, 72)

		override val text = CustomColor(230, 225, 229)
		override val background = CustomColor(28, 27, 31)

		override val textVariant = CustomColor(202, 196, 208)
		override val backgroundVariant = CustomColor(73, 69, 79)
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
				backgroundColor(Theme.current.background.css)
				color(Theme.current.text.css)
			}
		}
	) {
		block()
	}
}
