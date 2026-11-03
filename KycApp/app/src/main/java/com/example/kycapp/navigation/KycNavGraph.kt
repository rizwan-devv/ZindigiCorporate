package com.example.kycapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kycapp.camera.CameraCaptureScreen
import com.example.kycapp.dfs.DfsSession
import com.example.kycapp.dfs.DfsSessionStore
import com.example.kycapp.portal.PortalSession
import com.example.kycapp.portal.PortalSessionStore
import com.example.kycapp.ui.screens.ActionIntroScreen
import com.example.kycapp.ui.screens.FingerprintIntroScreen
import com.example.kycapp.ui.screens.FingerprintNameScreen
import com.example.kycapp.ui.screens.FingerprintRecordViewerScreen
import com.example.kycapp.ui.screens.FingerprintTutorialScreen
import com.example.kycapp.ui.screens.LoginScreen
import com.example.kycapp.ui.screens.MainMenuScreen
import com.example.kycapp.ui.screens.OnboardingScreen
import com.example.kycapp.ui.screens.PortalLoginScreen
import com.example.kycapp.ui.screens.SignupScreen
import com.example.kycapp.ui.screens.SignatureCaptureScreen
import com.example.kycapp.ui.screens.VerifyOtpScreen
import com.example.kycapp.ui.screens.WelcomeScreen

