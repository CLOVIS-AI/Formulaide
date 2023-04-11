package opensavvy.formulaide.mongo

import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.prepare
import opensavvy.formulaide.test.structure.prepared
import opensavvy.formulaide.test.usersTestSuite

class UserDbTest : TestExecutor() {

    override fun Suite.register() {
        val departments by prepared { FakeDepartments().spied() }

        val users by prepared {
            val database = testDatabase()

            UserDb(
                database,
                backgroundScope.coroutineContext,
                prepare(departments),
            )
        }

        usersTestSuite(departments, users)
    }


}
