package formulaide.client

import formulaide.api.Ping
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloTest {

	@Test
	fun testPing() {
		val expected = Ping(12345)
		val actual = ping()

		assertEquals(expected, actual)
	}

}
