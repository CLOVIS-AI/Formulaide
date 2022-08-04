package formulaide.ui.components

import androidx.compose.runtime.Composable
import formulaide.ui.theme.Theme
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun FilterChip(
	text: String,
	enabled: Boolean,
	onUpdate: (Boolean) -> Unit,
	onRemove: (() -> Unit)? = null,
) {
	Span(
		{
			classes("chip")

			style {
				borderRadius(10.px)
				paddingLeft(8.px)
				paddingRight(8.px)
				paddingTop(2.px)
				paddingBottom(2.px)

				if (enabled) {
					backgroundColor(Theme.current.secondaryContainer.css)

					// Always display the border to avoid movement when switching between selected and unselected
					border {
						color = Theme.current.secondaryContainer.css
						style = LineStyle.Solid
						width = 1.px
					}
				} else {
					border {
						color = Theme.current.secondary.css
						style = LineStyle.Solid
						width = 1.px
					}
				}
			}
		}
	) {
		if (enabled) Span(
			{
				style {
					marginRight(5.px)
				}
			}
		) {
			Text("✓")
		}

		Button(
			{
				onClick { onUpdate(!enabled) }
			}
		) {
			Text(text)
		}

		if (onRemove != null) Button(
			{
				style {
					marginLeft(5.px)
				}
			}
		) {
			Text("×")
		}
	}
}

@Composable
fun ChipContainer(
	content: @Composable () -> Unit,
) = Div(
	{
		classes("chip-container")
		style {
			display(DisplayStyle.Flex)
			flexDirection(FlexDirection.Row)
			gap(5.px)
		}
	}
) {
	content()
}

@Composable
fun ChipContainerSeparator() = Div(
	{
		style {
			width(2.px)
		}
	}
)
