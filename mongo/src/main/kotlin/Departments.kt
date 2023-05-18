package opensavvy.formulaide.mongo

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import org.litote.kmongo.*
import kotlin.time.Duration.Companion.minutes

@Serializable
private class MongoDepartmentDto(
    @SerialName("_id") val id: String,
    val name: String,
    val open: Boolean = true,
)

class MongoDepartments(
    database: Database,
    scope: CoroutineScope,
) : Department.Service<MongoDepartments.Ref> {

    private val collection = database.client.getCollection<MongoDepartmentDto>("departments")

    private val cache = cache<Ref, User.Role, Department.Failures.Get, Department> { ref, role ->
        out {
            ensure(role >= User.Role.Employee) { Department.Failures.Unauthenticated }

            val result = collection.findOneById(ref.id)
            ensureNotNull(result) { Department.Failures.NotFound(ref) }
            ensure(result.open || role >= User.Role.Administrator) { Department.Failures.NotFound(ref) }

            Department(result.name, result.open)
        }
    }.cachedInMemory(scope.coroutineContext.job)
        .expireAfter(30.minutes, scope)

    override suspend fun list(includeClosed: Boolean): Outcome<Department.Failures.List, List<Ref>> = out {
        ensureEmployee { Department.Failures.Unauthenticated }

        val filter = if (includeClosed) {
            ensureAdministrator { Department.Failures.Unauthorized }
            EMPTY_BSON
        } else {
            MongoDepartmentDto::open eq true
        }

        collection.find(filter)
            .toList()
            .map {
                val ref = Ref(it.id)

                cache.update(ref, currentRole(), Department(it.name, it.open))

                ref
            }
    }

    override suspend fun create(name: String): Outcome<Department.Failures.Create, Ref> = out {
        ensureEmployee { Department.Failures.Unauthenticated }
        ensureAdministrator { Department.Failures.Unauthorized }

        val id = newId<MongoDepartmentDto>().toString()

        collection.insertOne(
            MongoDepartmentDto(
                id = id,
                name,
                open = true,
            )
        )

        Ref(id)
    }

    override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

    inner class Ref internal constructor(
        internal val id: String,
    ) : Department.Ref {
        override suspend fun edit(open: Boolean?): Outcome<Department.Failures.Edit, Unit> = out {
            ensureEmployee { Department.Failures.Unauthenticated }
            ensureAdministrator { Department.Failures.Unauthorized }

            val id = id.toId<MongoDepartmentDto>()

            val updates = buildList {
                if (open != null)
                    add(setValue(MongoDepartmentDto::open, open))
            }

            if (updates.isEmpty())
                return@out // nothing to do

            collection.updateOneById(
                id,
                combine(updates),
            )

            cache.expire(this@Ref)
        }

        override fun request(): ProgressiveFlow<Department.Failures.Get, Department> = flow {
            emitAll(cache[this@Ref, currentRole()])
        }

        // region Overrides

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ref) return false

            return id == other.id
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString() = "MongoDepartments.Ref($id)"
        override fun toIdentifier() = Identifier(id)

        // endregion
    }
}
