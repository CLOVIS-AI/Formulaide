package opensavvy.formulaide.api.client

import kotlinx.coroutines.flow.emitAll
import kotlinx.datetime.toInstant
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.AbstractTemplateVersions
import opensavvy.formulaide.core.AbstractTemplates
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.state.bind
import opensavvy.formulaide.state.flatMapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.mapSuccess
import opensavvy.state.state
import opensavvy.formulaide.api.Template as ApiTemplate

class Templates(
	private val client: Client,
	override val cache: RefCache<Template>,
) : AbstractTemplates {
	override fun list(includeClosed: Boolean): State<List<Template.Ref>> = state {
		val params = ApiTemplate.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val result = client.http
			.request(api2.templates.get, api2.templates.idOf(), Unit, params, client.context.value)
			.flatMapSuccess { list ->
				val result =
					list.map { Template.Ref(bind(api2.templates.id.idFrom(it, client.context.value)), this@Templates) }
				emit(successful(result))
			}

		emitAll(result)
	}

	override fun create(name: String, firstVersion: Template.Version): State<Template.Ref> = state {
		val body = ApiTemplate.New(
			name,
			ApiTemplate.Version(
				firstVersion.creationDate,
				firstVersion.title,
				firstVersion.field.toApi()
			)
		)

		val result = client.http
			.request(api2.templates.create, api2.templates.idOf(), body, Parameters.Empty, client.context.value)
			.flatMapSuccess { (id, _) ->
				val template = bind(api2.templates.id.idFrom(id, client.context.value))
				emit(successful(Template.Ref(template, this@Templates)))
			}

		emitAll(result)
	}

	override fun createVersion(template: Template.Ref, version: Template.Version): State<Template.Version.Ref> = state {
		val new = ApiTemplate.Version(
			version.creationDate,
			version.title,
			version.field.toApi()
		)

		val result = client.http
			.request(
				api2.templates.id.create,
				api2.templates.id.idOf(template.id),
				new,
				Parameters.Empty,
				client.context.value
			)
			.flatMapSuccess { (id, _) ->
				val (templateId, versionId) = bind(api2.templates.id.version.idFrom(id, client.context.value))
				emit(
					successful(
						Template.Version.Ref(
							Template.Ref(templateId, this@Templates),
							versionId.toInstant(),
							client.templateVersions
						)
					)
				)
			}
			.onEachSuccess { cache.expire(template) }

		emitAll(result)
	}

	override fun edit(template: Template.Ref, name: String?, open: Boolean?): State<Unit> = state {
		val body = ApiTemplate.Edit(
			name,
			open,
		)

		val result = client.http
			.request(
				api2.templates.id.edit,
				api2.templates.id.idOf(template.id),
				body,
				Parameters.Empty,
				client.context.value
			)
			.onEachSuccess { cache.expire(template) }

		emitAll(result)
	}

	override fun directRequest(ref: Ref<Template>): State<Template> = state {
		ensureValid(ref is Template.Ref) { "${this@Templates} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.templates.id.get,
				api2.templates.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context.value
			)
			.flatMapSuccess { template ->
				val result = Template(
					template.name,
					template.versions.map {
						val (templateId, versionId) = bind(
							api2.templates.id.version.idFrom(it, client.context.value)
						)
						Template.Version.Ref(
							Template.Ref(templateId, this@Templates),
							versionId.toInstant(),
							client.templateVersions
						)
					},
					template.open,
				)
				emit(successful(result))
			}

		emitAll(result)
	}
}

class TemplateVersions(
	private val client: Client,
	override val cache: RefCache<Template.Version>,
) : AbstractTemplateVersions {
	override fun directRequest(ref: Ref<Template.Version>): State<Template.Version> = state {
		ensureValid(ref is Template.Version.Ref) { "${this@TemplateVersions} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.templates.id.version.get,
				api2.templates.id.version.idOf(ref.template.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				client.context.value
			)
			.mapSuccess {
				Template.Version(
					it.creationDate,
					it.title,
					it.field.toCore(client.templates, client.templateVersions)
				)
			}

		emitAll(result)
	}
}
