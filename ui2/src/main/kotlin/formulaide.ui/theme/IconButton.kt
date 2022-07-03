package formulaide.ui.theme

import androidx.compose.runtime.*
import formulaide.ui.utils.animateDouble
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Text

@Composable
fun RailButton(
	icon: String,
	iconSelected: String,
	text: String,
	selected: Boolean = false,
	action: () -> Unit,
) {
	val selectedTransition = animateDouble(if (selected) 1.0 else 0.0)

	var hover by remember { mutableStateOf(false) }
	val hoverTransition = animateDouble(if (hover) 1.0 else 0.0)

	Button(
		{
			onClick { action() }
			title(text)
		}
	) {
		I(
			{
				classes(if (selected) iconSelected else icon)

				style {
					property("font-size", "xx-large")

					if (selected) {
						backgroundColor(CustomColor.primaryContainer.css)
						paddingLeft(10.px)
						paddingRight(10.px)
						paddingTop(2.px)
						paddingBottom(2.px)
						borderRadius(24.px)
					} else {
						backgroundColor(CustomColor.secondaryContainer.copy(alpha = hoverTransition).css)
						paddingLeft((hoverTransition * 10).px)
						paddingRight((hoverTransition * 10).px)
						paddingTop(2.px)
						paddingBottom(2.px)
						borderRadius(24.px)
					}
				}

				onMouseEnter { hover = true }
				onMouseLeave { hover = false }
			}
		)

		if (selectedTransition != 0.0) {
			Div(
				{
					style {
						opacity(selectedTransition)
						maxHeight((selectedTransition * 1.5).em)
					}
				}
			) {
				Text(text)
			}
		}
	}
}
