package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.File.Companion.TTL_UNLINKED
import opensavvy.formulaide.core.File.Ref
import opensavvy.formulaide.core.File.Service
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

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<File> {

		/**
		 * Links this upload to a [record] and a [submission].
		 *
		 * @see Service.link
		 */
		suspend fun linkTo(record: Record.Ref, submission: Submission.Ref, field: Field.Id) =
			backbone.link(this, record, submission, field)

		/**
		 * Reads the binary contents of this file.
		 *
		 * @see Service.read
		 */
		suspend fun read() =
			backbone.read(this)
	}

	interface Service : Backbone<File> {

		/**
		 * Creates a new [File].
		 *
		 * The created upload will expire and be automatically downloaded after the [TTL_UNLINKED] duration is passed.
		 * To ensure it is retained, it must be [linked][link] during that timeframe.
		 */
		suspend fun create(
			mime: String,
			content: ByteIterator,
		): Outcome<Ref>

		/**
		 * Links [upload] to the provided [record] and [submission].
		 *
		 * When an upload is linked, its expiration time is extended to match what is configured in the field the upload
		 * was based on.
		 *
		 * This function can only be called server-side, there is no HTTP API to call it.
		 */
		suspend fun link(
			upload: Ref,
			record: Record.Ref,
			submission: Submission.Ref,
			field: Field.Id,
		): Outcome<Unit>

		/**
		 * Reads the binary contents of this file.
		 *
		 * Will fail with 'not found' if the upload has expired.
		 */
		suspend fun read(upload: Ref): Outcome<ByteIterator>

	}

	companion object {
		/**
		 * How long an upload is stored before being deleted, if it's not yet been linked.
		 */
		val TTL_UNLINKED = 1.hours
	}
}
