package formulaire.api.data

import formulaide.api.data.*
import formulaide.api.types.Arity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FormSubmissionTest {

	private val lastName = CompoundDataField(
		id = 2,
		order = 1,
		arity = Arity.mandatory(),
		name = "Nom de famille",
		data = Data.simple(Data.Simple.SimpleDataId.TEXT)
	)

	private val firstName = CompoundDataField(
		id = 3,
		order = 2,
		arity = Arity.mandatory(),
		name = "Prénom",
		data = Data.simple(Data.Simple.SimpleDataId.TEXT)
	)

	private val phoneNumber = CompoundDataField(
		id = 4,
		order = 3,
		arity = Arity.optional(),
		name = "Numéro de téléphone",
		data = Data.simple(Data.Simple.SimpleDataId.TEXT)
	)

	private val family = CompoundDataField(
		id = 5,
		order = 4,
		arity = Arity.optional(),
		name = "Famille",
		data = Data.compound(
			CompoundData(
				name = "Identité",
				id = "some random ID",
				fields = emptyList()
			)
		)
	)

	private val identity = CompoundData(
		name = "Identité",
		id = "some random ID",
		fields = listOf(
			lastName,
			firstName,
			phoneNumber,
			family
		)
	)

	@Test
	fun compound() {
		val form = Form(
			name = "Foo",
			id = 6,
			open = true,
			public = true,
			fields = listOf(
				FormField(
					id = 7,
					order = 1,
					name = "Demandeur",
					arity = Arity.mandatory(),
					data = Data.compound(identity),
					components = listOf(
						FormFieldComponent(
							arity = Arity.mandatory(),
							lastName
						),
						FormFieldComponent(
							arity = Arity.mandatory(),
							firstName
						),
						FormFieldComponent(
							arity = Arity.mandatory(),
							phoneNumber
						),
						FormFieldComponent(
							arity = Arity(0, 10),
							family,
							components = listOf(
								FormFieldComponent(
									arity = Arity.mandatory(),
									lastName
								),
								FormFieldComponent(
									arity = Arity.mandatory(),
									firstName
								),
								FormFieldComponent(
									arity = Arity.optional(),
									phoneNumber
								),
								FormFieldComponent(
									arity = Arity.forbidden(),
									family,
									components = listOf()
								)
							)
						)
					)
				),
				FormField(
					id = 9,
					order = 2,
					name = "Endroit préféré",
					arity = Arity.mandatory(),
					data = Data.union(
						listOf(
							UnionDataField(
								id = 10,
								type = Data.simple(id = Data.Simple.SimpleDataId.MESSAGE),
								name = "Proche de la mer",
							),
							UnionDataField(
								id = 11,
								type = Data.simple(id = Data.Simple.SimpleDataId.MESSAGE),
								name = "Proche de la mairie",
							)
						)
					)
				),
				FormField(
					id = 12,
					order = 3,
					name = "Notes",
					arity = Arity.optional(),
					data = Data.simple(Data.Simple.SimpleDataId.MESSAGE)
				)
			),
			actions = emptyList()
		)

		val submission1 = FormSubmission(
			form.id,
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"7:5:0:2" to "Le Nom de Famille de mon frère",
				"7:5:0:3" to "Le Prénom de mon frère",
				"7:5:1:2" to "Le Nom de Famille de ma sœur",
				"7:5:1:3" to "Le Prénom de ma sœur",
				"7:5:1:3" to "+33 2 34 56 78 91",
				"9:10" to "",
			)
		)
		submission1.checkValidity(form, listOf(identity))

		val submission2 = FormSubmission(
			form.id,
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"7:5:0:2" to "Le Nom de Famille de mon frère",
				"7:5:0:3" to "Le Prénom de mon frère",
				"7:5:1:2" to "Le Nom de Famille de ma sœur",
				"7:5:1:3" to "Le Prénom de ma sœur",
				"7:5:1:3" to "+33 2 34 56 78 91",
				"12" to "Mes notes",
				"9:11" to "",
			)
		)
		submission2.checkValidity(form, listOf(identity))

		val submission3 = FormSubmission(
			form.id,
			mapOf(
				"7:2" to "Mon Nom de Famille",
				"7:3" to "Mon Prénom",
				"7:4" to "+33 1 23 45 67 89",
				"12:0" to "Mes notes",
				"12:1" to "Mes notes 2",
				"9:10" to "",
			)
		)
		assertFails {
			submission3.checkValidity(form, listOf(identity))
		}
	}

	@Test
	fun flatAnswer() {
		val answer = FormSubmission.MutableAnswer("Root").apply {
			components += "1" to FormSubmission.MutableAnswer("First").apply {
				components += "0" to FormSubmission.MutableAnswer("Second")
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

}
