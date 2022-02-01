package formulaide.ui.utils

import formulaide.ui.reportExceptions
import kotlinx.browser.document
import kotlinx.dom.createElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.asList

/**
 * Selects an element by its [elementId], copies it to an invisible `iframe`, and asks the browser to print it.
 */
fun printElement(elementId: String) = reportExceptions {
	val contents = document.getElementById(elementId)
		?: error("L'élément $elementId est introuvable, cela ne devrait pas être possible.")

	val frame = document.getElementById("ifmcontentstoprint")
		?: error("Impossible de trouver la iframe 'ifmcontentstoprint'.")

	val iframe = frame as? HTMLIFrameElement
		?: error("L'iframe a un type incorrect")

	val window = iframe.contentWindow
		?: error("Impossible d'ouvrir la fenêtre de l'iframe.")

	with(window.document) {
		open()
		write("")
		appendChild(createElement("html") {
			appendChild(createElement("head") {
				for (style in document.querySelectorAll("style").asList()) {
					appendChild(createElement("style") {
						innerHTML = (style as Element).innerHTML
					})
				}
			})
			appendChild(createElement("body") {
				innerHTML = contents.innerHTML
			})
		})
		close()
		console.log("Temporary document created for printing", this)
	}

	with(window) {
		focus()
		print()
	}
}
