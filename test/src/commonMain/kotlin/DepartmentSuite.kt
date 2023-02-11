package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite
import opensavvy.state.outcome.orThrow

//region Test data

internal suspend fun createDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("A new department").orThrow()
}

internal suspend fun createOpenDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("An open department").orThrow()
		.apply { open().orThrow() }
}

private suspend fun createClosedDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("A closed department").orThrow()
		.apply { close().orThrow() }
}

//endregion

fun Suite.departmentTestSuite(
	createDepartments: Factory<Department.Service>,
) {
	list(createDepartments)
	request(createDepartments)
	create(createDepartments)
	edit(createDepartments)
}

private fun Suite.list(
	createDepartments: Factory<Department.Service>,
) = suite("List departments") {
	test("guests cannot list departments") {
		val departments = createDepartments()
		createOpenDepartment(departments)
		createClosedDepartment(departments)

		shouldNotBeAuthenticated(departments.list(includeClosed = false))
		shouldNotBeAuthenticated(departments.list(includeClosed = true))
	}

	test("employees can list open departments", employeeAuth) {
		val departments = createDepartments()
		val open = createOpenDepartment(departments)
		val closed = createClosedDepartment(departments)

		val results = departments.list(includeClosed = false).shouldSucceed()
		assertSoftly(results) {
			it shouldContain open
			it shouldNotContain closed
		}

		shouldSucceed(open.now())
	}

	test("employees cannot list closed departments", employeeAuth) {
		val departments = createDepartments()
		val closed = createClosedDepartment(departments)

		shouldNotBeAuthorized(departments.list(includeClosed = true))
		shouldNotBeFound(closed.now())
	}

	test("administrators can list open departments", administratorAuth) {
		val departments = createDepartments()
		val open = createOpenDepartment(departments)
		val closed = createClosedDepartment(departments)

		departments.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain open
			it shouldNotContain closed
		}
	}

	test("administrators can list all departments", administratorAuth) {
		val departments = createDepartments()
		val open = createOpenDepartment(departments)
		val closed = createClosedDepartment(departments)

		departments.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain open
			it shouldContain closed
		}
	}
}

private fun Suite.request(
	createDepartments: Factory<Department.Service>,
) = suite("Request a department") {
	test("guests cannot directly access departments") {
		val departments = createDepartments()
		val open = createOpenDepartment(departments)
		val closed = createClosedDepartment(departments)

		shouldNotBeAuthenticated(open.now())
		shouldNotBeAuthenticated(closed.now())
	}
}

private fun Suite.create(
	createDepartments: Factory<Department.Service>,
) = suite("Create a department") {
	test("guests cannot create departments") {
		val departments = createDepartments()

		shouldNotBeAuthenticated(departments.create("A new department"))
	}

	test("employees cannot create departments", employeeAuth) {
		val departments = createDepartments()

		shouldNotBeAuthorized(departments.create("A new department"))
	}

	test("administrators can create a department", administratorAuth) {
		val departments = createDepartments()

		val ref = departments.create("A new department").shouldSucceed()
		val department = ref.now().shouldSucceed()

		assertSoftly(department) {
			name shouldBe "A new department"
			open shouldBe true
		}
	}
}

private fun Suite.edit(
	createDepartments: Factory<Department.Service>,
) = suite("Edit a department") {
	test("guests cannot edit departments") {
		val departments = createDepartments()
		val dept = createDepartment(departments)

		shouldNotBeAuthenticated(dept.close())
		withClue("The user is not allowed to edit the department, it should not be modified") {
			withContext(administratorAuth) {
				dept.now() shouldSucceedAnd {
					it.open shouldBe true
				}
			}
		}

		shouldNotBeAuthenticated(dept.open())
		withClue("The user is not allowed to edit the department, it should not be modified") {
			withContext(administratorAuth) {
				dept.now() shouldSucceedAnd {
					it.open shouldBe true
				}
			}
		}
	}

	test("employees cannot edit departments", employeeAuth) {
		val departments = createDepartments()
		val dept = createOpenDepartment(departments)

		shouldNotBeAuthorized(dept.close())
		dept.now() shouldSucceedAnd {
			withClue("The user should not be able to edit the department, so it should still be open") {
				it.open shouldBe true
			}
		}

		shouldNotBeAuthorized(dept.open())
		dept.now() shouldSucceedAnd {
			withClue("The user should not be able to edit the department, so it should still be open") {
				it.open shouldBe true
			}
		}
	}

	test("administrators can edit departments", administratorAuth) {
		val departments = createDepartments()
		val dept = createDepartment(departments)

		shouldSucceed(dept.close())
		dept.now() shouldSucceedAnd {
			it.open shouldBe false
		}

		shouldSucceed(dept.open())
		dept.now() shouldSucceedAnd {
			it.open shouldBe true
		}
	}
}
