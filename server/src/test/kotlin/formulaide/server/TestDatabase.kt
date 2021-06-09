package formulaide.server

import formulaide.db.Database

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
