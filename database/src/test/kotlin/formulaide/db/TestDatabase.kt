package formulaide.db

import kotlinx.coroutines.Job

/**
 * Connects to the test database.
 */
fun testDatabase() = Database(
	"localhost",
	27017,
	"formulaide-test",
	"root",
	"development-password",
	Job()
)

/**
 * Creates and returns a service that can be used for tests.
 */
suspend fun Database.testService() = departments.create("Service des tests")
