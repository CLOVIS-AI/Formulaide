package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.types.Ref.Companion.createRef
import formulaide.ui.Screen
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import formulaide.ui.traceRenders
import formulaide.ui.useForms
import formulaide.ui.useNavigation
import react.RProps
import react.child
import react.fc

val FormList = fc<RProps> { _ ->
	traceRenders("FormList")

	val forms by useForms()

	styledCard(
		"Formulaires",
		null,
		contents = {
			for (form in forms) {
				child(FormDescription) {
					attrs {
						this.form = form
					}
				}
			}
		}
	)

}

internal external interface FormDescriptionProps : RProps {
	var form: Form
}

internal val FormDescription = fc<FormDescriptionProps> { props ->
	val form = props.form

	val (_, navigateTo) = useNavigation()

	styledField("form-${form.id}", form.name) {
		styledButton("Remplir") { navigateTo(Screen.SubmitForm(form.createRef())) }
	}
}
