package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
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
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import opensavvy.formulaide.test.utils.executeAs
import kotlin.random.Random
import kotlin.random.nextUInt

//region Test data

internal suspend fun createEmployee(users: User.Service) = withContext(administratorAuth) {
	val number = Random.nextUInt()

	users.create("employee-$number@formulaide".asEmail(), "Employee #$number").orThrow()
}

internal suspend fun createAdministrator(users: User.Service) = withContext(administratorAuth) {
	val number = Random.nextUInt()

	users.create("administrator-$number@formulaide".asEmail(), "Administrator #$number", administrator = true).orThrow()
}

//endregion

fun Suite.usersTestSuite(
	createDepartments: Setup<Department.Service>,
	createUsers: Setup<User.Service>,
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

private fun Suite.list(
	createUsers: Setup<User.Service>,
) = suite("List users") {
	test("guests cannot list users") {
		val users = prepare(createUsers)

		shouldNotBeAuthenticated(users.list(includeClosed = false))
		shouldNotBeAuthenticated(users.list(includeClosed = true))
	}
}

private fun Suite.create(
	createUsers: Setup<User.Service>,
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
		shouldBeInvalid(users.create(email, "Second user"))
	}
}

private fun Suite.joinLeave(
	createDepartments: Setup<Department.Service>,
	createUsers: Setup<User.Service>,
) = suite("Join or leave a department") {
	val testDepartment by createDepartment(createDepartments)

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
		val users = prepare(createUsers)
		val department = prepare(testDepartment)
		val user = createEmployee(users).first

		shouldNotBeAuthenticated(user.join(department))
		shouldHaveNoDepartments(user)

		shouldNotBeAuthenticated(user.leave(department))
		shouldHaveNoDepartments(user)
	}

	test("employees cannot make a user join or leave a department", employeeAuth) {
		val users = prepare(createUsers)
		val department = prepare(testDepartment)
		val user = createEmployee(users).first

		shouldNotBeAuthorized(user.join(department))
		shouldHaveNoDepartments(user)

		shouldNotBeAuthorized(user.leave(department))
		shouldHaveNoDepartments(user)
	}

	test("employees cannot join or leave a department", employeeAuth) {
		val users = prepare(createUsers)
		val department = prepare(testDepartment)
		val user = createEmployee(users).first

		executeAs(user) {
			shouldNotBeAuthorized(user.join(department))
			shouldHaveNoDepartments(user)

			shouldNotBeAuthorized(user.leave(department))
			shouldHaveNoDepartments(user)
		}
	}

	test("administrators can make a user join or leave a department", administratorAuth) {
		val users = prepare(createUsers)
		val department = prepare(testDepartment)
		val user = createEmployee(users).first

		shouldSucceed(user.join(department))
		user.now() shouldSucceedAnd {
			it.departments.map(Department.Ref::id) shouldBe setOf(department.id)
		}

		shouldSucceed(user.leave(department))
		user.now() shouldSucceedAnd {
			it.departments shouldBe emptySet()
		}
	}
}

private fun Suite.edit(
	createUsers: Setup<User.Service>,
) = suite("Edit a user") {
	test("guests cannot edit a user") {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldNotBeAuthenticated(target.enable())
		shouldNotBeAuthenticated(target.disable())
		shouldNotBeAuthenticated(target.promote())
		shouldNotBeAuthenticated(target.demote())
	}

	test("employees cannot edit a user", employeeAuth) {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldNotBeAuthorized(target.enable())
		shouldNotBeAuthorized(target.disable())
		shouldNotBeAuthorized(target.promote())
		shouldNotBeAuthorized(target.demote())
	}

	test("administrators can enable and disable users", administratorAuth) {
		val users = prepare(createUsers)
		val employee = createEmployee(users).first

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
		val users = prepare(createUsers)
		val me = createAdministrator(users).first

		executeAs(me) {
			shouldBeInvalid(me.disable())

			withClue("I'm not allowed to disable myself, so I should still be active") {
				me.now() shouldSucceedAnd {
					it.active shouldBe true
				}
			}
		}
	}

	test("administrators can promote and demote users", administratorAuth) {
		val users = prepare(createUsers)
		val employee = createEmployee(users).first

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
		val users = prepare(createUsers)
		val me = createAdministrator(users).first

		executeAs(me) {
			shouldBeInvalid(me.demote())

			withClue("I'm not allowed to demote myself, so I should still be an administrator") {
				me.now() shouldSucceedAnd {
					it.administrator shouldBe true
				}
			}
		}
	}
}

private fun Suite.request(
	createUsers: Setup<User.Service>,
) = suite("Access a user") {
	test("guests cannot access users") {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldNotBeAuthenticated(target.now())
	}

	test("employees can access users", employeeAuth) {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldSucceed(target.now())
	}
}

