package opensavvy.formulaide.test

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import opensavvy.backbone.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.structure.Setup
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth

//region Test data

internal fun <D : Department.Ref> createDepartment(departments: Setup<Department.Service<D>>) = prepared(administratorAuth) {
	prepare(departments).create("A new department").bind()
}

internal fun <D : Department.Ref> createOpenDepartment(departments: Setup<Department.Service<D>>) = prepared(administratorAuth) {
	prepare(departments).create("An open department").bind()
		.also { it.open().bind() }
}

private fun <D : Department.Ref> createClosedDepartment(departments: Setup<Department.Service<D>>) = prepared(administratorAuth) {
	prepare(departments).create("A closed department").bind()
		.also { it.close().bind() }
}

//endregion

fun <D : Department.Ref> Suite.departmentTestSuite(
	createDepartments: Setup<Department.Service<D>>,
) {
	list(createDepartments)
	request(createDepartments)
	create(createDepartments)
	edit(createDepartments)
}

private fun <D : Department.Ref> Suite.list(
	createDepartments: Setup<Department.Service<D>>,
) = suite("List departments") {
	val createOpen by createOpenDepartment(createDepartments)
	val createClosed by createClosedDepartment(createDepartments)

	test("guests cannot list departments") {
		val departments = prepare(createDepartments)
		prepare(createOpen)
		prepare(createClosed)

		shouldNotBeAuthenticated(departments.list(includeClosed = false))
		shouldNotBeAuthenticated(departments.list(includeClosed = true))
	}

	test("employees can list open departments", employeeAuth) {
		val departments = prepare(createDepartments)
		val open = prepare(createOpen)
		val closed = prepare(createClosed)

		val results = departments.list(includeClosed = false).shouldSucceed()
		assertSoftly(results) {
			it shouldContain open
			it shouldNotContain closed
		}

		shouldSucceed(open.now())
	}

	test("employees cannot list closed departments", employeeAuth) {
		val departments = prepare(createDepartments)
		val closed = prepare(createClosed)

		shouldNotBeAuthorized(departments.list(includeClosed = true))
		shouldNotBeFound(closed.now())
	}

	test("administrators can list open departments", administratorAuth) {
		val departments = prepare(createDepartments)
		val open = prepare(createOpen)
		val closed = prepare(createClosed)

		departments.list(includeClosed = false) shouldSucceedAnd {
			it shouldContain open
			it shouldNotContain closed
		}
	}

	test("administrators can list all departments", administratorAuth) {
		val departments = prepare(createDepartments)
		val open = prepare(createOpen)
		val closed = prepare(createClosed)

		departments.list(includeClosed = true) shouldSucceedAnd {
			it shouldContain open
			it shouldContain closed
		}
	}
}

private fun <D : Department.Ref> Suite.request(
	createDepartments: Setup<Department.Service<D>>,
) = suite("Request a department") {
	val createOpen by createOpenDepartment(createDepartments)
	val createClosed by createClosedDepartment(createDepartments)

	test("guests cannot directly access departments") {
		prepare(createDepartments)
		val open = prepare(createOpen)
		val closed = prepare(createClosed)

		shouldNotBeAuthenticated(open.now())
		shouldNotBeAuthenticated(closed.now())
	}
}

private fun <D : Department.Ref> Suite.create(
	createDepartments: Setup<Department.Service<D>>,
) = suite("Create a department") {
	test("guests cannot create departments") {
		val departments = prepare(createDepartments)

		shouldNotBeAuthenticated(departments.create("A new department"))
	}

	test("employees cannot create departments", employeeAuth) {
		val departments = prepare(createDepartments)

		shouldNotBeAuthorized(departments.create("A new department"))
	}

	test("administrators can create a department", administratorAuth) {
		val departments = prepare(createDepartments)

		val ref = departments.create("A new department").shouldSucceed()
		val department = ref.now().shouldSucceed()

		assertSoftly(department) {
			name shouldBe "A new department"
			open shouldBe true
		}
	}
}

private fun <D : Department.Ref> Suite.edit(
	createDepartments: Setup<Department.Service<D>>,
) = suite("Edit a department") {
	val createDepartment by createDepartment(createDepartments)
	val createOpen by createOpenDepartment(createDepartments)

	test("guests cannot edit departments") {
		prepare(createDepartments)
		val dept = prepare(createDepartment)

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
		prepare(createDepartments)
		val dept = prepare(createOpen)

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
		prepare(createDepartments)
		val dept = prepare(createDepartment)

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
