package opensavvy.formulaide.api.client

import kotlinx.datetime.toInstant
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.api.toApi
import opensavvy.formulaide.api.toCore
import opensavvy.formulaide.core.AbstractFormVersions
import opensavvy.formulaide.core.AbstractForms
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice
import opensavvy.formulaide.api.Form as ApiForm

class Forms(
	private val client: Client,
	override val cache: RefCache<Form>,
) : AbstractForms {
	override suspend fun list(includeClosed: Boolean, includePrivate: Boolean): Slice<List<Form.Ref>> = slice {
		val params = ApiForm.GetParams().apply {
			this.includeClosed = includeClosed
			this.includePrivate = includePrivate
		}

		client.http
			.request(api2.forms.get, api2.forms.idOf(), Unit, params, client.context.value)
			.bind()
			.map { Form.Ref(api2.forms.id.idFrom(it, client.context.value).bind(), this@Forms) }
	}

	override suspend fun create(name: String, public: Boolean, firstVersion: Form.Version): Slice<Form.Ref> = slice {
		val body = ApiForm.New(
			name,
			public,
			firstVersion.toApi()
		)

		val (id, _) = client.http
			.request(api2.forms.create, api2.forms.idOf(), body, Parameters.Empty, client.context.value)
			.bind()

		val form = api2.forms.id.idFrom(id, client.context.value).bind()
		Form.Ref(form, this@Forms)
	}

	override suspend fun createVersion(form: Form.Ref, version: Form.Version): Slice<Form.Version.Ref> = slice {
		val new = version.toApi()

		val (id, _) = client.http
			.request(api2.forms.id.create, api2.forms.id.idOf(form.id), new, Parameters.Empty, client.context.value)
			.bind()

		val (_, versionId) = api2.forms.id.version.idFrom(id, client.context.value).bind()

		cache.expire(form)

		Form.Version.Ref(
			form,
			versionId.toInstant(),
			client.formVersions,
		)
	}

	override suspend fun edit(form: Form.Ref, name: String?, public: Boolean?, open: Boolean?): Slice<Unit> = slice {
		val body = ApiForm.Edit(
			name,
			public,
			open,
		)

		client.http
			.request(api2.forms.id.edit, api2.forms.id.idOf(form.id), body, Parameters.Empty, client.context.value)
			.bind()

		cache.expire(form)
	}

	override suspend fun directRequest(ref: Ref<Form>): Slice<Form> = slice {
		ensureValid(ref is Form.Ref) { "${this@Forms} n'accepte pas la référence $ref" }

		val form = client.http
			.request(api2.forms.id.get, api2.forms.id.idOf(ref.id), Unit, Parameters.Empty, client.context.value)
			.bind()

		Form(
			form.name,
			form.public,
			form.open,
			form.versions.map {
				val (_, versionId) = api2.forms.id.version.idFrom(it, client.context.value).bind()
				Form.Version.Ref(
					ref,
					versionId.toInstant(),
					client.formVersions,
				)
			}
		)
	}

}

class FormVersions(
	private val client: Client,
	override val cache: RefCache<Form.Version>,
) : AbstractFormVersions {
	override suspend fun directRequest(ref: Ref<Form.Version>): Slice<Form.Version> = slice {
		ensureValid(ref is Form.Version.Ref) { "${this@FormVersions} n'accepte pas la référence $ref" }

		val form = client.http
			.request(
				api2.forms.id.version.get,
				api2.forms.id.version.idOf(ref.form.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				client.context.value
			).bind()


		Form.Version(
			form.creationDate,
			form.title,
			form.field.toCore(client.templates, client.templateVersions),
			form.steps.map {
				Form.Step(
					it.id,
					Department.Ref(
						api2.departments.id.idFrom(it.department, client.context.value).bind(),
						client.departments
					),
					it.field?.toCore(client.templates, client.templateVersions)
				)
			}
		)
	}
}

private fun Form.Version.toApi() = ApiForm.Version(
	creationDate,
	title,
	field.toApi(),
	reviewSteps.map {
		ApiForm.Step(
			it.id,
			api2.departments.id.idOf(it.reviewer.id),
			it.field?.toApi()
		)
	}
)
