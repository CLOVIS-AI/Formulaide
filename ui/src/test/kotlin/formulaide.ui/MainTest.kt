package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {

	@Test
	fun testHello() {
		assertEquals("Hello World!", helloWorld)
	}

	@Test
	fun testTest() {
		assertTrue(detectTests())
	}

}
