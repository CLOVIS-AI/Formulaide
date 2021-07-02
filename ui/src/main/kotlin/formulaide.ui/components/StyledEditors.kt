package formulaide.ui.components

import react.RBuilder

fun RBuilder.styledFieldEditorShell(
	id: String,
	displayName: String,
	contents: RBuilder.() -> Unit,
) {
	styledField(id, displayName) {
		styledNesting {
			contents()
		}
	}
}
