package formulaide.ui.components

import androidx.compose.runtime.Composable
import formulaide.ui.navigation.client
import formulaide.ui.navigation.localDevelopmentUrl
import formulaide.ui.theme.Theme
import formulaide.ui.theme.shade
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

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
				backgroundColor(Theme.current.default.background.css)
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

		if (client.hostUrl == localDevelopmentUrl) P(
			{
				style {
					marginTop(15.px)
					shade(Theme.current.error)
				}
			}
		) {
			Text("Mode de développement activé, connecté sur ${client.hostUrl}")
		} else if (!client.hostUrl.startsWith("https://")) P(
			{
				style {
					marginTop(15.px)
					shade(Theme.current.error)
				}
			}
		) {
			Text("La connection à Formulaide n'est pas sécurisée.")
		}
	}

	block()
}
