package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Email.Companion.asEmail
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.structure.Setup
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.formulaide.test.utils.executeAs
import opensavvy.state.outcome.failed
import kotlin.random.Random
import kotlin.random.nextUInt

//region Test data

internal fun <U : User.Ref> createEmployee(users: Setup<User.Service<U>>) = prepared(administratorAuth) {
	val number = Random.nextUInt()

	prepare(users).create("employee-$number@formulaide".asEmail(), "Employee #$number").bind()
}

internal fun <U : User.Ref> createAdministrator(users: Setup<User.Service<U>>) = prepared(administratorAuth) {
	val number = Random.nextUInt()

	prepare(users).create("administrator-$number@formulaide".asEmail(), "Administrator #$number", administrator = true)
		.bind()
}

//endregion

fun <D : Department.Ref, U : User.Ref> Suite.usersTestSuite(
	createDepartments: Setup<Department.Service<D>>,
	createUsers: Setup<User.Service<U>>,
) {
	suite("User management") {
		list(createUsers)
		create(createUsers)
		joinLeave(createDepartments, createUsers)
		edit(createUsers)
		request(createUsers)
	}

	suite("Authentication") {
		password(createUsers)
		token(createUsers)
	}
}

private fun generateId() = Random.nextUInt()

private fun <U : User.Ref> Suite.list(
	createUsers: Setup<User.Service<U>>,
) = suite("List users") {
	test("guests cannot list users") {
		val users = prepare(createUsers)

		shouldNotBeAuthenticated(users.list(includeClosed = false))
		shouldNotBeAuthenticated(users.list(includeClosed = true))
	}
}

private fun <U : User.Ref> Suite.create(
	createUsers: Setup<User.Service<U>>,
) = suite("Create users") {
	test("guests cannot create users") {
		val users = prepare(createUsers)

		shouldNotBeAuthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = true))
		shouldNotBeAuthenticated(users.create("my-email@gmail.com".asEmail(), "Me", administrator = false))
	}

	test("employees cannot create users", employeeAuth) {
		val users = prepare(createUsers)

		shouldNotBeAuthorized(users.create("my-email@gmail.com".asEmail(), "Me", administrator = true))
		shouldNotBeAuthorized(users.create("my-email@gmail.com".asEmail(), "Me", administrator = false))
	}

	test("administrators can create employees", administratorAuth) {
		val users = prepare(createUsers)

		val userEmail = "employee-${generateId()}@gmail.com".asEmail()

		val userRef = users.create(userEmail, "Me", administrator = false)
			.shouldSucceed()
			.first

		val user = userRef.now().shouldSucceed()

		assertSoftly(user) {
			email shouldBe userEmail
			name shouldBe "Me"
			administrator shouldBe false
			active shouldBe true
			singleUsePassword shouldBe true
		}
	}

	test("administrators can create administrators", administratorAuth) {
		val users = prepare(createUsers)

		val userEmail = "employee-${generateId()}@gmail.com".asEmail()

		val userRef = users.create(userEmail, "Me", administrator = true)
			.shouldSucceed()
			.first

		val user = userRef.now().shouldSucceed()

		assertSoftly(user) {
			email shouldBe userEmail
			name shouldBe "Me"
			administrator shouldBe true
			active shouldBe true
			singleUsePassword shouldBe true
		}
	}

	test("two users cannot have the same email address", administratorAuth) {
		val users = prepare(createUsers)
		val email = "my-email-${generateId()}@gmail.com".asEmail()

		shouldSucceed(users.create(email, "First user"))
		users.create(email, "Second user") shouldBe User.Failures.UserAlreadyExists(email).failed()
	}
}

