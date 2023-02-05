package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

class FakeTemplates(
	private val clock: Clock,
) : Template.Service {

	private val lock = Semaphore(1)
	private val templates = HashMap<String, Template>()
	private val _versions = FakeVersions()

	override val versions: Template.Version.Service
		get() = _versions

	override val cache: RefCache<Template> = defaultRefCache()

	private fun toRef(id: String) = Template.Ref(id, this)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Template.Ref>> = out {
		ensureEmployee()

		lock.withPermit {
			templates
				.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.map { (it, _) -> toRef(it) }
				.toList()
		}
	}

	override suspend fun create(name: String, firstVersion: Template.Version): Outcome<Template.Ref> = out {
		ensureAdministrator()

		firstVersion.field.validate().bind()

		val now = clock.now()
		val id = newId()

		lock.withPermit {
			_versions.versions[id to now] = firstVersion.copy(creationDate = now)

			templates[id] = Template(
				name,
				listOf(_versions.toRef(id, now)),
				open = true,
			)
		}

		toRef(id)
	}

	override suspend fun createVersion(
		template: Template.Ref,
		version: Template.Version,
	): Outcome<Template.Version.Ref> = out {
		ensureAdministrator()

		version.field.validate().bind()

		val now = clock.now()
		val ref = _versions.toRef(template.id, now)

		lock.withPermit {
			val value = templates[template.id]
			ensureFound(value != null) { "Couldn't find template $template" }

			_versions.versions[template.id to now] = version.copy(creationDate = now)
			templates[template.id] = value.copy(versions = value.versions + ref)
		}

		ref
	}

	override suspend fun edit(template: Template.Ref, name: String?, open: Boolean?): Outcome<Unit> = out {
		ensureAdministrator()

		lock.withPermit {
			val value = templates[template.id]
			ensureFound(value != null) { "Couldn't find template $template" }

			val new = value.copy(
				name = name ?: value.name,
				open = open ?: value.open,
			)

			templates[template.id] = new
		}
	}

	override suspend fun directRequest(ref: Ref<Template>): Outcome<Template> = out {
		ensureEmployee()
		ensureValid(ref is Template.Ref) { "Invalid ref $ref" }

		lock.withPermit {
			val result = templates[ref.id]
			ensureFound(result != null) { "Could not find template $ref" }
			result
		}
	}

	private inner class FakeVersions : Template.Version.Service {
		override val cache: RefCache<Template.Version> = defaultRefCache()

		val versions = HashMap<Pair<String, Instant>, Template.Version>()

		fun toRef(id: String, version: Instant) = Template.Version.Ref(toRef(id), version, this)

		override suspend fun directRequest(ref: Ref<Template.Version>): Outcome<Template.Version> = out {
			ensureEmployee()
			ensureValid(ref is Template.Version.Ref) { "Invalid ref $ref" }

			lock.withPermit {
				val result = versions[ref.template.id to ref.version]
				ensureFound(result != null) { "Could not find template version $ref" }
				result
			}
		}

	}
}
