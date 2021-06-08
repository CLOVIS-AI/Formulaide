package formulaide.db.document

import formulaide.db.Database
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import kotlin.random.Random

typealias DbServiceId = Int

@Serializable
data class DbService(
	val name: String,
	val id: DbServiceId,
)

suspend fun Database.findService(id: DbServiceId): DbService? =
	services.findOne(DbService::id eq id)

suspend fun Database.createService(name: String): DbService {
	var id: Int
	do {
		id = Random.nextInt()
	} while (findService(id) != null)

	val insert = DbService(name, id)

	services.insertOne(insert)

	return insert
}
