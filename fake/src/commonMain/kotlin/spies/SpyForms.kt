package opensavvy.formulaide.fake.spies

import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.loggerFor
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map
import opensavvy.state.progressive.map

class SpyForms(private val upstream: Form.Service) : Form.Service {

	private val log = loggerFor(upstream)

	private val _versions = SpyVersion(upstream.versions)
	override val versions: Form.Version.Service get() = _versions

	override suspend fun list(includeClosed: Boolean): Outcome<Form.Failures.List, List<Ref>> = spy(
		log, "list", includeClosed,
	) { upstream.list(includeClosed) }
		.map { it.map(::Ref) }

	override suspend fun create(name: String, firstVersionTitle: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.Create, Ref> = spy(
		log, "create", name, firstVersionTitle, field, *step,
	) { upstream.create(name, firstVersionTitle, field, *step) }
		.map(::Ref)

	override fun fromIdentifier(identifier: Identifier) = upstream.fromIdentifier(identifier).let(::Ref)

	inner class Ref internal constructor(
		private val upstream: Form.Ref,
	) : Form.Ref {
		override suspend fun edit(name: String?, open: Boolean?, public: Boolean?): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "edit", name, open, public,
		) { upstream.edit(name, open, public) }

		override suspend fun rename(name: String): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "name", name,
		) { upstream.rename(name) }

		override suspend fun open(): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "open",
		) { upstream.open() }

		override suspend fun close(): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "close",
		) { upstream.close() }

		override suspend fun publicize(): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "publicize",
		) { upstream.publicize() }

		override suspend fun privatize(): Outcome<Form.Failures.Edit, Unit> = spy(
			log, "privatize",
		) { upstream.privatize() }

		override suspend fun createVersion(title: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.CreateVersion, SpyVersion.Ref> = spy(
			log, "createVersion", title, field, *step,
		) { upstream.createVersion(title, field, *step) }
			.map(_versions::Ref)

		override fun versionOf(creationDate: Instant): Form.Version.Ref = _versions.Ref(upstream.versionOf(creationDate))

		override fun request(): ProgressiveFlow<Form.Failures.Get, Form> = spy(
			log, "request",
		) { upstream.request() }
			.map { out -> out.map { form -> form.copy(versions = form.versions.map(_versions::Ref)) } }

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return upstream == other.upstream
		}

		override fun hashCode(): Int {
			return upstream.hashCode()
		}

		override fun toString() = upstream.toString()
		override fun toIdentifier() = upstream.toIdentifier()

		// endregion
	}

	inner class SpyVersion(private val upstream: Form.Version.Service) : Form.Version.Service {
		private val log = loggerFor(upstream)

		inner class Ref internal constructor(
			private val upstream: Form.Version.Ref,
		) : Form.Version.Ref {
			override val creationDate: Instant get() = upstream.creationDate

			override val form: Form.Ref get() = Ref(upstream.form)

			override fun request(): ProgressiveFlow<Form.Version.Failures.Get, Form.Version> = spy(
				log, "request",
			) { upstream.request() }

			// region Overrides

			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (other !is Ref) return false

				return upstream == other.upstream
			}

			override fun hashCode(): Int {
				return upstream.hashCode()
			}

			override fun toString() = upstream.toString()
			override fun toIdentifier() = upstream.toIdentifier()

			// endregion
		}

		override fun fromIdentifier(identifier: Identifier) = upstream.fromIdentifier(identifier).let(::Ref)
	}

	companion object {
		fun Form.Service.spied() = SpyForms(this)
	}

}
