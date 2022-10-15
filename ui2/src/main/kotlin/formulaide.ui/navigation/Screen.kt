package formulaide.ui.navigation

import androidx.compose.runtime.Composable
import formulaide.core.User

data class Screen(
	val title: String,
	val requiredRole: User.Role,
	val route: String,
	val icon: String,
	val iconSelected: String = icon,
	val parent: Screen? = null,
	val actions: @Composable () -> Unit = {},
	val render: @Composable () -> Unit,
) {

	@Composable
	operator fun invoke() = render()
}
