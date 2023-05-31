package opensavvy.formulaide.remote

import kotlinx.serialization.json.Json

val ApiJson = Json {
	encodeDefaults = false

	@Suppress("OPT_IN_USAGE")
	explicitNulls = false
}
