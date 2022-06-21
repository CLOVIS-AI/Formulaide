package formulaide.ui.utils

/**
 * Detects whether the code is currently running in tests.
 *
 * This is necessary because the webpack-produced executable calls [formulaide.ui.main],
 * and React function fail (no DOM in tests).
 */
fun detectTests(): Boolean {
	js( //language=JavaScript
		"""
		//noinspection JSUnresolvedVariable __karma__ is not known at compile-time
		if (typeof window.__karma__ !== "undefined") {
			return true;
		}
	"""
	)

	return false
}