private fun <D : Department.Ref, U : User.Ref> Suite.joinLeave(
	createDepartments: Setup<Department.Service<D>>,
	createUsers: Setup<User.Service<U>>,
) = suite("Join or leave a department") {
	val testDepartment by createDepartment(createDepartments)
	val testEmployee by createEmployee(createUsers)

	suspend fun shouldHaveNoDepartments(userRef: User.Ref) {
		withClue("The user doesn't have the rights to join or leave a department, so it should not have been modified") {
			withContext(employeeAuth) {
				userRef.now() shouldSucceedAnd {
					it.departments shouldBe emptySet()
				}
			}
		}
	}

	test("guests cannot make a user join or leave a department") {
		val department = prepare(testDepartment)
		val user = prepare(testEmployee).first

		shouldNotBeAuthenticated(user.join(department))
		shouldHaveNoDepartments(user)

		shouldNotBeAuthenticated(user.leave(department))
		shouldHaveNoDepartments(user)
	}

	test("employees cannot make a user join or leave a department", employeeAuth) {
		val department = prepare(testDepartment)
		val user = prepare(testEmployee).first

		shouldNotBeAuthorized(user.join(department))
		shouldHaveNoDepartments(user)

		shouldNotBeAuthorized(user.leave(department))
		shouldHaveNoDepartments(user)
	}

	test("employees cannot join or leave a department", employeeAuth) {
		val department = prepare(testDepartment)
		val user = prepare(testEmployee).first

		executeAs(user) {
			shouldNotBeAuthorized(user.join(department))
			shouldHaveNoDepartments(user)

			shouldNotBeAuthorized(user.leave(department))
			shouldHaveNoDepartments(user)
		}
	}

	test("administrators can make a user join or leave a department", administratorAuth) {
		val department = prepare(testDepartment)
		val user = prepare(testEmployee).first

		shouldSucceed(user.join(department))
		user.now() shouldSucceedAnd {
			it.departments shouldBe setOf(department)
		}

		shouldSucceed(user.leave(department))
		user.now() shouldSucceedAnd {
			it.departments shouldBe emptySet()
		}
	}
}

private fun <U : User.Ref> Suite.edit(
	createUsers: Setup<User.Service<U>>,
) = suite("Edit a user") {
	val testEmployee by createEmployee(createUsers)
	val testAdministrator by createAdministrator(createUsers)

	test("guests cannot edit a user") {
		val target = prepare(testEmployee).first

		shouldNotBeAuthenticated(target.enable())
		shouldNotBeAuthenticated(target.disable())
		shouldNotBeAuthenticated(target.promote())
		shouldNotBeAuthenticated(target.demote())
	}

	test("employees cannot edit a user", employeeAuth) {
		val target = prepare(testEmployee).first

		shouldNotBeAuthorized(target.enable())
		shouldNotBeAuthorized(target.disable())
		shouldNotBeAuthorized(target.promote())
		shouldNotBeAuthorized(target.demote())
	}

	test("administrators can enable and disable users", administratorAuth) {
		val employee = prepare(testEmployee).first

		employee.disable().shouldSucceed()
		employee.now() shouldSucceedAnd {
			it.active shouldBe false
		}

		employee.enable().shouldSucceed()
		employee.now() shouldSucceedAnd {
			it.active shouldBe true
		}
	}

	test("administrators cannot edit themselves", administratorAuth) {
		val me = prepare(testAdministrator).first

		executeAs(me) {
			shouldNotBeAuthorized(me.disable())

			withClue("I'm not allowed to disable myself, so I should still be active") {
				me.now() shouldSucceedAnd {
					it.active shouldBe true
				}
			}
		}
	}

	test("administrators can promote and demote users", administratorAuth) {
		val employee = prepare(testEmployee).first

		employee.promote().shouldSucceed()
		employee.now() shouldSucceedAnd {
			it.administrator shouldBe true
		}

		employee.demote().shouldSucceed()
		employee.now() shouldSucceedAnd {
			it.administrator shouldBe false
		}
	}

	test("administrators cannot demote themselves", administratorAuth) {
		val me = prepare(testAdministrator).first

		executeAs(me) {
			shouldNotBeAuthorized(me.demote())

			withClue("I'm not allowed to demote myself, so I should still be an administrator") {
				me.now() shouldSucceedAnd {
					it.administrator shouldBe true
				}
			}
		}
	}
}

