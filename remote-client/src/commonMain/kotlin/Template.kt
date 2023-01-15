package opensavvy.formulaide.remote.client

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplate
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toTemplateVersion
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Templates(
	private val client: Client,
	cacheContext: CoroutineContext,
) : Template.Service {

	override val versions: Template.Version.Service = Versions(cacheContext)

	override val cache: RefCache<Template> = defaultRefCache<Template>()
		.cachedInMemory(cacheContext)
		.expireAfter(10.minutes, cacheContext)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Template.Ref>> = out {
		client.http.request(
			api.templates.get,
			api.templates.idOf(),
			Unit,
			SchemaDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).bind()
			.map { api.templates.id.refOf(it, this@Templates).bind() }
	}

	override suspend fun create(name: String, firstVersion: Template.Version): Outcome<Template.Ref> = out {
		client.http.request(
			api.templates.create,
			api.templates.idOf(),
			SchemaDto.New(name = name, firstVersion = firstVersion.toDto()),
			Parameters.Empty,
			Unit,
		).bind()
			.id
			.let { api.templates.id.refOf(it, this@Templates) }
			.bind()
	}

	override suspend fun createVersion(
		template: Template.Ref,
		version: Template.Version,
	): Outcome<Template.Version.Ref> = out {
		client.http.request(
			api.templates.id.create,
			api.templates.id.idOf(template.id),
			version.toDto(),
			Parameters.Empty,
			Unit,
		).bind()
			.id
			.let { api.templates.id.version.refOf(it, this@Templates) }
			.bind()
			.also { template.expire() }
	}

	override suspend fun edit(template: Template.Ref, name: String?, open: Boolean?): Outcome<Unit> = out {
		client.http.request(
			api.templates.id.edit,
			api.templates.id.idOf(template.id),
			SchemaDto.Edit(name = name, open = open),
			Parameters.Empty,
			Unit,
		).bind()

		template.expire()
	}

	override suspend fun directRequest(ref: Ref<Template>): Outcome<Template> = out {
		ensureValid(ref is Template.Ref) { "Expected Template.Ref, found $ref" }

		client.http.request(
			api.templates.id.get,
			api.templates.id.idOf(ref.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()
			.toTemplate(
				decodeTemplate = { id ->
					api.templates.id.version.refOf(id, this@Templates).bind()
				}
			)
	}

	private inner class Versions(
		cacheContext: CoroutineContext,
	) : Template.Version.Service {
		override val cache: RefCache<Template.Version> = defaultRefCache<Template.Version>()
			.cachedInMemory(cacheContext)
			.expireAfter(1.hours, cacheContext)

		override suspend fun directRequest(ref: Ref<Template.Version>): Outcome<Template.Version> = out {
			ensureValid(ref is Template.Version.Ref) { "Expected Template.Version.Ref, found $ref" }

			client.http.request(
				api.templates.id.version.get,
				api.templates.id.version.idOf(ref.template.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				Unit,
			).bind()
				.toTemplateVersion(
					decodeTemplate = { id ->
						api.templates.id.version.refOf(id, this@Templates).bind()
					}
				)
		}

	}
}
