package formulaide.api.data

import kotlinx.serialization.Serializable

/**
 * A submission of a [Form].
 *
 * Unlike [CompoundData], [UnionDataField] and [Form], this data point is not recursive,
 * in order to make it easier to use. Instead, this is simply a [dictionary][Map]
 * of each value given by the user.
 *
 * ### Format
 *
 * The [data] dictionary is composed of:
 * - A key, that identifies which field is being submitted,
 * - A value, which is the user's response.
 *
 * The value is the JSON data corresponding to the field's [type][DataId].
 * A value can be `null` when:
 * - The field is optional ([DataList.maxArity] is 0)
 * - The field doesn't require filling (for example, [DataId.MESSAGE])
 *
 * When a [DataId.UNION] is used, the value should be the [AbstractFormField.id] of the selected element.
 *
 * The key is formed by grouping different IDs, separated by the character 'colon' (`:`).
 * The key is built using the following steps (the order is important):
 * - Starting at the top-level of the form, the first ID is the top-level's field ID ([FormField.id])
 * - If the field has a [maximum arity][DataList.maxArity] greater than 1, an arbitrary integer
 * (of your choice) is used to represent the index of the element
 * (repeat until the maximum arity is 1).
 * - If the field is a [compound type][DataId.COMPOUND], [FormFieldComponent.id] is added
 * (repeat from the previous step as long as necessary).
 *
 * ### Example
 *
 * In this example, all IDs are specifically chosen to be different if and only if
 * they refer to different objects, for readability reasons. This is not the case
 * in the real world, see the documentation of each ID. The example has been greatly simplified,
 * and is provided in YAML-like format for readability (this format is **not** accepted by the API,
 * this is only an illustration of the key-value system).
 *
 * Example data (real data is modeled by [CompoundData]):
 * ```yaml
 *   name: Identity
 *   id: 1
 *   fields:
 *     - Last name (id: 2, mandatory, TEXT)
 *     - First name (id: 3, mandatory, TEXT)
 *     - Phone number (id: 4, optional, NUMBER)
 *     - Family (id: 5, optional, COMPOUND Identity)
 * ```
 *
 * Example form (read forms are modeled by [Form]):
 * ```yaml
 *   name: Foo
 *   id: 6
 *   fields:
 *     - Requester (id: 7, mandatory, Identity)
 *       - Last name (id: 2, mandatory, TEXT)
 *       - First name (id: 3, mandatory, TEXT)
 *       - Phone number (id: 4, mandatory, NUMBER)
 *       - Family (id: 8, list, COMPOUND Identity)
 *         - Last name (id: 2, mandatory, TEXT)
 *         - First name (id: 3, mandatory, TEXT)
 *         - Phone number (id: 4, optional, NUMBER)
 *     - Preferred location (id: 9, mandatory, UNION)
 *       - By the sea (id: 10)
 *       - By the town hall (id: 11)
 *     - Comments (id: 12, optional, MESSAGE)
 * ```
 *
 * A valid submission for this form would be:
 * ```yaml
 * Example submission:
 *   form: 6
 *   data:
 *     "7:2": "My Last Name"
 *     "7:3": "My First Name"
 *     "7:4": "+33 1 23 45 67 89"
 *     "7:8:0:2": "My Brother's Last Name"
 *     "7:8:0:3": "My Brother's First Name"
 *     "7:8:0:4": null
 *     "7:8:1:2": "My Sister's Last Name"
 *     "7:8:1:3": "My Sister's First Name"
 *     "7:8:1:4": null
 *     "9": "10"
 *     "12": null
 * ```
 * Here, the user has a brother and sister, provided their own phone number but not their sibling's,
 * wants to live by the sea, and doesn't have any comments.
 *
 * @property form The [Form] this submission corresponds to.
 * @property data A dictionary of the user's responses to the data requested by the [Form].
 * See [FormSubmission] for the format description.
 */
@Serializable
data class FormSubmission(
	val form: FormId,
	val data: Map<String, String?>,
)
