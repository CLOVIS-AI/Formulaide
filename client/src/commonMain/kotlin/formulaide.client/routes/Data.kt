package formulaide.client.routes

import formulaide.api.data.CompoundData
import formulaide.api.data.NewCompoundData
import formulaide.client.Client

/**
 * Finds the list of all available data.
 *
 * - GET /data/list
 * - Requires 'employee' authentication
 * - Response: list of [CompoundData]
 */
suspend fun Client.Authenticated.listData() =
	get<List<CompoundData>>("/data/list")

/**
 * Creates a new [CompoundData].
 *
 * - POST /data/create
 * - Requires 'administrator' authentication
 * - Body: [NewCompoundData]
 * - Response: [CompoundData]
 */
suspend fun Client.Authenticated.createData(newData: NewCompoundData) =
	post<CompoundData>("/data/create", body = newData)
