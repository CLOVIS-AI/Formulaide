package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.submitForm
import formulaide.ui.*
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledFormCard
import formulaide.ui.components2.useAsync
import formulaide.ui.fields.field
import formulaide.ui.utils.text
import kotlinx.html.id
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLFormElement
import org.w3c.xhr.FormData
import react.RProps
import react.fc
import react.useEffectOnce
import react.useState

@Suppress("FunctionName")
fun SubmitForm(formRef: Ref<Form>) = fc<RProps> {
	val forms by useForms()
	val scope = useAsync()
	val client by useClient()
	val composites by useComposites()
	val (_, navigateTo) = useNavigation()

	val formRefState by useState(formRef)

	if (!formRefState.loaded) {
		console.info("The current page refers to an unloaded form")

		val referencedForm = forms.find { it.id == formRefState.id }
		if (referencedForm != null)
			formRefState.load(referencedForm)
	}

	if (!formRefState.loaded) {
		console.info("Couldn't find which form is referenced by the current page")

		styledCard(
			"Formulaire",
			null,
		) { text("Chargement du formulaire…") }

		return@fc
	}

	val form = formRef.obj
	require(form.open) { "Impossible de saisir des données dans un formulaire fermé" }
	var referencedComposites by useState(emptyList<Composite>())

	useEffectOnce {
		if (referencedComposites.isEmpty())
			scope.reportExceptions {
				referencedComposites = client.compositesReferencedIn(form)
			}
	}

	try {
		form.load(composites + referencedComposites)
	} catch (e: RuntimeException) {
		console.warn("Cannot load form submission at the moment", e)

		styledCard(
			"Formulaire",
			null,
		) { text("Chargement des données référencées…") }

		return@fc
	}

	styledFormCard(
		form.name,
		"Ce formulaire est ${if (form.public) "public" else "interne"}, les champs marqués par une * sont obligatoires.",
		submit = "Envoyer",
		contents = {
			for (field in form.mainFields.fields.sortedBy { it.order }) {
				field(field)
			}
		},
	) {
		id = "form-submission"

		onSubmitFunction = { event ->
			event.preventDefault()

			val submission = reportExceptions {

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
					data = answers
				).also { it.checkValidity(form) }
			}

			scope.reportExceptions {
				client.submitForm(submission)

				navigateTo(Screen.ShowForms)
			}
		}
	}
}
