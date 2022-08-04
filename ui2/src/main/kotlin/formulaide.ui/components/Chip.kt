package formulaide.ui.components

import androidx.compose.runtime.Composable
import formulaide.ui.theme.Theme
import formulaide.ui.utils.animateColor
import formulaide.ui.utils.animateDouble
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
	val enabledTransition = animateDouble(if (enabled) 1.0 else 0.0)
	val enabledBorderColor = animateColor(if (enabled) Theme.current.secondaryContainer else Theme.current.secondary)

	Span(
		{
			classes("chip")

			style {
				borderRadius(10.px)
				paddingLeft(8.px)
				paddingRight(8.px)
				paddingTop(2.px)
				paddingBottom(2.px)

				backgroundColor(Theme.current.secondaryContainer.copy(alpha = enabledTransition).css)

				border {
					style = LineStyle.Solid
					color = enabledBorderColor.css
					width = 1.px
				}
			}
		}
	) {
		if (enabledTransition != 0.0) Span(
			{
				style {
					marginRight((enabledTransition * 5).px)
					opacity(enabledTransition)
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
