package opensavvy.formulaide.mongo

import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor

class DepartmentDbTest : TestExecutor() {

    override fun Suite.register() {
        departmentTestSuite {
            val database = testDatabase()

            DepartmentDb(
                database,
                backgroundScope.coroutineContext,
            )
        }
    }

}
