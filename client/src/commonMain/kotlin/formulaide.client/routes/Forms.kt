package formulaide.client.routes

import formulaide.api.data.Form
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
suspend fun Client.listForms() =
	get<List<Form>>("/forms/list")

/**
 * List of public and internal [forms][Form].
 *
 * - GET /forms/listPublicInternal
 * - Requires 'employee' authentication
 * - Response: list of [Form]
 *
 * The forms are always [open][Form.open].
 */
suspend fun Client.Authenticated.listAllForms() =
	get<List<Form>>("/forms/listPublicInternal")

/**
 * List of closed [forms][Form].
 *
 * - GET /forms/listClosed
 * - Requires 'administrator' authentication
 * - Response: list of [Form]
 */
suspend fun Client.Authenticated.listClosedForms() =
	get<List<Form>>("/forms/listClosed")

/**
 * Creates a [form].
 *
 * - POST /forms/create
 * - Requires 'administrator' authentication
 * - Body: [Form]
 * - Response: [Form]
 */
suspend fun Client.Authenticated.createForm(form: Form) =
	post<Form>("/forms/create", body = form)
