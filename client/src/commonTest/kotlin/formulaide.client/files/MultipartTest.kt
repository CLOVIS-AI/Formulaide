package formulaide.client.files

import formulaide.api.data.Action
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField
import formulaide.api.fields.SimpleField.Upload.Format.IMAGE
import formulaide.api.types.Arity
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.UploadRequest
import formulaide.client.routes.createForm
import formulaide.client.routes.getMe
import formulaide.client.routes.uploadFile
import formulaide.client.testAdministrator
import formulaide.client.testEmployee

// This is voluntarily not @Test
// Each platform should call this method with an appropriate file
suspend fun multipartTest(file: FileUpload) {
	val admin = testAdministrator()
	val client = testEmployee()

	lateinit var image: ShallowFormField.Simple
	val form = form(
		name = "test d'envoi de fichiers",
		public = false,
		mainFields = formRoot {
			image = simple("Image", SimpleField.Upload(
				Arity.mandatory(),
				allowedFormats = listOf(IMAGE),
			))
		},
		Action(
			"1",
			order = 0,
			reviewer = client.getMe().service,
			name = "Dossiers acceptés",
		)
	).let { admin.createForm(it) }

	client.uploadFile(
		UploadRequest(form.createRef(), null, image.id),
		file,
	)
}
