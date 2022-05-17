package formulaide.client.routes

import formulaide.api.types.Email
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.api.users.User
import formulaide.client.runTest
import formulaide.client.testAdministrator
import formulaide.client.testClient
import formulaide.client.testEmployee
import kotlin.random.Random
import kotlin.test.*

class UsersTest {

	@Test
	fun loginEmployee() = runTest {
		val client = testClient()
		val login = client.login(
			PasswordLogin("employee-development-password", "employee@formulaide")
		)
		println(login)
		assertTrue(true) // on failure, exceptions are thrown previously
	}

	@Test
	fun loginAdmin() = runTest {
		val anonymous = testClient()
		val token = anonymous.login(
			PasswordLogin("admin-development-password", "admin@formulaide")
		)
		println(token)
		assertTrue(true) // on failure, exceptions are thrown previously
	}

	@Test
	fun createUser() = runTest {
		val client = testAdministrator()

		val email = Email("mon email ${Random.nextInt()} @zut")
		val response = client.createUser(NewUser("mon mot de passe",
		                                         User(
			                                         email,
			                                         "Mon Identité",
			                                         client.listServices().mapTo(HashSet()) { it.createRef() },
			                                         false
		                                         )))

		println(response)
		assertTrue(true) // on failure, exceptions are thrown previously
	}

	@Test
	fun createUserUnauthenticated() = runTest {
		val client = testEmployee()

		assertFails {
			client.createUser(
				NewUser(
					"mon mot de passe",
					User(
						Email("mon email ${Random.nextInt()}"),
						"Mon Identité",
						client.listServices().mapTo(HashSet()) { it.createRef() },
						false
					)
				)
			)
		}
	}

	@Test
	fun getMeEmployee() = runTest {
		val client = testEmployee()

		val user = client.getMe()

		assertFalse(user.administrator)
		assertEquals("employee@formulaide", user.email.email)
		assertEquals("Employé", user.fullName)
	}

	@Test
	fun getMeAdministrator() = runTest {
		val client = testAdministrator()

		val user = client.getMe()

		assertTrue(user.administrator)
		assertEquals("admin@formulaide", user.email.email)
		assertEquals("Administrateur", user.fullName)
	}

}
