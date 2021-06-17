package formulaide.ui

import formulaide.ui.utils.detectTests
import kotlin.test.Test
import kotlin.test.assertTrue

class MainTest {

	@Test
	fun testTest() {
		assertTrue(detectTests())
	}

}
