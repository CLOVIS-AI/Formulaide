package formulaide.api.types

/**
 * Elements in a list that requires an order.
 *
 * Lists in JSON are unordered. Implementations of this interface are ordered.
 */
interface OrderedListElement {

	/**
	 * An arbitrary integer that represents the position of this element in the list.
	 * For two elements in the list, the one with smallest `order` appears first in the list, and the one with the greater `order` appears last.
	 *
	 * Can be used to reorder the list, for example with:
	 * ```
	 * someList.sortedBy { it.order }
	 * ```
	 */
	val order: Int

	companion object {
		/**
		 * Internal function to check the validity of a list of [OrderedListElement].
		 * Should be called by the constructor of each implementation.
		 */
		fun List<OrderedListElement>.checkOrderValidity() {
			val cleaned = distinct()
			require(cleaned == this) { "Tous les éléments devraient avoir un ordre différent" }
		}
	}
}
