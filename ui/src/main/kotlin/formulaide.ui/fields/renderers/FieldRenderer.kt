package formulaide.ui.fields.renderers

import formulaide.api.fields.Field
import formulaide.api.fields.FormField
import react.FC

val FieldRenderer = FC<FieldProps>("FieldRenderer") { props ->
	when (val field = props.field) {
		is Field.Simple -> SimpleFieldRenderer { +props }
		is FormField.Composite -> CompositeFormFieldRenderer { +props }
		is Field.Union<*> -> UnionFieldRenderer { +props }
		else -> error("Cannot render unknown field type ${field::class}: $field")
	}
}
