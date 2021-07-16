package formulaire.api.data

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.FormSubmission
import formulaide.api.data.FormSubmission.Companion.createSubmission
import formulaide.api.fields.*
import formulaide.api.fields.SimpleField.Text
import formulaide.api.types.Arity
import formulaide.api.types.Ref.Companion.createRef
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FormSubmissionTest {

	private val lastName = DataField.Simple(
		id = "2",
		order = 1,
		name = "Nom de famille",
		Text(Arity.mandatory())
	)

	private val firstName = DataField.Simple(
		id = "3",
		order = 2,
		name = "Prénom",
		Text(Arity.mandatory())
	)

	private val phoneNumber = DataField.Simple(
		id = "4",
		order = 3,
		name = "Numéro de téléphone",
		Text(Arity.optional())
	)

	private val family = DataField.Composite(
		id = "5",
		order = 4,
		name = "Famille",
		arity = Arity.list(0, 30),
		Composite("Identité", "some random ID", emptyList()).createRef()
	)

	private val identity = Composite(
		id = "some random ID",
		name = "Identité",
		listOf(
			lastName,
			firstName,
			phoneNumber,
			family
		)
	)

	@Test
	fun composite() {
		val form = Form(
			name = "Foo",
			id = "6",
			open = true,
			public = true,
			mainFields = FormRoot(
				listOf(
					ShallowFormField.Composite(
						id = "7",
						order = 1,
						name = "Demandeur",
						arity = Arity.mandatory(),
						ref = identity.createRef(),
						listOf(
							DeepFormField.Simple(
								lastName,
								Text(Arity.mandatory())
							),
							DeepFormField.Simple(
								firstName,
								Text(Arity.mandatory())
							),
							DeepFormField.Simple(
								phoneNumber,
								Text(Arity.mandatory())
							),
							DeepFormField.Composite(
								family,
								arity = Arity.list(0, 10),
								emptyList()
							)
						)
					),
					ShallowFormField.Union(
						id = "9",
						order = 2,
						name = "Endroit préféré",
						arity = Arity.mandatory(),
						listOf(
							ShallowFormField.Simple(
								id = "10",
								order = 1,
								name = "Proche de la mer",
								SimpleField.Message
							),
							ShallowFormField.Simple(
								id = "11",
								order = 2,
								name = "Proche de la mairie",
								SimpleField.Message
							)
						)
					),
					ShallowFormField.Simple(
						id = "12",
						order = 3,
						name = "Notes",
						Text(Arity.optional())
					),
					ShallowFormField.Simple(
						id = "13",
						order = 4,
						name = "Merci de votre coopération",
						SimpleField.Message
					)
				)
			),
			actions = emptyList()
		)

		println("Identity: " + Json.encodeToString(identity))
		println("Form: " + Json.encodeToString(form))

		form.validate(listOf(identity))

		val submission1 = FormSubmission(
			form.createRef(),
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"7:5:0:2" to "Le Nom de Famille de mon frère",
				"7:5:0:3" to "Le Prénom de mon frère",
				"7:5:1:2" to "Le Nom de Famille de ma sœur",
				"7:5:1:3" to "Le Prénom de ma sœur",
				"7:5:1:4" to "+33 2 34 56 78 91",
				"9" to "10",
			)
		)
		submission1.checkValidity(form)

		val submission2 = FormSubmission(
			form.createRef(),
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"7:5:0:2" to "Le Nom de Famille de mon frère",
				"7:5:0:3" to "Le Prénom de mon frère",
				"7:5:1:2" to "Le Nom de Famille de ma sœur",
				"7:5:1:3" to "Le Prénom de ma sœur",
				"7:5:1:4" to "+33 2 34 56 78 91",
				"12" to "Mes notes",
				"9" to "11",
			)
		)
		submission2.checkValidity(form)

		val submission3 = FormSubmission(
			form.createRef(),
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"12:0" to "Mes notes",
				"12:1" to "Mes notes 2",
				"9" to "10",
			)
		)
		assertFails {
			submission3.checkValidity(form)
		}
	}

	@Test
	fun flatAnswer() {
		val answer = FormSubmission.MutableAnswer("Root").apply {
			components += "1" to FormSubmission.MutableAnswer("First")
				.apply {
					components += "0" to FormSubmission.MutableAnswer(
						"Second"
					)
				}
			components += "2" to FormSubmission.MutableAnswer(null).apply {
				components += "3" to FormSubmission.MutableAnswer("Third")
				components += "4" to FormSubmission.MutableAnswer("Fourth")
			}
		}

		val expected = mapOf(
			"1" to "First",
			"1:0" to "Second",
			"2:3" to "Third",
			"2:4" to "Fourth"
		)

		assertEquals(expected, answer.flatten())
	}

	@Test
	fun submitDsl() {
		val lastNameField = DeepFormField.Simple(
			lastName,
			Text(Arity.mandatory())
		)
		val firstNameField = DeepFormField.Simple(
			firstName,
			Text(Arity.mandatory())
		)
		val phoneNumberField = DeepFormField.Simple(
			phoneNumber,
			Text(Arity.mandatory())
		)
		val phoneNumberRecursionField = DeepFormField.Simple(
			phoneNumber,
			Text(Arity.optional())
		)
		val familyRecursionField2 = DeepFormField.Composite(
			family,
			arity = Arity.forbidden(),
			emptyList()
		)
		val identityRecursionField = DeepFormField.Composite(
			family,
			arity = Arity.list(0, 10),
			listOf(
				lastNameField,
				firstNameField,
				phoneNumberRecursionField,
				familyRecursionField2,
			)
		)
		val identityField = ShallowFormField.Composite(
			id = "7",
			order = 1,
			name = "Demandeur",
			arity = Arity.mandatory(),
			identity.createRef(),
			listOf(
				lastNameField,
				firstNameField,
				phoneNumberField,
				identityRecursionField
			)
		)
		val unionChoice1 = ShallowFormField.Simple("1", 1, "1", SimpleField.Message)
		val unionChoice2 = ShallowFormField.Simple("2", 2, "2", SimpleField.Message)
		val union = ShallowFormField.Union(
			"12",
			2,
			"Choix",
			Arity.mandatory(),
			options = listOf(
				unionChoice1,
				unionChoice2
			)
		)
		val form = Form(
			id = "6",
			name = "Foo",
			open = true,
			public = true,
			mainFields = FormRoot(
				listOf(
					identityField,
					union
				),
			),
			actions = emptyList()
		)
		form.validate(listOf(identity))

		val submission = form.createSubmission {
			composite(identityField) {
				text(firstNameField, "Mon prénom")
				text(lastNameField, "Mon nom de famille")
				text(phoneNumberField, "+33 1 23 45 67 89")
				composite(identityRecursionField) {
					item(50) {
						text(lastNameField, "Le nom de famille de mon frère")
						text(firstNameField, "Le prénom de mon frère")
						text(phoneNumberRecursionField, "Le numéro de téléphone de mon frère")
					}
					item(100) {
						text(lastNameField, "Le nom de famille de ma sœur")
						text(firstNameField, "Le prénom de ma sœur")
					}
				}
			}
			union(union, unionChoice1) { /* it's a MESSAGE, nothing to provide */ }
		}
		val expected = FormSubmission(
			form = form.createRef(),
			data = mapOf(
				"7:2" to "Mon nom de famille",
				"7:3" to "Mon prénom",
				"7:4" to "+33 1 23 45 67 89",
				"7:5:50:2" to "Le nom de famille de mon frère",
				"7:5:50:3" to "Le prénom de mon frère",
				"7:5:50:4" to "Le numéro de téléphone de mon frère",
				"7:5:100:2" to "Le nom de famille de ma sœur",
				"7:5:100:3" to "Le prénom de ma sœur",
				"12" to "1",
			)
		)
		assertEquals(expected, submission)

		assertFails {
			form.createSubmission {
				// The top-level 'identity' field is missing
				text(firstNameField, "Mon prénom")
				text(lastNameField, "Mon nom de famille")
				text(phoneNumberField, "+33 1 23 45 67 89")
				composite(identityRecursionField) {
					item(50) {
						text(lastNameField, "Le nom de famille de mon frère")
						text(firstNameField, "Le prénom de mon frère")
						text(phoneNumberRecursionField, "Le numéro de téléphone de mon frère")
					}
					item(100) {
						text(lastNameField, "Le nom de famille de ma sœur")
						text(firstNameField, "Le prénom de ma sœur")
					}
				}
			}
		}
	}
}
