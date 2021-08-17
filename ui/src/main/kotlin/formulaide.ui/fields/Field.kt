package formulaide.ui.fields

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.UploadRequest
import formulaide.client.files.FileUploadJS
import formulaide.client.routes.uploadFile
import formulaide.ui.components.*
import formulaide.ui.reportExceptions
import formulaide.ui.useClient
import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.files.get
import react.*
import react.dom.*

private external interface FieldProps : RProps {
	var form: Form
	var root: Action?
	var field: FormField
	var id: String
	var fieldKey: String
}

private val RenderField = fc<FieldProps> { props ->
	val field = props.field
	val required = field.arity == Arity.mandatory()

	val (client) = useClient()
	val scope = useAsync()

	var simpleInputState by useState<String>()
	val simpleInput = { type: InputType, _required: Boolean, handler: INPUT.() -> Unit ->
		styledInput(type, props.id, required = _required) {
			onChangeFunction = { simpleInputState = (it.target as HTMLInputElement).value }
			handler()
		}
	}

	when (field) {
		is FormField.Simple -> {
			when (val simple = field.simple) {
				is SimpleField.Text -> simpleInput(InputType.text, required) {}
				is SimpleField.Integer -> simpleInput(InputType.number, required) {}
				is SimpleField.Decimal -> simpleInput(InputType.number, required) {
					step = "any"
				}
				is SimpleField.Boolean -> styledCheckbox(props.id, "", required = false)
				is SimpleField.Message -> Unit // The message has already been displayed
				is SimpleField.Email -> simpleInput(InputType.email, required) {}
				is SimpleField.Date -> simpleInput(InputType.date, required) {}
				is SimpleField.Time -> simpleInput(InputType.time, required) {}
				is SimpleField.Upload -> div {
					ul {
						li {
							text("Formats autorisés : ${
								simple.allowedFormats.flatMap { it.extensions }
									.joinToString(separator = ", ")
							}")
						}
						li {
							text("Taille maximale : ${simple.effectiveMaxSizeMB} Mo")
						}
						li {
							text("RGPD : Ce fichier sera conservé ${simple.effectiveExpiresAfterDays} jours")
						}
					}
					styledInput(InputType.file, "", required) {
						accept = simple.allowedFormats.flatMap { it.mimeTypes }
							.joinToString(separator = ", ")
						multiple = false
						onChangeFunction = {
							val target = it.target as HTMLInputElement
							reportExceptions {
								val file =
									requireNotNull(target.files?.get(0)) { "Aucun fichier n'a été trouvé : $target" }

								scope.reportExceptions {
									val uploaded = client.uploadFile(
										UploadRequest(
											form = props.form.createRef(),
											root = props.root?.createRef(),
											field = props.fieldKey
										),
										file = FileUploadJS(file, file.name)
									)
									simpleInputState = uploaded.id
								}
							}
						}
					}
					input(InputType.hidden, name = props.id) {
						attrs {
							id = props.id
							value = simpleInputState ?: ""
						}
					}
				}
			}

			if (!simpleInputState.isNullOrBlank())
				try {
					field.simple.parse(simpleInputState)
				} catch (e: Exception) {
					styledErrorText(" ${e.message}")
				}
		}
		is FormField.Composite -> {
			val subFields = field.fields

			styledNesting {
				for (subField in subFields) {
					field(props.form, props.root, subField, "${props.id}:${subField.id}")
				}
			}
		}
		is FormField.Union<*> -> {
			val subFields = field.options
			val (selected, setSelected) = useState(subFields.first())

			styledNesting {
				styledFormField {
					for (subField in subFields.sortedBy { it.order }) {
						styledRadioButton(
							radioId = props.id,
							buttonId = "${props.id}-${subField.id}",
							value = subField.id,
							text = subField.name,
							checked = subField == selected,
							onClick = { setSelected(subField) }
						)
					}
				}

				if (selected !is Field.Simple || selected.simple != SimpleField.Message) {
					field(props.form, props.root, selected, "${props.id}:${selected.id}")
				}
			}
		}
	}
}

private val Field: FunctionComponent<FieldProps> = fc { props ->

	if (props.field.arity.max == 1) {
		styledField(props.id, props.field.name) {
			child(RenderField) {
				attrs {
					this.form = props.form
					this.root = props.root
					this.field = props.field
					this.id = props.id
					this.fieldKey = props.id
				}
			}
		}
	} else if (props.field.arity.max > 1) {
		val (fieldIds, setFieldIds) = useState(List(props.field.arity.min) { it })

		styledField(props.id, props.field.name) {
			for ((i, fieldId) in fieldIds.withIndex()) {
				div {
					child(RenderField) {
						attrs {
							this.form = props.form
							this.root = props.root
							this.field = props.field
							this.id = "${props.id}:$fieldId"
							this.fieldKey = fieldId.toString()
						}
					}
					if (fieldIds.size > props.field.arity.min) {
						styledButton("×") {
							setFieldIds(
								fieldIds.subList(0, i) +
										fieldIds.subList(i + 1, fieldIds.size)
							)
						}
					}

					attrs {
						key = fieldId.toString()
					}
				}
			}
			if (fieldIds.size < props.field.arity.max) {
				styledButton("Ajouter une réponse") {
					setFieldIds(fieldIds + ((fieldIds.maxOrNull() ?: 0) + 1))
				}
			}
		}
	} // else: max arity is 0, the field is forbidden, so there is nothing to display
}

fun RBuilder.field(
	form: Form,
	root: Action?,
	field: FormField,
	id: String? = null,
	key: String? = null,
) = child(Field) {
	attrs {
		this.field = field
		this.id = id ?: field.id
		this.form = form
		this.root = root
		this.fieldKey = key ?: this.id
	}
}
