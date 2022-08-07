package formulaide.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button

@Composable
fun IconButton(
	icon: String,
	onClick: () -> Unit,
) = Button(
	{
		classes(icon)
		onClick { onClick() }

		style {
			marginLeft(2.px)
			marginRight(2.px)
		}
	}
)

@Composable
fun RefreshButton(
	onClick: () -> Unit,
) = IconButton("ri-refresh-line", onClick)

@Composable
fun AddButton(
	onClick: () -> Unit,
) = IconButton("ri-add-line", onClick)
