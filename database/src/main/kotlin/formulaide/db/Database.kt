package formulaide.db

import formulaide.api.data.Alert
import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.Record
import formulaide.core.Department
import formulaide.core.field.FlatField
import formulaide.core.form.Template
import formulaide.db.document.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
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
	private val fieldCollection = database.getCollection<FlatField.Container>("fields")
	private val templateCollection = database.getCollection<Template>("templates2")
	private val formCollection = database.getCollection<formulaide.core.form.Form>("forms2")
	private val recordCollection = database.getCollection<DbRecord2>("records2")

	internal val data = database.getCollection<Composite>("data")
	internal val legacyForms = database.getCollection<Form>("forms")
	internal val legacySubmissions = database.getCollection<DbSubmission>("submissions")
	internal val legacyRecords = database.getCollection<Record>("records")
	internal val uploads = database.getCollection<DbFile>("uploads")
	internal val alerts = database.getCollection<Alert>("alerts")

	val departments = Departments(
		serviceCollection,
		defaultRefCache<Department>()
			.cachedInMemory(job)
			.expireAfter(1.minutes, job)
	)

	val users = Users(
		this,
		userCollection,
		defaultRefCache()
	)

	val fields = Fields(
		fieldCollection,
		defaultRefCache(),
	)

	val templates = Templates(
		templateCollection,
		defaultRefCache<Template>()
			.cachedInMemory(job)
			.expireAfter(10.minutes, job),
	)

	val forms = Forms(
		formCollection,
		defaultRefCache<formulaide.core.form.Form>()
			.cachedInMemory(job)
			.expireAfter(10.minutes, job)
	)

	val records = Records(
		recordCollection,
		forms,
		users,
		defaultRefCache<formulaide.core.record.Record>()
			.cachedInMemory(job)
			.expireAfter(30.minutes, job)
	)

	init {
		CoroutineScope(job + Dispatchers.IO).launch { autoExpireFiles() }
	}

	private fun getParam(environmentVariable: String, defaultValue: String?): String =
		System.getenv(environmentVariable)
			?: defaultValue
			?: error("The database parameter '$environmentVariable' was found neither in environment variables nor constructor arguments.")
}
