package formulaide.ui.components

import androidx.compose.runtime.*
import formulaide.ui.theme.CustomColor
import formulaide.ui.theme.Shade
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
private fun AbstractButton(
	onClick: suspend () -> Unit,
	style: StyleScope.() -> Unit,
	content: @Composable () -> Unit,
) {
	val scope = rememberCoroutineScope()

	var loading by remember { mutableStateOf(false) }

	Button(
		{
			onClick {
				scope.launch {
					try {
						loading = true
						onClick()
					} finally {
						loading = false
					}
				}
			}

			style {
				display(DisplayStyle.Flex)
				gap(4.px)
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

		if (loading)
			Text("...")
	}
}

@Composable
fun MainButton(
	onClick: suspend () -> Unit,
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
	onClick: suspend () -> Unit,
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
	onClick: suspend () -> Unit,
	enabled: Boolean = true,
	content: @Composable () -> Unit,
) = AbstractButton(
	onClick,
	style = {
		if (enabled) shade(
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
