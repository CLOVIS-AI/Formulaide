package formulaide.ui.screens

import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.Screen
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import formulaide.ui.traceRenders
import formulaide.ui.useForms
import formulaide.ui.useNavigation
import react.RProps
import react.fc

val FormList = fc<RProps> { _ ->
	traceRenders("FormList")

	val forms by useForms()
	val (_, navigateTo) = useNavigation()

	styledCard(
		"Formulaires",
		null,
		contents = {
			for (form in forms) {
				styledField("form-${form.id}", form.name) {
					styledButton("Remplir") { navigateTo(Screen.SubmitForm(form.createRef())) }
				}
			}
		}
	)

}
