package formulaide.db

import formulaide.db.document.DbService
import formulaide.db.document.DbUser
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

/**
 * Abstraction over the database connection.
 *
 */
class Database(
	host: String,
	port: Int,
	databaseName: String,
	username: String,
	password: String,
) {

	private val client = KMongo.createClient("mongodb://$username:$password@$host:$port").coroutine
	private val database = client.getDatabase(databaseName)

	internal val users = database.getCollection<DbUser>("users")
	internal val services = database.getCollection<DbService>("services")

}
