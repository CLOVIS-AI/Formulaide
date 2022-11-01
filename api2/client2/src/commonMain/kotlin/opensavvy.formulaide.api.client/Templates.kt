package opensavvy.formulaide.api.client

import kotlinx.datetime.toInstant
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.AbstractTemplateVersions
import opensavvy.formulaide.core.AbstractTemplates
import opensavvy.formulaide.core.Template
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice
import opensavvy.formulaide.api.Template as ApiTemplate

class Templates(
	private val client: Client,
	override val cache: RefCache<Template>,
) : AbstractTemplates {
	override suspend fun list(includeClosed: Boolean): Slice<List<Template.Ref>> = slice {
		val params = ApiTemplate.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val list = client.http
			.request(api2.templates.get, api2.templates.idOf(), Unit, params, client.context.value)
			.bind()

		list.map { Template.Ref(api2.templates.id.idFrom(it, client.context.value).bind(), this@Templates) }
	}

	override suspend fun create(name: String, firstVersion: Template.Version): Slice<Template.Ref> = slice {
		val body = ApiTemplate.New(
			name,
			ApiTemplate.Version(
				firstVersion.creationDate,
				firstVersion.title,
				firstVersion.field.toApi()
			)
		)

		val (id, _) = client.http
			.request(api2.templates.create, api2.templates.idOf(), body, Parameters.Empty, client.context.value)
			.bind()

		val template = api2.templates.id.idFrom(id, client.context.value).bind()
		Template.Ref(template, this@Templates)
	}

	override suspend fun createVersion(template: Template.Ref, version: Template.Version): Slice<Template.Version.Ref> =
		slice {
			val new = ApiTemplate.Version(
				version.creationDate,
				version.title,
				version.field.toApi()
			)

			val (id, _) = client.http
				.request(
					api2.templates.id.create,
					api2.templates.id.idOf(template.id),
					new,
					Parameters.Empty,
					client.context.value
				).bind()

			val (templateId, versionId) = api2.templates.id.version.idFrom(id, client.context.value).bind()
			val result = Template.Version.Ref(
				Template.Ref(templateId, this@Templates),
				versionId.toInstant(),
				client.templateVersions
			)

			cache.expire(template)

			result
		}

	override suspend fun edit(template: Template.Ref, name: String?, open: Boolean?): Slice<Unit> = slice {
		val body = ApiTemplate.Edit(
			name,
			open,
		)

		client.http
			.request(
				api2.templates.id.edit,
				api2.templates.id.idOf(template.id),
				body,
				Parameters.Empty,
				client.context.value
			).bind()

		cache.expire(template)
	}

	override suspend fun directRequest(ref: Ref<Template>): Slice<Template> = slice {
		ensureValid(ref is Template.Ref) { "${this@Templates} n'accepte pas la référence $ref" }

		val template = client.http
			.request(
				api2.templates.id.get,
				api2.templates.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context.value
			).bind()

		Template(
			template.name,
			template.versions.map {
				val (templateId, versionId) = api2.templates.id.version.idFrom(it, client.context.value).bind()
				Template.Version.Ref(
					Template.Ref(templateId, this@Templates),
					versionId.toInstant(),
					client.templateVersions
				)
			},
			template.open,
		)
	}
}

class TemplateVersions(
	private val client: Client,
	override val cache: RefCache<Template.Version>,
) : AbstractTemplateVersions {
	override suspend fun directRequest(ref: Ref<Template.Version>): Slice<Template.Version> = slice {
		ensureValid(ref is Template.Version.Ref) { "${this@TemplateVersions} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.templates.id.version.get,
				api2.templates.id.version.idOf(ref.template.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				client.context.value
			).bind()

		Template.Version(
			result.creationDate,
			result.title,
			result.field.toCore(client.templates, client.templateVersions)
		)
	}
}
