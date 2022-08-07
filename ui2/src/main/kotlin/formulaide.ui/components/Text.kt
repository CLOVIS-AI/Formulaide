package formulaide.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text

@Composable
fun SectionTitle(text: String) = H2(
	{
		style {
			marginTop(20.px)
			marginBottom(5.px)

			property("font-size", "large")
		}
	}
) {
	Text(text)
}
