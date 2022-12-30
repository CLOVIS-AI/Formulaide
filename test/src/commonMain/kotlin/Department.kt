package opensavvy.formulaide.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.cases.TestCase
import opensavvy.formulaide.test.cases.TestUsers.administratorAuth
import opensavvy.formulaide.test.cases.TestUsers.employeeAuth
import opensavvy.state.slice.orThrow
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContains
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

		assertUnauthorized(departments.create("A new department"))
	}

	@Test
	@JsName("administratorsCanCreateDepartments")
	fun `administrators can create departments`() = runTest(administratorAuth) {
		val departments = new()

		val ref = assertSuccess(departments.create("A new department"))
		val department = assertSuccess(ref.now())

		assertEquals("A new department", department.name)
		assertEquals(true, department.open)
	}

	@Test
	@JsName("employeesListOpenDepartments")
	fun `employees can list open departments`() = runTest(employeeAuth) {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		val results = assertSuccess(departments.list(includeClosed = false))
		assertContains(results, open)
		assertNotContains(results, closed)

		assertUnauthorized(departments.list(includeClosed = true))

		assertSuccess(open.now())
		assertNotFound(closed.now())
	}

	@Test
	@JsName("anonymousListDepartments")
	fun `guests cannot access departments`() = runTest {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		assertUnauthenticated(departments.list(includeClosed = false))
		assertUnauthenticated(departments.list(includeClosed = true))
		assertUnauthenticated(open.now())
		assertUnauthenticated(closed.now())
	}

	@Test
	@JsName("adminsListDepartments")
	fun `administrators can list all departments`() = runTest(administratorAuth) {
		val departments = new()
		val open = testOpenDepartment(departments)
		val closed = testClosedDepartment(departments)

		val resultsOpen = assertSuccess(departments.list(includeClosed = false))
		assertContains(resultsOpen, open)
		assertNotContains(resultsOpen, closed)

		val resultsAll = assertSuccess(departments.list(includeClosed = true))
		assertContains(resultsAll, open)
		assertContains(resultsOpen, open)

		assertSuccess(open.now())
		assertSuccess(closed.now())
	}

	@Test
	@JsName("employeesEditDept")
	fun `employees cannot edit departments`() = runTest(employeeAuth) {
		val departments = new()
		val dept = testDepartment(departments)

		assertUnauthorized(dept.close())
		assertSuccess(dept.now()).apply {
			assertEquals(true, open)
		}

		assertUnauthorized(dept.open())
		assertSuccess(dept.now()).apply {
			assertEquals(true, open)
		}
	}

	@Test
	@JsName("adminsEditDept")
	fun `administrators can edit departments`() = runTest(administratorAuth) {
		val departments = new()
		val dept = testDepartment(departments)

		assertSuccess(dept.close())
		assertSuccess(dept.now()).apply {
			assertEquals(false, open)
		}

		assertSuccess(dept.open())
		assertSuccess(dept.now()).apply {
			assertEquals(true, open)
		}
	}

}
