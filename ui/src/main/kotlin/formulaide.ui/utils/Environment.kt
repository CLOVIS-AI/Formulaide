package formulaide.ui.utils

import formulaide.client.Client
import kotlinx.browser.window

internal var inProduction = true
internal val defaultClient
	get() = when (inProduction) {
		true -> Client.Anonymous.connect(window.location.protocol + "//" + window.location.host)
		false -> Client.Anonymous.connect("http://localhost:8000")
	}
