package com.hanyz.stopme.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanyz.stopme.model.TransportServiceType
import com.hanyz.stopme.ui.activity.ActivitiesScreen
import com.hanyz.stopme.ui.activity.ActivitiesViewModel
import com.hanyz.stopme.ui.home.HomeScreen
import com.hanyz.stopme.ui.home.HomeViewModel
import com.hanyz.stopme.ui.profile.ProfileScreen
import com.hanyz.stopme.ui.profile.ProfileViewModel
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.DisabledGrey
import com.hanyz.stopme.ui.theme.LightGreenChip
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.SecondaryBackground

@Composable
fun MainContainerScreen(
    onServiceSelected: (TransportServiceType) -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(BottomBarTab.HOME) }

    val homeViewModel: HomeViewModel = viewModel()
    val activitiesViewModel: ActivitiesViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val permissionViewModel: com.hanyz.stopme.ui.permission.PermissionViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.White,
                contentColor = NavyPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                BottomBarTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = stringResource(id = tab.labelRes),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = tab.labelRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        // Gaya Figma: ikon navy, tanpa sorotan; tab tidak aktif sedikit pudar
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            unselectedIconColor = NavyPrimary.copy(alpha = 0.45f),
                            unselectedTextColor = NavyPrimary.copy(alpha = 0.45f),
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BottomBarTab.HOME -> {
                    val homeUiState by homeViewModel.uiState.collectAsState()
                    HomeScreen(
                        uiState = homeUiState,
                        onSearchChange = homeViewModel::onSearchQueryChanged,
                        onProfileClick = { currentTab = BottomBarTab.PROFILE },
                        onServiceSelected = onServiceSelected
                    )
                }

                BottomBarTab.ACTIVITIES -> {
                    val activitiesUiState by activitiesViewModel.uiState.collectAsState()
                    ActivitiesScreen(
                        uiState = activitiesUiState,
                        onBackClick = { currentTab = BottomBarTab.HOME },
                        onToggleMapStyle = activitiesViewModel::toggleMapStyle,
                        onStopTripClick = {
                            activitiesViewModel.stopTrip(context)
                        },
                        onHistoryTabSelected = activitiesViewModel::selectHistoryTab,
                        onToggleFavorite = activitiesViewModel::toggleFavorite,
                        onShowHistoryDetail = activitiesViewModel::showDetail,
                        onDismissHistoryDetail = activitiesViewModel::dismissDetail,
                        onStartFromHistory = { item ->
                            // Perjalanan butuh izin lokasi; arahkan ke Profil > Pengaturan jika belum ada
                            val hasLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasLocation) {
                                activitiesViewModel.startFromHistory(context, item)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Izinkan akses lokasi di Profil > Pengaturan terlebih dahulu",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                currentTab = BottomBarTab.PROFILE
                            }
                        }
                    )
                }

                BottomBarTab.PROFILE -> {
                    val profileUiState by profileViewModel.uiState.collectAsState()
                    val permissionUiState by permissionViewModel.uiState.collectAsState()
                    ProfileScreen(
                        uiState = profileUiState,
                        onSignOutClick = profileViewModel::showLogoutDialog,
                        onConfirmSignOut = profileViewModel::confirmSignOut,
                        onDismissSignOutDialog = profileViewModel::dismissLogoutDialog,
                        onSignedOutNavigate = onSignedOut,
                        permissionUiState = permissionUiState,
                        onRefreshPermissions = { permissionViewModel.checkPermissions(context) },
                        onAvatarClick = profileViewModel::showPhotoSource,
                        onDismissPhotoSource = profileViewModel::dismissPhotoSource,
                        onGalleryPicked = { uri -> profileViewModel.onGalleryPhotoPicked(context, uri) },
                        onCameraTaken = profileViewModel::onCameraPhotoTaken,
                        onEditNameClick = profileViewModel::showEditName,
                        onEditNameChange = profileViewModel::onEditNameChanged,
                        onDismissEditName = profileViewModel::dismissEditName,
                        onSaveName = profileViewModel::saveName,
                        onToggleEmail = profileViewModel::toggleEmailVisibility,
                        onMessageShown = profileViewModel::clearMessage
                    )
                }
            }
        }
    }
}
