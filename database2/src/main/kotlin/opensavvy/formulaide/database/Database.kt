package opensavvy.formulaide.database

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Database {

	private val database: CoroutineDatabase

	init {
		val host = getParam("formulaide_host")
		val port = getParam("formulaide_port")
		val name = getParam("formulaide_database") + "2"
		val username = getParam("formulaide_username")
		val password = getParam("formulaide_password")

		database = KMongo.createClient("mongodb://$username:$password@$host:$port").coroutine
			.getDatabase(name)
	}

	private fun getParam(environmentVariable: String): String =
		System.getenv(environmentVariable)
			?: error("The parameter '$environmentVariable' required by the database connector is missing.")

}
