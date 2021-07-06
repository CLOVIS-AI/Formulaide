package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.routes.submitForm
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledFormCard
import formulaide.ui.fields.field
import formulaide.ui.launchAndReportExceptions
import formulaide.ui.reportExceptions
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLFormElement
import org.w3c.xhr.FormData
import react.functionalComponent

@Suppress("FunctionName")
fun SubmitForm(form: Form) = functionalComponent<ScreenProps> { props ->
	require(form.open) { "Impossible de saisir des données dans un formulaire fermé" }

	form.validate(props.composites)

	styledFormCard(
		form.name,
		"Ce formulaire est ${if (form.public) "public" else "interne"}, les champs marqués par une * sont obligatoires.",
		submit = "Envoyer",
		contents = {
			for (field in form.mainFields.fields) {
				field(props, field)
			}
		},
	) {
		id = "form-submission"

		onSubmitFunction = { event ->
			event.preventDefault()

			val submission = reportExceptions(props) {

				@Suppress("UNUSED_VARIABLE") // used in 'js' function
				val formData = FormData(event.target as HTMLFormElement)

				//language=JavaScript
				val formDataObject = js("""Object.fromEntries(formData.entries())""")

				@Suppress("JSUnresolvedVariable") //language=JavaScript
				val formDataArray = js("""Object.keys(formDataObject)""") as Array<String>
				val answers = formDataArray.associateWith { (formDataObject[it] as String) }
					.filterValues { it.isNotBlank() }

				FormSubmission(
					form.createRef(),
					answers
				).also { it.checkValidity(form) }
			}

			launchAndReportExceptions(props) {
				props.client.submitForm(submission)

				props.navigateTo(Screen.ShowForms)
			}
		}
	}
}
