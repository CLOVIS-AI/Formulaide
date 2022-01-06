package formulaide.ui.fields

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.UploadRequest
import formulaide.client.Client
import formulaide.client.files.FileUploadJS
import formulaide.client.routes.uploadFile
import formulaide.ui.components.*
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import org.w3c.files.get
import react.*
import react.dom.html.InputHTMLAttributes
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

private external interface FieldProps : Props {
	var form: Form?
	var root: Action?
	var field: Field
	var id: String
	var fieldKey: String
	var input: ((String, String) -> Unit)?
}

private val RenderField = FC<FieldProps>("RenderField") { props ->
	when (val field = props.field) {
		is Field.Simple -> RenderFieldSimple { +props }
		is FormField.Composite -> RenderFieldComposite { +props }
		is Field.Union<*> -> RenderFieldUnion { +props }
		else -> error("Unknown field type in RenderField: ${field::class}")
	}
}

private val RenderFieldSimple = FC<FieldProps>("RenderFieldSimple") { props ->
	val field = props.field
	require(field is Field.Simple) { "Le champ n'est pas simple, mais c'est obligatoire (RenderFieldSimple): $field" }

	val required = field.arity == Arity.mandatory()

	val (client) = useClient()
	val scope = useAsync()

	var simpleInputState by useState<String>()
	val simpleInput =
		{ type: InputType, _required: Boolean, simple: SimpleField, handler: InputHTMLAttributes<HTMLInputElement>.() -> Unit ->
			styledInput(type, props.id, required = _required) {
				onChange = {
					val newValue = it.target.value
					simpleInputState = newValue
					props.input?.let { input -> input(props.fieldKey, newValue) }
				}
				if (simple.defaultValue != null)
					placeholder = simple.defaultValue.toString()
				handler()
			}
		}

	when (val simple = field.simple) {
		is SimpleField.Text -> simpleInput(InputType.text, required, simple) {}
		is SimpleField.Integer -> simpleInput(InputType.number, required, simple) {}
		is SimpleField.Decimal -> simpleInput(InputType.number, required, simple) {
			step = 0.01
		}
		is SimpleField.Boolean -> styledCheckbox(props.id, "", required = false)
		is SimpleField.Message -> Unit // The message has already been displayed
		is SimpleField.Email -> simpleInput(InputType.email, required, simple) {}
		is SimpleField.Phone -> simpleInput(InputType.tel, required, simple) {}
		is SimpleField.Date -> simpleInput(InputType.date, required, simple) {}
		is SimpleField.Time -> simpleInput(InputType.time, required, simple) {}
		is SimpleField.Upload -> div {
			if (props.form != null)
				upload(simple,
				       scope,
				       client,
				       props.form!!,
				       props.root,
				       props.id,
				       props.fieldKey,
				       simpleInputState,
				       onChange = { key, id ->
					       props.input?.invoke(key, id); simpleInputState = id
				       })
			else
				styledErrorText("Ce champ ne correspond à aucun formulaire, il n'est pas possible d'envoyer un fichier.")
		}
	}

	if (!simpleInputState.isNullOrBlank())
		try {
			field.simple.parse(simpleInputState)
		} catch (e: Exception) {
			styledErrorText(" ${e.message}")
		}
}

private val RenderFieldComposite = FC<FieldProps>("RenderFieldComposite") { props ->
	val field = props.field
	require(field is FormField.Composite) { "Le champ n'est pas une donnée composée d'un formulaire, mais c'est obligatoire (RenderFieldComposite): $field" }

	val subFields = field.fields

	styledNesting {
		for (subField in subFields) {
			field(props.form,
			      props.root,
			      subField,
			      props.input,
			      "${props.id}:${subField.id}")
		}
	}
}

private val RenderFieldUnion = FC<FieldProps>("RenderFieldUnion") { props ->
	val field = props.field
	require(field is Field.Union<*>) { "Le champ n'est pas une union, mais c'est obligatoire (RenderFieldUnion): $field" }

	val subFields = field.options
	val (selected, setSelected) = useState(subFields.first())

	styledNesting {
		styledFormField {
			for (subField in subFields.sortedBy { it.order }) {
				styledRadioButton(
					radioId = props.id,
					buttonId = "${props.fieldKey}-${subField.id}",
					value = subField.id,
					text = subField.name,
					checked = subField == selected,
					onClick = { setSelected(subField) }
				)
			}
		}

		if (selected !is Field.Simple || selected.simple != SimpleField.Message) {
			field(props.form,
			      props.root,
			      selected,
			      props.input,
			      id = "${props.id}:${selected.id}")
		}
	}
}

private fun ChildrenBuilder.upload(
	simple: SimpleField.Upload,
	scope: CoroutineScope,
	client: Client,
	form: Form,
	root: Action?,
	id: String,
	fieldKey: String,
	simpleInputState: String?,
	onChange: ((String, String) -> Unit)?,
) {
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
	styledInput(InputType.file, "", simple.arity.min > 0) {
		accept = simple.allowedFormats.flatMap { it.mimeTypes }
			.joinToString(separator = ", ")
		multiple = false
		this.onChange = {
			reportExceptions {
				val file =
					requireNotNull(it.target.files?.get(0)) { "Aucun fichier n'a été trouvé : ${it.target}" }

				scope.reportExceptions {
					val uploaded = client.uploadFile(
						UploadRequest(
							form = form.createRef(),
							root = root?.createRef(),
							field = id
						),
						file = FileUploadJS(file, file.name)
					)
					onChange?.let { onChange -> onChange(fieldKey, uploaded.id) }
				}
			}
		}
	}
	input {
		type = InputType.hidden
		name = id

		this.id = id
		value = simpleInputState ?: ""
	}
}

private val Field: FC<FieldProps> = FC("Field") { props ->
	traceRenders("Field ${props.fieldKey}")
	if (props.field.arity.max == 1) {
		styledField(props.id, props.field.name) {
			RenderField {
				this.form = props.form
				this.root = props.root
				this.field = props.field
				this.id = props.id
				this.fieldKey = "${props.fieldKey}:${props.id}"
				this.input = props.input
			}
		}
	} else if (props.field.arity.max > 1) {
		val (fieldIds, setFieldIds) = useState(List(props.field.arity.min) { it })

		styledField(props.id, props.field.name) {
			for ((i, fieldId) in fieldIds.withIndex()) {
				div {
					RenderField {
						this.form = props.form
						this.root = props.root
						this.field = props.field
						this.id = "${props.id}:$fieldId"
						this.fieldKey = fieldId.toString()
						this.input = props.input
					}

					if (fieldIds.size > props.field.arity.min) {
						StyledButton {
							text = "×"
							action = {
								setFieldIds(
									fieldIds.subList(0, i) +
											fieldIds.subList(i + 1, fieldIds.size)
								)
							}
						}
					}

					key = fieldId.toString()
				}
			}
			if (fieldIds.size < props.field.arity.max) {
				StyledButton {
					text = "Ajouter une réponse"
					action = { setFieldIds(fieldIds + ((fieldIds.maxOrNull() ?: 0) + 1)) }
				}
			}
		}
	} // else: max arity is 0, the field is forbidden, so there is nothing to display
}

fun ChildrenBuilder.field(
	form: Form?,
	root: Action?,
	field: Field,
	input: ((String, String) -> Unit)? = null,
	id: String? = null,
	key: String? = null,
) {
	Field {
		this.field = field
		this.id = id ?: field.id
		this.form = form
		this.root = root
		this.fieldKey = key ?: this.id
		this.input = input
	}
}
