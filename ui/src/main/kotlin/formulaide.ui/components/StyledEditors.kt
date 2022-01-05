package formulaide.ui.components

import react.ChildrenBuilder

fun ChildrenBuilder.styledFieldEditorShell(
	id: String,
	displayName: String,
	contents: ChildrenBuilder.() -> Unit,
) {
	styledField(id, displayName) {
		styledNesting {
			contents()
		}
	}
}
