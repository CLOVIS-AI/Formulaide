package opensavvy.formulaide.mongo

import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepared

class DepartmentDbTest : TestExecutor() {

    override fun Suite.register() {
        val database = testDatabase()

        val departments by prepared { DepartmentDb(database, backgroundScope.coroutineContext) }

        departmentTestSuite(departments)
    }

}
