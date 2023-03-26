package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

class FakeForms(
	private val clock: Clock,
) : Form.Service {

	private val lock = Semaphore(1)
	private val forms = HashMap<String, Form>()
	private val _versions = FakeVersions()

	override val versions: Form.Version.Service
		get() = _versions

	private fun toRef(id: String) = Form.Ref(id, this)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Form.Ref>> = out {
		if (includeClosed)
			ensureEmployee()

		val role = currentRole()

		lock.withPermit {
			forms
				.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.filter { (_, it) -> it.public || role >= User.Role.Employee }
				.map { (it, _) -> toRef(it) }
				.toList()
		}
	}

	override suspend fun create(name: String, firstVersion: Form.Version): Outcome<Form.Ref> = out {
		ensureAdministrator()

		firstVersion.fields.forEach { it.validate().bind() }

		val now = clock.now()
		val id = newId()

		lock.withPermit {
			_versions.versions[id to now] = firstVersion.copy(creationDate = now)

			forms[id] = Form(
				name,
				listOf(_versions.toRef(id, now)),
				open = true,
				public = false,
			)
		}

		toRef(id)
	}

	override suspend fun createVersion(form: Form.Ref, version: Form.Version): Outcome<Form.Version.Ref> = out {
		ensureAdministrator()

		version.fields.forEach { it.validate().bind() }

		val now = clock.now()
		val ref = _versions.toRef(form.id, now)

		lock.withPermit {
			val value = forms[form.id]
			ensureFound(value != null) { "Couldn't find form $form" }

			_versions.versions[form.id to now] = version.copy(creationDate = now)
			forms[form.id] = value.copy(versions = value.versions + ref)
		}

		form.expire()

		ref
	}

	override suspend fun edit(form: Form.Ref, name: String?, open: Boolean?, public: Boolean?): Outcome<Unit> = out {
		ensureAdministrator()

		lock.withPermit {
			val value = forms[form.id]
			ensureFound(value != null) { "Couldn't find form $form" }

			val new = value.copy(
				name = name ?: value.name,
				open = open ?: value.open,
				public = public ?: value.public,
			)

			forms[form.id] = new
		}

		form.expire()
	}

	override val cache: RefCache<Form> = defaultRefCache()

	override suspend fun directRequest(ref: Ref<Form>): Outcome<Form> = out {
		ensureValid(ref is Form.Ref) { "Invalid ref $ref" }

		lock.withPermit {
			val result = forms[ref.id]
			ensureFound(result != null) { "Could not find form $ref" }
			ensureFound(result.public || currentRole() >= User.Role.Employee) { "Could not find form $ref" }
			result
		}
	}

	private inner class FakeVersions : Form.Version.Service {
		override val cache: RefCache<Form.Version> = defaultRefCache()

		val versions = HashMap<Pair<String, Instant>, Form.Version>()

		fun toRef(id: String, version: Instant) = Form.Version.Ref(toRef(id), version, this)

		override suspend fun directRequest(ref: Ref<Form.Version>): Outcome<Form.Version> = out {
			ensureValid(ref is Form.Version.Ref) { "Invalid ref $ref" }

			lock.withPermit {
				val result = versions[ref.form.id to ref.version]
				ensureFound(result != null) { "Could not find form version $ref" }
				result
			}
		}
	}
}
