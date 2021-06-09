package formulaide.db

import formulaide.db.document.createService

/**
 * Connects to the test database.
 */
fun testDatabase() = Database(
	"localhost",
	27017,
	"formulaide-test",
	"root",
	"development-password"
)

/**
 * Creates and returns a service that can be used for tests.
 */
suspend fun Database.testService() = createService("Service des tests")
