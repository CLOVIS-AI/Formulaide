package formulaide.ui.utils

import formulaide.api.data.reportEmailOrDefault
import formulaide.client.Client
import formulaide.ui.components.text.FooterText
import formulaide.ui.useClient
import formulaide.ui.useConfig
import react.FC
import react.Props
import react.dom.html.ReactHTML.a

external fun encodeURIComponent(encodedURI: String): String

external interface ReportIssueProps : Props {
	var text: String?
	var footer: Boolean?
}

val ReportIssue = FC<ReportIssueProps>("ReportIssue") { props ->
	val config by useConfig()
	val client by useClient()

	val text = props.text ?: "Signaler un problème"
	val footer = props.footer ?: false
	var body = """
		MERCI DE REMPLACER CE MESSAGE EN EXPLIQUANT CE QUE VOUS ÉTIEZ EN TRAIN DE FAIRE.
		N'OUBLIEZ PAS D'AJOUTER UN SUJET QUI RÉSUME LE PROBLÈME EN QUELQUES MOTS.
		
		La suite de ce message contient des informations utiles pour les administrateurs pour pouvoir trouver l'origine du problème. Merci de ne pas les modifier.
	""".trimIndent()

	body += """		
		
		API URL: ${client.hostUrl}
		User: ${(client as? Client.Authenticated)?.me?.toString() ?: "anonymous user"}
	""".trimIndent()

	a {
		href = "mailto:${config.reportEmailOrDefault.email}?body=${encodeURIComponent(body)}"

		if (footer) FooterText {
			classes = "hover:underline"
			this.text = text
		} else {
			classes = "hover:underline"
			+text
		}
	}
}
