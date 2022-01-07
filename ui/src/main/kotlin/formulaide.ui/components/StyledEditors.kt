package formulaide.ui.components

import formulaide.ui.components.fields.Nesting
import react.ChildrenBuilder

fun ChildrenBuilder.styledFieldEditorShell(
	id: String,
	displayName: String,
	contents: ChildrenBuilder.() -> Unit,
) {
	styledField(id, displayName) {
		Nesting {
			contents()
		}
	}
}
