package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.execution.prepare
import opensavvy.formulaide.test.execution.prepared
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class FormDbTest : Executor() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun Suite.register() {
        val testDepartments by prepared { FakeDepartments().spied() }
        val testTemplates by prepared { FakeTemplates(testClock()).spied() }
        val testForms by prepared {
            val database = testDatabase()
            val departments = prepare(testDepartments)
            val templates = prepare(testTemplates)

            FormDb(database, backgroundScope.coroutineContext, departments, templates, testClock())
        }

        formTestSuite(
            testDepartments,
            testTemplates,
            testForms,
        )
    }

}
