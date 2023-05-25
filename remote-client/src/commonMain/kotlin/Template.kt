package opensavvy.formulaide.remote.client

import arrow.core.toNonEmptyListOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.currentRole
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplate
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplateVersion
import opensavvy.spine.Parameters
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.client.request
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.mapFailure
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RemoteTemplates(
	private val client: Client,
	scope: CoroutineScope,
) : Template.Service {

	private val _versions = Versions()
	override val versions: Template.Version.Service = _versions

	private val cache = cache<RemoteTemplates.Ref, User.Role, Template.Failures.Get, Template> { ref, role ->
		out {
			client.http.request(
				api.templates.id.get,
				api.templates.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Template.Failures.Unauthenticated
					SpineFailure.Type.NotFound -> Template.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.toTemplate { _versions.fromIdentifier(api.templates.id.version.identifierOf(it)) }
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(10.minutes, scope)

	private val versionCache = cache<RemoteTemplates.Versions.Ref, User.Role, Template.Version.Failures.Get, Template.Version> { ref, role ->
		out {
			client.http.request(
				api.templates.id.version.get,
				api.templates.id.version.idOf(ref.template.toIdentifier().text, ref.creationDate.toString()),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Template.Version.Failures.Unauthenticated
					SpineFailure.Type.NotFound -> Template.Version.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.toTemplateVersion(
					decodeTemplate = { id ->
						_versions.fromIdentifier(api.templates.id.version.identifierOf(id))
					},
				)
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(1.hours, scope)

	override suspend fun list(includeClosed: Boolean): Outcome<Template.Failures.List, List<Template.Ref>> = out {
		client.http.request(
			api.templates.get,
			api.templates.idOf(),
			Unit,
			SchemaDto.ListParams().apply { this.includeClosed = includeClosed },
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Template.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> Template.Failures.Unauthorized
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.map { fromIdentifier(api.templates.id.identifierOf(it)) }
	}

	override suspend fun create(name: String, initialVersionTitle: String, field: Field): Outcome<Template.Failures.Create, Template.Ref> = out {
		client.http.request(
			api.templates.create,
			api.templates.idOf(),
			SchemaDto.New(
				name = name,
				firstVersion = SchemaDto.NewVersion(
					title = initialVersionTitle,
					field = field.toDto(),
				)
			),
			Parameters.Empty,
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Template.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> Template.Failures.Unauthorized
				SpineFailure.Type.InvalidRequest -> {
					check(it is SpineFailure.Payload) { "Expected a JSON response from the server, but received ${it::class}: $it" }
					Template.Failures.InvalidImport((it.payload as SchemaDto.NewFailures.InvalidImport).failures.map { it.toCore(this@RemoteTemplates) }.toNonEmptyListOrNull()!!)
				}
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.id
			.let { fromIdentifier(api.templates.id.identifierOf(it)) }
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

	inner class Ref internal constructor(
		internal val id: String,
	) : Template.Ref {
		override suspend fun edit(name: String?, open: Boolean?): Outcome<Template.Failures.Edit, Unit> = out {
			client.http.request(
				api.templates.id.edit,
				api.templates.id.idOf(id),
				SchemaDto.Edit(name = name, open = open),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Template.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Template.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Template.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		override suspend fun createVersion(title: String, field: Field): Outcome<Template.Failures.CreateVersion, Template.Version.Ref> = out {
			client.http.request(
				api.templates.id.create,
				api.templates.id.idOf(id),
				SchemaDto.NewVersion(
					title = title,
					field = field.toDto(),
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Template.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Template.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Template.Failures.NotFound(this@Ref)
					SpineFailure.Type.InvalidRequest -> {
						check(it is SpineFailure.Payload) { "Expected a JSON response from the server, but received ${it::class}: $it" }
						Template.Failures.InvalidImport((it.payload as SchemaDto.NewFailures.InvalidImport).failures.map { it.toCore(this@RemoteTemplates) }.toNonEmptyListOrNull()!!)
					}
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.id
				.let { _versions.fromIdentifier(api.templates.id.version.identifierOf(it)) }
				.also { cache.expire(this@Ref) }
		}

		override fun versionOf(creationDate: Instant): Template.Version.Ref = _versions.Ref(this, creationDate)

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

		override fun toString() = "RemoteTemplates.Ref($id)"

		override fun toIdentifier() = Identifier(id)

		// endregion
	}

	inner class Versions : Template.Version.Service {

		override fun fromIdentifier(identifier: Identifier): Template.Version.Ref {
			val (template, creationDate) = identifier.text.split("_", limit = 2)
			return Ref(
				this@RemoteTemplates.fromIdentifier(Identifier(template)),
				creationDate.toInstant(),
			)
		}

		inner class Ref internal constructor(
			override val template: RemoteTemplates.Ref,
			override val creationDate: Instant,
		) : Template.Version.Ref {
			override fun request(): ProgressiveFlow<Template.Version.Failures.Get, Template.Version> = flow {
				emitAll(versionCache[this@Ref, currentRole()])
			}

			// region Overrides

			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (other !is Ref) return false

				if (template != other.template) return false
				return creationDate == other.creationDate
			}

			override fun hashCode(): Int {
				var result = template.hashCode()
				result = 31 * result + creationDate.hashCode()
				return result
			}

			override fun toString() = "RemoteTemplates.Ref(${template.id}).Version($creationDate)"

			override fun toIdentifier() = Identifier("${template.id}_$creationDate")

			// endregion
		}
	}
}
