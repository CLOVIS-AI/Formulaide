package formulaide.ui.components

import androidx.compose.runtime.Composable
import opensavvy.state.Progression
import opensavvy.state.Slice
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Text

@Composable
fun Loading(data: Slice<Any>) {
	val progression = data.progression

	if (progression is Progression.Loading) {
		Div(
			{
				classes("animate-spin")

				style {
					display(DisplayStyle.InlineBlock)
				}
			}
		) {
			I(
				{
					classes("ri-loader-5-line")
				}
			)
		}

		if (progression is Progression.Loading.Quantified) {
			Text("${progression.percent}%")
		}
	}
}
