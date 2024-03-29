package opensavvy.formulaide.mongo

import opensavvy.formulaide.mongo.utils.commonIds
import opensavvy.formulaide.test.identifierParsingSuite
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.templateTestSuite

class MongoTemplateTest : TestExecutor() {

    override fun Suite.register() {
        val templates by prepared { MongoTemplate(testDatabase(), backgroundScope, clock) }

        templateTestSuite(templates)

        identifierParsingSuite(
            templates,
            *commonIds,
        ) { prepare(templates).Ref(it) }
    }

}
