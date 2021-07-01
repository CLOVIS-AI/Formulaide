package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.routes.submitForm
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.styledCard
import formulaide.ui.fields.field
import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLFormElement
import org.w3c.xhr.FormData
import react.dom.attrs
import react.dom.br
import react.dom.form
import react.dom.input
import react.functionalComponent

@Suppress("FunctionName")
fun SubmitForm(form: Form) = functionalComponent<ScreenProps> { props ->
	require(form.open) { "Impossible de saisir des données dans un formulaire fermé" }

	form.validate(props.composites)

	styledCard(
		form.name,
		"Ce formulaire est ${if (form.public) "public" else "interne"}, les champs marqués par une * sont obligatoires.",
	) {
		form {
			for (field in form.mainFields.fields) {
				field(props, field)
			}

			br {}
			input(InputType.submit) {
				attrs {
					value = "Confirmer cette saisie"
				}
			}

			attrs {
				id = "form-submission"

				onSubmitFunction = { event ->
					event.preventDefault()

					@Suppress("UNUSED_VARIABLE") // used in 'js' function
					val formData = FormData(event.target as HTMLFormElement)

					//language=JavaScript
					val formDataObject = js("""Object.fromEntries(formData.entries())""")

					@Suppress("JSUnresolvedVariable") //language=JavaScript
					val formDataArray = js("""Object.keys(formDataObject)""") as Array<String>
					val answers = formDataArray.associate { it to (formDataObject[it] as String) }
						.filterValues { it.isNotBlank() }

					val submission = FormSubmission(
						form.createRef(),
						answers
					)

					submission.checkValidity(form) //TODO: display error message if any

					props.scope.launch {
						props.client.submitForm(submission)

						props.navigateTo(Screen.ShowForms)
					}
				}
			}
		}
	}
}
