package opensavvy.formulaide.mongo

import opensavvy.formulaide.fake.FakeDepartments
import opensavvy.formulaide.fake.FakeTemplates
import opensavvy.formulaide.fake.spies.SpyDepartments.Companion.spied
import opensavvy.formulaide.fake.spies.SpyTemplates.Companion.spied
import opensavvy.formulaide.mongo.utils.commonIds
import opensavvy.formulaide.test.formTestSuite
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.*

class FormDbTest : TestExecutor() {

    override fun Suite.register() {
        val testDepartments by prepared { FakeDepartments().spied() }
        val testTemplates by prepared { FakeTemplates(clock).spied() }
        val testForms by prepared {
            val database = testDatabase()
            val departments = prepare(testDepartments)
            val templates = prepare(testTemplates)

            FormDb(database, backgroundScope, departments, templates, clock)
        }

        formTestSuite(
            testDepartments,
            testTemplates,
            testForms,
        )

        identifierParsingSuite(
            testForms,
            *commonIds,
        ) { prepare(testForms).Ref(it) }
    }

}
