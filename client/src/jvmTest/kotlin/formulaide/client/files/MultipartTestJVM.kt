package formulaide.client.files

import formulaide.client.runTest
import org.junit.Test
import java.io.File

class MultipartTestJVM {

	@Test
	fun multipartTestJvm() = runTest {
		@Suppress("BlockingMethodInNonBlockingContext")
		multipartTest(FileUploadJVM(File.createTempFile("formulaide-multipart-test-jvm", ".txt")))
	}

}
