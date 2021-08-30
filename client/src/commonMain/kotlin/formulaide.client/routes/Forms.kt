package formulaide.client.routes

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.client.Client

/**
 * List of public [forms][Form].
 *
 * - GET /forms/list
 * - Response: list of [Form]
 *
 * The forms are always [open][Form.open].
 * @see Form.open
 */
suspend fun Client.listForms(): List<Form> =
	get("/forms/list")

/**
 * List of public and internal [forms][Form].
 *
 * - GET /forms/listPublicInternal
 * - Requires 'employee' authentication
 * - Response: list of [Form]
 *
 * The forms are always [open][Form.open].
 */
suspend fun Client.Authenticated.listAllForms(): List<Form> =
	get("/forms/listPublicInternal")

/**
 * List of closed [forms][Form].
 *
 * - GET /forms/listClosed
 * - Requires 'administrator' authentication
 * - Response: list of [Form]
 */
suspend fun Client.Authenticated.listClosedForms(): List<Form> =
	get("/forms/listClosed")

/**
 * Creates a [form].
 *
 * - POST /forms/create
 * - Requires 'administrator' authentication
 * - Body: [Form]
 * - Response: [Form]
 */
suspend fun Client.Authenticated.createForm(form: Form): Form =
	post("/forms/create", body = form)

/**
 * Fetches all the composites that are referenced in a [form].
 *
 * - POST /forms/references
 * - Body: [Form.id]
 * - Response: list of [Composite]
 */
suspend fun Client.compositesReferencedIn(form: Form): List<Composite> =
	post("/forms/references", body = form.id)

/**
 * Edits form attributes, as specified in [edition].
 *
 * - POST /forms/editMetadata
 * - Body: [FormMetadata]
 * - Response: the modified [Form]
 */
suspend fun Client.Authenticated.editForm(edition: FormMetadata): Form =
	post("/forms/editMetadata", body = edition)
