package formulaide.db.document

import formulaide.api.users.Service
import formulaide.db.Database
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import kotlin.random.Random

/**
 * Id of a [DbService].
 */
typealias DbServiceId = Int

/**
 * Database representation of a [Service].
 *
 * @property open Whether this service is currently opened or not. Only the administrator can see closed services.
 */
@Serializable
data class DbService(
	val name: String,
	val id: DbServiceId,
	val open: Boolean,
)

suspend fun Database.findService(id: DbServiceId): DbService? =
	services.findOne(DbService::id eq id)

suspend fun Database.createService(name: String): DbService {
	var id: Int
	do {
		id = Random.nextInt()
	} while (findService(id) != null)

	val insert = DbService(name, id, true)

	services.insertOne(insert)

	return insert
}

/**
 * Opens or closes a [DbService].
 *
 * @param open When `true`, opens the service, when `false`, closes the service.
 */
suspend fun Database.manageService(service: DbServiceId, open: Boolean) {
	services.updateOne(DbService::id eq service, setValue(DbService::open, open))
}

/**
 * Finds all the currently opened services.
 *
 * To also find [closed services][DbService.open], see [allServicesIgnoreOpen].
 */
suspend fun Database.allServices(): List<DbService> =
	services.find(DbService::open eq true).toList()

/**
 * Finds all the services, no matter if they are [open][DbService.open] or not.
 *
 * @see allServices
 */
suspend fun Database.allServicesIgnoreOpen(): List<DbService> =
	services.find().toList()

fun DbService.toApi() = Service(id.toString(), name, open)
