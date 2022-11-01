package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.state.slice.Slice

class Form(
	val name: String,
	val public: Boolean,
	val open: Boolean,
	val versions: List<Version.Ref>,
) {

	data class Ref(
		val id: String,
		override val backbone: AbstractForms,
	) : opensavvy.backbone.Ref<Form> {

		override fun toString() = "Formulaire $id"
	}

	data class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
		val reviewSteps: List<Step>,
	) {

		data class Ref(
			val form: Form.Ref,
			val version: Instant,
			override val backbone: AbstractFormVersions,
		) : opensavvy.backbone.Ref<Version> {

			override fun toString() = "$form $version"
		}
	}

	data class Step(
		val id: Int,
		val reviewer: Department.Ref,
		val field: Field?,
	)
}

interface AbstractForms : Backbone<Form> {

	/**
	 * Lists all forms.
	 */
	suspend fun list(includeClosed: Boolean = false, includePrivate: Boolean = false): Slice<List<Form.Ref>>

	/**
	 * Creates a new form.
	 *
	 * Only administrators can create new forms.
	 */
	suspend fun create(name: String, public: Boolean, firstVersion: Form.Version): Slice<Form.Ref>

	/**
	 * Creates a new version of a given form.
	 *
	 * Only administrators can create new versions.
	 */
	suspend fun createVersion(form: Form.Ref, version: Form.Version): Slice<Form.Version.Ref>

	/**
	 * Edits a form.
	 *
	 * Only administrators can edit forms.
	 */
	suspend fun edit(form: Form.Ref, name: String? = null, public: Boolean? = null, open: Boolean? = null): Slice<Unit>

}

interface AbstractFormVersions : Backbone<Form.Version>
