package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

fun testDatabase() = Database(
	hostname = "localhost",
	port = "27017",
	username = "root",
	password = "development-password",
	database = "formulaide2-test",
)

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseTest {

	@Test
	fun connect() = runTest {
		val database = testDatabase()

		// The test fails if this call throws an error
		val collections = database.client.listCollectionNames()

		if (collections.isEmpty())
			println("No collections already exist.")
		else
			println("Existing collections:${collections.joinToString(separator = "\n - ", prefix = "\n - ")}")
	}

}
