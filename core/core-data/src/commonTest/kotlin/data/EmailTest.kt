package opensavvy.formulaide.core.data

import opensavvy.formulaide.core.data.Email.Companion.asEmail
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFails

class EmailTest {

	@Test
	@JsName("validEmail")
	fun `valid email`() {
		"my.name@formulaide.com".asEmail()
		"support@formulaide.dev".asEmail()
		"formulaide@opensavvy.dev".asEmail()
	}

	@Test
	@JsName("emailMissingAt")
	fun `email without @`() {
		assertFails { "email".asEmail() }
		assertFails { "".asEmail() }
		assertFails { "a.real-email.com".asEmail() }
	}

}
