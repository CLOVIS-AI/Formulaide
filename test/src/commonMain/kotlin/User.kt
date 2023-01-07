package opensavvy.formulaide.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email.Companion.asEmail
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

//region Test data

internal suspend fun testEmployee(users: User.Service) = withContext(administratorAuth) {
	val number = Random.nextUInt()

	users.create("employee-$number@formulaide".asEmail(), "Employee #$number").orThrow()
}

internal suspend fun testAdministrator(users: User.Service) = withContext(administratorAuth) {
	val number = Random.nextUInt()

	users.create("administrator-$number@formulaide".asEmail(), "Administrator #$number", administrator = true).orThrow()
}

//endregion

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("FunctionName")
abstract class UserTestCases : TestCase<User.Service> {

	private fun generateId() = Random.nextUInt()

	@Test
	@JsName("guestsAccess")
	fun `guests cannot access users`() = runTest {
		val users = new()
		val department = testDepartment(FakeDepartments())

		assertUnauthenticated(users.list(includeClosed = false))
		assertUnauthenticated(users.list(includeClosed = true))

		assertUnauthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = true))
		assertUnauthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = false))

		assertUnauthenticated(testEmployee(users).first.now())

		for (target in listOf(testEmployee(users).first, testAdministrator(users).first)) {
			assertUnauthenticated(target.join(department))
			assertUnauthenticated(target.leave(department))

			assertUnauthenticated(target.enable())
			assertUnauthenticated(target.disable())
			assertUnauthenticated(target.promote())
			assertUnauthenticated(target.demote())

			assertUnauthenticated(target.resetPassword())
			assertUnauthenticated(target.setPassword("old password", Password("new password")))

			assertUnauthenticated(target.logOut(Token("a token")))
		}
	}

	@Test
	@JsName("employeesCreateUsers")
	fun `employees cannot create users`() = runTest(employeeAuth) {
		val users = new()

		assertUnauthorized(users.create("my-email@gmail.com".asEmail(), "Me"))
	}

	@Test
	@JsName("administratorsCreateUsers")
	fun `administrators can create users`() = runTest(administratorAuth) {
		val users = new()

		val employeeEmail = "employee-${generateId()}@gmail.com".asEmail()
		val adminEmail = "admin-${generateId()}@gmail.com".asEmail()

		val employee = assertSuccess(users.create(employeeEmail, "Me", administrator = false)).first
		val administrator = assertSuccess(users.create(adminEmail, "Me", administrator = true)).first

		assertSuccess(employee.now()) {
			assertEquals(employeeEmail, email)
			assertEquals("Me", name)
			assertEquals(false, this.administrator)
			assertEquals(true, active)
			assertEquals(true, singleUsePassword)
		}

		assertSuccess(administrator.now()) {
			assertEquals(adminEmail, email)
			assertEquals("Me", name)
			assertEquals(true, this.administrator)
			assertEquals(true, active)
			assertEquals(true, singleUsePassword)
		}
	}

	@Test
	@JsName("emailDuplication")
	fun `two users cannot have the same email address`() = runTest(administratorAuth) {
		val users = new()
		val email = "my-email-${generateId()}@gmail.com".asEmail()

		assertSuccess(users.create(email, "First version"))
		assertInvalid(users.create(email, "Second version"))
	}

	@Test
	@JsName("deptManagement")
	fun `department management`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first
		val department = testDepartment(FakeDepartments())

		assertSuccess(employee.join(department)) {
			assertSuccess(employee.now()) {
				assertEquals(setOf(department), this.departments)
			}
		}

		assertSuccess(employee.leave(department)) {
			assertSuccess(employee.now()) {
				assertEquals(emptySet(), this.departments)
			}
		}
	}

	@Test
	@JsName("userDisabling")
	fun `user disabling`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first

		assertSuccess(employee.disable()) {
			assertSuccess(employee.now()) {
				assertEquals(false, active)
			}
		}

		assertSuccess(employee.enable()) {
			assertSuccess(employee.now()) {
				assertEquals(true, active)
			}
		}
	}

	@Test
	@JsName("disableYourself")
	fun `cannot disable yourself`() = runTest(administratorAuth) {
		val users = new()
		val me = testAdministrator(users).first

		withContext(Auth(User.Role.Administrator, me)) {
			assertInvalid(me.disable())
		}
	}

	@Test
	@JsName("userPromotion")
	fun `user promotion`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first

		assertSuccess(employee.promote()) {
			assertSuccess(employee.now()) {
				assertEquals(true, administrator)
			}
		}

		assertSuccess(employee.demote()) {
			assertSuccess(employee.now()) {
				assertEquals(false, administrator)
			}
		}
	}

	@Test
	@JsName("demoteYourself")
	fun `cannot demote yourself`() = runTest(administratorAuth) {
		val users = new()
		val me = testAdministrator(users).first

		withContext(Auth(User.Role.Administrator, me)) {
			assertInvalid(me.demote())
		}
	}

	@Test
	@JsName("singleUse")
	fun `single-use password`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUsePassword) = testEmployee(users)

		assertSuccess(employee.now()) {
			assertTrue(this.singleUsePassword)
		}

		val email = employee.now().orThrow().email

		// First usage of the single-use password
		assertSuccess(users.logIn(email, singleUsePassword)) {
			val (ref, token) = this

			assertEquals(employee, ref)
			assertSuccess(users.verifyToken(employee, token))
		}

		// Second usage is blocked
		assertUnauthenticated(users.logIn(email, singleUsePassword))

		// Set a new password to make the account multi-use again
		val password = Password("an-amazing-password")
		withContext(Auth(User.Role.Employee, employee)) {
			assertSuccess(employee.setPassword(singleUsePassword.value, password))
		}

		assertSuccess(employee.now()) {
			assertFalse(this.singleUsePassword)
		}

		assertSuccess(users.logIn(email, password)) {
			val (ref, token) = this

			assertEquals(employee, ref)
			assertSuccess(users.verifyToken(employee, token))
		}
	}

	@Test
	@JsName("cannotSetOtherPassword")
	fun `cannot set the password of another user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		assertUnauthorized(employee.setPassword(password.value, Password("a strong password")))

		assertSuccess(employee.now()) {
			assertTrue(singleUsePassword)
			assertTrue(active)
		}
	}

	@Test
	@JsName("passwordReset")
	fun `password reset`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)
		val password = Password("new password")

		withContext(Auth(User.Role.Employee, employee)) {
			assertSuccess(employee.setPassword(singleUse.value, password))
		}
		assertSuccess(employee.now()) {
			assertFalse(singleUsePassword)
			assertTrue(active)
		}

		val newPassword = assertSuccess(employee.resetPassword())
		assertSuccess(employee.now()) {
			assertTrue(singleUsePassword)
			assertTrue(active)
		}

		val email = employee.now().orThrow().email

		assertUnauthenticated(users.logIn(email, password))
		assertSuccess(users.logIn(email, newPassword))
		assertSuccess(employee.now()) {
			assertTrue(singleUsePassword)
			assertTrue(active)
		}
	}

	@Test
	@JsName("logInDisabled")
	fun `cannot log in as a disabled user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)

		assertSuccess(employee.disable())
		val email = employee.now().orThrow().email

		assertNotFound(users.logIn(email, singleUse))
	}

	@Test
	@JsName("logOut")
	fun `log out`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		val token = users.logIn(employee.now().orThrow().email, password).orThrow().second
		assertSuccess(employee.verifyToken(token))

		withContext(Auth(User.Role.Employee, employee)) {
			// Disabling a token which was already invalid does nothing
			assertSuccess(employee.logOut(Token("this is definitely not the correct token")))
			assertSuccess(employee.now()) {
				assertTrue(active)
			}

			// Disabling the token does not block the user
			assertSuccess(employee.logOut(token))
			assertSuccess(employee.now()) {
				assertTrue(active)
			}

			// We just logged out, the token should not be valid anymore
			assertUnauthenticated(employee.verifyToken(token))
		}

		// We already logged in once, we cannot log in a single time with this password
		assertUnauthenticated(users.logIn(employee.now().orThrow().email, password))
	}

	@Test
	@JsName("logOutSomeoneElse")
	fun `cannot log out someone else`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		val token = users.logIn(employee.now().orThrow().email, password).orThrow().second

		// I'm not 'employee', so I shouldn't be able to log them out, even with the correct token
		assertUnauthenticated(employee.logOut(token))
	}

	@Test
	@JsName("passwordResetInvalidateTokens")
	fun `resetting a password delogs the user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)

		//region Log in with two tokens
		val password = Password("password-${Random.nextUInt()}")
		withContext(Auth(User.Role.Employee, employee)) {
			employee.setPassword(singleUse.value, password)
		}
		val email = employee.now().orThrow().email

		val token1 = users.logIn(email, password).orThrow().second
		val token2 = users.logIn(email, password).orThrow().second
		//endregion

		assertSuccess(employee.resetPassword())
		assertUnauthenticated(employee.verifyToken(token1))
		assertUnauthenticated(employee.verifyToken(token2))
	}

	@Test
	@JsName("passwordSetInvalidateTokens")
	fun `setting password delogs the user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)

		//region Log in with two tokens
		val password = Password("password-${Random.nextUInt()}")
		withContext(Auth(User.Role.Employee, employee)) {
			employee.setPassword(singleUse.value, password)
		}
		val email = employee.now().orThrow().email

		val token1 = users.logIn(email, password).orThrow().second
		val token2 = users.logIn(email, password).orThrow().second
		//endregion

		withContext(Auth(User.Role.Employee, employee)) {
			assertSuccess(employee.setPassword(password.value, Password("Some new password :)")))
		}
		assertUnauthenticated(employee.verifyToken(token1))
		assertUnauthenticated(employee.verifyToken(token2))
	}

}
