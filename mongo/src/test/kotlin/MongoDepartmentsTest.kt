package opensavvy.formulaide.mongo

import opensavvy.formulaide.mongo.utils.commonIds
import opensavvy.formulaide.test.departmentTestSuite
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared

class MongoDepartmentsTest : TestExecutor() {

    override fun Suite.register() {
        val database = testDatabase()

        val departments by prepared { MongoDepartments(database, backgroundScope) }

        departmentTestSuite(departments)

        identifierParsingSuite(
            departments,
            *commonIds,
        ) { prepare(departments).Ref(it) }
    }

}
