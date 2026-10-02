package com.hanyz.stopme.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Permission : Screen("permission")
    data object MainContainer : Screen("main_container")
    data object TripPlanner : Screen("trip_planner/{serviceType}") {
        fun createRoute(serviceType: String) = "trip_planner/$serviceType"
    }
}

enum class BottomBarTab(val route: String, val labelRes: Int, val iconRes: Int) {
    HOME("tab_home", com.hanyz.stopme.R.string.tab_home, com.hanyz.stopme.R.drawable.ic_nav_home),
    ACTIVITIES("tab_activities", com.hanyz.stopme.R.string.tab_activities, com.hanyz.stopme.R.drawable.ic_nav_activities),
    PROFILE("tab_profile", com.hanyz.stopme.R.string.tab_profile, com.hanyz.stopme.R.drawable.ic_nav_profile)
}
