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
fun SignupScreen(
    onOtpSent: (email: String, devOtpHint: String?) -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        error = null
        loading = true
        scope.launch {
            try {
                val res = PortalApiClient.signup(fullName, businessName, email, phone)
                onOtpSent(
                    res.optString("email", email.trim()),
                    res.optString("devOtpHint").takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) {
                error = (e as? DfsApiException)?.message ?: (e.message ?: "Signup failed")
            } finally {
                loading = false
            }
        }
    }

    DfsScreen {
        DfsAnimatedSection {
            DfsBrandMark(compact = true)
            Spacer(modifier = Modifier.height(14.dp))
            DfsStatusBadge(label = "Signup")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Create corporate account",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "We will email a one-time code to verify your address.",
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
            FieldLabel("Authorized person full name")
            AppTextField(fullName, { fullName = it }, "Full name")
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Legal / business name")
            AppTextField(businessName, { businessName = it }, "Business name")
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Email")
            AppTextField(email, { email = it }, "you@company.com", KeyboardType.Email)
            Spacer(modifier = Modifier.height(12.dp))
            FieldLabel("Mobile (KYC user ID)")
            AppTextField(phone, { phone = it }, "03XXXXXXXXX", KeyboardType.Phone)
            Spacer(modifier = Modifier.height(18.dp))
            DfsPrimaryButton(
                text = if (loading) "Sending OTP…" else "Send OTP",
                onClick = { submit() },
                enabled = !loading && fullName.isNotBlank() && businessName.isNotBlank()
                    && email.isNotBlank() && phone.isNotBlank()
            )
            Spacer(modifier = Modifier.height(10.dp))
            DfsSecondaryButton(text = "Back", onClick = onBack, enabled = !loading)
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        color = DfsColors.OnBackground,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
}
