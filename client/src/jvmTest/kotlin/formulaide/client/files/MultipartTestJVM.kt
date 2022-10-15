@file:OptIn(ExperimentalCoroutinesApi::class)

package formulaide.client.files

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertFails

class MultipartTestJVM {

	@Test
	fun emptyJvm() = runTest {
		assertFails {
			multipartTest(FileUploadJVM(File.createTempFile("formulaide-multipart-test-jvm",
			                                                ".txt")))
		}
	}

	@Test
	fun imageJvm() = runTest {
		val resource = MultipartTestJVM::class.java
			.getResource("/images/book-flowers.jpg")
			?: error("Could not find resource")

		val file = resource
			.path
			.let { File(it) }

		multipartTest(FileUploadJVM(file))
	}

}
