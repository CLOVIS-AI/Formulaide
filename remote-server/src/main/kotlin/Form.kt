package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.now
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toCore
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.failures.asEmbedded
import opensavvy.formulaide.remote.server.utils.invalidRequest
import opensavvy.formulaide.remote.server.utils.notFound
import opensavvy.formulaide.remote.server.utils.unauthenticated
import opensavvy.formulaide.remote.server.utils.unauthorized
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.map
import opensavvy.state.outcome.mapFailure

fun Routing.forms(
	departments: Department.Service<*>,
	templates: Template.Service,
	forms: Form.Service,
) {

	route(api.forms.get, contextGenerator) {
		forms.list(includeClosed = parameters.includeClosed)
			.mapFailure {
				when (it) {
					Form.Failures.Unauthenticated -> unauthenticated()
					Form.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.map { api.forms.id.idOf(it.toIdentifier().text) }
	}

	route(api.forms.create, contextGenerator) {
		forms.create(
			body.name,
			body.firstVersion.title,
			body.firstVersion.field.toCore { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
			*(body.firstVersion.steps ?: emptyList()).map {
				it.toCore(
					decodeDepartment = { departments.fromIdentifier(api.departments.id.identifierOf(it)) },
					decodeTemplate = { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
				)
			}.toTypedArray()
		).mapFailure {
			when (it) {
				is Form.Failures.InvalidImport -> invalidRequest(SchemaDto.NewFailures.InvalidImport(it.failures.map { it.toDto() }))
				is Form.Failures.NotFound -> notFound(forms.fromIdentifier(api.forms.id.identifierOf(id)))
				Form.Failures.Unauthenticated -> unauthenticated()
				Form.Failures.Unauthorized -> unauthorized()
			}
		}.map { api.forms.id.idOf(it.toIdentifier().text) }
			.map { Identified(it, Unit) }
			.toEither()
			.bind()
	}

	route(api.forms.id.create, contextGenerator) {
		val ref = api.forms.id.identifierOf(id)
			.let(forms::fromIdentifier)

		ref.createVersion(
			title = body.title,
			field = body.field.toCore { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
			*(body.steps ?: emptyList()).map {
				it.toCore(
					decodeDepartment = { departments.fromIdentifier(api.departments.id.identifierOf(it)) },
					decodeTemplate = { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
				)
			}.toTypedArray()
		)
			.mapFailure {
				when (it) {
					is Form.Failures.InvalidImport -> invalidRequest(SchemaDto.NewFailures.InvalidImport(it.failures.map { it.toDto() }))
					is Form.Failures.NotFound -> notFound(forms.fromIdentifier(api.forms.id.identifierOf(id)))
					Form.Failures.Unauthenticated -> unauthenticated()
					Form.Failures.Unauthorized -> unauthorized()
				}
			}
			.map { api.forms.id.version.idOf(it.form.toIdentifier().text, it.creationDate.toString()) }
			.map { Identified(it, Unit) }
			.toEither()
			.bind()
	}

	route(api.forms.id.edit, contextGenerator) {
		val ref = api.forms.id.identifierOf(id)
			.let(forms::fromIdentifier)

		ref.edit(
			name = body.name,
			open = body.open,
			public = body.public,
		).mapFailure {
			when (it) {
				is Form.Failures.NotFound -> notFound(forms.fromIdentifier(api.forms.id.identifierOf(id)))
				Form.Failures.Unauthenticated -> unauthenticated()
				Form.Failures.Unauthorized -> unauthorized()
			}
		}.toEither()
			.bind()
	}

	route(api.forms.id.get, contextGenerator) {
		val ref = api.forms.id.identifierOf(id)
			.let(forms::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Form.Failures.NotFound -> notFound(forms.fromIdentifier(api.forms.id.identifierOf(id)))
					Form.Failures.Unauthenticated -> unauthenticated()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

	route(api.forms.id.version.get, contextGenerator) {
		val ref = api.forms.id.version.identifierOf(id)
			.let(forms.versions::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Form.Version.Failures.CouldNotGetForm -> invalidRequest(SchemaDto.GetVersionFailures.CouldNotGetForm(when (it.error) {
						Form.Failures.Unauthenticated -> unauthenticated()
						is Form.Failures.NotFound -> notFound(ref.form)
					}.asEmbedded()))

					is Form.Version.Failures.NotFound -> notFound(ref)
					Form.Version.Failures.Unauthenticated -> unauthenticated()
				}
			}.toEither()
			.bind()
			.toDto()
	}

}