private fun <U : User.Ref> Suite.request(
	createUsers: Setup<User.Service<U>>,
) = suite("Access a user") {
	val testEmployee by createEmployee(createUsers)

	test("guests cannot access users") {
		val target = prepare(testEmployee).first

		shouldNotBeAuthenticated(target.now())
	}

	test("employees can access users", employeeAuth) {
		val target = prepare(testEmployee).first

		shouldSucceed(target.now())
	}
}

private fun <U : User.Ref> Suite.password(
	createUsers: Setup<User.Service<U>>,
) = suite("Password management") {
	val testEmployee by createEmployee(createUsers)

	test("guests cannot edit passwords") {
		val target = prepare(testEmployee).first

		shouldNotBeAuthenticated(target.resetPassword())
		shouldNotBeAuthenticated(target.setPassword("old password", Password("new password")))
	}

	test("the single-use password can only be used once", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		employee.now() shouldSucceedAnd {
			it.singleUsePassword shouldBe true
		}

		val email = employee.now().bind().email

		withClue("First usage of the single-use password") {
			users.logIn(email, singleUsePassword) shouldSucceedAnd { (ref, token) ->
				ref shouldBe employee
				employee.verifyToken(token).shouldSucceed()
			}
		}

		withClue("Second usage of the single-use password, it should be invalid") {
			users.logIn(email, singleUsePassword) shouldFailWithKey User.Failures.IncorrectCredentials
		}
	}

	test("setting a password unlocks an account with a used single-use password", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		val email = employee.now().bind().email

		withClue("Using the single-use password two times to lock the account") {
			users.logIn(email, singleUsePassword)
			users.logIn(email, singleUsePassword)
		}

		val password = Password("an-amazing-password")
		executeAs(employee) {
			shouldSucceed(employee.setPassword(singleUsePassword.value, password))
		}

		employee.now() shouldSucceedAnd {
			it.singleUsePassword shouldBe false
		}

		withClue("The user should not be blocked anymore") {
			users.logIn(email, password) shouldSucceedAnd { (ref, token) ->
				ref shouldBe employee
				ref.verifyToken(token).shouldSucceed()
			}
		}
	}

	test("it is not possible to use a previous password", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		val email = employee.now().bind().email

		// Cannot use the single-use password after it has been changed

		val password1 = Password("my-first-great-password")

		executeAs(employee) {
			employee.setPassword(singleUsePassword.value, password1).shouldSucceed()
		}

		withClue("The password has been changed to ${password1.value}, the single-use password ${singleUsePassword.value} should not be valid anymore") {
			users.logIn(email, singleUsePassword) shouldFailWithKey User.Failures.IncorrectCredentials
		}

		// Cannot use any previous password after is has been changed

		val password2 = Password("my-second-great-password")

		executeAs(employee) {
			employee.setPassword(password1.value, password2).shouldSucceed()
		}

		withClue("The password has been changed to ${password2.value}, the previous password ${password1.value} should not be valid anymore") {
			users.logIn(email, password1) shouldFailWithKey User.Failures.IncorrectCredentials
		}

		withClue("The password has been changed to ${password2.value}, the single-use password ${singleUsePassword.value} should not be valid anymore") {
			users.logIn(email, singleUsePassword) shouldFailWithKey User.Failures.IncorrectCredentials
		}
	}

	test("cannot edit the password of another user", administratorAuth) {
		val (employee, password) = prepare(testEmployee)

		shouldNotBeAuthorized(employee.setPassword(password.value, Password("a strong password")))

		employee.now() shouldSucceedAnd {
			withClue("The password should not have been modified, so the user's status should be unchanged") {
				it.singleUsePassword shouldBe true
				it.active shouldBe true
			}
		}
	}

	test("administrators can reset a user's password", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)
		val password = Password("new password")

		// We first need to make the password non-single use
		executeAs(employee) {
			employee.setPassword(singleUsePassword.value, password).bind()
		}

		val email = employee.now().bind().email
		val newPassword = employee.resetPassword().shouldSucceed()

		withClue("We just reset the password, the user should be in single-use mode") {
			employee.now() shouldSucceedAnd {
				it.singleUsePassword shouldBe true
				it.active shouldBe true
			}
		}

		withClue("We just reset the password, the generated password should allow logging in") {
			users.logIn(email, newPassword) shouldSucceedAnd { (ref, token) ->
				ref shouldBe employee
				ref.verifyToken(token).shouldSucceed()
			}
		}
	}

	test("cannot log in as a disabled user", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		// Prepare the user
		employee.disable().shouldSucceed()
		val email = employee.now().bind().email

		// Do not return NotFound! -> It would help an attacker enumerate users
		users.logIn(email, singleUsePassword) shouldFailWithKey User.Failures.IncorrectCredentials
	}

	test("cannot log in as a user that doesn't exist") {
		val users = prepare(createUsers)

		withClue("No user was created, this user does not exist") {
			// Do not return NotFound! -> it would help an attacker enumerate accounts
			users.logIn(Email("this-email-does-not-exist@google.com"), Password("whatever")) shouldFailWithKey User.Failures.IncorrectCredentials
		}
	}
}

