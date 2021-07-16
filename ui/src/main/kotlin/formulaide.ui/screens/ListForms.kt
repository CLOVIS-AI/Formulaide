package formulaide.ui.screens

import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import react.functionalComponent

val FormList = functionalComponent<ScreenProps> { props ->
	val forms = props.forms

	styledCard(
		"Formulaires",
		null,
		contents = {
			for (form in forms) {
				styledField("form-${form.id}", form.name) {
					styledButton("Remplir") { props.navigateTo(Screen.SubmitForm(form.createRef())) }
				}
			}
		}
	)

}
