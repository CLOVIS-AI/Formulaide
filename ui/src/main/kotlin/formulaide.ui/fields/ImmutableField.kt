package formulaide.ui.fields

import formulaide.api.data.*
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.client.Client
import formulaide.client.routes.downloadFile
import formulaide.ui.components.styledButton
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.utils.text
import kotlinx.browser.window
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import react.FunctionComponent
import react.Props
import react.RBuilder
import react.dom.div
import react.fc
import kotlin.js.Date

fun RBuilder.immutableFields(answers: ParsedSubmission) {
	for (answer in answers.fields) {
		immutableField(answer)
	}
}

private fun RBuilder.immutableField(answer: ParsedField<*>) {
	child(ImmutableField) {
		attrs {
			this.answer = answer
		}
	}
}

private external interface ImmutableFieldProps : Props {
	var answer: ParsedField<*>
}

private const val miniNesting = "px-4"

private val ImmutableField: FunctionComponent<ImmutableFieldProps> = fc { props ->
	traceRenders(props.answer.toString())

	val (client) = useClient()

	require(client is Client.Authenticated) { "Cette page est uniquement accessible par les employés." }

	div {
		when (val answer = props.answer) {
			is ParsedSimple<*> -> {
				val field = answer.constraint
				if (field.simple !is SimpleField.Message) {
					text("${answer.constraint.name} : ")

					when (field.simple) {
						is SimpleField.Boolean -> text(if (answer.value.toBoolean()) "✓" else "✗")
						is SimpleField.Date -> {
							val value = answer.value ?: error("Cette date n'a pas de valeur")
							text(Date(value).toLocaleDateString())
						}
						is SimpleField.Upload -> styledButton("Ouvrir", action = {
							val fileId = answer.value ?: error("Ce fichier n'a pas d'identifiants")
							val file = client.downloadFile(fileId)

							val blob = Blob(arrayOf(file.data), BlobPropertyBag(
								type = file.mime
							))

							val url = URL.createObjectURL(blob)
							window.open(url, target = "_blank", features = "noopener,noreferrer")
						})
						else -> text(answer.value.toString())
					}
				} // else: don't display messages here
			}
			is ParsedUnion<*, *> -> {
				val value = answer.value
				if (value is FormField.Simple && value.simple is SimpleField.Message) {
					text(answer.constraint.name + " : " + value.name)
				} else {
					text(answer.constraint.name)
					div(miniNesting) {
						immutableField(answer.children.first())
					}
				}
			}
			is ParsedList<*> -> {
				for (child in answer.children) {
					immutableField(child)
				}
			}
			is ParsedComposite<*> -> {
				val compositeField = answer.constraint

				text(compositeField.name)
				for (child in answer.children) {
					div(miniNesting) {
						immutableField(child)
					}
				}
			}
		}
	}
}
