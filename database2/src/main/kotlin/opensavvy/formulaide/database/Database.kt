package opensavvy.formulaide.database

import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import opensavvy.formulaide.database.document.Departments
import opensavvy.formulaide.database.document.Forms
import opensavvy.formulaide.database.document.Templates
import opensavvy.formulaide.database.document.Users
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Database(context: CoroutineContext) {

	private val client: CoroutineClient

	init {
		val host = getParam("formulaide_host")
		val port = getParam("formulaide_port")
		val username = getParam("formulaide_username")
		val password = getParam("formulaide_password")

		client = KMongo.createClient("mongodb://$username:$password@$host:$port")
			.coroutine
	}

	private val database = client.getDatabase(getParam("formulaide_database") + "2")

	val departments = Departments(
		database.getCollection("departments"),
		defaultRefCache<Department>()
			.cachedInMemory(context)
			.expireAfter(1.minutes, context)
	)

	val users = Users(
		database.getCollection("users"),
		defaultRefCache<User>()
			.cachedInMemory(context)
			.expireAfter(5.minutes, context),
		context,
		this,
	)

	val templates = Templates(
		database.getCollection("templates"),
		cache = defaultRefCache<Template>()
			.cachedInMemory(context)
			.expireAfter(10.minutes, context),
		versionCache = defaultRefCache<Template.Version>()
			.cachedInMemory(context)
			.expireAfter(1.hours, context),
		context,
	)

	val forms = Forms(
		database.getCollection("forms"),
		cache = defaultRefCache<Form>()
			.cachedInMemory(context)
			.expireAfter(10.minutes, context),
		versionCache = defaultRefCache<Form.Version>()
			.cachedInMemory(context)
			.expireAfter(1.hours, context),
		context,
		this,
	)

	private fun getParam(environmentVariable: String): String =
		System.getenv(environmentVariable)
			?: error("The parameter '$environmentVariable' required by the database connector is missing.")

}
