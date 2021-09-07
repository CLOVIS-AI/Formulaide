package formulaide.client.routes

import formulaide.api.data.Composite
import formulaide.api.data.CompositeMetadata
import formulaide.client.Client

/**
 * Finds the list of all available data.
 *
 * - GET /data/list
 * - Requires 'employee' authentication
 * - Response: list of [Composite]
 */
suspend fun Client.Authenticated.listData(): List<Composite> =
	get("/data/list")

/**
 * Creates a new [Composite].
 *
 * - POST /data/create
 * - Requires 'administrator' authentication
 * - Body: [Composite]
 * - Response: [Composite]
 */
suspend fun Client.Authenticated.createData(newData: Composite): Composite =
	post("/data/create", body = newData)

/**
 * Edits a [Composite].
 *
 * - POST /data/editMetadata
 * - Requires 'administrator' authentication
 * - Body: [CompositeMetadata]
 * - Response: "SUCCESS"
 */
suspend fun Client.Authenticated.editData(edit: CompositeMetadata): String =
	post("/data/editMetadata", body = edit)
