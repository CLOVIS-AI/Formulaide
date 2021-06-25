package formulaide.api.fields

import formulaide.api.types.Arity
import kotlinx.serialization.Serializable
import kotlin.collections.List as KotlinList

interface Field {
	val id: Int

	//region Behavior interfaces

	interface Named : Field {
		val name: String
	}

	interface List : Field {
		val arity: Arity
	}

	interface Container<F : Contained> : Field {
		val fields: KotlinList<F>

		@Serializable
		data class TopLevel<F : Contained>(
			override val id: Int,
			override val fields: KotlinList<F>,
		) : Container<F>
	}

	interface Contained : Field {
		val order: Int
	}

	interface Union<F : Contained> : Field {
		val options: KotlinList<F>
	}

}
