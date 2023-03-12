package opensavvy.formulaide.mongo

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.CacheAdapter
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.logger.Logger.Companion.info
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.*
import opensavvy.state.progressive.firstValue
import org.litote.kmongo.*
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

@Serializable
private data class UserDbDto(
    @SerialName("_id") val id: String,
    val name: String,
    val email: String,
    val hashedPassword: String,
    val active: Boolean = true,
    val administrator: Boolean,
    val departments: Set<String> = emptySet(),
    val singleUsePassword: Boolean,
    val passwordWasUsed: Boolean = false,
    val tokens: Set<String> = emptySet(),
)

@Serializable
private data class UserIdDto(
    @SerialName("_id") val id: String,
)

class UserDb(
    database: Database,
    context: CoroutineContext,
    private val departments: Department.Service,
) : User.Service {

    private val collection = database.client.getCollection<UserDbDto>("users")

    init {
        CoroutineScope(context).launch {
            collection.ensureUniqueIndex(UserDbDto::email)
        }
    }

    override val cache: RefCache<User> = defaultRefCache<User>()
        .cachedInMemory(context)
        .expireAfter(10.minutes, context)

    private fun toRef(id: String) = User.Ref(id, this)

    private val log = loggerFor(this)

    //region Hashing

    private fun hash(password: String) = BCrypt.withDefaults()
        .hashToString(12, password.toCharArray())

    private fun checkHash(password: String, hash: String) = BCrypt.verifyer()
        .verify(
            password.toCharArray(),
            hash.toCharArray(),
        ).verified

    //endregion
    //region Management

    override suspend fun list(includeClosed: Boolean): Outcome<List<User.Ref>> = out {
        ensureAdministrator()

        collection
            .withDocumentClass<UserIdDto>()
            .find(
                (UserDbDto::active eq true).takeIf { !includeClosed },
            )
            .toList()
            .map { toRef(it.id) }
    }

    override suspend fun create(
        email: Email,
        fullName: String,
        administrator: Boolean
    ): Outcome<Pair<User.Ref, Password>> = out {
        ensureAdministrator()

        run { // Check the email address is available
            val previous = collection.findOne(UserDbDto::email eq email.value)
            ensureValid(previous == null) { "L'adresse email ${email.value} été déjà utilisée par un autre utilisateur" }
        }

        val id = newId<UserDbDto>().toString()
        val singleUsePassword = Password(UUID.randomUUID().toString())

        collection.insertOne(
            UserDbDto(
                id = id,
                name = fullName,
                email = email.value,
                administrator = administrator,
                singleUsePassword = true,
                hashedPassword = hash(singleUsePassword.value),
            )
        )

        toRef(id) to singleUsePassword
    }

    override suspend fun join(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
        ensureAdministrator()

        collection.updateOneById(
            user.id,
            addToSet(UserDbDto::departments, department.id),
        )

        user.expire()
    }

    override suspend fun leave(user: User.Ref, department: Department.Ref): Outcome<Unit> = out {
        ensureAdministrator()

        collection.updateOneById(
            user.id,
            pull(UserDbDto::departments, department.id),
        )

        user.expire()
    }

    override suspend fun edit(user: User.Ref, active: Boolean?, administrator: Boolean?): Outcome<Unit> = out {
        ensureAdministrator()
        ensureValid(user.id != currentUser()!!.id) { "Il n'est pas autorisé de s'auto-modifier" }

        val updates = buildList {
            if (active != null)
                add(setValue(UserDbDto::active, active))

            if (administrator != null)
                add(setValue(UserDbDto::administrator, administrator))
        }

        if (updates.isEmpty())
            return@out // nothing to do

        collection.updateOneById(
            user.id,
            combine(updates),
        )

        user.expire()
    }

    //endregion
    //region Password & tokens

    private val cacheByEmail = CacheAdapter.cache { email: String ->
        collection.findOne(UserDbDto::email eq email)
            ?.id
            ?.let(::toRef)
    }
        .cachedInMemory(context)
        .expireAfter(10.minutes, context)

    private val tokenCache = CacheAdapter.cache { user: User.Ref ->
        collection.findOneById(user.id)
            ?.tokens
            ?: emptySet()
    }
        .cachedInMemory(context)
        .expireAfter(3.minutes, context)

    override suspend fun logIn(email: Email, password: Password): Outcome<Pair<User.Ref, Token>> = out {
        val ref = cacheByEmail[email.value].firstValue().bind()
        ensureAuthenticated(ref != null) {
            log.warn(email) { "Blocked log in attempt for an email address that doesn't exist: $email" }
            GENERIC_LOGIN_ERROR_MESSAGE
        }

        val user = collection.findOneById(ref.id)
        ensureAuthenticated(user != null && user.active) {
            log.info(user) { "Blocked log in attempt for an inactive user" }
            GENERIC_LOGIN_ERROR_MESSAGE
        }

        ensureAuthenticated(!user.singleUsePassword || !user.passwordWasUsed) {
            log.warn(user) { "Blocked log in attempt because the password is single-use, and has already been used" }
            GENERIC_LOGIN_ERROR_MESSAGE
        }

        val hashed = user.hashedPassword
        ensureAuthenticated(checkHash(password.value, hashed)) {
            log.warn(ref) { "Log in attempt with an incorrect password" }
            GENERIC_LOGIN_ERROR_MESSAGE
        }

        val token = Token(UUID.randomUUID().toString())

        collection.updateOneById(
            ref.id,
            combine(
                addToSet(UserDbDto::tokens, token.value),
                setValue(UserDbDto::passwordWasUsed, true),
            ),
        )

        tokenCache.expire(ref)

        ref to token
    }

    override suspend fun resetPassword(user: User.Ref): Outcome<Password> = out {
        ensureAdministrator()
        ensureAuthorized(currentUser() != user) { "Il n'est pas possible de réinitialiser son propre mot de passe" }

        val newPassword = Password(UUID.randomUUID().toString())

        collection.updateOneById(
            user.id,
            combine(
                setValue(UserDbDto::hashedPassword, hash(newPassword.value)),
                setValue(UserDbDto::singleUsePassword, true),
                setValue(UserDbDto::passwordWasUsed, false),
                setValue(UserDbDto::tokens, emptySet()),
            )
        )

        tokenCache.expire(user)
        cache.expire(user)

        newPassword
    }

    override suspend fun setPassword(user: User.Ref, oldPassword: String, newPassword: Password): Outcome<Unit> = out {
        ensureEmployee()
        ensureAuthorized(currentUser() == user) { "Il n'est pas possible de modifier le mot de passe d'un autre utilisateur" }

        val current = collection.findOneById(user.id)
        ensureFound(current != null) { "Impossible de trouver l'utilisateur $current" }

        ensureAuthenticated(checkHash(oldPassword, current.hashedPassword)) {
            log.warn(user) { "Blocked attempt to set the password, because the provided password doesn't match the user." }
            INCORRECT_OLD_PASSWORD
        }

        collection.updateOneById(
            user.id,
            combine(
                setValue(UserDbDto::hashedPassword, hash(newPassword.value)),
                setValue(UserDbDto::singleUsePassword, false),
                setValue(UserDbDto::passwordWasUsed, false),
                setValue(UserDbDto::tokens, emptySet()),
            )
        )

        tokenCache.expire(user)
        cache.expire(user)
    }

    override suspend fun verifyToken(user: User.Ref, token: Token): Outcome<Unit> = out {
        val tokens = tokenCache[user].firstValue().bind()

        ensureAuthenticated(token.value in tokens) {
            log.warn(user) { "Blocked token verification" }
            "Le jeton de connexion est invalide pour cet utilisateur"
        }
    }

    override suspend fun logOut(user: User.Ref, token: Token): Outcome<Unit> = out {
        ensureEmployee()
        ensureAuthenticated(user == currentUser()) { "Il n'est pas autorisé de déconnecter quelqu'un d'autre que soit-même" }

        collection.updateOneById(
            user.id,
            pull(UserDbDto::tokens, token.value),
        )

        tokenCache.expire(user)
    }

    //endregion

    override suspend fun directRequest(ref: Ref<User>): Outcome<User> = out {
        ensureValid(ref is User.Ref) { "Référence invalide : $ref" }
        ensureEmployee()

        val result = collection.findOneById(ref.id)
        ensureFound(result != null) { "Utilisateur introuvable : $ref" }

        User(
            email = Email(result.email),
            name = result.name,
            active = result.active,
            administrator = result.administrator,
            departments = result.departments.mapTo(HashSet()) { Department.Ref(it, departments) },
            singleUsePassword = result.singleUsePassword,
        )
    }

    companion object {
        private const val GENERIC_LOGIN_ERROR_MESSAGE =
            "Les informations de connexion ne correspondent à aucun utilisateur"
        private const val INCORRECT_OLD_PASSWORD = "Le mot de passe actuel est incorrect"
    }
}
