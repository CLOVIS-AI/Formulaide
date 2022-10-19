package opensavvy.formulaide.database.document

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.cache.Cache
import opensavvy.cache.CacheAdapter
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.AbstractUsers
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.database.Database
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.*
import opensavvy.state.Slice.Companion.successful
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import opensavvy.formulaide.core.User as CoreUser

@Serializable
class User(
	@SerialName("_id") val id: String,
	val email: String,
	val name: String,
	val open: Boolean,
	val departments: Set<String>,
	val administrator: Boolean,
	val password: String,
	val tokens: Set<String>,
	/** If `true`, the current password must only be used once. */
	val singleUsePassword: Boolean,
	/** If `true`, the current password has been used at least once. */
	val usedPassword: Boolean,
)

class Users(
	private val users: CoroutineCollection<User>,
	override val cache: RefCache<CoreUser>,
	context: CoroutineContext,
	private val database: Database,
) : AbstractUsers {

	private val log = loggerFor(this)

	init {
		CoroutineScope(context).launch {
			users.ensureUniqueIndex(User::email)
		}
	}

	@Suppress("SpellCheckingInspection")
	private val hasher = BCrypt.withDefaults()
	private val verifier = BCrypt.verifyer()

	private val userCache: Cache<CoreUser.Ref, User> = CacheAdapter<CoreUser.Ref, User> {
		state {
			val user = users.findOne(User::id eq it.id)
			ensureFound(user != null) { "Utilisateur introuvable : $it" }
			emit(successful(user))
		}
	}.cachedInMemory(context)
		.expireAfter(2.minutes, context)

	override fun list(includeClosed: Boolean): State<List<CoreUser.Ref>> = state {
		val filter = if (includeClosed)
			null
		else
			User::open eq true

		val result = users.find(filter)
			.batchSize(10)
			.toList()
		emit(successful(result.map { CoreUser.Ref(it.id, this@Users) }))
	}

	/**
	 * Checks that [token] is one of [user]'s tokens.
	 *
	 * If it is, returns [Unit].
	 */
	fun verifyToken(user: CoreUser.Ref, token: String): State<Unit> = state {
		val dbUser = userCache[user]
			.onEach {
				when (val status = it.status) {
					is Status.Successful -> {}
					is Status.Pending -> emit(Slice(status, it.progression))
					is Status.Failed -> emit(Slice(status, it.progression))
				}
			}
			.firstResultOrNull() ?: throw StateBuilderCancellation()

		val verified = dbUser.tokens.any {
			verifier.verify(token.toCharArray(), it.toCharArray()).verified
		}
		ensureAuthenticated(verified) { "Le token de connexion est invalide" }

		emit(successful(Unit))
	}

	/**
	 * Checks that [email] and [password] match.
	 *
	 * If it is, returns a token they can use to authenticate.
	 */
	fun logIn(email: String, password: String): State<Pair<CoreUser.Ref, String>> = state {
		val dbUser = users.findOne(User::email eq email)
		ensureAuthenticated(dbUser != null) {
			log.warn(email) { "Someone attempted to log in with an email that matches no user." }

			// The end user will NOT know that they got refused because the account doesn't exist
			"Les informations de connexion sont invalides"
		}

		ensureAuthenticated(dbUser.open) {
			log.warn(email) { "Someone attempted to log in to an account that was closed." }

			"Les informations de connexion sont invalides"
		}

		val passwordVerification = verifier.verify(password.toCharArray(), dbUser.password)
		ensureAuthenticated(passwordVerification.verified) {
			log.warn(email) { "Someone attempted to log in with an incorrect password." }

			"Les informations de connexion sont invalides"
		}

		// If the password has already been used, refuse the connexion

		ensureAuthenticated(!dbUser.singleUsePassword || !dbUser.usedPassword) { "Ce mot de passe temporaire a déjà été utilisé, veuillez contacter un administrateur pour débloquer votre compte" }

		if (!dbUser.usedPassword)
			users.updateOne(User::id eq dbUser.id, setValue(User::usedPassword, true))

		val ref = CoreUser.Ref(dbUser.id, this@Users)

		// The password is correct, create a new token for them

		val token = UUID.randomUUID().toString() // 128 bits, 122 bits are crypto-secure
		val hashedToken = hasher.hashToString(12, token.toCharArray())
		users.updateOne(User::id eq dbUser.id, addToSet(User::tokens, hashedToken))

		userCache.expire(ref)
		emit(successful(ref to token))
	}

	fun logOut(user: CoreUser.Ref, token: String): State<Unit> = state {
		val dbUser = userCache[user]
			.onEach {
				when (val status = it.status) {
					is Status.Successful -> {}
					is Status.Pending -> emit(Slice(status, it.progression))
					is Status.Failed -> emit(Slice(status, it.progression))
				}
			}
			.firstResultOrNull() ?: throw StateBuilderCancellation()

		val hashedToken = dbUser.tokens.find {
			verifier.verify(token.toCharArray(), it).verified
		}

		if (hashedToken != null) {
			users.updateOne(User::id eq user.id, pull(User::tokens, hashedToken))
			userCache.expire(user)
			// cache.expire(ref) <- useless, because it doesn't store the token
		} // else: the token they sent is invalid anyway, they're already logged out

		emit(successful(Unit))
	}

	override fun create(
		email: String,
		fullName: String,
		departments: Set<Department.Ref>,
		administrator: Boolean,
	): State<Pair<CoreUser.Ref, String>> = state {
		log.info { "New user creation request for email '$email'" }

		// Checking that the email address is available
		val previous = users.findOne(User::email eq email)
		ensureValid(previous == null) { "Un utilisateur possède déjà cette adresse électronique : '$email'" }

		// Creating the new user…

		val id = newId<User>().toString()

		val randomPassword = UUID.randomUUID().toString()
		val hashedPassword = hasher.hashToString(12, randomPassword.toCharArray())

		users.insertOne(
			User(
				id,
				email,
				fullName,
				open = true,
				departments = departments.mapTo(HashSet()) { it.id },
				administrator,
				hashedPassword,
				tokens = emptySet(),
				singleUsePassword = true,
				usedPassword = false,
			)
		)

		emit(successful(CoreUser.Ref(id, this@Users) to randomPassword))
	}

	override fun edit(
		user: CoreUser.Ref,
		open: Boolean?,
		administrator: Boolean?,
		departments: Set<Department.Ref>?,
	): State<Unit> = state {
		val edit = ArrayList<Bson>()

		if (open != null)
			edit += setValue(User::open, open)

		if (administrator != null)
			edit += setValue(User::administrator, administrator)

		if (departments != null)
			edit += setValue(User::departments, departments.mapTo(HashSet()) { it.id })

		ensureValid(edit.isNotEmpty()) { "Impossible d'effectuer une modification vide, aucune demande n'a été reçue" }

		users.updateOne(User::id eq user.id, combine(edit))
		userCache.expire(user)
		cache.expire(user)
		emit(successful(Unit))
	}

	fun setPassword(user: CoreUser.Ref, oldPassword: String, newPassword: String): State<Unit> = state {
		log.info(user) { "Password modification request received." }

		val dbUser = users.findOne(User::id eq user.id)
		ensureFound(dbUser != null) { "Utilisateur introuvable : $user" }

		val result = verifier.verify(oldPassword.toCharArray(), dbUser.password)
		ensureValid(result.verified) { "Le mot de passe fourni est incorrect" }

		val hashed = hasher.hashToString(12, newPassword.toCharArray())
		users.updateOne(
			User::id eq user.id, combine(
				setValue(User::password, hashed),
				setValue(User::singleUsePassword, false),
				setValue(User::usedPassword, false),
				setValue(User::tokens, emptySet()),
			)
		)

		userCache.expire(user)
		cache.expire(user)
		emit(successful(Unit))
	}

	override fun resetPassword(user: CoreUser.Ref): State<String> = state {
		log.info(user) { "Received a password reset request." }

		val randomPassword = UUID.randomUUID().toString()
		val hashedPassword = hasher.hashToString(12, randomPassword.toCharArray())
		users.updateOne(
			User::id eq user.id, combine(
				setValue(User::password, hashedPassword),
				setValue(User::singleUsePassword, true),
				setValue(User::usedPassword, false),
				setValue(User::tokens, emptySet()),
			)
		)

		userCache.expire(user)
		cache.expire(user)
		emit(successful(randomPassword))
	}

	override fun directRequest(ref: Ref<CoreUser>): State<CoreUser> = state {
		ensureValid(ref is CoreUser.Ref) { "${this@Users} n'accepte pas la référence $ref" }

		val result = users.findOne(User::id eq ref.id)
		ensureFound(result != null) { "Utilisateur introuvable : $ref" }

		val user = CoreUser(
			email = result.email,
			name = result.name,
			open = result.open,
			departments = result.departments.mapTo(HashSet()) { Department.Ref(it, database.departments) },
			administrator = result.administrator,
			forceResetPassword = result.singleUsePassword,
		)

		emit(successful(user))
	}

}
