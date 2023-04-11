package opensavvy.formulaide.mongo

import opensavvy.formulaide.test.structure.Suite
import opensavvy.formulaide.test.structure.TestExecutor
import opensavvy.formulaide.test.structure.clock
import opensavvy.formulaide.test.templateTestSuite

class TemplateDbTest : TestExecutor() {

    override fun Suite.register() {
        templateTestSuite {
            TemplateDb(
                testDatabase(),
                backgroundScope.coroutineContext,
                clock,
            )
        }
    }

}
