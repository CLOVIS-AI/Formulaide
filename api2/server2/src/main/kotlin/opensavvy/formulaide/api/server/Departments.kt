package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.Department
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.database.Database
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.formulaide.core.Department as CoreDepartment

fun Routing.departments(database: Database, contextGenerator: ContextGenerator<Context>) {

	route(api2.departments.get, contextGenerator) {
		ensureEmployee { "Seuls les employés peuvent accéder à la liste des départements" }

		if (parameters.includeClosed)
			ensureAdministrator { "Seuls les administrateurs peuvent accéder aux départements fermés" }

		database.departments.list(parameters.includeClosed).bind()
			.map { api2.departments.id.idOf(it.id) }
	}

	route(api2.departments.id.get, contextGenerator) {
		ensureEmployee { "Seuls les employés peuvent accéder aux départements" }

		val id = api2.departments.id.idFrom(id, context).bind()
		val department = CoreDepartment.Ref(id, database.departments)
			.requestValue().bind()

		if (!department.open)
			ensureAdministrator { "Seuls les administrateurs peuvent accéder aux départements fermés" }

		Department(name = department.name, open = department.open)
	}

	route(api2.departments.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des départements" }

		val result = database.departments.create(body.name).bind()

		api2.departments.id.idOf(result.id) to Unit
	}

	route(api2.departments.id.visibility, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier le statut d'un département" }

		val id = api2.departments.id.idFrom(id, context).bind()
		val ref = CoreDepartment.Ref(id, database.departments)

		if (body.open)
			ref.open().bind()
		else
			ref.close().bind()
	}

}
