@file:OptIn(ExperimentalCoroutinesApi::class)

package formulaide.client.routes

import formulaide.api.data.Action
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.data.RecordState
import formulaide.api.data.RecordStateTransition
import formulaide.api.data.ReviewRequest
import formulaide.api.dsl.form
import formulaide.api.dsl.formRoot
import formulaide.api.dsl.simple
import formulaide.api.dsl.union
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField.Message
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.api.users.canAccess
import formulaide.client.testAdministrator
import formulaide.client.testEmployee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class RecordTest {

	@Test
	fun todolist() = runTest {
		val admin = testAdministrator()

		val client = testEmployee()
		val me = client.getMe()

		val otherService =
			admin.createService("Un service qui sert juste à assigner la review à quelqu'un d'autre")

		lateinit var familyName: ShallowFormField.Simple
		lateinit var firstName: ShallowFormField.Simple
		lateinit var favoriteAnimal: ShallowFormField.Union
		lateinit var cat: ShallowFormField.Simple
		lateinit var dog: ShallowFormField.Simple
		lateinit var action0: Action
		lateinit var reviewText: ShallowFormField.Simple
		val assignedForm = admin.createForm(form(
			"Test de suivi, assigné à l'utilisateur de test",
			public = false,
			mainFields = formRoot {
				familyName = simple("Nom de famille", Text(Arity.mandatory()))
				firstName = simple("Prénom", Text(Arity.optional()))

				favoriteAnimal = union("Animal préféré", Arity.mandatory()) {
					cat = simple("Chat", Message)
					dog = simple("Chien", Message)
				}
			},
			Action(
				"0",
				0,
				me.services.first(),
				"Vérification de l'identité",
				formRoot {
					reviewText = simple("Un texte normal", Text(Arity.mandatory()))
				}
			).also { action0 = it },
			Action("1", 1, otherService.createRef(), "Étape 2"),
			Action("2", 2, me.services.first(), "Étape 3"),
		))

		val notAssignedForm = admin.createForm(form(
			"Test de suivi, pas assigné à l'utilisateur de test",
			public = false,
			mainFields = formRoot {
				simple("Un champ", Text(Arity.mandatory()))
			},
			Action("0", 0, otherService.createRef(), "Étape 1"),
		))

		val assignedTodoList = client.todoList()
			.onEach { println("Formulaire qui m'est assigné : $it") }

		assertTrue(assignedTodoList.any { it.id == assignedForm.id })
		assertFalse(assignedTodoList.any { it.id == notAssignedForm.id })

		assertTrue(client.todoListFor(assignedForm, RecordState.Action(Ref("0")))
			           .isEmpty())

		val alphaSubmission = assignedForm.createSubmission(null) {
			text(familyName, "Alpha 1")
			text(firstName, "Alpha 2")
			union(favoriteAnimal, cat) {}
		}.also { client.submitForm(it) }

		val betaSubmission = assignedForm.createSubmission(null) {
			text(familyName, "Beta 1")
			text(firstName, "Beta 2")
			union(favoriteAnimal, dog) {}
		}.also { client.submitForm(it) }

		val records = client.todoListFor(assignedForm, RecordState.Action(Ref("0")))
			.onEach { println("Saisie pour l'étape 0 du formulaire ${assignedForm.id} : $it") }
		assertEquals(2, records.size)
		for (record in records) {
			record.form.load(assignedForm) // if this fails, todoListFor is broken
			for (it in record.submissions) {
				it.load { client.findSubmission(it) }
			}
		}

		val alphaSubmissionActualList =
			records.filter { it.submissions.any { it.obj.root == null && it.obj.data.values.any { "Alpha" in it } } }
		assertEquals(1, alphaSubmissionActualList.size)
		val alphaSubmissionActual = alphaSubmissionActualList.first().submissions.first().obj
		assertEquals(alphaSubmission.form, alphaSubmissionActual.form)
		assertEquals(alphaSubmission.root, alphaSubmissionActual.root)
		assertEquals(alphaSubmission.data, alphaSubmissionActual.data)

		val betaSubmissionActualList =
			records.filter { it.submissions.any { it.obj.root == null && it.obj.data.values.any { "Beta" in it } } }
		assertEquals(1, betaSubmissionActualList.size)
		val betaSubmissionActual = betaSubmissionActualList.first().submissions.first().obj
		assertEquals(betaSubmission.form, betaSubmissionActual.form)
		assertEquals(betaSubmission.root, betaSubmissionActual.root)
		assertEquals(betaSubmission.data, betaSubmissionActual.data)

		withContext(Dispatchers.Default) {
			// Force wait to ensure the database has had time to create all necessary objects
			delay(1000)
		}

		client.review(
			ReviewRequest(
				alphaSubmissionActualList.first().createRef(),
				RecordStateTransition(
					timestamp = Long.MAX_VALUE,
					previousState = RecordState.Action(Ref("0")),
					nextState = RecordState.Action(Ref("1")),
					assignee = me.createRef(),
					reason = null,
				),
			fields = assignedForm.createSubmission(action0) {
				text(reviewText, "J'accepte le passage à l'étape suivante")
			}
		))

		client.review(ReviewRequest(
			betaSubmissionActualList.first().createRef(),
			RecordStateTransition(
				timestamp = Long.MAX_VALUE,
				previousState = RecordState.Action(Ref("0")),
				nextState = RecordState.Refused,
				assignee = me.createRef(),
				reason = "Je n'aime pas cette réponse",
			),
			fields = null,
		))

		assertTrue(me.canAccess(assignedForm, null))

		assertEquals(
			0,
			client.todoListFor(assignedForm, RecordState.Action(Ref("0")))
				.also { println("Étape 0 : $it") }.size
		)
		assertFails {
			// The action #1 is not assigned to me
			client.todoListFor(assignedForm, RecordState.Action(Ref("1"))).size
		}
		assertEquals(
			1,
			// The admin is not assigned either, but they can still access it
			admin.todoListFor(assignedForm, RecordState.Action(Ref("1"))).size
		)
		assertEquals(
			1,
			client.todoListFor(assignedForm, RecordState.Refused)
				.also { println("Refusés : $it") }.size
		)

		// Cleanup…
		admin.closeService(otherService)
	}

	@Test
	fun delete() = runTest {
		val admin = testAdministrator()

		val client = testEmployee()
		val me = client.getMe()

		lateinit var familyName: ShallowFormField.Simple
		lateinit var firstName: ShallowFormField.Simple
		val assignedForm = admin.createForm(
			form(
				"Test de suppression de données",
				public = false,
				mainFields = formRoot {
					familyName = simple("Nom de famille", Text(Arity.mandatory()))
					firstName = simple("Prénom", Text(Arity.optional()))
				},
				Action(
					"0",
					0,
					me.services.first(),
					"Vérification",
				),
			)
		)

		val submission1 = assignedForm.createSubmission(null) {
			text(familyName, "Family name")
			text(firstName, "First name")
		}.also { client.submitForm(it) }

		assignedForm.createSubmission(null) {
			text(familyName, "Family name other")
			text(firstName, "First name other")
		}.also { client.submitForm(it) }

		//region Scenario 1
		// -> Try to delete a record by ignoring the challenge
		// => forbidden
		val records = client.todoListFor(assignedForm, RecordState.Action(Ref("0")))
		assertTrue(records.size == 2)
		val (record1, record2) = records
		assertFails {
			admin.deleteRecord(record1.createRef(), "")
		}
		assertEquals(records, client.todoListFor(assignedForm, RecordState.Action(Ref("0"))))
		//endregion

		//region Scenario 2
		// -> Request a deletion for a record
		// -> Attempt to delete another one with the valid challenge response
		// => forbidden
		val challenge = admin.requestDeleteRecord(record1.createRef())
		val response = findAnswer(challenge.challenge)
		assertFails {
			admin.deleteRecord(record2.createRef(), response)
		}
		assertEquals(records, client.todoListFor(assignedForm, RecordState.Action(Ref("0"))))
		//endregion

		//region Scenario 3
		// -> Request a deletion for a record
		// -> Attempt to delete it with an invalid challenge response
		// => forbidden
		// => cancels the deletion request for that challenge (impossible to try with the valid password)
		val challenge3 = admin.requestDeleteRecord(record1.createRef())
		val response3 = findAnswer(challenge3.challenge)
		assertFails {
			admin.deleteRecord(record2.createRef(), "$response3...")
		}
		assertFails {
			admin.deleteRecord(record2.createRef(), response3)
		}
		assertEquals(records, client.todoListFor(assignedForm, RecordState.Action(Ref("0"))))
		//endregion

		//region Scenario 4
		// -> Request a deletion for a record
		// -> Attempt to delete it as a non-admin
		// => forbidden
		val challenge4 = admin.requestDeleteRecord(record1.createRef())
		val response4 = findAnswer(challenge4.challenge)
		assertFails {
			client.deleteRecord(record1.createRef(), response4)
		}
		assertEquals(records, client.todoListFor(assignedForm, RecordState.Action(Ref("0"))))
		//endregion

		//region Scenario 5
		// -> Request a deletion for a record
		// -> Attempt to delete it correctly
		// => success
		// => the record doesn't exist anymore
		// => the submission doesn't exist anymore
		val challenge5 = admin.requestDeleteRecord(record1.createRef())
		val response5 = findAnswer(challenge5.challenge)
		admin.deleteRecord(record1.createRef(), response5)
		assertEquals(records - record1, client.todoListFor(assignedForm, RecordState.Action(Ref("0"))))
		assertFails {
			admin.findSubmission(submission1.id)
		}
		//endregion
	}
}

private fun findAnswer(challenge: String): String {
	println("Challenge : $challenge")

	val (first, second, third) = challenge
		.split(" ")
		.mapNotNull { it.toIntOrNull() }

	return (first * second + third).toString()
}
