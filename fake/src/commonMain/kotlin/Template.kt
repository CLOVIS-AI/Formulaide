package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.cache.contextual.cache
import opensavvy.formulaide.core.*
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome

class FakeTemplates(
	private val clock: Clock,
) : Template.Service {

	private val lock = Semaphore(1)
	private val templates = HashMap<Long, Template>()
	private val _versions = FakeVersions()

	override val versions: Template.Version.Service
		get() = _versions

	private val cache = cache<Ref, User.Role, Template.Failures.Get, Template> { it, role ->
		out {
			ensure(role >= User.Role.Employee) { Template.Failures.Unauthenticated }

			lock.withPermit {
				val result = templates[it.id]
				ensureNotNull(result) { Template.Failures.NotFound(it) }
				result
			}
		}
	}

	override suspend fun list(includeClosed: Boolean): Outcome<Template.Failures.List, List<Template.Ref>> = out {
		ensureEmployee { Template.Failures.Unauthenticated }

		lock.withPermit {
			templates
				.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.map { (it, _) -> Ref(it) }
				.toList()
		}
	}

	override suspend fun create(name: String, initialVersionTitle: String, field: Field): Outcome<Template.Failures.Create, Template.Ref> = out {
		ensureEmployee { Template.Failures.Unauthenticated }
		ensureAdministrator { Template.Failures.Unauthorized }

		field.validate()
			.mapLeft { Template.Failures.InvalidImport(it) }
			.bind()

		val now = clock.now()

		val firstVersion = Template.Version(
			creationDate = now,
			title = initialVersionTitle,
			field = field,
		)

		val ref = Ref(newId())
		lock.withPermit {
			_versions.versions[ref.id to now] = firstVersion

			templates[ref.id] = Template(
				name,
				listOf(_versions.Ref(ref, now)),
				open = true,
			)
		}

		ref
	}

	inner class Ref internal constructor(
		val id: Long,
	) : Template.Ref {
		private suspend fun edit(name: String? = null, open: Boolean? = null): Outcome<Template.Failures.Edit, Unit> = out {
			ensureEmployee { Template.Failures.Unauthenticated }
			ensureAdministrator { Template.Failures.Unauthorized }

			lock.withPermit {
				val template = templates[id]
				ensureNotNull(template) { Template.Failures.NotFound(this@Ref) }

				val new = template.copy(
					name = name ?: template.name,
					open = open ?: template.open,
				)

				templates[id] = new
			}
		}

		override suspend fun rename(name: String): Outcome<Template.Failures.Edit, Unit> =
			edit(name = name)

		override suspend fun open(): Outcome<Template.Failures.Edit, Unit> =
			edit(open = true)

		override suspend fun close(): Outcome<Template.Failures.Edit, Unit> =
			edit(open = false)

		override suspend fun createVersion(title: String, field: Field): Outcome<Template.Failures.CreateVersion, Template.Version.Ref> = out {
			ensureEmployee { Template.Failures.Unauthenticated }
			ensureAdministrator { Template.Failures.Unauthorized }

			field.validate()
				.mapLeft { Template.Failures.InvalidImport(it) }
				.bind()

			val now = clock.now()

			val version = Template.Version(
				creationDate = now,
				title = title,
				field = field,
			)

			val ref = _versions.Ref(this@Ref, now)
			lock.withPermit {
				val template = templates[id]
				ensureNotNull(template) { Template.Failures.NotFound(this@Ref) }

				_versions.versions[id to now] = version
				templates[id] = template.copy(versions = template.versions + ref)
			}

			ref
		}

		override fun request(): ProgressiveFlow<Template.Failures.Get, Template> = flow {
			emitAll(cache[this@Ref, currentRole()])
		}

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		override fun toString() = "FakeTemplates.Ref($id)"

		// endregion
	}

	private inner class FakeVersions : Template.Version.Service {
		val versions = HashMap<Pair<Long, Instant>, Template.Version>()

		private val cache = cache { it: Ref, role: User.Role ->
			out<Template.Version.Failures.Get, Template.Version> {
				ensure(role >= User.Role.Employee) { Template.Version.Failures.Unauthenticated }

				lock.withPermit {
					val result = versions[it.template.id to it.creationDate]
					ensureNotNull(result) { Template.Version.Failures.NotFound(it) }
					result
				}
			}
		}

		inner class Ref internal constructor(
			override val template: FakeTemplates.Ref,
			val creationDate: Instant,
		) : Template.Version.Ref {
			override fun request(): ProgressiveFlow<Template.Version.Failures.Get, Template.Version> = flow {
				emitAll(cache[this@Ref, currentRole()])
			}
		}
	}
}
