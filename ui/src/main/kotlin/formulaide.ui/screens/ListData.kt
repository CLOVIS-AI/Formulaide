package formulaide.ui.screens

import formulaide.ui.*
import formulaide.ui.Role.Companion.role
import formulaide.ui.components.styledButton
import formulaide.ui.components.styledCard
import formulaide.ui.components.styledField
import react.RProps
import react.fc

val DataList = fc<RProps> {
	traceRenders("DataList")

	val user by useUser()
	val composites by useComposites()

	styledCard(
		"Groupes",
		null,
		contents = {
			for (composite in composites) {
				styledField("composite-${composite.id}", composite.name) {
					if (user.role >= Role.ADMINISTRATOR)
						styledButton("Copier", action = { navigateTo(Screen.NewData(composite)) })
				}
			}
		}
	)

}
