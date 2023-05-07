package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import opensavvy.formulaide.core.*
import opensavvy.formulaide.fake.utils.newId
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.withProgress

class FakeForms(
	private val clock: Clock,
) : Form.Service {

	private val log = loggerFor(this)

	private val lock = Mutex()
	private val forms = HashMap<Long, Form>()
	private val _versions = FakeVersions()

	override val versions: Form.Version.Service
		get() = _versions

	override suspend fun list(includeClosed: Boolean): Outcome<Form.Failures.List, List<Form.Ref>> = out {
		if (includeClosed)
			ensureEmployee { Form.Failures.Unauthenticated }

		val role = currentRole()

		lock.withLock("list") {
			forms
				.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.filter { (_, it) -> it.public || role >= User.Role.Employee }
				.map { (it, _) -> Ref(it) }
				.toList()
		}
	}

	override suspend fun create(name: String, firstVersionTitle: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.Create, Form.Ref> = out {
		ensureEmployee { Form.Failures.Unauthenticated }
		ensureAdministrator { Form.Failures.Unauthorized }

		field.validate()
			.mapLeft { Form.Failures.InvalidImport(it) }
			.bind()

		val now = clock.now()

		val version = Form.Version(
			creationDate = now,
			title = firstVersionTitle,
			field = field,
			steps = step.asList(),
		)

		val ref = Ref(newId())

		lock.withLock("create") {
			_versions.versions[ref.id to now] = version

			forms[ref.id] = Form(
				name,
				listOf(_versions.Ref(ref, now)),
				open = true,
				public = false,
			)
		}

		ref
	}

	inner class Ref internal constructor(
		val id: Long,
	) : Form.Ref {
		private suspend fun edit(name: String? = null, open: Boolean? = null, public: Boolean? = null): Outcome<Form.Failures.Edit, Unit> = out {
			ensureEmployee { Form.Failures.Unauthenticated }
			ensureAdministrator { Form.Failures.Unauthorized }

			lock.withLock("edit") {
				val value = forms[id]
				ensureNotNull(value) { Form.Failures.NotFound(this@Ref) }

				val new = value.copy(
					name = name ?: value.name,
					open = open ?: value.open,
					public = public ?: value.public,
				)

				forms[id] = new
			}
		}

		override suspend fun rename(name: String): Outcome<Form.Failures.Edit, Unit> =
			edit(name = name)

		override suspend fun open(): Outcome<Form.Failures.Edit, Unit> =
			edit(open = true)

		override suspend fun close(): Outcome<Form.Failures.Edit, Unit> =
			edit(open = false)

		override suspend fun publicize(): Outcome<Form.Failures.Edit, Unit> =
			edit(public = true)

		override suspend fun privatize(): Outcome<Form.Failures.Edit, Unit> =
			edit(public = false)

		override suspend fun createVersion(
			title: String,
			field: Field,
			vararg step: Form.Step,
		): Outcome<Form.Failures.CreateVersion, Form.Version.Ref> = out {
			ensureEmployee { Form.Failures.Unauthenticated }
			ensureAdministrator { Form.Failures.Unauthorized }

			field.validate()
				.mapLeft { Form.Failures.InvalidImport(it) }
				.bind()

			val now = clock.now()
			val ref = _versions.Ref(this@Ref, now)

			val version = Form.Version(
				creationDate = now,
				title = title,
				field = field,
				steps = step.asList(),
			)

			lock.withLock("createVersion") {
				val value = forms[id]
				ensureNotNull(value) { Form.Failures.NotFound(this@Ref) }

				_versions.versions[id to now] = version
				forms[id] = value.copy(versions = value.versions + ref)
			}

			ref
		}

		override fun request(): ProgressiveFlow<Form.Failures.Get, Form> = flow {
			out<Form.Failures.Get, Form> {
				lock.withLock("request") {
					val result = forms[id]
					ensureNotNull(result) { Form.Failures.NotFound(this@Ref) }
					ensure((result.public && result.open) || currentRole() >= User.Role.Employee) {
						log.warn { "${currentUser()} (${currentRole()}) requested to access ${this@Ref}, but it is private" }
						Form.Failures.NotFound(this@Ref)
					}
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

		override fun toString() = "FakeForms.Ref($id)"

		// endregion
	}

	private inner class FakeVersions : Form.Version.Service {
		val versions = HashMap<Pair<Long, Instant>, Form.Version>()

		inner class Ref internal constructor(
			override val form: FakeForms.Ref,
			override val creationDate: Instant,
		) : Form.Version.Ref {
			override fun request(): ProgressiveFlow<Form.Version.Failures.Get, Form.Version> = flow {
				out<Form.Version.Failures.Get, Form.Version> {
					lock.withLock("version:request") {
						val result = versions[form.id to creationDate]
						ensureNotNull(result) { Form.Version.Failures.NotFound(this@Ref) }
						result
					}
				}.also { emit(it.withProgress()) }
			}

			// region Overrides

			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (other !is Ref) return false

				if (form != other.form) return false
				return creationDate == other.creationDate
			}

			override fun hashCode(): Int {
				var result = form.hashCode()
				result = 31 * result + creationDate.hashCode()
				return result
			}

			override fun toString() = "FakeForms.Ref(${form.id}).Version($creationDate)"

			override fun toIdentifier() = Identifier("${form.id}_$creationDate")

			// endregion

		}

		override fun fromIdentifier(identifier: Identifier): Ref {
			val (form, version) = identifier.text.split('_', limit = 2)

			return Ref(
				this@FakeForms.fromIdentifier(Identifier(form)),
				Instant.parse(version),
			)
		}
	}
}
