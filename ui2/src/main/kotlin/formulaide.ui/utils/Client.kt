package formulaide.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import opensavvy.formulaide.api.client.Client
import opensavvy.formulaide.core.User

val Client.user: User.Ref?
	@Composable get() = context.collectAsState().value.user

val Client.role: User.Role
	@Composable get() = context.collectAsState().value.role
