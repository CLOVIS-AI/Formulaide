package opensavvy.formulaide.fake.spies

import kotlinx.coroutines.flow.map
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.loggerFor
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map
import opensavvy.state.progressive.map

class SpyTemplates(private val upstream: Template.Service) : Template.Service {

	private val log = loggerFor(upstream)

	private val _versions = SpyVersion(upstream.versions)
	override val versions: Template.Version.Service = _versions

	override suspend fun list(includeClosed: Boolean): Outcome<Template.Failures.List, List<Ref>> = spy(
		log, "list", includeClosed,
	) { upstream.list(includeClosed) }
		.map { it.map(::Ref) }

	override suspend fun create(name: String, initialVersionTitle: String, field: Field): Outcome<Template.Failures.Create, Ref> = spy(
		log, "create", name, initialVersionTitle, field,
	) { upstream.create(name, initialVersionTitle, field) }
		.map(::Ref)

	override fun fromIdentifier(identifier: Identifier) = upstream.fromIdentifier(identifier).let(::Ref)

	inner class Ref internal constructor(
		private val upstream: Template.Ref,
	) : Template.Ref {
		override suspend fun rename(name: String): Outcome<Template.Failures.Edit, Unit> = spy(
			log, "rename", name,
		) { upstream.rename(name) }

		override suspend fun open(): Outcome<Template.Failures.Edit, Unit> = spy(
			log, "open",
		) { upstream.open() }

		override suspend fun close(): Outcome<Template.Failures.Edit, Unit> = spy(
			log, "close",
		) { upstream.close() }

		override suspend fun createVersion(title: String, field: Field): Outcome<Template.Failures.CreateVersion, SpyVersion.Ref> = spy(
			log, "createVersion", title, field,
		) { upstream.createVersion(title, field) }
			.map(_versions::Ref)

		override fun request(): ProgressiveFlow<Template.Failures.Get, Template> = spy(
			log, "request",
		) { upstream.request() }
			.map { out -> out.map { template -> template.copy(versions = template.versions.map(_versions::Ref)) } }

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

	inner class SpyVersion(private val upstream: Template.Version.Service) : Template.Version.Service {
		private val log = loggerFor(upstream)

		inner class Ref internal constructor(
			private val upstream: Template.Version.Ref,
		) : Template.Version.Ref {
			override val template: Template.Ref
				get() = Ref(upstream.template)

			override fun request(): ProgressiveFlow<Template.Version.Failures.Get, Template.Version> = spy(
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

		fun Template.Service.spied() = SpyTemplates(this)

	}
}
