package com.example.kycapp.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kycapp.dfs.DfsApiException
import com.example.kycapp.dfs.DfsSession
import com.example.kycapp.portal.PortalApiClient
import com.example.kycapp.portal.PortalSession
import com.example.kycapp.ui.components.AppTextField
import com.example.kycapp.ui.components.DfsAnimatedSection
import com.example.kycapp.ui.components.DfsBrandMark
import com.example.kycapp.ui.components.DfsCard
import com.example.kycapp.ui.components.DfsPrimaryButton
import com.example.kycapp.ui.components.DfsScreen
import com.example.kycapp.ui.components.DfsSecondaryButton
import com.example.kycapp.ui.components.DfsStatusBadge
import com.example.kycapp.ui.theme.DfsColors
import kotlinx.coroutines.launch

/**
 * Email + password login after entity submit (portal credentials).
 * Routes to onboarding (DRAFT/REJECTED/INCOMPLETE) or personal KYC when submitted.
 */
@Composable
fun PortalLoginScreen(
    onNeedOnboarding: (PortalSession) -> Unit,
    onPersonalKyc: (DfsSession) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun login() {
        error = null
        loading = true
        scope.launch {
            try {
                val portal = PortalApiClient.portalLogin(email, password)
                val status = portal.partyStatus.uppercase()
                when (status) {
                    "DRAFT", "REJECTED", "INCOMPLETE" -> onNeedOnboarding(portal)
                    "SUBMITTED", "PENDING_APPROVAL", "ACTIVE" -> {
                        try {
                            onPersonalKyc(PortalApiClient.startPersonalKyc(portal.token))
                        } catch (_: Exception) {
                            // Lead may not have a personal KYC row — open onboarding/status instead
                            onNeedOnboarding(portal)
                        }
                    }
                    else -> onNeedOnboarding(portal)
                }
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Login failed")
            } finally {
                loading = false
            }
        }
    }

    DfsScreen {
        DfsAnimatedSection {
            DfsBrandMark(compact = true)
            Spacer(modifier = Modifier.height(14.dp))
            DfsStatusBadge(label = "Login")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Corporate login",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Use the email + temporary password emailed after you submit onboarding.",
                color = DfsColors.MutedText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (error != null) {
            DfsCard {
                Text(text = error!!, color = DfsColors.Danger, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        DfsCard {
            FieldLabel("Email")
            AppTextField(email, { email = it }, "you@company.com", KeyboardType.Email)
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Password")
            AppTextField(password, { password = it }, "Password", password = true)
            Spacer(modifier = Modifier.height(18.dp))
            DfsPrimaryButton(
                text = if (loading) "Signing in…" else "Continue",
                onClick = { login() },
                enabled = !loading && email.isNotBlank() && password.isNotBlank()
            )
            Spacer(modifier = Modifier.height(10.dp))
            DfsSecondaryButton(text = "Back", onClick = onBack, enabled = !loading)
        }
    }
}
