package opensavvy.formulaide.core

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertTrue

class UserTest {

	@Test
	@JsName("rolePrecedence")
	fun `role precedence`() {
		assertTrue(User.Role.Guest < User.Role.Employee)
		assertTrue(User.Role.Employee < User.Role.Administrator)
	}

}
