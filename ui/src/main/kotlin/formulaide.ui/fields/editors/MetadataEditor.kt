package formulaide.ui.fields.editors

import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import react.FC
import react.useMemo

val MetadataEditor = FC<EditableFieldProps>("MetadataEditor") { props ->
	val field = props.field
	val defaultField = useMemo(field) {
		field.requestCopy(name = "Valeur par défaut", arity = Arity.optional())
	}

	if (field !is Field.Simple) return@FC // Only simple fields can have metadata

	val simple = field.simple
	when (simple) {
		is SimpleField.Text -> TextMetadataEditor { +props }
		is SimpleField.Integer -> IntegerMetadataEditor { +props }
		is SimpleField.Upload -> UploadMetadataEditor { +props }

		// Fields that do not have metadata can be ignored
		is SimpleField.Decimal, is SimpleField.Boolean, is SimpleField.Email,
		is SimpleField.Phone, is SimpleField.Date, is SimpleField.Time,
		is SimpleField.Message,
		-> Unit
	}

	if (simple !is SimpleField.Upload && simple != SimpleField.Message) {
		formulaide.ui.fields.renderers.Field {
			this.field = defaultField
			this.onInput = { _, value ->
				props.replace(field.requestCopy(simple.requestCopy(defaultValue = value)))
			}
		}
	}
}

internal fun metadataEditorId(fieldKey: String, attributeName: String) =
	"field-editor-metadata-${fieldKey}-$attributeName"