private fun <U : User.Ref> Suite.token(
	createUsers: Setup<User.Service<U>>,
) = suite("Access token management") {
	val testEmployee by createEmployee(createUsers)

	test("guests cannot edit tokens") {
		val target = prepare(testEmployee).first

		shouldNotBeAuthenticated(target.logOut(Token("a token")))
	}

	test("logging out invalidates the token", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, password) = prepare(testEmployee)

		val token = users.logIn(employee.now().bind().email, password).bind().second
		shouldSucceed(employee.verifyToken(token))

		executeAs(employee) {
			shouldSucceed(employee.logOut(token))
		}

		withClue("We just logged out, the token should not be valid anymore") {
			employee.verifyToken(token) shouldFailWithKey User.Failures.IncorrectCredentials
		}

		employee.now() shouldSucceedAnd {
			it.active shouldBe true
		}
	}

	test("logging out with an invalid token does nothing", administratorAuth) {
		val (employee, _) = prepare(testEmployee)

		executeAs(employee) {
			shouldSucceed(employee.logOut(Token("this is definitely not a valid token")))
		}

		employee.now() shouldSucceedAnd {
			it.active shouldBe true
		}
	}

	test("cannot log out someone else", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, password) = prepare(testEmployee)

		val token = users.logIn(employee.now().bind().email, password).bind().second

		withClue("I'm not 'employee', so I shouldn't be able to log them out, even with the correct token") {
			shouldNotBeAuthorized(employee.logOut(token))
		}

		withClue("i'm not allowed to edit the token, so it should still be valid") {
			shouldSucceed(employee.verifyToken(token))
		}
	}

	test("reset a password delogs the user", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		//region Setup: log in with two tokens
		val password = Password("password-${generateId()}")
		executeAs(employee) {
			employee.setPassword(singleUsePassword.value, password)
		}
		val email = employee.now().bind().email

		val token1 = users.logIn(email, password).bind().second
		val token2 = users.logIn(email, password).bind().second
		//endregion

		shouldSucceed(employee.resetPassword())

		withClue("The user's password just changed, previously-created tokens should now invalid") {
			employee.verifyToken(token1) shouldFailWithKey User.Failures.IncorrectCredentials
			employee.verifyToken(token2) shouldFailWithKey User.Failures.IncorrectCredentials
		}
	}

	test("setting a password delogs the user", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = prepare(testEmployee)

		//region Setup: log in with two tokens
		val password = Password("password-${generateId()}")
		executeAs(employee) {
			employee.setPassword(singleUsePassword.value, password)
		}
		val email = employee.now().bind().email

		val token1 = users.logIn(email, password).bind().second
		val token2 = users.logIn(email, password).bind().second
		//endregion

		executeAs(employee) {
			employee.setPassword(password.value, Password("Some new password"))
		}

		withClue("The user's password just changed, previously-created tokens should now invalid") {
			employee.verifyToken(token1) shouldFailWithKey User.Failures.IncorrectCredentials
			employee.verifyToken(token2) shouldFailWithKey User.Failures.IncorrectCredentials
		}
	}
}
