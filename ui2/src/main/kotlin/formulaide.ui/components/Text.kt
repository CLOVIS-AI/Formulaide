package formulaide.ui.components

import androidx.compose.runtime.Composable
import opensavvy.state.Progression
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Text

@Composable
fun SectionTitle(text: String, loading: Progression = Progression.Done) = H2(
	{
		style {
			marginTop(20.px)
			marginBottom(5.px)

			property("font-size", "large")
		}
	}
) {
	Text(text)

	Loading(loading)
}

@Composable
fun Paragraph(
	title: String,
	loading: Progression = Progression.Done,
	header: (@Composable () -> Unit)? = null,
	block: @Composable () -> Unit,
) = Section {
	SectionTitle(title, loading)

	header?.invoke()

	block()
}
