package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import formulaide.api.bones.ApiNewUser
import formulaide.api.bones.ApiPasswordLogin
import formulaide.db.document.toCore
import kotlinx.coroutines.runBlocking
import opensavvy.backbone.Ref.Companion.requestValueOrThrow
import opensavvy.state.firstValueOrThrow
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthTest {

	@Test
	fun testHashingManual() {
		val password = "1234"
		val bcryptHashString = BCrypt.withDefaults()
			.hashToString(12, password.toCharArray())

		val result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHashString)
		assertTrue(result.verified)
	}

	@Test
	fun testHashing() {
		val message = "This is a super secret message"
		println(message)

		val hashed1 = Auth.hash(message)
		val hashed2 = Auth.hash(message)
		println("Hashes:\n - $hashed1\n - $hashed2")

		assertTrue(Auth.checkHash(message, hashed1))
		assertTrue(Auth.checkHash(message, hashed2))
	}

	@Test
	fun testAuth() = runBlocking {
		val db = testDatabase()
		val auth = Auth(db)
		val service = db.departments.create("Service des tests").firstValueOrThrow()

		val email = "new${Random.nextInt()}@ville-arcachon.fr"
		val password = "this is my super-safe password"

		// Creating the account

		val user = ApiNewUser(email, "Auth Test User", setOf(service), false, password)
		val dbUser1 = auth.newAccount(user)

		// Logging in

		val (token2, _, dbUser2) = auth.login(ApiPasswordLogin(email, password))

		// Checking token validity

		assertEquals(
			dbUser1.requestValueOrThrow(),
			dbUser2.toCore(db),
			"I should retrieve the same user as the one that was created"
		)
		assertNotNull(auth.checkToken(token2))
		Unit
	}

}
