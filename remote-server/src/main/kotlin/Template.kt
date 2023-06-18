package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.now
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.formulaide.remote.dto.SchemaDto
import opensavvy.formulaide.remote.dto.SchemaDto.Companion.toDto
import opensavvy.formulaide.remote.server.utils.invalidRequest
import opensavvy.formulaide.remote.server.utils.notFound
import opensavvy.formulaide.remote.server.utils.unauthenticated
import opensavvy.formulaide.remote.server.utils.unauthorized
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.map
import opensavvy.state.outcome.mapFailure

fun Routing.templates(
	templates: Template.Service,
) {

	route(api.templates.get, contextGenerator) {
		templates.list(includeClosed = parameters.includeClosed)
			.mapFailure {
				when (it) {
					Template.Failures.Unauthenticated -> unauthenticated()
					Template.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.map { api.templates.id.idOf(it.toIdentifier().text) }
	}

	route(api.templates.create, contextGenerator) {
		templates.create(
			body.name,
			body.firstVersion.title,
			body.firstVersion.field.toCore { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
		).mapFailure {
			when (it) {
				is Template.Failures.InvalidImport -> invalidRequest(SchemaDto.NewFailures.InvalidImport(it.failures.map { it.toDto() }))
				is Template.Failures.NotFound -> notFound(templates.fromIdentifier(api.templates.id.identifierOf(id)))
				Template.Failures.Unauthenticated -> unauthenticated()
				Template.Failures.Unauthorized -> unauthorized()
			}
		}.map { api.templates.id.idOf(it.toIdentifier().text) }
			.map { Identified(it, Unit) }
			.toEither()
			.bind()
	}

	route(api.templates.id.create, contextGenerator) {
		val ref = api.templates.id.identifierOf(id)
			.let(templates::fromIdentifier)

		ref.createVersion(
			title = body.title,
			field = body.field.toCore { templates.versions.fromIdentifier(api.templates.id.version.identifierOf(it)) },
		).mapFailure {
			when (it) {
				is Template.Failures.InvalidImport -> invalidRequest(SchemaDto.NewFailures.InvalidImport(it.failures.map { it.toDto() }))
				is Template.Failures.NotFound -> notFound(templates.fromIdentifier(api.forms.id.identifierOf(id)))
				Template.Failures.Unauthenticated -> unauthenticated()
				Template.Failures.Unauthorized -> unauthorized()
			}
		}.map { api.templates.id.version.idOf(it.template.toIdentifier().text, it.creationDate.toString()) }
			.map { Identified(it, Unit) }
			.toEither()
			.bind()
	}

	route(api.templates.id.edit, contextGenerator) {
		val ref = api.templates.id.identifierOf(id)
			.let(templates::fromIdentifier)

		ref.edit(
			name = body.name,
			open = body.open,
		).mapFailure {
			when (it) {
				is Template.Failures.NotFound -> notFound(templates.fromIdentifier(api.forms.id.identifierOf(id)))
				Template.Failures.Unauthenticated -> unauthenticated()
				Template.Failures.Unauthorized -> unauthorized()
			}
		}.toEither()
			.bind()
	}

	route(api.templates.id.get, contextGenerator) {
		val ref = api.templates.id.identifierOf(id)
			.let(templates::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Template.Failures.NotFound -> notFound(templates.fromIdentifier(api.forms.id.identifierOf(id)))
					Template.Failures.Unauthenticated -> unauthenticated()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

	route(api.templates.id.version.get, contextGenerator) {
		val ref = api.templates.id.version.identifierOf(id)
			.let(templates.versions::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Template.Version.Failures.NotFound -> notFound(ref)
					Template.Version.Failures.Unauthenticated -> unauthenticated()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

}
