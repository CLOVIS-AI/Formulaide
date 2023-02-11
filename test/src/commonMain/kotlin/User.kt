package opensavvy.formulaide.test

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Auth
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
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

@Suppress("FunctionName")
abstract class UserTestCases : TestCase<User.Service> {

	private fun generateId() = Random.nextUInt()

	@Test
	@JsName("guestsAccess")
	fun `guests cannot access users`() = runTest {
		val users = new()
		val department = createDepartment(FakeDepartments())

		shouldNotBeAuthenticated(users.list(includeClosed = false))
		shouldNotBeAuthenticated(users.list(includeClosed = true))

		shouldNotBeAuthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = true))
		shouldNotBeAuthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = false))

		shouldNotBeAuthenticated(testEmployee(users).first.now())

		for (target in listOf(testEmployee(users).first, testAdministrator(users).first)) {
			shouldNotBeAuthenticated(target.join(department))
			shouldNotBeAuthenticated(target.leave(department))

			shouldNotBeAuthenticated(target.enable())
			shouldNotBeAuthenticated(target.disable())
			shouldNotBeAuthenticated(target.promote())
			shouldNotBeAuthenticated(target.demote())

			shouldNotBeAuthenticated(target.resetPassword())
			shouldNotBeAuthenticated(target.setPassword("old password", Password("new password")))

			shouldNotBeAuthenticated(target.logOut(Token("a token")))
		}
	}

	@Test
	@JsName("employeesCreateUsers")
	fun `employees cannot create users`() = runTest(employeeAuth) {
		val users = new()

		shouldNotBeAuthorized(users.create("my-email@gmail.com".asEmail(), "Me"))
	}

	@Test
	@JsName("administratorsCreateUsers")
	fun `administrators can create users`() = runTest(administratorAuth) {
		val users = new()

		val employeeEmail = "employee-${generateId()}@gmail.com".asEmail()
		val adminEmail = "admin-${generateId()}@gmail.com".asEmail()

		val employee = shouldSucceed(users.create(employeeEmail, "Me", administrator = false)).first
		val administrator = shouldSucceed(users.create(adminEmail, "Me", administrator = true)).first

		employee.now().shouldSucceedAnd {
			assertEquals(employeeEmail, it.email)
			assertEquals("Me", it.name)
			assertEquals(false, it.administrator)
			assertEquals(true, it.active)
			assertEquals(true, it.singleUsePassword)
		}

		administrator.now().shouldSucceedAnd {
			assertEquals(adminEmail, it.email)
			assertEquals("Me", it.name)
			assertEquals(true, it.administrator)
			assertEquals(true, it.active)
			assertEquals(true, it.singleUsePassword)
		}
	}

	@Test
	@JsName("emailDuplication")
	fun `two users cannot have the same email address`() = runTest(administratorAuth) {
		val users = new()
		val email = "my-email-${generateId()}@gmail.com".asEmail()

		shouldSucceed(users.create(email, "First version"))
		shouldBeInvalid(users.create(email, "Second version"))
	}

	@Test
	@JsName("deptManagement")
	fun `department management`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first
		val department = createDepartment(FakeDepartments())

		employee.join(department).shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(setOf(department.id), it.departments.mapTo(HashSet()) { it.id })
			}
		}

		employee.leave(department).shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(emptySet(), it.departments)
			}
		}
	}

	@Test
	@JsName("userDisabling")
	fun `user disabling`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first

		employee.disable().shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(false, it.active)
			}
		}

		employee.enable().shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(true, it.active)
			}
		}
	}

	@Test
	@JsName("disableYourself")
	fun `cannot disable yourself`() = runTest(administratorAuth) {
		val users = new()
		val me = testAdministrator(users).first

		withContext(Auth(User.Role.Administrator, me)) {
			shouldBeInvalid(me.disable())
		}
	}

	@Test
	@JsName("userPromotion")
	fun `user promotion`() = runTest(administratorAuth) {
		val users = new()
		val employee = testEmployee(users).first

		employee.promote().shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(true, it.administrator)
			}
		}

		employee.demote().shouldSucceedAnd {
			employee.now().shouldSucceedAnd {
				assertEquals(false, it.administrator)
			}
		}
	}

	@Test
	@JsName("demoteYourself")
	fun `cannot demote yourself`() = runTest(administratorAuth) {
		val users = new()
		val me = testAdministrator(users).first

		withContext(Auth(User.Role.Administrator, me)) {
			shouldBeInvalid(me.demote())
		}
	}

	@Test
	@JsName("singleUse")
	fun `single-use password`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUsePassword) = testEmployee(users)

		employee.now().shouldSucceedAnd {
			assertTrue(it.singleUsePassword)
		}

		val email = employee.now().orThrow().email

		// First usage of the single-use password
		users.logIn(email, singleUsePassword).shouldSucceedAnd { (ref, token) ->
			assertEquals(employee, ref)
			shouldSucceed(users.verifyToken(employee, token))
		}

		// Second usage is blocked
		shouldNotBeAuthenticated(users.logIn(email, singleUsePassword))

		// Set a new password to make the account multi-use again
		val password = Password("an-amazing-password")
		withContext(Auth(User.Role.Employee, employee)) {
			shouldSucceed(employee.setPassword(singleUsePassword.value, password))
		}

		employee.now().shouldSucceedAnd {
			assertFalse(it.singleUsePassword)
		}

		users.logIn(email, password).shouldSucceedAnd { (ref, token) ->
			assertEquals(employee, ref)
			shouldSucceed(users.verifyToken(employee, token))
		}
	}

	@Test
	@JsName("cannotSetOtherPassword")
	fun `cannot set the password of another user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		shouldNotBeAuthorized(employee.setPassword(password.value, Password("a strong password")))

		employee.now().shouldSucceedAnd {
			assertTrue(it.singleUsePassword)
			assertTrue(it.active)
		}
	}

	@Test
	@JsName("passwordReset")
	fun `password reset`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)
		val password = Password("new password")

		withContext(Auth(User.Role.Employee, employee)) {
			shouldSucceed(employee.setPassword(singleUse.value, password))
		}
		employee.now().shouldSucceedAnd {
			assertFalse(it.singleUsePassword)
			assertTrue(it.active)
		}

		val newPassword = shouldSucceed(employee.resetPassword())
		employee.now().shouldSucceedAnd {
			assertTrue(it.singleUsePassword)
			assertTrue(it.active)
		}

		val email = employee.now().orThrow().email

		shouldNotBeAuthenticated(users.logIn(email, password))
		shouldSucceed(users.logIn(email, newPassword))
		employee.now().shouldSucceedAnd {
			assertTrue(it.singleUsePassword)
			assertTrue(it.active)
		}
	}

	@Test
	@JsName("logInDisabled")
	fun `cannot log in as a disabled user`() = runTest(administratorAuth) {
		val users = new()
		val (employee, singleUse) = testEmployee(users)

		shouldSucceed(employee.disable())
		val email = employee.now().orThrow().email

		// do not return NotFound! -> it would help an attacker enumerate users
		shouldNotBeAuthenticated(users.logIn(email, singleUse))
	}

	@Test
	@JsName("logOut")
	fun `log out`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		val token = users.logIn(employee.now().orThrow().email, password).orThrow().second
		shouldSucceed(employee.verifyToken(token))

		withContext(Auth(User.Role.Employee, employee)) {
			// Disabling a token which was already invalid does nothing
			shouldSucceed(employee.logOut(Token("this is definitely not the correct token")))
			employee.now().shouldSucceedAnd {
				assertTrue(it.active)
			}

			// Disabling the token does not block the user
			shouldSucceed(employee.logOut(token))
			employee.now().shouldSucceedAnd {
				assertTrue(it.active)
			}

			// We just logged out, the token should not be valid anymore
			shouldNotBeAuthenticated(employee.verifyToken(token))
		}

		// We already logged in once, we cannot log in a single time with this password
		shouldNotBeAuthenticated(users.logIn(employee.now().orThrow().email, password))
	}

	@Test
	@JsName("logOutSomeoneElse")
	fun `cannot log out someone else`() = runTest(administratorAuth) {
		val users = new()
		val (employee, password) = testEmployee(users)

		val token = users.logIn(employee.now().orThrow().email, password).orThrow().second

		// I'm not 'employee', so I shouldn't be able to log them out, even with the correct token
		shouldNotBeAuthenticated(employee.logOut(token))
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

		shouldSucceed(employee.resetPassword())
		shouldNotBeAuthenticated(employee.verifyToken(token1))
		shouldNotBeAuthenticated(employee.verifyToken(token2))
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
			shouldSucceed(employee.setPassword(password.value, Password("Some new password :)")))
		}
		shouldNotBeAuthenticated(employee.verifyToken(token1))
		shouldNotBeAuthenticated(employee.verifyToken(token2))
	}

	@Test
	@JsName("logInFalseUser")
	fun `cannot log in as a user that doesn't exist`() = runTest {
		val users = new()

		// No user was created, this user cannot exist
		//   do not return a NotFound! it would help an attacker enumerate accounts
		shouldNotBeAuthenticated(users.logIn(Email("this-email-does-not-exist@google.com"), Password("whatever")))
	}

}
