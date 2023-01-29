package opensavvy.formulaide.core

import arrow.core.continuations.EffectScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import opensavvy.cache.PassThroughContext
import opensavvy.formulaide.core.Auth.Companion.currentAuth
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.state.Failure
import opensavvy.state.outcome.ensureAuthenticated
import opensavvy.state.outcome.ensureAuthorized
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Authentication information for the current user.
 *
 * This information is passed implicitly within the [CoroutineContext] to all functions in the application.
 * Because of this, authentication is only available in `suspend` functions.
 *
 * To authenticate the current function call, use [withContext]:
 * ```
 * withContext(Auth(User.Role.Employee, theirReference)) {
 *     // Inside this block of code, the user is authenticated as an Employee
 * }
 * ```
 *
 * To access the authentication information in your code, the accessor [currentAuth] is provided.
 */
data class Auth(
	/**
	 * The role of the current user.
	 *
	 * @see currentRole
	 */
	val role: User.Role,

	/**
	 * The current user.
	 *
	 * @see currentUser
	 */
	val user: User.Ref?,
) : AbstractCoroutineContextElement(Key), PassThroughContext {

	init {
		if (role >= User.Role.Employee)
			requireNotNull(user) { "Erreur interne : vous êtes authentifié comme un employé ou administrateur, mais votre identifiant d'utilisateur est manquant" }
	}

	object Key : CoroutineContext.Key<Auth>

	companion object {

		val Guest = Auth(role = User.Role.Guest, user = null)

		//region Accessors

		/**
		 * Accesses the authentication information for the current scope.
		 *
		 * If no authentication information is available, returns `null`.
		 */
		suspend fun currentAuthOrNull(): Auth? =
			currentCoroutineContext()[Key]

		/**
		 * Accesses the authentication information for the current scope.
		 *
		 * If no authentication information is available, returns [Guest].
		 *
		 * @see Auth
		 */
		suspend fun currentAuth(): Auth =
			currentAuthOrNull() ?: Guest

		/**
		 * Accesses the current user.
		 *
		 * @see Auth
		 * @see user
		 */
		suspend fun currentUser(): User.Ref? =
			currentAuth().user

		/**
		 * Accesses the role of the current user.
		 *
		 * If the current session is not authenticated, [User.Role.Guest] is returned.
		 *
		 * @see Auth
		 * @see role
		 */
		suspend fun currentRole(): User.Role =
			currentAuth().role

		//endregion

		/**
		 * Checks that the user has at least the role [User.Role.Employee].
		 */
		suspend fun EffectScope<Failure>.ensureEmployee(
			lazyMessage: () -> String = { "Seuls les employés ont accès à cette ressource" },
		) {
			ensureAuthenticated(currentRole() >= User.Role.Employee, lazyMessage)
		}

		/**
		 * Checks that the user has at least the role [User.Role.Administrator].
		 */
		suspend fun EffectScope<Failure>.ensureAdministrator(
			lazyMessage: () -> String = { "Seuls les administrateurs ont accès à cette ressource" },
		) {
			ensureEmployee(lazyMessage)
			ensureAuthorized(currentRole() >= User.Role.Administrator, lazyMessage)
		}

	}
}
