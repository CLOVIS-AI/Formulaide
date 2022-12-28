package opensavvy.formulaide.fake

import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.test.UserTestCases

class UserTest : UserTestCases() {

	override suspend fun new(): User.Service = FakeUsers()
		.spied()

}
