package formulaide.ui.components

import androidx.compose.runtime.Composable
import formulaide.ui.theme.Theme
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun Page(
	title: String,
	header: (@Composable () -> Unit)? = null,
	block: @Composable () -> Unit,
) = Article {
	Div(
		{
			style {
				paddingTop(15.px)
				paddingBottom(15.px)

				position(Position.Sticky)
				top(0.px)
				backgroundColor(Theme.current.background.css)
			}
		}
	) {
		H1(
			{
				style {
					property("font-size", "x-large")
				}
			}
		) {
			Text(title)
		}

		if (header != null) Div(
			{
				style {
					marginTop(15.px)
				}
			}
		) {
			header()
		}
	}

	block()
}
