package formulaide.client.routes

import formulaide.api.bones.toLegacy
import formulaide.api.users.Service
import formulaide.client.Client
import formulaide.core.Department
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
		.map { it.toLegacy() }
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
		.map { it.toLegacy() }

/**
 * Creates a new service with the given [name].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.createService(name: String): Service {
	val ref = departments.create(name)
	return ref.requestValue().toLegacy()
}

/**
 * Closes a [service].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.closeService(service: Service): Service {
	val ref = Department.Ref(service.id, departments)
	departments.close(ref)
	return ref.requestValue().toLegacy()
}

/**
 * Opens a [service].
 *
 * > This method is part of the legacy API.
 * > It will be deprecated in the future.
 * > See [Client.departments]
 */
suspend fun Client.Authenticated.reopenService(service: Service): Service {
	val ref = Department.Ref(service.id, departments)
	departments.open(ref)
	return ref.requestValue().toLegacy()
}
