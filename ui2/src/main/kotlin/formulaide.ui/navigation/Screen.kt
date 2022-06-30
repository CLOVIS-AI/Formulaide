package formulaide.ui.navigation

import androidx.compose.runtime.Composable
import formulaide.ui.utils.Role

data class Screen(
	val title: String,
	val requiredRole: Role,
	val route: String,
	val render: @Composable () -> Unit,
) {

	@Composable
	operator fun invoke() = render()
}
