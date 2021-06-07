package formulaide.server

import arrow.core.Either

sealed class Failure

object InvalidToken : Failure()

class DatabaseFailure(val failure: formulaide.db.Failure) : Failure() {

	companion object {
		fun <T> Either<formulaide.db.Failure, T>.asServerFailure() =
			mapLeft { DatabaseFailure(it) }
	}
}

class UnknownFailure(val message: String) : Failure()
