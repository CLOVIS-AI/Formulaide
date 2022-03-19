package formulaide.ui.utils

import csstype.ClassName
import react.dom.DOMAttributes

/**
 * Shorthand to set the [className][DOMAttributes.className].
 */
var DOMAttributes<*>.classes: String?
	get() = className?.toString()
	set(value) {
		className = if (value != null) ClassName(value) else null
	}
