package formulaide.client.routes

import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.types.ReferenceId
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

/**
 * Gets a [FormSubmission] from its [id][submission].
 *
 * - GET /submissions/get
 * - Requires 'employee' authentication
 * - Body: [ReferenceId]
 * - Response: [FormSubmission]
 */
suspend fun Client.Authenticated.findSubmission(submission: ReferenceId): FormSubmission =
	post("/submissions/get", body = submission)
