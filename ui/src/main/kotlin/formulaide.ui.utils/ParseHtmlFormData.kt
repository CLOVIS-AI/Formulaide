package formulaide.ui.utils

import formulaide.api.data.Action
import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import org.w3c.dom.HTMLFormElement
import org.w3c.xhr.FormData

fun parseHtmlForm(htmlForm: HTMLFormElement, form: Form, root: Action?): FormSubmission {
	@Suppress("UNUSED_VARIABLE") // used in 'js' function
	val formData = FormData(htmlForm)

	//language=JavaScript
	val formDataObject = js("""Object.fromEntries(formData.entries())""")

	@Suppress("JSUnresolvedVariable") //language=JavaScript
	val formDataArray = js("""Object.keys(formDataObject)""") as Array<String>
	val answers = formDataArray.associateWith { (formDataObject[it] as String) }
		.filterValues { it.isNotBlank() }

	return FormSubmission(
		Ref.SPECIAL_TOKEN_NEW,
		form.createRef(),
		data = answers.mapValues { (_, v) -> v.trim() },
		root = root?.createRef(),
	).also { it.parse(form) }
}
