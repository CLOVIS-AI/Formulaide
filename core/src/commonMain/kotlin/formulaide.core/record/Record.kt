package formulaide.core.record

import formulaide.core.User
import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.record.Record.Snapshot
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone
import opensavvy.backbone.Ref.Companion.requestValue
import kotlin.js.JsName

/**
 * A user's request to Formulaide.
 *
 * When a user fills in form, a [Record] is created.
 * The record is updated each time an employee makes an action on it.
 *
 * This object only stores the tracking information for the request, the values given by the user are stored in [Submission] (see [snapshots]).
 *
 * On each employee decision, a new [Snapshot] is added to the object.
 * [modifiedAt] and [currentStep] are updated.
 */
@Serializable
class Record(
	@SerialName("_id") val id: String,

	val form: @Contextual Form.Ref,
	/** The version of [form] this record was created for. It does not change throughout the record's lifetime. */
	val formVersion: Instant,
	/** The review step this record is waiting for (in the form [form] at version [formVersion]). Updated everytime an employee makes a decision. `null` if refused. */
	val currentStep: Int?,

	/** Timestamp of the original request by the user. */
	val createdAt: Instant,
	/** Timestamp of the last snapshot. */
	val modifiedAt: Instant,

	val snapshots: List<Snapshot>,
) {

	@JsName("getFormVersion")
	suspend fun formVersion() = form.requestValue().versions.first { it.creationDate == formVersion }

	@JsName("getCurrentStep")
	suspend fun currentStep() = currentStep?.let { formVersion().reviewSteps.first { it.id == currentStep } }

	@Serializable
	class Snapshot(
		/** The user responsible for this modification, `null` for the anonymous user. */
		val author: @Contextual User.Ref?,
		/** The step this snapshot updates, `null` for the initial submission. */
		val forStep: Int?,
		/** The decision that was made for the future of this record. `null` for the initial submission. */
		val decision: Decision?,
		/** The reason the [decision] was made. `null` for the initial submission, or if the reason is unknown. */
		val reason: String?,
		val createdAt: Instant,
		val submission: Submission?,
	) {
		init {
			if (forStep == null) {
				require(decision == null) { "Il est impossible de prendre une décision à propos de la saisie initiale" }
				require(reason == null) { "Il est impossible de fournir une raison à une décision à propos de la saisie initiale" }
				require(submission != null) { "Une saisie initiale doit comporter une saisie" }
			} else {
				require(decision != null) { "Chaque nouvelle saisie doit faire l'objet d'une décision" }
				require(author != null) { "Une nouvelle saisie ne peut pas être produite par un utilisateur anonyme" }
			}

			if (decision == Decision.Refused)
				requireNotNull(reason) { "Il est obligatoire d'expliquer pourquoi ce dossier est refusé" }
		}
	}

	@Serializable
	sealed class Decision {
		/**
		 * This record was accepted for the current step.
		 *
		 * It will be moved to the next step.
		 */
		object Accepted : Decision()

		/**
		 * This record was refused for the current step.
		 *
		 * It will be moved to the 'refused' step, until another user un-refuses it.
		 */
		object Refused : Decision()

		/**
		 * This record was kept in the current step.
		 *
		 * This may happen, for example, if the current step corresponds to selecting a date for a rendez-vous, but the
		 * user did not answer their phone.
		 */
		object Snoozed : Decision()

		/**
		 * This record was sent back to a previous step.
		 *
		 * This may happen, for example, if the previous step was incorrectly filled in, or if it became invalid.
		 */
		data class MovedToPreviousStep(val step: Int) : Decision()
	}

	data class Ref(val id: String, override val backbone: RecordBackbone) : opensavvy.backbone.Ref<Record> {
		override fun toString() = "Record $id"
	}
}

interface RecordBackbone : Backbone<Record> {

	/**
	 * Creates a new record.
	 */
	suspend fun create(form: Form.Ref, version: Instant, user: User.Ref?, submission: Submission): Record.Ref

	/**
	 * Creates a new [Snapshot] that updates the initial submission.
	 */
	suspend fun editInitial(record: Record.Ref, user: User.Ref, submission: Submission)

	/**
	 * Creates a new [Snapshot] that represents a decision on the record.
	 */
	suspend fun review(
		record: Record.Ref,
		user: User.Ref,
		step: Int?,
		decision: Record.Decision,
		reason: String?,
		submission: Submission?,
	)

	/**
	 * Lists existing records.
	 */
	//TODO in #123: search criteria
	suspend fun list(): List<Record.Ref>

}