@Composable
fun KycNavGraph(
    navController: NavHostController,
    inviteToken: String? = null,
    onInviteConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val kycStore = remember { DfsSessionStore(context) }
    val portalStore = remember { PortalSessionStore(context) }
    var session by remember { mutableStateOf(kycStore.load()) }
    var portal by remember { mutableStateOf(portalStore.load()) }
    var pendingInvite by remember { mutableStateOf(inviteToken) }
    var otpEmail by remember { mutableStateOf("") }
    var otpHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inviteToken) {
        if (!inviteToken.isNullOrBlank()) {
            pendingInvite = inviteToken
            session = null
            kycStore.clear()
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.WELCOME) { inclusive = false }
            }
        }
    }

    val start = when {
        session != null -> Routes.MAIN_MENU
        portal != null -> Routes.ONBOARDING
        else -> Routes.WELCOME
    }

    fun goPersonalKyc(s: DfsSession) {
        kycStore.save(s)
        session = s
        navController.navigate(Routes.MAIN_MENU) {
            popUpTo(Routes.WELCOME) { inclusive = true }
        }
    }

    fun goOnboarding(p: PortalSession) {
        portalStore.save(p)
        portal = p
        navController.navigate(Routes.ONBOARDING) {
            popUpTo(Routes.WELCOME) { inclusive = false }
        }
    }

    fun clearAllAuth() {
        kycStore.clear()
        portalStore.clear()
        session = null
        portal = null
    }

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onSignup = { navController.navigate(Routes.SIGNUP) },
                onPortalLogin = { navController.navigate(Routes.PORTAL_LOGIN) },
                onPartnerKycLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onOtpSent = { email, hint ->
                    otpEmail = email
                    otpHint = hint
                    navController.navigate(Routes.VERIFY_OTP)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.VERIFY_OTP) {
            VerifyOtpScreen(
                email = otpEmail,
                devOtpHint = otpHint,
                onVerified = { p -> goOnboarding(p) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PORTAL_LOGIN) {
            PortalLoginScreen(
                onNeedOnboarding = { p -> goOnboarding(p) },
                onPersonalKyc = { s -> goPersonalKyc(s) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ONBOARDING) {
            val p = portal
            if (p == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
                return@composable
            }
            OnboardingScreen(
                portal = p,
                onPersonalKycReady = { s -> goPersonalKyc(s) },
                onSignOut = {
                    clearAllAuth()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                inviteToken = pendingInvite,
                onLoggedIn = { s ->
                    kycStore.save(s)
                    session = s
                    pendingInvite = null
                    onInviteConsumed()
                    navController.navigate(Routes.MAIN_MENU) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN_MENU) {
            if (session == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.MAIN_MENU) { inclusive = true }
                    }
                }
                return@composable
            }
            MainMenuScreen(
                session = session,
                onIdCardClick = { navController.navigate(Routes.ID_CARD_INTRO) },
                onOcrClick = { navController.navigate(Routes.OCR_INTRO) },
                onFaceMatchClick = { navController.navigate(Routes.FACE_MATCH_INTRO) },
                onFingerprintClick = { navController.navigate(Routes.FINGERPRINT_INTRO) },
                onFingerprintRecordsClick = { navController.navigate(Routes.fingerprintRecords()) },
                onSignatureClick = { navController.navigate(Routes.SIGNATURE) },
                onSessionUpdated = { updated: DfsSession ->
                    kycStore.save(updated)
                    session = updated
                },
                onSignOut = {
                    clearAllAuth()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.MAIN_MENU) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNATURE) {
            val s = session
            if (s == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.SIGNATURE) { inclusive = true }
                    }
                }
                return@composable
            }
            SignatureCaptureScreen(
                session = s,
                onDone = { updated ->
                    kycStore.save(updated)
                    session = updated
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ID_CARD_INTRO) {
            ActionIntroScreen(
                title = "ID Card Detection",
                buttonLabel = "Start Scanning",
                onStartClick = {
                    navController.navigate(Routes.camera(CaptureMode.ID_CARD.name))
                }
            )
        }

        composable(Routes.OCR_INTRO) {
            ActionIntroScreen(
                title = "Perform OCR",
                buttonLabel = "Start OCR",
                onStartClick = {
                    navController.navigate(Routes.camera(CaptureMode.OCR.name))
                }
            )
        }

        composable(Routes.FACE_MATCH_INTRO) {
            ActionIntroScreen(
                title = "Face Matching",
                buttonLabel = "Start Face Scan",
                onStartClick = {
                    navController.navigate(Routes.camera(CaptureMode.FACE_MATCH.name))
                }
            )
        }

        composable(Routes.FINGERPRINT_INTRO) {
            FingerprintIntroScreen(
                onEnrollClick = {
                    navController.navigate(Routes.FINGERPRINT_NAME_ENTRY)
                },
                onMatchClick = { hand ->
                    navController.navigate(
                        Routes.fingerprintTutorial(CaptureMode.FINGERPRINT_MATCH.name, hand.name)
                    )
                },
                onViewRecordsClick = {
                    navController.navigate(Routes.fingerprintRecords())
                }
            )
        }

        composable(
            route = Routes.FINGERPRINT_RECORDS,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = Routes.decodeName(backStackEntry.arguments?.getString("name"))
            FingerprintRecordViewerScreen(
                personNameFilter = name,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FINGERPRINT_NAME_ENTRY) {
            FingerprintNameScreen(
                onContinue = { name ->
                    navController.navigate(
                        Routes.fingerprintTutorial(CaptureMode.FINGERPRINT_ENROLL.name, HandSide.LEFT.name, name)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FINGERPRINT_TUTORIAL,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("hand") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val mode = CaptureMode.fromRoute(backStackEntry.arguments?.getString("mode") ?: "")
            val hand = HandSide.fromRoute(backStackEntry.arguments?.getString("hand") ?: "")
            val name = Routes.decodeName(backStackEntry.arguments?.getString("name"))

            FingerprintTutorialScreen(
                hand = hand,
                onScanClick = { navController.navigate(Routes.camera(mode.name, hand.name, name)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("hand") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val mode = CaptureMode.fromRoute(backStackEntry.arguments?.getString("mode") ?: "")
            val hand = HandSide.fromRoute(backStackEntry.arguments?.getString("hand") ?: "")
            val name = Routes.decodeName(backStackEntry.arguments?.getString("name"))

            CameraCaptureScreen(
                mode = mode,
                hand = hand,
                personName = name,
                onBack = { navController.popBackStack() },
                onScanSuccess = {
                    if (mode == CaptureMode.FINGERPRINT_ENROLL && hand == HandSide.LEFT) {
                        navController.navigate(
                            Routes.fingerprintTutorial(CaptureMode.FINGERPRINT_ENROLL.name, HandSide.RIGHT.name, name)
                        ) {
                            popUpTo(Routes.FINGERPRINT_NAME_ENTRY)
                        }
                    } else if (mode == CaptureMode.FINGERPRINT_ENROLL) {
                        navController.navigate(Routes.fingerprintRecords(name)) {
                            popUpTo(Routes.MAIN_MENU)
                        }
                    } else {
                        navController.navigate(Routes.MAIN_MENU) {
                            popUpTo(Routes.MAIN_MENU) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
