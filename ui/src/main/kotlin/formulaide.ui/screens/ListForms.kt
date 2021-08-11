package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.todoListFor
import formulaide.ui.*
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledNesting
import formulaide.ui.components.useAsync
import formulaide.ui.utils.text
import react.*
import react.dom.div
import react.dom.p

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

	p { text(form.name) }
	styledNesting(depth = 0) {

		div {
			text("Actions : ")

			styledButton("Remplir") { navigateTo(Screen.SubmitForm(form.createRef())) }
		}

		div {
			text("Dossiers : ")

			for (action in form.actions.sortedBy { it.order }) {
				child(ActionDescription) {
					attrs {
						this.form = form
						this.state = RecordState.Action(action.createRef())
					}
				}
			}

			child(ActionDescription) {
				attrs {
					this.form = form
					this.state = RecordState.Refused
				}
			}
		}
	}
}

internal external interface ActionDescriptionProps : RProps {
	var form: Form
	var state: RecordState
}

internal val ActionDescription = fc<ActionDescriptionProps> { props ->
	val form = props.form
	val state = props.state

	val (client) = useClient()
	val user by useUser()
	val scope = useAsync()
	val (_, navigateTo) = useNavigation()

	var records by useState(emptyList<Record>())
	useEffect(client, user) {
		if (user != null && client is Client.Authenticated)
			scope.reportExceptions {
				records = client.todoListFor(form, state)
			}
	}

	val stateName = when (state) {
		is RecordState.Action -> state.displayName()
			.takeIf { user?.service?.id == state.current.obj.reviewer.id }
		is RecordState.Refused -> state.displayName()
	}
	if (stateName != null) {
		val title = stateName +
				when {
					records.size == 1 -> " (1 dossier)"
					records.size == Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION -> " (${Record.Companion.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION}+ dossiers)"
					records.isNotEmpty() -> " (${records.size} dossiers)"
					else -> ""
				}

		styledButton(title, action = { navigateTo(Screen.Review(form, state, records)) })
	}
}
