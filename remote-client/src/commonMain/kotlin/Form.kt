package opensavvy.formulaide.remote.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toForm
import opensavvy.spine.Parameters
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.client.request
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.mapFailure
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RemoteForms(
	private val client: Client,
	private val departments: Department.Service<*>,
	private val templates: Template.Service,
	scope: CoroutineScope,
) : Form.Service {

	private val _versions = Versions()
	override val versions: Form.Version.Service = _versions

	private val cache = cache<Ref, User.Role, Form.Failures.Get, Form> { ref, role ->
		out {
			client.http.request(
				api.forms.id.get,
				api.forms.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Form.Failures.Unauthenticated
					SpineFailure.Type.NotFound -> Form.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.toForm { _versions.fromIdentifier(api.forms.id.version.identifierOf(it)) }
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(10.minutes, scope)

	private val versionCache = cache<Versions.Ref, User.Role, Form.Version.Failures.Get, Form.Version> { ref, role ->
		out {
			client.http.request(
				api.forms.id.version.get,
				api.forms.id.version.idOf(ref.form.id, ref.creationDate.toString()),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Form.Version.Failures.Unauthenticated
					SpineFailure.Type.NotFound -> Form.Version.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.toForm(
					decodeTemplate = { id ->
						templates.versions.fromIdentifier(api.templates.id.version.identifierOf(id))
					},
					decodeDepartment = { id ->
						departments.fromIdentifier(api.departments.id.identifierOf(id))
					}
				)
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(1.hours, scope)

	override suspend fun list(includeClosed: Boolean): Outcome<Form.Failures.List, List<Form.Ref>> = out {
		client.http.request(
			api.forms.get,
			api.forms.idOf(),
			Unit,
			SchemaDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Form.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> Form.Failures.Unauthorized
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.map { fromIdentifier(api.forms.id.identifierOf(it)) }
	}

	override suspend fun create(name: String, firstVersionTitle: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.Create, Form.Ref> = out {
		client.http.request(
			api.forms.create,
			api.forms.idOf(),
			SchemaDto.New(
				name = name,
				firstVersion = SchemaDto.NewVersion(
					title = firstVersionTitle,
					field = field.toDto(),
					steps = step.map { it.toDto() },
				)
			),
			Parameters.Empty,
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Form.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> Form.Failures.Unauthorized
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.id
			.let { fromIdentifier(api.forms.id.identifierOf(it)) }
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

	inner class Ref internal constructor(
		internal val id: String,
	) : Form.Ref {
		private suspend fun edit(name: String? = null, open: Boolean? = null, public: Boolean? = null): Outcome<Form.Failures.Edit, Unit> = out {
			client.http.request(
				api.forms.id.edit,
				api.forms.id.idOf(id),
				SchemaDto.Edit(name = name, open = open, public = public),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Form.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Form.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Form.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		override suspend fun rename(name: String): Outcome<Form.Failures.Edit, Unit> = edit(name = name)

		override suspend fun open(): Outcome<Form.Failures.Edit, Unit> = edit(open = true)

		override suspend fun close(): Outcome<Form.Failures.Edit, Unit> = edit(open = false)

		override suspend fun publicize(): Outcome<Form.Failures.Edit, Unit> = edit(public = true)

		override suspend fun privatize(): Outcome<Form.Failures.Edit, Unit> = edit(public = false)

		override suspend fun createVersion(title: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.CreateVersion, Form.Version.Ref> = out {
			client.http.request(
				api.forms.id.create,
				api.forms.id.idOf(id),
				SchemaDto.NewVersion(
					title = title,
					field = field.toDto(),
					steps = step.map { it.toDto() },
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Form.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Form.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Form.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.id
				.let { _versions.fromIdentifier(api.forms.id.version.identifierOf(it)) }
				.also { cache.expire(this@Ref) }
		}

		override fun versionOf(creationDate: Instant) = _versions.Ref(this, creationDate)

		override fun request(): ProgressiveFlow<Form.Failures.Get, Form> = flow {
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

		override fun toString() = "RemoteForms.Ref($id)"

		override fun toIdentifier() = Identifier(id)

		// endregion
	}

	inner class Versions : Form.Version.Service {

		override fun fromIdentifier(identifier: Identifier): Form.Version.Ref {
			val (form, creationDate) = identifier.text.split("_", limit = 2)
			return Ref(
				this@RemoteForms.fromIdentifier(Identifier(form)),
				creationDate.toInstant(),
			)
		}

		inner class Ref internal constructor(
			override val form: RemoteForms.Ref,
			override val creationDate: Instant,
		) : Form.Version.Ref {
			override fun request(): ProgressiveFlow<Form.Version.Failures.Get, Form.Version> = flow {
				emitAll(versionCache[this@Ref, currentRole()])
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

			override fun toString() = "RemoteForms.Ref(${form.id}).Version($creationDate)"

			override fun toIdentifier() = Identifier("${form.id}_$creationDate")
		}
	}
}
