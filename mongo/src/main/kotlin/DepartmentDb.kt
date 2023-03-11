package opensavvy.formulaide.mongo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import org.litote.kmongo.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

@Serializable
private class DepartmentDbDto(
    @SerialName("_id") val id: String,
    val name: String,
    val open: Boolean = true,
)

class DepartmentDb(
    database: Database,
    context: CoroutineContext,
) : Department.Service {

    private val collection = database.client.getCollection<DepartmentDbDto>("departments")

    override val cache = defaultRefCache<Department>()
        .cachedInMemory(context)
        .expireAfter(30.minutes, context)

    private fun toRef(id: String) = Department.Ref(id, this)

    override suspend fun list(includeClosed: Boolean): Outcome<List<Department.Ref>> = out {
        ensureEmployee()

        val filter = if (includeClosed) {
            ensureAdministrator()
            EMPTY_BSON
        } else {
            DepartmentDbDto::open eq true
        }

        collection.find(filter)
            .toList()
            .map {
                val ref = toRef(it.id)

                cache.update(ref, Department(it.name, it.open))

                ref
            }
    }

    override suspend fun create(name: String): Outcome<Department.Ref> = out {
        ensureAdministrator()

        val id = newId<DepartmentDbDto>().toString()

        collection.insertOne(
            DepartmentDbDto(
                id = id,
                name,
                open = true,
            )
        )

        toRef(id)
    }

    override suspend fun edit(department: Department.Ref, open: Boolean?): Outcome<Unit> = out {
        ensureAdministrator()

        val id = department.id.toId<DepartmentDbDto>()

        val updates = buildList {
            if (open != null)
                add(setValue(DepartmentDbDto::open, open))
        }

        if (updates.isEmpty())
            return@out // nothing to do

        collection.updateOneById(
            id,
            combine(updates),
        )

        department.expire()
    }

    override suspend fun directRequest(ref: Ref<Department>): Outcome<Department> = out {
        ensureValid(ref is Department.Ref) { "Référence invalide : $ref" }
        ensureEmployee()

        val result = collection.findOneById(ref.id)
        ensureFound(result != null) { "Département introuvable : $ref" }
        ensureFound(result.open || currentRole() >= User.Role.Administrator) { "Département introuvable : $ref" }

        Department(result.name, result.open)
    }
}
