/*
 * Bindings for https://www.npmjs.com/package/use-error-boundary
 * Generated by Dukat, then edited by hand
 */

package formulaide.ui.utils

import react.ReactElement

typealias UseErrorBoundaryWrapper = (props: ErrorBoundaryProps) -> ReactElement<ErrorBoundaryProps>
typealias OnDidCatchCallback = (error: Any, errorInfo: Any) -> Unit

//region Kotlin extensions

operator fun UseErrorBoundaryState.component1() = ErrorBoundary
operator fun UseErrorBoundaryState.component2() = didCatch
operator fun UseErrorBoundaryState.component3() = error

//endregion