package formulaide.ui.components

import androidx.compose.runtime.Composable
import formulaide.ui.theme.CustomColor
import formulaide.ui.theme.Shade
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div

@Composable
private fun AbstractButton(
	onClick: () -> Unit,
	style: StyleScope.() -> Unit,
	content: @Composable () -> Unit,
) = Button(
	{
		onClick { onClick() }

		style {
			display(DisplayStyle.InlineBlock)
			marginTop(4.px)
			marginBottom(4.px)

			borderRadius(30.px)
			paddingLeft(15.px)
			paddingRight(15.px)
			paddingTop(5.px)
			paddingBottom(5.px)

			style()
		}
	}
) {
	content()
}

@Composable
fun MainButton(
	onClick: () -> Unit,
	content: @Composable () -> Unit,
) = AbstractButton(
	onClick,
	style = {
		shade(Theme.current.primary)
	},
	content,
)

@Composable
fun SecondaryButton(
	onClick: () -> Unit,
	content: @Composable () -> Unit,
) = AbstractButton(
	onClick,
	style = {
		shade(Theme.current.primaryContainer)
	},
	content,
)

@Composable
fun TextButton(
	onClick: () -> Unit,
	content: @Composable () -> Unit,
) = AbstractButton(
	onClick,
	style = {
		shade(
			Shade(
				Theme.current.primary.background,
				CustomColor.transparent,
			)
		)

		padding(5.px)
	},
	content,
)

@Composable
fun ButtonContainer(
	contents: @Composable () -> Unit,
) = Div(
	{
		style {
			display(DisplayStyle.Flex)
			flexDirection(FlexDirection.Row)
			gap(5.px)
		}
	}
) {
	contents()
}
