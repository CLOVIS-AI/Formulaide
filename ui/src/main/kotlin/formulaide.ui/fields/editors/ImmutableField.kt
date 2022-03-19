package formulaide.ui.fields.editors

import formulaide.api.data.*
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.client.Client
import formulaide.client.routes.downloadFile
import formulaide.ui.components.StyledButton
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.utils.classes
import kotlinx.browser.window
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import kotlin.js.Date

external interface ImmutableFieldProps : Props {
	var answer: ParsedField<*>
}

private const val miniNesting = "px-4"

private val LazyImmutableField get() = ImmutableField

/**
 * Displays a user's submission to a field.
 */
val ImmutableField: FC<ImmutableFieldProps> = FC("ImmutableField") { props ->
	traceRenders(props.answer.toString())

	val (client) = useClient()

	require(client is Client.Authenticated) { "Cette page est uniquement accessible par les employés." }

	div {
		when (val answer = props.answer) {
			is ParsedSimple<*> -> {
				val field = answer.constraint
				if (field.simple !is SimpleField.Message) {
					+"${answer.constraint.name} : "

					when (field.simple) {
						is SimpleField.Boolean -> +if (answer.value.toBoolean()) "✓" else "✗"
						is SimpleField.Date -> {
							val value = answer.value ?: error("Cette date n'a pas de valeur")
							+Date(value).toLocaleDateString()
						}
						is SimpleField.Upload -> StyledButton {
							text = "Ouvrir"
							this.action = {
								val fileId = answer.value ?: error("Ce fichier n'a pas d'identifiants")
								val file = client.downloadFile(fileId)

								val blob = Blob(arrayOf(file.data), BlobPropertyBag(
									type = file.mime
								))

								val url = URL.createObjectURL(blob)
								window.open(url, target = "_blank", features = "noopener,noreferrer")
							}
						}
						else -> +answer.value.toString()
					}
				} // else: don't display messages here
			}
			is ParsedUnion<*, *> -> {
				val value = answer.value
				if (value is FormField.Simple && value.simple is SimpleField.Message) {
					+(answer.constraint.name + " : " + value.name)
				} else {
					+answer.constraint.name
					div {
						classes = miniNesting
						LazyImmutableField {
							this.answer = answer.children.first()
						}
					}
				}
			}
			is ParsedList<*> -> {
				for (child in answer.children) {
					LazyImmutableField {
						this.answer = child
					}
				}
			}
			is ParsedComposite<*> -> {
				val compositeField = answer.constraint

				+compositeField.name
				for (child in answer.children) {
					div {
						classes = miniNesting
						LazyImmutableField {
							this.answer = child
						}
					}
				}
			}
		}
	}
}
