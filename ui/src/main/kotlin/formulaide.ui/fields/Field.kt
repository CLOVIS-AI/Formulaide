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
import formulaide.ui.CrashReporter
import formulaide.ui.components.*
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.utils.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.files.get
import react.*
import react.dom.*

private external interface FieldProps : Props {
	var form: Form?
	var root: Action?
	var field: Field
	var id: String
	var fieldKey: String
	var input: ((String, String) -> Unit)?
}

private val RenderField = fc<FieldProps>("RenderField") { props ->
	when (val field = props.field) {
		is Field.Simple -> child(RenderFieldSimple, props)
		is FormField.Composite -> child(RenderFieldComposite, props)
		is Field.Union<*> -> child(RenderFieldUnion, props)
		else -> error("Unknown field type in RenderField: ${field::class}")
	}
}

private val RenderFieldSimple = fc<FieldProps>("RenderFieldSimple") { props ->
	val field = props.field
	require(field is Field.Simple) { "Le champ n'est pas simple, mais c'est obligatoire (RenderFieldSimple): $field" }

	val required = field.arity == Arity.mandatory()

	val (client) = useClient()
	val scope = useAsync()

	var simpleInputState by useState<String>()
	val simpleInput =
		{ type: InputType, _required: Boolean, simple: SimpleField, handler: INPUT.() -> Unit ->
			styledInput(type, props.id, required = _required) {
				onChangeFunction = {
					val newValue = (it.target as HTMLInputElement).value
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
			step = "any"
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

private val RenderFieldComposite = fc<FieldProps>("RenderFieldComposite") { props ->
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

private val RenderFieldUnion = fc<FieldProps>("RenderFieldUnion") { props ->
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

private fun RBuilder.upload(
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
		onChangeFunction = {
			val target = it.target as HTMLInputElement
			reportExceptions {
				val file =
					requireNotNull(target.files?.get(0)) { "Aucun fichier n'a été trouvé : $target" }

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
	input(InputType.hidden, name = id) {
		attrs {
			this.id = id
			value = simpleInputState ?: ""
		}
	}
}

private val Field: FunctionComponent<FieldProps> = fc("Field") { props ->
	traceRenders("Field ${props.fieldKey}")
	if (props.field.arity.max == 1) {
		styledField(props.id, props.field.name) {
			child(RenderField) {
				attrs {
					this.form = props.form
					this.root = props.root
					this.field = props.field
					this.id = props.id
					this.fieldKey = "${props.fieldKey}:${props.id}"
					this.input = props.input
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
							this.input = props.input
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
	form: Form?,
	root: Action?,
	field: Field,
	input: ((String, String) -> Unit)? = null,
	id: String? = null,
	key: String? = null,
) = child(Field) {
	attrs {
		this.field = field
		this.id = id ?: field.id
		this.form = form
		this.root = root
		this.fieldKey = key ?: this.id
		this.input = input
	}
}
