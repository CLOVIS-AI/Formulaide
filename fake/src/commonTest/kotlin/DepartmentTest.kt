package opensavvy.formulaide.fake

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.DepartmentTestCases

class DepartmentTest : DepartmentTestCases() {
	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): Department.Service =
		FakeDepartments()
			.spied()
}
