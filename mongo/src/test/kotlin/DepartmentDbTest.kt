package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite

class DepartmentDbTest : Executor() {

    @OptIn(ExperimentalCoroutinesApi::class)
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
