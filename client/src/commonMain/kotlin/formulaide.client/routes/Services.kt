package formulaide.client.routes

import formulaide.api.bones.ApiDepartment.Companion.toApi
import formulaide.api.bones.ApiDepartment.Companion.toLegacy
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.client.bones.DepartmentRef
import opensavvy.backbone.Ref.Companion.requestValue

/**
 * Finds the list of all services.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.listServices(): Set<Service> =
	departments.all()
		.map { it.requestValue() }
		.map { it.toApi().toLegacy() }
		.toSet()

/**
 * Finds the list of all services, including those are that closed.
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.listAllServices(): List<Service> =
	departments.all(includeClosed = true)
		.map { it.requestValue() }
		.map { it.toApi().toLegacy() }

/**
 * Creates a new service with the given [name].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.createService(name: String): Service {
	val ref = departments.create(name)
	return ref.requestValue().toApi().toLegacy()
}

/**
 * Closes a [service].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.closeService(service: Service): Service {
	val ref = DepartmentRef(service.id.toInt(), departments)
	departments.close(ref)
	return ref.requestValue().toApi().toLegacy()
}

/**
 * Opens a [service].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.reopenService(service: Service): Service {
	val ref = DepartmentRef(service.id.toInt(), departments)
	departments.open(ref)
	return ref.requestValue().toApi().toLegacy()
}
