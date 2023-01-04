package opensavvy.formulaide.fake

import kotlinx.coroutines.CoroutineScope
import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.spies.SpyUsers.Companion.spied
import opensavvy.formulaide.test.UserTestCases

class UserTest : UserTestCases() {

	override suspend fun new(
		foreground: CoroutineScope,
		background: CoroutineScope,
	): User.Service =
		FakeUsers()
			.spied()

}
