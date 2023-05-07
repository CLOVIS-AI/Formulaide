package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.File.Companion.TTL_UNLINKED
import opensavvy.formulaide.core.File.Ref
import opensavvy.formulaide.core.File.Service
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.state.outcome.Outcome
import kotlin.time.Duration.Companion.hours

/**
 * An uploaded file.
 *
 * Files are [uploaded][Service.create] before a submission is created, with a limited [time-to-live][TTL_UNLINKED].
 * After the submission is created, they are [linked][Ref.linkTo] to the related submission, and their time-to-live is
 * updated to match the field's configuration.
 */
data class File(
	/**
	 * The reason the upload was created.
	 *
	 * When the file is uploaded, it is not yet linked with its origin, thus this field is `null`. If it is not linked
	 * fast enough (see [TTL_UNLINKED]), the entire upload is destroyed.
	 *
	 * In practice, because the main way to get a reference on an upload is by listing submissions, this field should never
	 * be `null`.
	 */
	val origin: Origin?,
	val mime: String,
	val uploadedAt: Instant,
) {

	data class Origin(
		val form: Form.Ref,
		val record: Record.Ref,
		val submission: Submission.Ref,
		val field: Field.Id,
	)

	interface Ref : opensavvy.backbone.Ref<Failures.Get, File> {

		val id: String

		/**
		 * Links this upload to a [record] and a [submission].
		 *
		 * When a file is linked, its expiration time is extended to match what is configured in the field the file is
		 * based on.
		 *
		 * This function can only be called server-side, there is no HTTP API to call it.
		 */
		suspend fun linkTo(record: Record.Ref, submission: Submission.Ref, field: Field.Id): Outcome<Failures.Link, Unit>

		/**
		 * Reads the binary contents of this file.
		 */
		suspend fun read(): Outcome<Failures.Read, ByteIterator>
	}

	interface Service : Backbone<Ref, Failures.Get, File> {

		/**
		 * Creates a new [File].
		 *
		 * The created upload will expire and be automatically downloaded after the [TTL_UNLINKED] duration is passed.
		 * To ensure it is retained, it must be [linked][link] during that timeframe.
		 */
		suspend fun create(
			mime: String,
			content: ByteIterator,
		): Outcome<Failures.Create, Ref>

		fun fromId(id: String): Ref
	}

	sealed interface Failures {
		sealed interface Get : Failures
		sealed interface Create : Failures
		sealed interface Link : Failures
		sealed interface Read : Failures

		data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
			Get,
			Link,
			Read

		object Unauthenticated : StandardUnauthenticated,
			Get,
			Create,
			Link,
			Read

		object Unauthorized : StandardUnauthorized,
			Get,
			Create,
			Link,
			Read

		data class RecordNotFound(
			override val id: Record.Ref,
			val failure: Record.Failures.Get?,
		) : StandardNotFound<Record.Ref>,
			Link

		data class SubmissionNotFound(
			override val id: Submission.Ref,
			val failure: Submission.Failures.Get?,
		) : StandardNotFound<Submission.Ref>,
			Read

		data class FormVersionNotFound(
			override val id: Form.Version.Ref,
			val form: Form.Version.Failures.Get,
		) : StandardNotFound<Form.Version.Ref>,
			Read

		data class InvalidField(
			val reason: String,
		) : Read

		data class Expired(val ref: Ref) : Read

		data class AlreadyLinked(val ref: Ref) : Link
	}

	companion object {
		/**
		 * How long an upload is stored before being deleted, if it's not yet been linked.
		 */
		val TTL_UNLINKED = 1.hours
	}
}