private fun Suite.password(
	createUsers: Setup<User.Service>,
) = suite("Password management") {
	test("guests cannot edit passwords") {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldNotBeAuthenticated(target.resetPassword())
		shouldNotBeAuthenticated(target.setPassword("old password", Password("new password")))
	}

	test("the single-use password can only be used once", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = createEmployee(users)

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
			shouldNotBeAuthenticated(users.logIn(email, singleUsePassword))
		}
	}

	test("setting a password unlocks an account with a used single-use password", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, singleUsePassword) = createEmployee(users)

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
		val (employee, singleUsePassword) = createEmployee(users)

        val email = employee.now().bind().email

		// Cannot use the single-use password after it has been changed

		val password1 = Password("my-first-great-password")

		executeAs(employee) {
			employee.setPassword(singleUsePassword.value, password1).shouldSucceed()
		}

		withClue("The password has been changed to ${password1.value}, the single-use password ${singleUsePassword.value} should not be valid anymore") {
			shouldNotBeAuthenticated(users.logIn(email, singleUsePassword))
		}

		// Cannot use any previous password after is has been changed

		val password2 = Password("my-second-great-password")

		executeAs(employee) {
			employee.setPassword(password1.value, password2).shouldSucceed()
		}

		withClue("The password has been changed to ${password2.value}, the previous password ${password1.value} should not be valid anymore") {
			shouldNotBeAuthenticated(users.logIn(email, password1))
		}

		withClue("The password has been changed to ${password2.value}, the single-use password ${singleUsePassword.value} should not be valid anymore") {
			shouldNotBeAuthenticated(users.logIn(email, singleUsePassword))
		}
	}

	test("cannot edit the password of another user", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, password) = createEmployee(users)

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
		val (employee, singleUsePassword) = createEmployee(users)
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
		val (employee, singleUsePassword) = createEmployee(users)

		// Prepare the user
		employee.disable().shouldSucceed()
        val email = employee.now().bind().email

		// Do not return NotFound! -> It would help an attacker enumerate users
		shouldNotBeAuthenticated(users.logIn(email, singleUsePassword))
	}

	test("cannot log in as a user that doesn't exist") {
		val users = prepare(createUsers)

		withClue("No user was created, this user does not exist") {
			// Do not return NotFound! -> it would help an attacker enumerate accounts
			shouldNotBeAuthenticated(users.logIn(Email("this-email-does-not-exist@google.com"), Password("whatever")))
		}
	}
}

private fun Suite.token(
	createUsers: Setup<User.Service>,
) = suite("Access token management") {
	test("guests cannot edit tokens") {
		val users = prepare(createUsers)
		val target = createEmployee(users).first

		shouldNotBeAuthenticated(target.logOut(Token("a token")))
	}

	test("logging out invalidates the token", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, password) = createEmployee(users)

        val token = users.logIn(employee.now().bind().email, password).bind().second
		shouldSucceed(employee.verifyToken(token))

		executeAs(employee) {
			shouldSucceed(employee.logOut(token))
		}

		withClue("We just logged out, the token should not be valid anymore") {
			shouldNotBeAuthenticated(employee.verifyToken(token))
		}

		employee.now() shouldSucceedAnd {
			it.active shouldBe true
		}
	}

	test("logging out with an invalid token does nothing", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, _) = createEmployee(users)

		executeAs(employee) {
			shouldSucceed(employee.logOut(Token("this is definitely not a valid token")))
		}

		employee.now() shouldSucceedAnd {
			it.active shouldBe true
		}
	}

	test("cannot log out someone else", administratorAuth) {
		val users = prepare(createUsers)
		val (employee, password) = createEmployee(users)

        val token = users.logIn(employee.now().bind().email, password).bind().second

		withClue("I'm not 'employee', so I shouldn't be able to log them out, even with the correct token") {
			shouldNotBeAuthenticated(employee.logOut(token))
		}

		withClue("i'm not allowed to edit the token, so it should still be valid") {
			shouldSucceed(employee.verifyToken(token))
		}
	}

	test("reset a password delogs the user", administratorAuth) {
        val users = prepare(createUsers)
        val (employee, singleUsePassword) = createEmployee(users)

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
            shouldNotBeAuthenticated(employee.verifyToken(token1))
            shouldNotBeAuthenticated(employee.verifyToken(token2))
        }
	}

	test("setting a password delogs the user", administratorAuth) {
        val users = prepare(createUsers)
        val (employee, singleUsePassword) = createEmployee(users)

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
            shouldNotBeAuthenticated(employee.verifyToken(token1))
            shouldNotBeAuthenticated(employee.verifyToken(token2))
        }
	}
}
