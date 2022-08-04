package formulaide.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Article
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun Page(
	title: String,
	block: @Composable () -> Unit,
) = Article {
	Div(
		{
			style {
				marginTop(30.px)
				marginBottom(15.px)
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
	}

	block()
}
