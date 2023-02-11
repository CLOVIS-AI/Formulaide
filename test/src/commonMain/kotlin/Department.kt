package opensavvy.formulaide.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.state.outcome.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

//region Test data

internal suspend fun testDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("A new department").orThrow()
}

internal suspend fun testOpenDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("An open department").orThrow()
		.apply { open().orThrow() }
}

private suspend fun testClosedDepartment(departments: Department.Service) = withContext(administratorAuth) {
	departments.create("A closed department").orThrow()
		.apply { close().orThrow() }
}

//endregion

@Suppress("FunctionName")
@OptIn(ExperimentalCoroutinesApi::class)
abstract class DepartmentTestCases : TestCase<Department.Service> {

	@Test
	@JsName("employeeCannotCreateDepartments")
	fun `employees cannot create departments`() = runTest(employeeAuth) {
		val departments = new()

		shouldNotBeAuthorized(departments.create("A new department"))
	}

	@Test
	@JsName("administratorsCanCreateDepartments")
	fun `administrators can create departments`() = runTest(administratorAuth) {
		val departments = new()

		val ref = shouldSucceed(departments.create("A new department"))
		val department = shouldSucceed(ref.now())

		assertEquals("A new department", department.name)
		assertEquals(true, department.open)
	}

	@Test
	@JsName("employeesListOpenDepartments")
	fun `employees can list open departments`() = runTest(employeeAuth) {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		val results = shouldSucceed(departments.list(includeClosed = false))
		results shouldContain open
		results shouldNotContain closed

		shouldNotBeAuthorized(departments.list(includeClosed = true))

		shouldSucceed(open.now())
		shouldNotBeFound(closed.now())
	}

	@Test
	@JsName("anonymousListDepartments")
	fun `guests cannot access departments`() = runTest {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		shouldNotBeAuthenticated(departments.list(includeClosed = false))
		shouldNotBeAuthenticated(departments.list(includeClosed = true))
		shouldNotBeAuthenticated(open.now())
		shouldNotBeAuthenticated(closed.now())
	}

	@Test
	@JsName("adminsListDepartments")
	fun `administrators can list all departments`() = runTest(administratorAuth) {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		val resultsOpen = shouldSucceed(departments.list(includeClosed = false))
		resultsOpen shouldContain open
		resultsOpen shouldNotContain closed

		val resultsAll = shouldSucceed(departments.list(includeClosed = true))
		resultsAll shouldContain open
		resultsAll shouldContain closed

		shouldSucceed(open.now())
		shouldSucceed(closed.now())
	}

	@Test
	@JsName("employeesEditDept")
	fun `employees cannot edit departments`() = runTest(employeeAuth) {
		val departments = new()
		val dept = testDepartment(departments)

		shouldNotBeAuthorized(dept.close())
		shouldSucceed(dept.now()).apply {
			assertEquals(true, open)
		}

		shouldNotBeAuthorized(dept.open())
		shouldSucceed(dept.now()).apply {
			assertEquals(true, open)
		}
	}

	@Test
	@JsName("adminsEditDept")
	fun `administrators can edit departments`() = runTest(administratorAuth) {
		val departments = new()
		val dept = testDepartment(departments)

		shouldSucceed(dept.close())
		shouldSucceed(dept.now()).apply {
			assertEquals(false, open)
		}

		shouldSucceed(dept.open())
		shouldSucceed(dept.now()).apply {
			assertEquals(true, open)
		}
	}

}
