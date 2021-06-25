package formulaide.api.fields

import formulaide.api.fields.Field.*
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Referencable

/**
 * Some data that can be requested of a user.
 *
 * There are four types of fields:
 * - Simple fields (see [Simple], [SimpleField])
 * - Lists (represented implicitly by [arity])
 * - Composites (see [Container], [Reference])
 * - Unions (see [Union]).
 *
 * The types 'simple field', 'composite' and 'union' are distinct: no field can be multiple of those at the same time.
 * However, any field can also be a list.
 * In practice, this means that an implementation must extend at most one of [Simple], [Union] and [Container].
 *
 * Since composites and unions can be recursive, fields appear as trees.
 * Depending on the root of the tree, two possible configurations exist:
 * see [DataRoot] and [FormRoot] for more information.
 */
interface Field : Referencable {

	/**
	 * The name of this field (as displayed on screen to the user).
	 */
	val name: String

	/**
	 * The number of answers accepted by this field.
	 */
	val arity: Arity

	/**
	 * The order in which this field should appear inside its parent container.
	 *
	 * Because JSON lists are not ordered, this can be used to ensure the order was not lost.
	 */
	val order: Int

	/**
	 * A field that represents a single data entry.
	 *
	 * Simple fields:
	 * - can never have children,
	 * - can have additional properties (see [simple]),
	 * - do not handle their own [arity] (it is handled by [simple]).
	 */
	interface Simple : Field {
		val simple: SimpleField

		/**
		 * The number of answers accepted by this [SimpleField].
		 *
		 * @see Field.arity
		 * @see SimpleField.arity
		 */
		override val arity get() = simple.arity
	}

	/**
	 * A field that allows the user to choose between multiple [options].
	 */
	interface Union<F: Field> : Field {

		/**
		 * The different options that this [Union] allows.
		 *
		 * The user must choose a single one (no matter their [arity]).
		 */
		val options: List<F>
	}

	/**
	 * A field that [contains][fields] multiple other fields that the user must fill in.
	 */
	interface Container<F: Field> : Field {

		/**
		 * The different fields contained by this [Container].
		 *
		 * The user must fill in all of them (depending on their [arity]).
		 */
		val fields: List<F>
	}

	/**
	 * A field that is somehow linked to another field.
	 *
	 * See the different implementations for more information.
	 */
	interface Reference<R : Referencable> : Field {
		val ref: Ref<R>
	}
}
