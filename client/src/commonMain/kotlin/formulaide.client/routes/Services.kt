package formulaide.client.routes

import formulaide.api.users.Service
import formulaide.client.Client

suspend fun Client.Authenticated.listServices() =
	get<List<Service>>("/services/list")
