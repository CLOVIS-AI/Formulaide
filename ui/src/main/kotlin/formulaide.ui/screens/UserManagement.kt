package formulaide.ui.screens

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.NewUser
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.createUser
import formulaide.client.routes.editUser
import formulaide.client.routes.listUsers
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Text
import formulaide.ui.utils.replace
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.option

val UserList = FC<Props>("UserList") {
	traceRenders("UserList")

	val scope = useAsync()

	val (client) = useClient()
	require(client is Client.Authenticated) { "Seul un administrateur a le droit de voir la liste des utilisateurs" }
	val (me) = useUser()

	if (me == null) {
		styledCard("Employés", contents = { Text { text = "Récupération de l'utilisateur…" } })
		return@FC
	}

	var listDisabledUsers by useState(false)
	var users by useState(emptyList<User>())
	useEffect(client, listDisabledUsers) {
		scope.reportExceptions {
			users = client.listUsers(listDisabledUsers).sortedBy { it.fullName }
		}
	}

	styledCard(
		"Employés",
		null,
		"Créer un employé" to { navigateTo(Screen.NewUser) },
		loading = users.isEmpty(),
	) {
		styledField("hide-disabled", "Utilisateurs désactivés") {
			styledCheckbox("hide-disabled", "Afficher les comptes désactivés") {
				onChange = { listDisabledUsers = it.target.checked }
			}
		}

		for ((i, user) in users.withIndex()) {
			styledFormField {
				Text { text = user.fullName + " " }
				LightText { text = user.email.email }

				div { // buttons

					if (user != me) {
						StyledButton {
							text = if (user.enabled) "Désactiver" else "Activer"
							action = {
								editUser(user, client, enabled = !user.enabled) {
									users = users.replace(i, it)
								}
							}
						}

						StyledButton {
							text =
								if (user.administrator) "Enlever le droit d'administration" else "Donner le droit d'administration"
							action = {
								editUser(user, client, administrator = !user.administrator) {
									users = users.replace(i, it)
								}
							}
						}
					}

					StyledButton {
						text = "Modifier le mot de passe"
						action = {
							navigateTo(Screen.EditPassword(user.email, Screen.ShowUsers))
						}
					}
				}
			}
		}
	}
}

private suspend fun editUser(
	user: User,
	client: Client.Authenticated,
	enabled: Boolean? = null,
	administrator: Boolean? = null,
	onChange: (User) -> Unit,
) {
	val newUser = client.editUser(user, enabled, administrator)
	onChange(newUser)
}

val CreateUser = FC<Props>("CreateUser") {
	val services by useServices()

	val email = useRef<HTMLInputElement>()
	val fullName = useRef<HTMLInputElement>()
	var selectedService by useState(services.firstOrNull())
	val admin = useRef<HTMLInputElement>()
	val password1 = useRef<HTMLInputElement>()
	val password2 = useRef<HTMLInputElement>()

	useEffectOnce { admin.current?.value = "false" } // Ensure the default value is 'false'

	val (client) = useClient()
	require(client is Client.Authenticated) { "Un employé anonyme ne peut pas créer d'utilisateurs" }

	styledFormCard(
		"Ajouter un employé",
		null,
		"Créer" to {
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

			launch {
				client.createUser(NewUser(
					passwordOrFail,
					User(
						email = Email(emailOrFail),
						fullName = fullNameOrFail,
						service = Ref(serviceOrFail),
						administrator = adminOrFail.toBoolean()
					)
				))

				navigateTo(Screen.ShowUsers)
			}
		}
	) {
		styledField("employee-email", "Adresse mail") {
			styledInput(InputType.email,
			            "employee-email",
			            required = true,
			            ref = email)
		}

		styledField("employee-name", "Nom affiché") {
			styledInput(InputType.text, "employee-name", required = true, ref = fullName)
		}

		styledField("employee-service", "Service") {
			styledSelect(onSelect = { option ->
				selectedService = services.find { it.id == option.value }
			}) {
				for (service in services) {
					option {
						Text { text = service.name }
						value = service.id
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
				minLength = 5
			}
		}

		styledField("employee-password-2", "Confirmer le mot de passe") {
			styledInput(InputType.password,
			            "employee-password-2",
			            required = true,
			            ref = password2) {
				minLength = 5
			}
		}
	}
}
