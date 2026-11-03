package com.example.kycapp.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kycapp.BuildConfig
import com.example.kycapp.ui.components.DfsAnimatedSection
import com.example.kycapp.ui.components.DfsBrandMark
import com.example.kycapp.ui.components.DfsCard
import com.example.kycapp.ui.components.DfsPrimaryButton
import com.example.kycapp.ui.components.DfsScreen
import com.example.kycapp.ui.components.DfsSecondaryButton
import com.example.kycapp.ui.components.DfsStatusBadge
import com.example.kycapp.ui.theme.DfsColors

@Composable
fun WelcomeScreen(
    onSignup: () -> Unit,
    onPortalLogin: () -> Unit,
    onPartnerKycLogin: () -> Unit
) {
    DfsScreen {
        DfsAnimatedSection {
            DfsBrandMark()
            Spacer(modifier = Modifier.height(16.dp))
            DfsStatusBadge(label = "Corporate app")
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Onboard & verify",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Register your entity, upload documents, then complete personal KYC — all in this app.",
                color = DfsColors.MutedText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        DfsCard {
            Text(
                text = "New corporate applicant",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Signup → OTP → entity details → documents → submit → personal KYC",
                color = DfsColors.MutedText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            DfsPrimaryButton(text = "Sign up", onClick = onSignup)
            Spacer(modifier = Modifier.height(10.dp))
            DfsSecondaryButton(text = "Login (email + password)", onClick = onPortalLogin)
        }

        Spacer(modifier = Modifier.height(14.dp))

        DfsCard {
            Text(
                text = "Partner / invite KYC",
                color = DfsColors.OnBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Already invited? Use your deep link or phone + temporary PIN.",
                color = DfsColors.MutedText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            DfsSecondaryButton(text = "Partner KYC login", onClick = onPartnerKycLogin)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "API: ${BuildConfig.DFS_API_BASE}",
                color = DfsColors.MutedText,
                fontSize = 11.sp
            )
        }
    }
}
