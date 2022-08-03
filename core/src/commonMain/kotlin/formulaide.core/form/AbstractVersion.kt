package formulaide.core.form

import formulaide.core.field.FlatField
import kotlinx.datetime.Instant

/**
 * A version of a [Form] or [Template].
 */
abstract class AbstractVersion {
	abstract val creationDate: Instant

	/**
	 * The name of this version.
	 *
	 * This is similar to a commit message.
	 */
	abstract val title: String

	abstract val fields: FlatField.Container.Ref
}
