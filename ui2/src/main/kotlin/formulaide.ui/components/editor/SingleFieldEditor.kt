package formulaide.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import formulaide.ui.components.TextButton
import formulaide.ui.components.TextField
import formulaide.ui.theme.Theme
import opensavvy.formulaide.core.Field
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

@Composable
fun SingleFieldEditor(
	root: MutableField,
	id: Field.Id,
	field: MutableField,
	onReplace: (Field.Id, MutableField) -> Unit,
	onSelect: (Field.Id) -> Unit,
) {
	Div(
		{
			style {
				color(Theme.current.secondaryContainer.content.css)
				backgroundColor(Theme.current.secondaryContainer.background.css)
				padding(10.px)
				borderRadius(10.px)

				display(DisplayStyle.Flex)
				flexDirection(FlexDirection.Column)
				alignItems(AlignItems.Start)
				gap(5.px)
			}
		}
	) {
		var label by field.label

		TextField("Libellé", label, onChange = { label = it })

		if (field.importedFrom == null) {
			SelectFieldType(field, onReplace = { onReplace(id, it) })
			FieldMetadataEditor(field)
		}

		if (field.importedFrom == null && id.parts.isNotEmpty()) {
			TextButton(onClick = {
				val parent = findFieldById(root, id, last = id.parts.size - 1)
				(parent.fields as MutableMap).remove(id.parts.last())
				onSelect(Field.Id(id.parts.dropLast(1)))
			}) {
				Text("Supprimer")
			}
		}
	}
}
