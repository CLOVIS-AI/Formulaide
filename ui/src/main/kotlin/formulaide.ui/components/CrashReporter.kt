package formulaide.ui.components

import formulaide.ui.components.cards.Card
import formulaide.ui.utils.*
import kotlinx.js.jso
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.p

val CrashReporter = FC<PropsWithChildren>("CrashReporter") { props ->
	val (boundary, didCatch, error) = useErrorBoundary()

	if (didCatch) {
		Card {
			title = "Erreur"
			subtitle = "Le site a rencontré un échec fatal"
			failed = true

			p {
				+"Merci de signaler cette erreur à l'administrateur "
				ReportIssue {
					text = "en cliquant ici"
					this.error = error
				}
				+"."
			}
		}
	} else {
		child(boundary(
			jso<ErrorBoundaryProps>()
				.apply { children = props.children }
		))
	}
}
