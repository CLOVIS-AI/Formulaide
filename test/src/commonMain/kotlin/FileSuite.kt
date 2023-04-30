package opensavvy.formulaide.test

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import opensavvy.backbone.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.test.assertions.*
import opensavvy.formulaide.test.structure.*
import opensavvy.formulaide.test.utils.TestUsers.administratorAuth
import opensavvy.formulaide.test.utils.TestUsers.employeeAuth
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

fun Suite.fileTestSuite(
	testDepartment: Setup<Department.Ref>,
	testForms: Setup<Form.Service>,
	testRecords: Setup<Record.Service>,
	testFiles: Setup<File.Service>,
) {
	suite("A user can upload a file") {
		/**
		 * A simple file with contents 'Hello world!' of MIME type 'text/plain'
		 */
		val testFile by prepared {
			val files = prepare(testFiles)

			files.create("text/plain", "Hello world!".encodeToByteArray().iterator()).bind()
		}

		suite("Metadata access") {
			test("Guests cannot access its metadata") {
				val file = prepare(testFile)

				shouldNotBeAuthenticated(file.now())
			}

			test("Employees can access its metadata", employeeAuth) {
				val file = prepare(testFile)

				file.now() shouldSucceedAnd {
					it.mime shouldBe "text/plain"
				}
			}
		}

		/**
		 * The metadata of `testFile`
		 */
		val testFileMetadata by prepared(administratorAuth) {
			val file = prepare(testFile)

			file.now().bind()
		}

		suite("Metadata validity") {
			test("The creation date is correct", employeeAuth) {
				val before = currentInstant
				advanceTimeBy(10)

				val meta = prepare(testFileMetadata)

				advanceTimeBy(10)
				val after = currentInstant

				meta.uploadedAt shouldBeGreaterThan before
				meta.uploadedAt shouldBeLessThan after
			}

			test("The origin is null", employeeAuth) {
				val file = prepare(testFile)

				file.now() shouldSucceedAnd {
					it.origin shouldBe null
				}
			}
		}

		suite("File content") {
			test("Employees cannot read it", employeeAuth) {
				val file = prepare(testFile)

				shouldNotBeFound(file.read())
			}

			test("Admins can read it", administratorAuth) {
				val file = prepare(testFile)

				file.read() shouldSucceedAnd {
					it.asSequence().toList().toByteArray() shouldBe "Hello world!".encodeToByteArray()
				}
			}
		}

		suite("Deletion after the timeout") {
			/**
			 * `testFile` after its expiration timeout (it has not been linked).
			 */
			val oldFile by prepared {
				prepare(testFile)
					.also { delay(File.TTL_UNLINKED + 10.seconds) }
			}

			test("The file cannot be read anymore after the timeout", administratorAuth) {
				val file = prepare(oldFile)

				file.read() shouldFailWithKey File.Failures.Expired
			}

			test("The metadata is not impacted", employeeAuth) {
				val initial = prepare(testFile)
				val initialMetadata = initial.now().bind()

				val old = prepare(oldFile)
				val oldMetadata = old.now().bind()

				oldMetadata shouldBe initialMetadata
			}
		}
	}

	/**
	 * The simplest possible valid file: a CSV.
	 */
	val testCsv by prepared {
		val files = prepare(testFiles)

		val content = """
			Column 1,Column 2
			Value 1,Value 2
			Value 3,Value 4
		""".trimIndent()

		files.create("text/csv", content.encodeToByteArray().iterator()).bind()
	}

	/**
	 * The simplest possible form that can request a file upload.
	 */
	val testForm by prepared(administratorAuth) {
		val forms = prepare(testForms)
		val department = prepare(testDepartment)

		val file = Input.upload(
			allowedFormats = setOf(Input.Upload.Format.Tabular),
			expiresAfter = 30.days,
		).bind()

		check(file.expiresAfter!! >= File.TTL_UNLINKED) { "The expiration delay used in tests should always be greater than TTL_UNLINKED" }

		forms.create(
			"File upload test",
			"Initial version",
			Field.input("Uploaded file", file),
			Form.Step(0, "Validation", department, null),
		).bind()
	}

	/**
	 * A submission for form `testForm` which contains `testCsv`.
	 */
	val testRecord by prepared {
		val form = prepare(testForm)
		val records = prepare(testRecords)
		val csv = prepare(testCsv)

		val firstFormVersion = form.now().bind()
			.versions.first()

		records.create(
			firstFormVersion,
			"" to csv.id, // The answer to the root field is the provided field
		).bind()
	}

	suite("When a matching submission is created, the file is linked") {
		/**
		 * The metadata of `testCsv`.
		 */
		val testFileMetadata by prepared(administratorAuth) {
			val file = prepare(testCsv)
			prepare(testRecord)

			file.now().bind()
		}

		suite("The origin should be filled in") {
			test("The file has an origin") {
				val origin = prepare(testFileMetadata).origin

				origin shouldNotBe null
			}

			test("The origin is the correct form") {
				val origin = prepare(testFileMetadata).origin
				val form = prepare(testForm)

				origin!!.form shouldBe form
			}

			test("The origin is the correct record") {
				val origin = prepare(testFileMetadata).origin
				val record = prepare(testRecord)

				origin!!.record shouldBe record
			}

			test("The origin is the correct submission") {
				val origin = prepare(testFileMetadata).origin
				val record = prepare(testRecord).now().bind()
				val submission = record.initialSubmission.submission

				origin!!.submission shouldBe submission
			}

			test("The origin is the correct field") {
				val origin = prepare(testFileMetadata).origin

				origin!!.field shouldBe Field.Id.root
			}
		}

		test("A linked file is not deleted after the regular timeout", employeeAuth) {
			val file = prepare(testCsv)
			prepare(testRecord)

			delay(File.TTL_UNLINKED + 10.seconds)

			shouldSucceed(file.read())
		}

		test("A linked file is deleted after the timeout declared in the form", administratorAuth) {
			val file = prepare(testCsv)
			prepare(testRecord)

			delay(30.days + 10.seconds)

			file.read() shouldFailWithKey File.Failures.Expired
		}
	}
}
