package formulaide.ui.components

import react.RBuilder
import react.useMemo

class LambdaStorage {
	val lambdas = HashMap<String, StoredLambda<*>>()
}

class StoredLambda<T>(var lambda: T, var dependencies: List<Any?>)

@Suppress("unused") // RBuilder to force the call to be in an FC
fun RBuilder.useLambdas() = useMemo { LambdaStorage() }

/**
 * Stores a lambda identified by a [key], and its [dependencies], to reuse the same reference if no dependency was modified.
 */
fun <T> T.memoIn(storage: LambdaStorage, key: String, vararg dependencies: Any?): T {
	val candidate = this
	val previous = storage.lambdas[key]

	if (previous == null || previous.dependencies != dependencies.asList()) {
		storage.lambdas[key] = StoredLambda(candidate, dependencies.asList())
		return candidate
	} else {
		@Suppress("UNCHECKED_CAST") // Cannot fail, we only insert valid values into storage
		return previous.lambda as T
	}
}
