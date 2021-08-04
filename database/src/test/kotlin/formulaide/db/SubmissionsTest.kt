package formulaide.db

import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.dsl.*
import formulaide.api.fields.*
import formulaide.api.fields.SimpleField.Text
import formulaide.api.search.SearchCriterion
import formulaide.api.types.Arity
import formulaide.db.document.DbSubmissionData.Companion.toApi
import formulaide.db.document.DbSubmissionData.Companion.toDbSubmissionData
import formulaide.db.document.createComposite
import formulaide.db.document.createForm
import formulaide.db.document.saveSubmission
import formulaide.db.document.searchSubmission
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SubmissionsTest {

	@Test
	fun simple() = runBlocking {
		val db = testDatabase()

		lateinit var lastName: FormField.Simple
		lateinit var firstName: FormField.Simple

		val form = db.createForm(form(
			"Un formulaire intéressant",
			public = true,
			mainFields = formRoot {
				lastName = simple("Nom", Text(Arity.mandatory()))
				firstName = simple("Prénom", Text(Arity.optional()))
			}
		))

		val submission1 = form.createSubmission {
			text(lastName, "Mon nom de famille")
			text(firstName, "Mon prénom")
		}

		val submission2 = form.createSubmission {
			text(lastName, "Mon nom de famille 2")
			text(firstName, "Mon prénom 2")
		}

		db.saveSubmission(submission1)
		db.saveSubmission(submission2)
		Unit
	}

	@Test
	fun dbSubmissions() = runBlocking<Unit> {
		val database = testDatabase()

		lateinit var familyName: DataField.Simple
		lateinit var firstName: DataField.Simple

		val composite = composite("Une donnée composée") {
			simple("Nom de famille", Text(Arity.mandatory())).also { familyName = it }
			simple("Prénom", Text(Arity.optional())).also { firstName = it }
		}.let { database.createComposite(it) }

		lateinit var phoneNumber: ShallowFormField.Simple
		lateinit var genre: ShallowFormField.Union
		lateinit var male: ShallowFormField.Simple
		lateinit var female: ShallowFormField.Simple
		lateinit var other: ShallowFormField.Simple
		lateinit var list: ShallowFormField.Simple
		lateinit var identity: ShallowFormField.Composite
		lateinit var familyName2: DeepFormField.Simple
		lateinit var firstName2: DeepFormField.Simple

		val form = form(
			"Nouveau test de saisie",
			public = false,
			mainFields = formRoot {
				simple("Numéro de téléphone", Text(Arity.mandatory())).also { phoneNumber = it }

				union("Genre", Arity.optional()) {
					simple("Homme", SimpleField.Message).also { male = it }
					simple("Femme", SimpleField.Message).also { female = it }
					simple("Autre", Text(Arity.mandatory())).also { other = it }
				}.also { genre = it }

				simple("Entrées quelconques", Text(Arity.list(0, 5))).also { list = it }

				composite("Identité", Arity.mandatory(), composite) {
					simple(familyName, Text(Arity.mandatory())).also { familyName2 = it }
					simple(firstName, Text(Arity.optional())).also { firstName2 = it }
				}.also { identity = it }
			}
		).let { database.createForm(it) }

		val submission1 = form.createSubmission {
			text(phoneNumber, "01 23 45 67 89")
			union(genre, male) {}
			composite(identity) {
				text(familyName2, "F1")
				text(firstName2, "F2")
			}
		}
		val submission2 = form.createSubmission {
			text(phoneNumber, "01 23 45 67 89")
			union(genre, female) {}
			list(list) {
				text(list, "Entry 1")
				text(list, "Entry 2")
			}
			composite(identity) {
				text(familyName2, "G1")
			}
		}
		val submission3 = form.createSubmission {
			text(phoneNumber, "01 23 45 67 89")
			union(genre, other) {
				text(other, "Unspecified")
			}
			list(list) {
				text(list, "Entry")
			}
			composite(identity) {
				text(familyName2, "H1")
				text(firstName2, "H2")
			}
		}
		val submissions = listOf(
			submission1,
			submission2,
			submission3
		)

		for (submission in submissions) {
			database.saveSubmission(submission)

			println("\nSubmission: ${submission.data}")

			val parsed = submission.parse(form)
			println("Parsed: $parsed")

			val dbSubmission = parsed.toDbSubmissionData()
			println("Db submission: $dbSubmission")

			val submissionReparsed = dbSubmission.toApi()
			println("Back to submission: $submissionReparsed")

			assertEquals(submission.data, submissionReparsed)
		}

		suspend fun query(vararg queries: SearchCriterion<*>) =
			database.searchSubmission(form, null, queries.asList())

		val allResults = query(SearchCriterion.TextContains("0", "1"))
		val maleResults = query(SearchCriterion.TextEquals("1", "0"))
		val femaleResults = query(SearchCriterion.TextEquals("1", "1"))
		val otherResults = query(SearchCriterion.TextEquals("1", "2"))
		val resultsThatRepliedToList = query(SearchCriterion.Exists("2"))
		val resultsThatGaveTheFirstName = query(SearchCriterion.Exists("3:1"))

		assertEquals(3, allResults.size, "Found: $allResults")
		assertEquals(1, maleResults.size, "Found: $maleResults")
		assertEquals(1, femaleResults.size, "Found: $femaleResults")
		assertEquals(1, otherResults.size, "Found: $otherResults")
		assertEquals(2, resultsThatRepliedToList.size, "Found: $resultsThatRepliedToList")
		assertEquals(2, resultsThatGaveTheFirstName.size, "Found: $resultsThatGaveTheFirstName")

		val malesThatGaveTheFirstName =
			query(SearchCriterion.Exists("3:1"), SearchCriterion.TextEquals("1", "0"))
		assertEquals(1, malesThatGaveTheFirstName.size, "Found: $malesThatGaveTheFirstName")
		println(malesThatGaveTheFirstName)
	}
}
