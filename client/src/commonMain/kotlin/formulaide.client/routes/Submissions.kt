package formulaide.client.routes

import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.client.Client

/**
 * Uploads the user's answers related to a specific [Form].
 *
 * - POST /submissions/create
 * - Body: [FormSubmission]
 * - Response: `"Success"`
 *
 * @see Form
 * @see FormSubmission
 * @see createSubmission
 */
suspend fun Client.submitForm(submission: FormSubmission): String =
	post("/submissions/create", body = submission)
