package com.hanyz.stopme.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hanyz.stopme.data.AuthRepository
import com.hanyz.stopme.data.OnboardingPrefs
import com.hanyz.stopme.model.TransportServiceType
import com.hanyz.stopme.service.TrackingService
import com.hanyz.stopme.ui.auth.AuthScreen
import com.hanyz.stopme.ui.auth.AuthViewModel
import com.hanyz.stopme.ui.permission.PermissionScreen
import com.hanyz.stopme.ui.permission.PermissionViewModel
import com.hanyz.stopme.ui.planner.TripPlannerScreen
import com.hanyz.stopme.ui.planner.TripPlannerViewModel
import com.hanyz.stopme.ui.splash.SplashScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Layar 1: Splash
        composable(Screen.Splash.route) {
            SplashScreen(
                onStartClick = {
                    if (AuthRepository.isUserLoggedIn()) {
                        // Layar izin hanya muncul pertama kali
                        val next = if (OnboardingPrefs.isPermissionOnboardingDone(context)) {
                            Screen.MainContainer.route
                        } else {
                            Screen.Permission.route
                        }
                        navController.navigate(next) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Layar 2: Registrasi
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel()
            val authUiState by authViewModel.uiState.collectAsState()

            // Jalur cadangan: login Google metode lama bila Credential Manager gagal
            val legacyGoogleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                authViewModel.onLegacyGoogleResult(result.data)
            }

            androidx.compose.runtime.LaunchedEffect(authUiState.requestLegacyGoogleSignIn) {
                if (authUiState.requestLegacyGoogleSignIn) {
                    authViewModel.consumeLegacyGoogleRequest()
                    @Suppress("DEPRECATION")
                    val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    )
                        .requestIdToken(com.hanyz.stopme.BuildConfig.WEB_CLIENT_ID)
                        .requestEmail()
                        .build()
                    @Suppress("DEPRECATION")
                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, options)
                    legacyGoogleLauncher.launch(client.signInIntent)
                }
            }

            AuthScreen(
                uiState = authUiState,
                onGoogleClick = {
                    // Web Client ID dibaca dari local.properties (WEB_CLIENT_ID)
                    authViewModel.signInWithGoogle(context, com.hanyz.stopme.BuildConfig.WEB_CLIENT_ID)
                },
                onEmailClick = authViewModel::showEmailDialog,
                onDismissDialog = authViewModel::dismissEmailDialog,
                onEmailChange = authViewModel::onEmailInputChanged,
                onPasswordChange = authViewModel::onPasswordInputChanged,
                onToggleMode = authViewModel::toggleAuthMode,
                onSubmitEmail = authViewModel::submitEmailAuth,
                onForgotPassword = authViewModel::sendPasswordReset,
                onSuccess = {
                    val next = if (OnboardingPrefs.isPermissionOnboardingDone(context)) {
                        Screen.MainContainer.route
                    } else {
                        Screen.Permission.route
                    }
                    navController.navigate(next) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Layar 3: Izin Aplikasi
        composable(Screen.Permission.route) {
            val permissionViewModel: PermissionViewModel = viewModel()
            val permissionUiState by permissionViewModel.uiState.collectAsState()

            PermissionScreen(
                uiState = permissionUiState,
                onRefreshPermissions = { permissionViewModel.checkPermissions(context) },
                onStartNowClick = {
                    OnboardingPrefs.setPermissionOnboardingDone(context)
                    navController.navigate(Screen.MainContainer.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        // Layar 4, 6, 8: Kontainer Utama (Home, Aktivitas, Profil)
        composable(Screen.MainContainer.route) {
            MainContainerScreen(
                onServiceSelected = { serviceType ->
                    navController.navigate(Screen.TripPlanner.createRoute(serviceType.name))
                },
                onSignedOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.MainContainer.route) { inclusive = true }
                    }
                }
            )
        }

        // Layar 5: Isi Perjalanan
        composable(
            route = Screen.TripPlanner.route,
            arguments = listOf(navArgument("serviceType") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceTypeName = backStackEntry.arguments?.getString("serviceType") ?: TransportServiceType.TRANSJAKARTA.name
            val serviceType = try {
                TransportServiceType.valueOf(serviceTypeName)
            } catch (e: Exception) {
                TransportServiceType.TRANSJAKARTA
            }

            val plannerViewModel: TripPlannerViewModel = viewModel()
            androidx.compose.runtime.LaunchedEffect(serviceType) {
                plannerViewModel.initService(serviceType)
            }

            val plannerUiState by plannerViewModel.uiState.collectAsState()

            TripPlannerScreen(
                uiState = plannerUiState,
                onBackClick = { navController.popBackStack() },
                onActiveFieldChange = plannerViewModel::onActiveFieldChanged,
                onDepartureQueryChange = plannerViewModel::onDepartureQueryChanged,
                onDestinationQueryChange = plannerViewModel::onDestinationQueryChanged,
                onStopSelected = plannerViewModel::onStopSelected,
                onSelectRouteCandidate = plannerViewModel::selectRouteCandidate,
                onDismissRouteSheet = plannerViewModel::dismissRouteSelectionSheet,
                onRadiusIndexChange = plannerViewModel::onRadiusIndexChanged,
                onMinutesThresholdChange = plannerViewModel::onMinutesThresholdChanged,
                onAlarmModeChange = plannerViewModel::onAlarmModeChanged,
                onShowAllStops = plannerViewModel::onShowAllStops,
                onStartTripClick = {
                    val started = plannerViewModel.startTrip()
                    if (started) {
                        TrackingService.start(context)
                        navController.navigate(Screen.MainContainer.route) {
                            popUpTo(Screen.MainContainer.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
