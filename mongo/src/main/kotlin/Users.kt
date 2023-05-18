package opensavvy.formulaide.mongo

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.cache.cache
import opensavvy.cache.cachedInMemory
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.cache.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.data.Email
import opensavvy.formulaide.core.data.Password
import opensavvy.formulaide.core.data.Token
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.coroutines.now
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.failed
import opensavvy.state.outcome.success
import org.litote.kmongo.*
import java.util.*
import kotlin.time.Duration.Companion.minutes

@Serializable
private data class MongoUserDto(
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
private data class MongoUserIdDto(
    @SerialName("_id") val id: String,
)

class MongoUsers(
    database: Database,
    scope: CoroutineScope,
    private val departments: Department.Service<*>,
) : User.Service<MongoUsers.Ref> {

    private val log = loggerFor(this)
    private val collection = database.client.getCollection<MongoUserDto>("users")

    init {
        scope.launch {
            collection.ensureUniqueIndex(MongoUserDto::email)
        }
    }

    private val cache = cache<Ref, User.Role, User.Failures.Get, User> { ref, role ->
        out {
            ensure(role >= User.Role.Employee) { User.Failures.Unauthenticated }

            val result = collection.findOneById(ref.id)
            ensureNotNull(result) { User.Failures.NotFound(ref) }

            User(
                email = Email(result.email),
                name = result.name,
                active = result.active,
                administrator = result.administrator,
                departments = result.departments.mapTo(HashSet()) { departments.fromIdentifier(Identifier(it)) },
                singleUsePassword = result.singleUsePassword,
            )
        }
    }.cachedInMemory(scope.coroutineContext.job)
        .expireAfter(10.minutes, scope)

    //region Hashing

    private fun hash(password: String) = BCrypt.withDefaults()
        .hashToString(12, password.toCharArray())

    private fun checkHash(password: String, hash: String) = BCrypt.verifyer()
        .verify(
            password.toCharArray(),
            hash.toCharArray(),
        ).verified

    // endregion
    // region Management

    override suspend fun list(includeClosed: Boolean): Outcome<User.Failures.List, List<User.Ref>> = out {
        ensureEmployee { User.Failures.Unauthenticated }
        ensureAdministrator { User.Failures.Unauthorized }

        collection
            .withDocumentClass<MongoUserIdDto>()
            .find(
                (MongoUserDto::active eq true).takeIf { !includeClosed },
            )
            .toList()
            .map { Ref(it.id) }
    }

    override suspend fun create(
        email: Email,
        fullName: String,
        administrator: Boolean,
    ): Outcome<User.Failures.Create, Pair<User.Ref, Password>> = out {
        ensureEmployee { User.Failures.Unauthenticated }
        ensureAdministrator { User.Failures.Unauthorized }

        run { // Check the email address is available
            val previous = collection.findOne(MongoUserDto::email eq email.value)
            ensure(previous == null) { User.Failures.UserAlreadyExists(email) }
        }

        val id = newId<MongoUserDto>().toString()
        val singleUsePassword = Password(UUID.randomUUID().toString())

        collection.insertOne(
            MongoUserDto(
                id = id,
                name = fullName,
                email = email.value,
                administrator = administrator,
                singleUsePassword = true,
                hashedPassword = hash(singleUsePassword.value),
            )
        )

        Ref(id) to singleUsePassword
    }

    // endregion
    // region Password & tokens

    private val cacheByEmail = cache<String, Unit, Ref> {
        collection.findOne(MongoUserDto::email eq it)
            ?.id
            ?.let(::Ref)
            ?.success()
            ?: Unit.failed()
    }.cachedInMemory(scope.coroutineContext.job)
        .expireAfter(10.minutes, scope)

    private val tokenCache = cache<Ref, Nothing, Set<String>> {
        val tokens = collection.findOneById(it.id)
            ?.tokens
            ?: emptySet()

        tokens.success()
    }

    override suspend fun logIn(email: Email, password: Password): Outcome<User.Failures.LogIn, Pair<User.Ref, Token>> = out {
        val ref = cacheByEmail[email.value].now()
            .toEither()
            .mapLeft {
                log.warn(email) { "Blocked log in attempt for an email address that doesn't exist: $email" }
                User.Failures.IncorrectCredentials
            }
            .bind()

        val user = collection.findOneById(ref.id)
        ensureNotNull(user) {
            log.warn(email, ref) { "Blocked log in attempt because the email cache links to an invalid ID" }
            cacheByEmail.expire(email.value)
            User.Failures.IncorrectCredentials
        }

        ensure(user.active) {
            log.warn(ref, user) { "Blocked log in attempt for an inactive user" }
            User.Failures.IncorrectCredentials
        }

        ensure(!user.singleUsePassword || !user.passwordWasUsed) {
            log.warn(ref, user) { "Blocked log in attempt because the password is single-use, and has already been used" }
            User.Failures.IncorrectCredentials
        }

        val hashed = user.hashedPassword
        ensure(checkHash(password.value, hashed)) {
            log.warn(ref, user) { "Blocked log in attempt because the password is incorrect" }
            User.Failures.IncorrectCredentials
        }

        val token = Token(UUID.randomUUID().toString())

        collection.updateOneById(
            ref.id,
            combine(
                addToSet(MongoUserDto::tokens, token.value),
                setValue(MongoUserDto::passwordWasUsed, true),
            ),
        )

        tokenCache.expire(ref)

        ref to token
    }

    override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

    // endregion

    inner class Ref internal constructor(
        internal val id: String,
    ) : User.Ref {
        override suspend fun join(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensureAdministrator { User.Failures.Unauthorized }

            collection.updateOneById(
                id,
                addToSet(MongoUserDto::departments, department.toIdentifier().text),
            )

            cache.expire(this@Ref)
        }

        override suspend fun leave(department: Department.Ref): Outcome<User.Failures.Edit, Unit> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensureAdministrator { User.Failures.Unauthorized }

            collection.updateOneById(
                id,
                pull(MongoUserDto::departments, department.toIdentifier().text),
            )

            cache.expire(this@Ref)
        }

        private suspend fun edit(active: Boolean? = null, administrator: Boolean? = null): Outcome<User.Failures.SecurityEdit, Unit> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensureAdministrator { User.Failures.Unauthorized }

            ensure(currentUser() != this@Ref) { User.Failures.CannotEditYourself }

            val updates = buildList {
                if (active != null)
                    add(setValue(MongoUserDto::active, active))

                if (administrator != null)
                    add(setValue(MongoUserDto::administrator, administrator))
            }

            if (updates.isEmpty())
                return@out // nothing to do

            collection.updateOneById(
                id,
                combine(updates),
            )

            cache.expire(this@Ref)
        }

        override suspend fun enable(): Outcome<User.Failures.SecurityEdit, Unit> = edit(active = true)

        override suspend fun disable(): Outcome<User.Failures.SecurityEdit, Unit> = edit(active = false)

        override suspend fun promote(): Outcome<User.Failures.SecurityEdit, Unit> = edit(administrator = true)

        override suspend fun demote(): Outcome<User.Failures.SecurityEdit, Unit> = edit(administrator = false)

        override suspend fun resetPassword(): Outcome<User.Failures.Edit, Password> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensureAdministrator { User.Failures.Unauthorized }

            val newPassword = Password(UUID.randomUUID().toString())

            collection.updateOneById(
                id,
                combine(
                    setValue(MongoUserDto::hashedPassword, hash(newPassword.value)),
                    setValue(MongoUserDto::singleUsePassword, true),
                    setValue(MongoUserDto::passwordWasUsed, false),
                    setValue(MongoUserDto::tokens, emptySet()),
                )
            )

            tokenCache.expire(this@Ref)
            cache.expire(this@Ref)

            newPassword
        }

        override suspend fun setPassword(oldPassword: String, newPassword: Password): Outcome<User.Failures.SetPassword, Unit> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensure(currentUser() == this@Ref) { User.Failures.CanOnlySetYourOwnPassword }

            val current = collection.findOneById(id)
            ensureNotNull(current) { User.Failures.NotFound(this@Ref) }

            ensure(checkHash(oldPassword, current.hashedPassword)) {
                log.warn(this@Ref) { "Blocked attempt to set the password, because the provided password doesn't match the user." }
                User.Failures.IncorrectPassword
            }

            collection.updateOneById(
                id,
                combine(
                    setValue(MongoUserDto::hashedPassword, hash(newPassword.value)),
                    setValue(MongoUserDto::singleUsePassword, false),
                    setValue(MongoUserDto::passwordWasUsed, false),
                    setValue(MongoUserDto::tokens, emptySet()),
                )
            )

            tokenCache.expire(this@Ref)
            cache.expire(this@Ref)
        }

        override suspend fun verifyToken(token: Token): Outcome<User.Failures.TokenVerification, Unit> = out {
            val tokens = tokenCache[this@Ref].now().bind()

            ensure(token.value in tokens) {
                log.warn(this@Ref) { "The token is invalid" }
                User.Failures.IncorrectCredentials
            }
        }

        override suspend fun logOut(token: Token): Outcome<User.Failures.Get, Unit> = out {
            ensureEmployee { User.Failures.Unauthenticated }
            ensure(this@Ref == currentUser()) { User.Failures.Unauthorized }

            collection.updateOneById(
                id,
                pull(MongoUserDto::tokens, token.value)
            )

            tokenCache.expire(this@Ref)
        }

        override fun request(): ProgressiveFlow<User.Failures.Get, User> = flow {
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

        override fun toString() = "MongoUsers.Ref($id)"
        override fun toIdentifier() = Identifier(id)

        // endregion
    }
}
