package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.usersTestSuite

class UserDbTest : Executor() {

    @OptIn(ExperimentalCoroutinesApi::class)
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
