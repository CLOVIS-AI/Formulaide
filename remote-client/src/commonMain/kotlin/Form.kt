package opensavvy.formulaide.remote.client

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toForm
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Forms(
	private val client: Client,
	private val departments: Department.Service,
	private val templates: Template.Service,
	cacheContext: CoroutineContext,
) : Form.Service {

	override val versions: Form.Version.Service = Versions(cacheContext)

	override val cache: RefCache<Form> = defaultRefCache<Form>()
		.cachedInMemory(cacheContext)
		.expireAfter(10.minutes, cacheContext)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Form.Ref>> = out {
		client.http.request(
			api.forms.get,
			api.forms.idOf(),
			Unit,
			SchemaDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).bind()
			.map { api.forms.id.refOf(it, this@Forms).bind() }
	}

	override suspend fun create(name: String, firstVersion: Form.Version): Outcome<Form.Ref> = out {
		client.http.request(
			api.forms.create,
			api.forms.idOf(),
			SchemaDto.New(name = name, firstVersion = firstVersion.toDto()),
			Parameters.Empty,
			Unit,
		).bind()
			.id
			.let { api.forms.id.refOf(it, this@Forms) }
			.bind()
	}

	override suspend fun createVersion(
		form: Form.Ref,
		version: Form.Version,
	): Outcome<Form.Version.Ref> = out {
		client.http.request(
			api.forms.id.create,
			api.forms.id.idOf(form.id),
			version.toDto(),
			Parameters.Empty,
			Unit,
		).bind()
			.id
			.let { api.forms.id.version.refOf(it, this@Forms) }
			.bind()
			.also { form.expire() }
	}

	override suspend fun edit(form: Form.Ref, name: String?, open: Boolean?, public: Boolean?): Outcome<Unit> = out {
		client.http.request(
			api.forms.id.edit,
			api.forms.id.idOf(form.id),
			SchemaDto.Edit(name = name, open = open, public = public),
			Parameters.Empty,
			Unit,
		).bind()

		form.expire()
	}

	override suspend fun directRequest(ref: Ref<Form>): Outcome<Form> = out {
		ensureValid(ref is Form.Ref) { "Expected Form.Ref, found $ref" }

		client.http.request(
			api.forms.id.get,
			api.forms.id.idOf(ref.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()
			.toForm(
				decodeForm = { id ->
					api.forms.id.version.refOf(id, this@Forms).bind()
				},
			)
	}

	private inner class Versions(
		cacheContext: CoroutineContext,
	) : Form.Version.Service {
		override val cache: RefCache<Form.Version> = defaultRefCache<Form.Version>()
			.cachedInMemory(cacheContext)
			.expireAfter(1.hours, cacheContext)

		override suspend fun directRequest(ref: Ref<Form.Version>): Outcome<Form.Version> = out {
			ensureValid(ref is Form.Version.Ref) { "Expected Form.Version.Ref, found $ref" }

			client.http.request(
				api.forms.id.version.get,
				api.forms.id.version.idOf(ref.form.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				Unit,
			).bind()
				.toForm(
					decodeTemplate = { id ->
						api.templates.id.version.refOf(id, templates).bind()
					},
					decodeDepartment = { id ->
						api.departments.id.refOf(id, departments).bind()
					}
				)
		}

	}
}
