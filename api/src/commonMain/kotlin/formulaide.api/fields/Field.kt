package formulaide.api.fields

import formulaide.api.types.Arity

interface Referencable {
	val id: Int
}

interface Field : Referencable {
	val name: String
	val arity: Arity

	interface Simple : Field {
		val simple: SimpleField

		override val arity get() = simple.arity
	}

	interface Union<F: Field> : Field {
		val options: List<F>
	}

	interface Container<F: Field> : Field {
		val fields: List<F>
	}

	interface Reference<R : Referencable> : Field {
		val ref: R
	}
}
