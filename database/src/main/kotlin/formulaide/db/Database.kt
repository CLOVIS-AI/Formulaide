package formulaide.db

import formulaide.api.data.CompoundData
import formulaide.api.data.Form
import formulaide.db.document.DbService
import formulaide.db.document.DbSubmission
import formulaide.db.document.DbUser
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

/**
 * Abstraction over the database connection.
 *
 * The parameters [host], [port], [databaseName], [username] and [password] are
 * overridden by the corresponding environment variables.
 */
class Database(
	host: String? = null,
	port: Int? = null,
	databaseName: String? = null,
	username: String? = null,
	password: String? = null,
) {

	private val host = getParam("formulaide_host", host)
	private val port = getParam("formulaide_port", port.toString()).toInt()
	private val databaseName = getParam("formulaide_database", databaseName)
	private val username = getParam("formulaide_username", username)
	private val password = getParam("formulaide_password", password)

	private val client =
		KMongo.createClient("mongodb://${this.username}:${this.password}@${this.host}:${this.port}").coroutine
	private val database = client.getDatabase(this.databaseName)

	internal val users = database.getCollection<DbUser>("users")
	internal val services = database.getCollection<DbService>("services")
	internal val data = database.getCollection<CompoundData>("data")
	internal val forms = database.getCollection<Form>("forms")
	internal val submissions = database.getCollection<DbSubmission>("submissions")

	private fun getParam(environmentVariable: String, defaultValue: String?): String =
		System.getenv(environmentVariable)
			?: defaultValue
			?: error("The database parameter '$environmentVariable' was found neither in environment variables nor constructor arguments.")
}
