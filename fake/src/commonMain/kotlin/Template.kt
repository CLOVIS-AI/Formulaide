package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.withProgress

class FakeTemplates(
	private val clock: Clock,
) : Template.Service {

	private val lock = Mutex()
	private val templates = HashMap<Long, Template>()
	private val _versions = FakeVersions()

	override val versions: Template.Version.Service
		get() = _versions

	override suspend fun list(includeClosed: Boolean): Outcome<Template.Failures.List, List<Template.Ref>> = out {
		ensureEmployee { Template.Failures.Unauthenticated }

		lock.withLock("list") {
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
		lock.withLock("create") {
			_versions.versions[ref.id to now] = firstVersion

			templates[ref.id] = Template(
				name,
				listOf(_versions.Ref(ref, now)),
				open = true,
			)
		}

		ref
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text.toLong())

	inner class Ref internal constructor(
		val id: Long,
	) : Template.Ref {
		override suspend fun edit(name: String?, open: Boolean?): Outcome<Template.Failures.Edit, Unit> = out {
			ensureEmployee { Template.Failures.Unauthenticated }
			ensureAdministrator { Template.Failures.Unauthorized }

			lock.withLock("edit") {
				val template = templates[id]
				ensureNotNull(template) { Template.Failures.NotFound(this@Ref) }

				val new = template.copy(
					name = name ?: template.name,
					open = open ?: template.open,
				)

				templates[id] = new
			}
		}

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
			lock.withLock("createVersion") {
				val template = templates[id]
				ensureNotNull(template) { Template.Failures.NotFound(this@Ref) }

				_versions.versions[id to now] = version
				templates[id] = template.copy(versions = template.versions + ref)
			}

			ref
		}

		override fun versionOf(creationDate: Instant): Template.Version.Ref = _versions.Ref(this, creationDate)

		override fun request(): ProgressiveFlow<Template.Failures.Get, Template> = flow {
			out {
				ensure(currentRole() >= User.Role.Employee) { Template.Failures.Unauthenticated }

				lock.withLock("request") {
					val result = templates[id]
					ensureNotNull(result) { Template.Failures.NotFound(this@Ref) }
					result
				}
			}.also { emit(it.withProgress()) }
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
		override fun toIdentifier() = Identifier(id.toString())

		// endregion
	}

	private inner class FakeVersions : Template.Version.Service {
		val versions = HashMap<Pair<Long, Instant>, Template.Version>()

		inner class Ref internal constructor(
			override val template: FakeTemplates.Ref,
			override val creationDate: Instant,
		) : Template.Version.Ref {
			override fun request(): ProgressiveFlow<Template.Version.Failures.Get, Template.Version> = flow {
				out {
					ensure(currentRole() >= User.Role.Employee) { Template.Version.Failures.Unauthenticated }

					lock.withLock("version:request") {
						val result = versions[template.id to creationDate]
						ensureNotNull(result) { Template.Version.Failures.NotFound(this@Ref) }
						result
					}
				}.also { emit(it.withProgress()) }
			}

			override fun toIdentifier() = Identifier("${template.id}_$creationDate")
		}

		override fun fromIdentifier(identifier: Identifier): FakeTemplates.FakeVersions.Ref {
			val (form, version) = identifier.text.split('_', limit = 2)

			return Ref(
				this@FakeTemplates.fromIdentifier(Identifier(form)),
				Instant.parse(version),
			)
		}
	}
}
