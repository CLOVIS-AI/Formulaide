package formulaide.db

sealed class Failure

//region Internal failures

/**
 * A failure due to an internal state issue, that should *not* be communicated to the end-user.
 *
 * If this failure happens, the correct course of action is to return a 500 "internal server error".
 */
sealed class InternalFailure : Failure()

/**
 * A global failure case for errors that do not deserve their own failure class.
 */
data class UnknownFailure internal constructor(val message: String) : InternalFailure()

//endregion
//region Public failures

/**
 * A failure that can be communicated to the end-user, such has "This email already exists in the database", constraint violations, etc.
 */
sealed class PublicFailure : Failure()

//endregion
