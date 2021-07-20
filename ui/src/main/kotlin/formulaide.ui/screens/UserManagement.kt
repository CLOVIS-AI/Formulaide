package formulaide.ui.screens

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.NewUser
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.createUser
import formulaide.client.routes.editUser
import formulaide.client.routes.listUsers
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.*
import formulaide.ui.launchAndReportExceptions
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.dom.attrs
import react.dom.div
import react.dom.option
import react.fc
import react.useEffect
import react.useRef
import react.useState

val UserList = fc<ScreenProps> { props ->
	styledCard(
		"Employés",
		null,
		"Créer un employé" to { props.navigateTo(Screen.NewUser) }
	) {
		var listDisabledUsers by useState(false)

		styledField("hide-disabled", "Utilisateurs désactivés") {
			styledCheckbox("hide-disabled", "Afficher les comptes désactivés") {
				onChangeFunction = { listDisabledUsers = (it.target as HTMLInputElement).checked }
			}
		}

		var users by useState(emptyList<User>())
		useEffect(props.user, props.client, listDisabledUsers) {
			launchAndReportExceptions(props) {
				val client = props.client
				require(client is Client.Authenticated) { "Seul un administrateur a le droit de voir la liste des utilisateurs" }

				users = client.listUsers(listDisabledUsers)
			}
		}

		for ((i, user) in users.withIndex()) {
			styledFormField {
				text(user.fullName + " ")
				styledLightText(user.email.email)

				div { // buttons

					if (user != props.user) {
						styledButton(if (user.enabled) "Désactiver" else "Activer",
						             default = false) {
							editUser(user, props, enabled = !user.enabled) {
								users = users.replace(i, it)
							}
						}

						styledButton(if (user.administrator) "Enlever le droit d'administration" else "Donner le droit d'administration",
						             default = false) {
							editUser(user, props, administrator = !user.administrator) {
								users = users.replace(i, it)
							}
						}
					}

					styledButton("Modifier le mot de passe") {
						launchAndReportExceptions(props) {
							props.navigateTo(Screen.EditPassword(user.email, Screen.ShowUsers))
						}
					}

				}
			}
		}
	}
}

private fun editUser(
	user: User,
	props: ScreenProps,
	enabled: Boolean? = null,
	administrator: Boolean? = null,
	onChange: (User) -> Unit,
) {
	launchAndReportExceptions(props) {
		val client = props.client
		require(client is Client.Authenticated) { "Seul un administrateur peut activer ou désactiver un compte" }

		val newUser = client.editUser(user, enabled, administrator)
		onChange(newUser)
	}
}

val CreateUser = fc<ScreenProps> { props ->
	val email = useRef<HTMLInputElement>()
	val fullName = useRef<HTMLInputElement>()
	val (selectedService, setSelectedService) = useState(props.services.firstOrNull())
	val admin = useRef<HTMLInputElement>()
	val password1 = useRef<HTMLInputElement>()
	val password2 = useRef<HTMLInputElement>()

	val client = props.client
	require(client is Client.Authenticated) { "Un employé anonyme ne peut pas créer d'utilisateurs" }

	styledFormCard(
		"Ajouter un employé",
		null,
		"Créer",
		contents = {
			styledField("employee-email", "Adresse mail") {
				styledInput(InputType.email, "employee-email", required = true, ref = email)
			}

			styledField("employee-name", "Nom affiché") {
				styledInput(InputType.text, "employee-name", required = true, ref = fullName)
			}

			styledField("employee-service", "Service") {
				styledSelect(onSelect = { option -> setSelectedService(props.services.find { it.id == option.value }) }) {
					for (service in props.services) {
						option {
							text(service.name)
							attrs {
								value = service.id
							}
						}
					}
				}
			}

			styledField("employee-is-admin", "Droits") {
				styledCheckbox("employee-is-admin",
				               "Cet employé est un administrateur",
				               ref = admin)
			}

			styledField("employee-password-1", "Mot de passe") {
				styledInput(InputType.password,
				            "employee-password-1",
				            required = true,
				            ref = password1) {
					minLength = "5"
				}
			}

			styledField("employee-password-2", "Confirmer le mot de passe") {
				styledInput(InputType.password,
				            "employee-password-2",
				            required = true,
				            ref = password2) {
					minLength = "5"
				}
			}
		}
	) {
		onSubmitFunction = {
			it.preventDefault()

			launchAndReportExceptions(props) {
				val password1Value = password1.current?.value
				val password2Value = password2.current?.value

				require((password1Value
					?: Unit) == password2Value) { "Les deux mots de passes ne correspondent pas." }

				val passwordOrFail = (password1Value
					?: error("Le mot de passe ne peut pas être vide, trouvé $password1"))

				val emailOrFail = (email.current?.value
					?: error("L'adresse mail ne peut pas être vide, trouvé $email"))

				val fullNameOrFail = (fullName.current?.value
					?: error("Le nom ne peut pas être vide, trouvé $fullName"))

				val serviceOrFail = (selectedService?.id
					?: error("L'utilisateur doit avoir choisi un service, trouvé $selectedService"))

				val adminOrFail = admin.current?.value
					?: error("Il faut préciser si l'utilisateur est un administrateur ou non, trouvé $admin")

				client.createUser(NewUser(
					passwordOrFail,
					User(
						email = Email(emailOrFail),
						fullName = fullNameOrFail,
						service = Ref(serviceOrFail),
						administrator = adminOrFail == "on"
					)
				))

				props.navigateTo(Screen.ShowUsers)
			}
		}
	}
}
