package formulaide.db

import formulaide.api.data.Alert
import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.core.Department
import formulaide.db.document.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import opensavvy.backbone.Cache
import opensavvy.backbone.cache.ExpirationCache.Companion.expireAfter
import opensavvy.backbone.cache.MemoryCache.Companion.cachedInMemory
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.time.Duration.Companion.minutes

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
	job: Job,
) {

	private val host = getParam("formulaide_host", host)
	private val port = getParam("formulaide_port", port.toString()).toInt()
	private val databaseName = getParam("formulaide_database", databaseName)
	private val username = getParam("formulaide_username", username)
	private val password = getParam("formulaide_password", password)

	private val client =
		KMongo.createClient("mongodb://${this.username}:${this.password}@${this.host}:${this.port}").coroutine
	private val database = client.getDatabase(this.databaseName)

	private val userCollection = database.getCollection<DbUser>("users")
	private val serviceCollection = database.getCollection<DbService>("services")
	internal val data = database.getCollection<Composite>("data")
	internal val legacyForms = database.getCollection<Form>("forms")
	internal val legacySubmissions = database.getCollection<DbSubmission>("submissions")
	internal val records = database.getCollection<Record>("records")
	internal val uploads = database.getCollection<DbFile>("uploads")
	internal val alerts = database.getCollection<Alert>("alerts")

	val departments = Departments(
		serviceCollection,
		Cache.Default<Department>()
			.cachedInMemory(job)
			.expireAfter(1.minutes, job)
	)

	val users = Users(
		this,
		userCollection,
		Cache.Default()
	)

	init {
		CoroutineScope(job + Dispatchers.IO).launch { autoExpireFiles() }
	}

	private fun getParam(environmentVariable: String, defaultValue: String?): String =
		System.getenv(environmentVariable)
			?: defaultValue
			?: error("The database parameter '$environmentVariable' was found neither in environment variables nor constructor arguments.")
}
