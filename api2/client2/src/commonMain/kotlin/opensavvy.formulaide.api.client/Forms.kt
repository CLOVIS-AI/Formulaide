package opensavvy.formulaide.api.client

import kotlinx.coroutines.flow.emitAll
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
import opensavvy.formulaide.state.bind
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.flatMapSuccess
import opensavvy.state.state
import opensavvy.formulaide.api.Form as ApiForm

class Forms(
	private val client: Client,
	override val cache: RefCache<Form>,
) : AbstractForms {
	override fun list(includeClosed: Boolean, includePrivate: Boolean): State<List<Form.Ref>> = state {
		val params = ApiForm.GetParams().apply {
			this.includeClosed = includeClosed
			this.includePrivate = includePrivate
		}

		val result = client.http
			.request(api2.forms.get, api2.forms.idOf(), Unit, params, client.context.value)
			.flatMapSuccess { list ->
				val result = list.map { Form.Ref(bind(api2.forms.id.idFrom(it, client.context.value)), this@Forms) }
				emit(successful(result))
			}

		emitAll(result)
	}

	override fun create(name: String, public: Boolean, firstVersion: Form.Version): State<Form.Ref> = state {
		val body = ApiForm.New(
			name,
			public,
			firstVersion.toApi()
		)

		val result = client.http
			.request(api2.forms.create, api2.forms.idOf(), body, Parameters.Empty, client.context.value)
			.flatMapSuccess { (id, _) ->
				val form = bind(api2.forms.id.idFrom(id, client.context.value))
				emit(successful(Form.Ref(form, this@Forms)))
			}

		emitAll(result)
	}

	override fun createVersion(form: Form.Ref, version: Form.Version): State<Form.Version.Ref> = state {
		val new = version.toApi()

		val result = client.http
			.request(api2.forms.id.create, api2.forms.id.idOf(form.id), new, Parameters.Empty, client.context.value)
			.flatMapSuccess { (id, _) ->
				val (_, versionId) = bind(api2.forms.id.version.idFrom(id, client.context.value))
				emit(
					successful(
						Form.Version.Ref(
							form,
							versionId.toInstant(),
							client.formVersions,
						)
					)
				)
			}
			.onEachSuccess { cache.expire(form) }

		emitAll(result)
	}

	override fun edit(form: Form.Ref, name: String?, public: Boolean?, open: Boolean?): State<Unit> = state {
		val body = ApiForm.Edit(
			name,
			public,
			open,
		)

		val result = client.http
			.request(api2.forms.id.edit, api2.forms.id.idOf(form.id), body, Parameters.Empty, client.context.value)
			.onEachSuccess { cache.expire(form) }

		emitAll(result)
	}

	override fun directRequest(ref: Ref<Form>): State<Form> = state {
		ensureValid(ref is Form.Ref) { "${this@Forms} n'accepte pas la référence $ref" }

		val result = client.http
			.request(api2.forms.id.get, api2.forms.id.idOf(ref.id), Unit, Parameters.Empty, client.context.value)
			.flatMapSuccess { form ->
				val result = Form(
					form.name,
					form.public,
					form.open,
					form.versions.map {
						val (_, versionId) = bind(api2.forms.id.version.idFrom(it, client.context.value))
						Form.Version.Ref(
							ref,
							versionId.toInstant(),
							client.formVersions,
						)
					}
				)
				emit(successful(result))
			}

		emitAll(result)
	}

}

class FormVersions(
	private val client: Client,
	override val cache: RefCache<Form.Version>,
) : AbstractFormVersions {
	override fun directRequest(ref: Ref<Form.Version>): State<Form.Version> = state {
		ensureValid(ref is Form.Version.Ref) { "${this@FormVersions} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.forms.id.version.get,
				api2.forms.id.version.idOf(ref.form.id, ref.version.toString()),
				Unit,
				Parameters.Empty,
				client.context.value
			)
			.mapSuccess { form ->
				Form.Version(
					form.creationDate,
					form.title,
					form.field.toCore(client.templates, client.templateVersions),
					form.steps.map {
						Form.Step(
							it.id,
							Department.Ref(
								bind(api2.departments.id.idFrom(it.department, client.context.value)),
								client.departments
							),
							it.field?.toCore(client.templates, client.templateVersions)
						)
					})
			}

		emitAll(result)
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
