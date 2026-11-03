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

@Composable
fun VerifyOtpScreen(
    email: String,
    devOtpHint: String?,
    onVerified: (PortalSession) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf(devOtpHint.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun verify() {
        error = null
        loading = true
        scope.launch {
            try {
                onVerified(PortalApiClient.verifyOtp(email, code))
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "OTP failed")
            } finally {
                loading = false
            }
        }
    }

    DfsScreen {
        DfsAnimatedSection {
            DfsBrandMark(compact = true)
            Spacer(modifier = Modifier.height(14.dp))
            DfsStatusBadge(label = "Verify email")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Enter OTP",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sent to $email",
                color = DfsColors.MutedText,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (error != null) {
            DfsCard {
                Text(text = error!!, color = DfsColors.Danger, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!devOtpHint.isNullOrBlank()) {
            DfsCard {
                Text(
                    text = "Dev OTP (mail fallback): $devOtpHint",
                    color = DfsColors.Warning,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        DfsCard {
            FieldLabel("6-digit code")
            AppTextField(code, { code = it }, "123456", KeyboardType.Number)
            Spacer(modifier = Modifier.height(18.dp))
            DfsPrimaryButton(
                text = if (loading) "Verifying…" else "Continue",
                onClick = { verify() },
                enabled = !loading && code.length >= 4
            )
            Spacer(modifier = Modifier.height(10.dp))
            DfsSecondaryButton(text = "Back", onClick = onBack, enabled = !loading)
        }
    }
}
