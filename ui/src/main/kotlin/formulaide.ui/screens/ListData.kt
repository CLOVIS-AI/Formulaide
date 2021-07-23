package formulaide.ui.screens

import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import formulaide.ui.traceRenders
import formulaide.ui.useComposites
import react.RProps
import react.fc

val DataList = fc<RProps> {
	traceRenders("DataList")

	val composites by useComposites()

	styledCard(
		"Groupes",
		null,
		contents = {
			for (composite in composites) {
				styledField("composite-${composite.id}", composite.name) {
					// Buttons
				}
			}
		}
	)

}
