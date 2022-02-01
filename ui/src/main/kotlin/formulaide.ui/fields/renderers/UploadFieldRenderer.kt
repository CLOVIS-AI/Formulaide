package formulaide.ui.fields.renderers

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.UploadRequest
import formulaide.client.files.FileUploadJS
import formulaide.client.routes.uploadFile
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.text.ErrorText
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.useClient
import org.w3c.files.get
import react.FC
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.ul

external interface UploadFieldRendererProps : FieldProps {
	var value: String?
}

val UploadFieldRenderer = FC<UploadFieldRendererProps>("UploadFieldRenderer") { props ->
	val scope = useAsync()
	val client by useClient()

	if (props.form == null) {
		p {
			ErrorText {
				text = "Ce champ ne correspond à aucun formulaire." +
						" Il est impossible d'envoyer un fichier dans cette situation."
			}
		}
		console.warn("UploadFieldRenderer a été appelé sans fournir de formulaire.", props)
		return@FC
	}

	val field = props.field
	require(field is Field.Simple) { "UploadFieldRenderer: expected a Field.Simple, but found a ${field::class}: $field" }
	val simple = field.simple
	require(simple is SimpleField.Upload) { "UploadFieldRenderer: expected a SimpleField.Upload, but found a ${simple::class}: $simple" }

	div {
		ul {
			li {
				+"Formats autorisés : ${simple.allowedFormats.flatMap { it.extensions }.joinToString(", ")}"
			}

			li {
				+"Taille maximale : ${simple.effectiveMaxSizeMB} Mo"
			}

			li {
				+"RGPD : Ce fichier sera conservé ${simple.effectiveExpiresAfterDays} jours"
			}
		}

		Input {
			type = InputType.file
			required = simple.arity.min > 0
			accept = simple.allowedFormats.flatMap { it.mimeTypes }.joinToString(", ")
			multiple = false
			onChange = {
				val file = it.target.files?.get(0)

				scope.reportExceptions {
					requireNotNull(file) { "Aucun fichier n'a été trouvé : ${it.target}" }

					val uploaded = client.uploadFile(
						request = UploadRequest(
							form = props.form?.createRef() ?: error("Ce champ ne fait partie d'aucun formulaire, il n'est pas possible de publier un fichier."),
							root = props.root?.createRef(),
							field = props.idOrDefault
						),
						file = FileUploadJS(file, file.name),
					)

					props.onInput?.invoke(props.fieldKeyOrDefault, uploaded.id)
				}
			}
		}

		input {
			type = InputType.hidden
			name = props.idOrDefault
			id = props.idOrDefault
			value = props.value
		}
	}
}
