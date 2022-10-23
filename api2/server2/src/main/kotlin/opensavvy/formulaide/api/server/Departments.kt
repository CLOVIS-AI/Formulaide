package opensavvy.formulaide.api.server

import io.ktor.server.routing.*
import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.Ref.Companion.request
import opensavvy.formulaide.api.Context
import opensavvy.formulaide.api.Department
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.database.Database
import opensavvy.formulaide.state.bind
import opensavvy.formulaide.state.mapSuccess
import opensavvy.formulaide.state.onEachSuccess
import opensavvy.spine.ktor.server.ContextGenerator
import opensavvy.spine.ktor.server.route
import opensavvy.formulaide.core.Department as CoreDepartment

fun Routing.departments(database: Database, contextGenerator: ContextGenerator<Context>) {

	route(api2.departments.get, contextGenerator) {
		ensureEmployee { "Seuls les employés peuvent accéder à la liste des départements" }

		if (parameters.includeClosed)
			ensureAdministrator { "Seuls les administrateurs peuvent accéder aux départements fermés" }

		val result = database.departments.list(parameters.includeClosed)
			.mapSuccess { list ->
				list.map { api2.departments.id.idOf(it.id) }
			}

		emitAll(result)
	}

	route(api2.departments.id.get, contextGenerator) {
		ensureEmployee { "Seuls les employés peuvent accéder aux départements" }

		val id = api2.departments.id.idFrom(id, context).let { bind(it) }
		val result = CoreDepartment.Ref(id, database.departments)
			.request()
			.onEachSuccess {
				if (!it.open)
					ensureAdministrator { "Seuls les administrateurs peuvent accéder aux départements fermés" }
			}
			.mapSuccess { Department(name = it.name, open = it.open) }

		emitAll(result)
	}

	route(api2.departments.create, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent créer des départements" }

		val result = database.departments.create(body.name)
			.mapSuccess { api2.departments.id.idOf(it.id) to Unit }

		emitAll(result)
	}

	route(api2.departments.id.visibility, contextGenerator) {
		ensureAdministrator { "Seuls les administrateurs peuvent modifier le statut d'un département" }

		val id = api2.departments.id.idFrom(id, context).let { bind(it) }
		val ref = CoreDepartment.Ref(id, database.departments)

		val result = if (body.open)
			ref.open()
		else
			ref.close()

		emitAll(result)
	}

}
