package formulaide.db.document

import formulaide.api.users.User
import formulaide.core.Department
import formulaide.core.UserBackbone
import formulaide.db.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.backbone.Result
import org.bson.conversions.Bson
import org.litote.kmongo.combine
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.inc
import org.litote.kmongo.setValue

typealias DbUserId = String

/**
 * Database class that corresponds to [User].
 *
 * @property tokenVersion An integer
 */
@Serializable
data class DbUser(
	val id: DbUserId? = null,
	val email: String,
	val hashedPassword: String,
	val fullName: String,
	@Deprecated(message = "Replaced by 'services', kept for backward compatibility of previous database installations")
	val service: DbServiceId? = null,
	val services: Set<DbServiceId> = emptySet(),
	val isAdministrator: Boolean,
	val enabled: Boolean? = true,
	val tokenVersion: Long = 0,
	val blockedUntil: Long = 0,
)

class Users(
	private val database: Database,
	private val users: CoroutineCollection<DbUser>,
	override val cache: Cache<formulaide.core.User>,
) : UserBackbone {
	override suspend fun all(includeClosed: Boolean): List<formulaide.core.Ref<formulaide.core.User>> {
		val results = users.find(
			(DbUser::enabled eq true).takeIf { !includeClosed }
		)

		return results
			.toList()
			.map { formulaide.core.Ref(it.email, this) }
	}

	@Deprecated("This function cannot be implemented server-side")
	override suspend fun me(): formulaide.core.Ref<formulaide.core.User> {
		error("Impossible to implement this function on the server-side")
	}

	@Deprecated("This function cannot be implemented server-side")
	override suspend fun logIn(email: String, password: String): String {
		error("Impossible to implement this function on the server-side")
	}

	override suspend fun create(
		email: String,
		fullName: String,
		departments: Set<formulaide.core.Ref<Department>>,
		administrator: Boolean,
		password: String,
	): formulaide.core.Ref<formulaide.core.User> {
		for (department in departments) {
			// Check that they all exist
			department.requestValue()
		}

		check(users.findOne(DbUser::email eq email) == null) { "Un utilisateur avec cette adresse mail existe déjà : $email" }

		users.insertOne(
			DbUser(
				email = email,
				hashedPassword = password,
				fullName = fullName,
				services = departments.mapTo(HashSet()) { it.id.toInt() },
				enabled = true,
				isAdministrator = administrator,
			)
		)

		return formulaide.core.Ref(email, this)
	}

	override suspend fun edit(
		user: formulaide.core.Ref<formulaide.core.User>,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<formulaide.core.Ref<Department>>?,
	) {
		val edits = mutableListOf<Bson>()

		if (open != null)
			edits.add(setValue(DbUser::enabled, open))

		if (administrator != null)
			edits.add(setValue(DbUser::isAdministrator, administrator))

		if (departments != null)
			edits.add(setValue(DbUser::services, departments.mapTo(HashSet()) { it.id.toInt() }))

		users.updateOne(DbUser::email eq user.id, combine(edits))
	}

	suspend fun blockUntil(user: formulaide.core.Ref<formulaide.core.User>, until: Long) {
		users.updateOne(DbUser::email eq user.id, setValue(DbUser::blockedUntil, until))
	}

	fun fromId(id: String) = formulaide.core.Ref(id, this)

	/**
	 * Internal method used by the authenticator.
	 */
	suspend fun getFromDb(user: formulaide.core.Ref<formulaide.core.User>): DbUser? {
		return users.findOne(DbUser::email eq user.id)
	}

	override suspend fun setPassword(
		user: formulaide.core.Ref<formulaide.core.User>,
		oldPassword: String?,
		newPassword: String,
	) {
		users.updateOne(
			DbUser::email eq user.id, combine(
				setValue(DbUser::hashedPassword, newPassword),
				inc(DbUser::tokenVersion, 1),
			)
		)
	}

	override fun directRequest(ref: Ref<formulaide.core.User>): Flow<Data<formulaide.core.User>> = flow {
		require(ref is formulaide.core.Ref) { "$this doesn't support the reference $ref" }

		val db = users.findOne(DbUser::email eq ref.id) ?: error("L'utilisateur $ref n'existe pas")
		val core = formulaide.core.User(
			db.email,
			db.fullName,
			db.services.mapTo(HashSet()) { formulaide.core.Ref(it.toString(), database.departments) },
			db.isAdministrator,
			db.enabled ?: true,
		)
		emit(Data(Result.Success(core), Data.Status.Completed, ref))
	}
}

fun DbUser.toCore(database: Database) = formulaide.core.User(
	email,
	fullName,
	services.mapTo(HashSet()) { database.departments.fromId(it) },
	isAdministrator,
	enabled ?: true
)
