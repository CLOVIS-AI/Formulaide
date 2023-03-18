package opensavvy.formulaide.mongo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.formulaide.test.execution.Executor
import opensavvy.formulaide.test.execution.Suite
import opensavvy.formulaide.test.templateTestSuite
import opensavvy.formulaide.test.utils.TestClock.Companion.testClock

class TemplateDbTest : Executor() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun Suite.register() {
        templateTestSuite {
            TemplateDb(
                testDatabase(),
                backgroundScope.coroutineContext,
                testClock(),
            )
        }
    }

}
