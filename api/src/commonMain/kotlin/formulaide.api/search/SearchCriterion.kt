package formulaide.api.search

import formulaide.api.data.FormSubmission
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API Object to represent a filter for a query.
 *
 * Each criterion applies to a [field][fieldKey].
 * Most criterion also store additional data.
 */
@Serializable
sealed class SearchCriterion<F : FormField> {

	/**
	 * The [FormField] this criterion applies to.
	 *
	 * This key represents the path from the [FormRoot] to the field we want to mention;
	 * each traversed field's [id][FormField.id] is used, separated by semi-colons (`:`).
	 *
	 * Unlike [FormSubmission], this key doesn't have any specific syntax for fields with [arity][FormField.arity] greater than 1.
	 * They are treated as normal fields.
	 *
	 * Example:
	 * ```text
	 * FormRoot:          key
	 *   1. Simple text   "1"
	 *   2. Simple text   "2"
	 *   3. Union         "3"
	 *      1. Option 1   "3:1"
	 *      2. Option 2   "3:2"
	 *   4. Composite     "4"
	 *      1. Field 1    "4:1"
	 *      2. Field 2    "4:2"
	 * ```
	 */
	abstract val fieldKey: String

	/**
	 * Accepts elements that come after [min] (included) in natural ordering.
	 */
	@Serializable
	@SerialName("AFTER")
	data class OrderAfter(
		override val fieldKey: String,
		val min: String,
	) : SearchCriterion<FormField.Simple>()

	/**
	 * Accepts elements that come before [max] (included) in natural ordering.
	 */
	@Serializable
	@SerialName("BEFORE")
	data class OrderBefore(
		override val fieldKey: String,
		val max: String,
	) : SearchCriterion<FormField.Simple>()

	/**
	 * Accepts answers that contain [text] (ignoring case).
	 */
	@Serializable
	@SerialName("CONTAINS")
	data class TextContains(
		override val fieldKey: String,
		val text: String,
	) : SearchCriterion<FormField.Simple>()

	/**
	 * Accepts answers that are exactly [text].
	 */
	@Serializable
	@SerialName("EQUALS")
	data class TextEquals(
		override val fieldKey: String,
		val text: String,
	) : SearchCriterion<FormField.Simple>()

	/**
	 * Accepts fields that have been answered (eg. excludes optional fields that the user didn't provide an answer to).
	 */
	@Serializable
	@SerialName("EXISTS")
	data class Exists(
		override val fieldKey: String,
	) : SearchCriterion<FormField>()
}
