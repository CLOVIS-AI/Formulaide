package formulaide.client.files

import formulaide.client.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertFails

class MultipartTestJVM {

	@Test
	fun emptyJvm() = runTest {
		assertFails {
			@Suppress("BlockingMethodInNonBlockingContext")
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
