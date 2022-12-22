package opensavvy.formulaide.fake

import opensavvy.formulaide.core.Department
import opensavvy.formulaide.test.DepartmentTestCases

class DepartmentTest : DepartmentTestCases() {
	override suspend fun new(): Department.Service = FakeDepartments()
}
