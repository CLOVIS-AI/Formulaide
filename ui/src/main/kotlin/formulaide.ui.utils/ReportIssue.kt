package formulaide.ui.utils

import formulaide.api.data.reportEmailOrDefault
import formulaide.ui.components.text.FooterText
import formulaide.ui.useConfig
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.memo

external interface ReportIssueProps : Props {
	var text: String?
	var footer: Boolean?
}

val ReportIssue = memo(FC<ReportIssueProps>("ReportIssue") { props ->
	val config by useConfig()
	val text = props.text ?: "Signaler un problème"
	val footer = props.footer ?: false

	a {
		href = "mailto:${config.reportEmailOrDefault.email}"

		if (footer) FooterText {
			classes = "hover:underline"
			this.text = text
		} else {
			classes = "hover:underline"
			+text
		}
	}
})
