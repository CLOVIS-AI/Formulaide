package formulaide.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button

@Composable
fun IconButton(
	icon: String,
	onClick: suspend () -> Unit,
) {
	val scope = rememberCoroutineScope()

	Button(
		{
			classes(icon)
			onClick { scope.launch { onClick() } }

			style {
				marginLeft(2.px)
				marginRight(2.px)
			}
		}
	)
}

@Composable
fun RefreshButton(
	onClick: suspend () -> Unit,
) = IconButton("ri-refresh-line", onClick)

@Composable
fun AddButton(
	onClick: suspend () -> Unit,
) = IconButton("ri-add-line", onClick)
