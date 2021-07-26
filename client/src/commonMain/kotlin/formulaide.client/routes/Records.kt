package formulaide.client.routes

import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.api.data.RecordState
import formulaide.api.data.RecordsToReviewRequest
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client

/**
 * The list of [forms][Form] that the user should take a look at.
 *
 * - GET /submissions/formsToReview
 * - Requires 'employee' authentication
 * - Response: list of [Form]
 */
suspend fun Client.Authenticated.todoList(): List<Form> =
	get("/submissions/formsToReview")

/**
 * The list of [records][Record] that are in a given [state], in a given [form].
 *
 * - POST /submissions/RecordsToReview
 * - Requires 'employee' authentication
 * - Body: [RecordsToReviewRequest]
 * - Response: list of [Record]
 */
suspend fun Client.Authenticated.todoListFor(form: Form, state: RecordState): List<Record> =
	post("/submissions/recordsToReview", body = RecordsToReviewRequest(form.createRef(), state))
