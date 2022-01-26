package formulaide.ui.screens

import formulaide.api.data.Form
import formulaide.api.types.Ref
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.submitForm
import formulaide.ui.*
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.submit
import formulaide.ui.components.text.ErrorText
import formulaide.ui.components.useAsync
import formulaide.ui.fields.renderers.Field
import formulaide.ui.utils.parseHtmlForm
import kotlinx.coroutines.launch
import react.FC
import react.Props
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState

@Suppress("FunctionName")
fun SubmitForm(formRef: Ref<Form>) = FC<Props>("SubmitForm") {
	traceRenders("SubmitForm")

	val forms by useForms()
	val client by useClient()
	val composites by useComposites()

	var failedRef by useState(false)

	useEffect(forms) {
		if (!formRef.loaded) {
			console.info("The current page refers to an unloaded form: ${formRef.id}")

			val referencedForm = forms.find { it.id == formRef.id }
			if (referencedForm != null)
				formRef.load(referencedForm)
			else failedRef = true
		}
	}

	val form = if (formRef.loaded) formRef.obj else null

	/**
	 * - `null`: first render, no attempts to load have been made
	 * - `true`: the form is loaded
	 * - `false`: the form couldn't be loaded using the composite cache
	 */
	var formLoadedFromCache by useState<Boolean>()

	useEffect(form, composites, formLoadedFromCache) {
		if (form != null && formLoadedFromCache != true) {
			formLoadedFromCache = try {
				console.info("Loading composites of this form from the cache")
				form.load(composites, lazy = true)
				true
			} catch (e: Exception) {
				console.log("Failed to load the form using the composites in the cache.", e)
				false
			}
		}
	}

	/** Same as `formLoadedFromCache`. */
	var formLoadedFromServer by useState<Boolean>()
	val scope = useAsync()

	useEffect(form, formLoadedFromCache) {
		if (form != null && formLoadedFromCache == false && formLoadedFromServer != true)
			scope.launch {
				formLoadedFromServer = try {
					console.info("Loading composites of this form by asking the server")
					val referencedComposites = formulaide.ui.reportExceptions {
						client.compositesReferencedIn(form)
					}
					form.load(referencedComposites)
					true
				} catch (e: Exception) {
					console.error("Failed to load the form using referenced composites from the server.",
					              e)
					false
				}
			}
	}

	// No hooks from here on

	if (failedRef) {
		ErrorText { text = "Formulaire introuvable. Vous n'avez peut-être pas les droits d'y accéder ?" }
		return@FC
	}

	if (form == null) {
		p {
			+"Chargement du formulaire…"
			LoadingSpinner()
		}
		return@FC
	}
	traceRenders("SubmitForm … the form is known")

	if (!form.open) {
		ErrorText { text = "Ce formulaire a été fermé, il ne peut plus être rempli." }
		return@FC
	}
	traceRenders("SubmitForm … the form is open")

	if (formLoadedFromCache == null) {
		p {
			+"Chargement des champs depuis le cache…"
			LoadingSpinner()
		}
		return@FC
	}
	traceRenders("SubmitForm … the form is not currently loading from the cache")

	if (formLoadedFromCache != true && formLoadedFromServer == null) {
		p {
			+"Chargement des champs depuis le serveur…"
			LoadingSpinner()
		}
		return@FC
	}
	traceRenders("SubmitForm … the form is not currently loading from the server")

	if (formLoadedFromServer == false) {
		ErrorText {
			text =
				"Le chargement des données composées référencées a échoué. Veuillez signaler ce problème à l'administrateur."
		}
		return@FC
	}
	traceRenders("SubmitForm … the form is loaded")

	FormCard {
		title = form.name
		subtitle =
			"Ce formulaire est ${if (form.public) "public" else "interne"}, les champs marqués par une * sont obligatoires."

		submit("Envoyer") { htmlFormElement ->
			val submission = parseHtmlForm(
				htmlFormElement,
				form = form,
				root = null,
			)

			launch {
				client.submitForm(submission)

				navigateTo(Screen.ShowForms)
			}
		}

		for (field in form.mainFields.fields.sortedBy { it.order }) {
			Field {
				this.form = form
				this.field = field
				this.id = field.id
			}
		}
	}
}
