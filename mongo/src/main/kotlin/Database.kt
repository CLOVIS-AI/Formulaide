package opensavvy.formulaide.mongo

import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Database(
	hostname: String,
	port: String,
	username: String,
	password: String,
	database: String,
) {

	internal val client = KMongo.createClient("mongodb://$username:$password@$hostname:$port")
		.coroutine
		.getDatabase(database)

}
