package formulaide.db.document

import formulaide.api.users.User
import formulaide.core.Department
import formulaide.core.UserBackbone
import formulaide.db.Database
import kotlinx.serialization.Serializable
import opensavvy.backbone.*
import opensavvy.backbone.Ref.Companion.requestValue
import opensavvy.cache.Cache
import opensavvy.state.*
import opensavvy.state.Slice.Companion.successful
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
	override val cache: Cache<Ref<formulaide.core.User>, formulaide.core.User>,
) : UserBackbone {
	override suspend fun all(includeClosed: Boolean): List<formulaide.core.User.Ref> {
		val results = users.find(
			(DbUser::enabled eq true).takeIf { !includeClosed }
		)

		return results
			.toList()
			.map { formulaide.core.User.Ref(it.email, this) }
	}

	@Deprecated("This function cannot be implemented server-side")
	override suspend fun me(): formulaide.core.User.Ref {
		error("Impossible to implement this function on the server-side")
	}

	@Deprecated("This function cannot be implemented server-side")
	override suspend fun logIn(email: String, password: String): String {
		error("Impossible to implement this function on the server-side")
	}

	override suspend fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
		password: String,
	): formulaide.core.User.Ref {
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

		return formulaide.core.User.Ref(email, this)
	}

	override suspend fun edit(
		user: formulaide.core.User.Ref,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Department.Ref>?,
	) {
		val edits = mutableListOf<Bson>()

		if (open != null)
			edits.add(setValue(DbUser::enabled, open))

		if (administrator != null)
			edits.add(setValue(DbUser::isAdministrator, administrator))

		if (departments != null)
			edits.add(setValue(DbUser::services, departments.mapTo(HashSet()) { it.id.toInt() }))

		users.updateOne(DbUser::email eq user.email, combine(edits))
	}

	suspend fun blockUntil(user: formulaide.core.User.Ref, until: Long) {
		users.updateOne(DbUser::email eq user.email, setValue(DbUser::blockedUntil, until))
	}

	fun fromId(id: String) = formulaide.core.User.Ref(id, this)

	/**
	 * Internal method used by the authenticator.
	 */
	suspend fun getFromDb(user: formulaide.core.User.Ref): DbUser? {
		return users.findOne(DbUser::email eq user.email)
	}

	override suspend fun setPassword(
		user: formulaide.core.User.Ref,
		oldPassword: String?,
		newPassword: String,
	) {
		users.updateOne(
			DbUser::email eq user.email, combine(
				setValue(DbUser::hashedPassword, newPassword),
				inc(DbUser::tokenVersion, 1),
			)
		)
	}

	override fun directRequest(ref: Ref<formulaide.core.User>): State<formulaide.core.User> = state {
		ensureValid(ref is formulaide.core.User.Ref) { "${this@Users} doesn't support the reference $ref" }

		val db = users.findOne(DbUser::email eq ref.email)
		ensureFound(db != null) { "L'utilisateur $ref n'existe pas" }

		val core = formulaide.core.User(
			db.email,
			db.fullName,
			db.services.mapTo(HashSet()) { Department.Ref(it.toString(), database.departments) },
			db.isAdministrator,
			db.enabled ?: true,
		)
		emit(successful(core))
	}
}

fun DbUser.toCore(database: Database) = formulaide.core.User(
	email,
	fullName,
	services.mapTo(HashSet()) { database.departments.fromId(it) },
	isAdministrator,
	enabled ?: true
)
