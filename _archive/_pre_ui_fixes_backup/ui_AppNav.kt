package com.bhagavatam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bhagavatam.app.data.Lang
import com.bhagavatam.app.state.AppState
import com.bhagavatam.app.ui.components.FloatingTabBar
import com.bhagavatam.app.ui.components.MiniPlayer
import com.bhagavatam.app.ui.components.Tab
import com.bhagavatam.app.ui.screens.AdhyayasScreen
import com.bhagavatam.app.ui.screens.DownloadsScreen
import com.bhagavatam.app.ui.screens.GlossaryScreen
import com.bhagavatam.app.ui.screens.GranthScreen
import com.bhagavatam.app.ui.screens.HomeScreen
import com.bhagavatam.app.ui.screens.LanguagesScreen
import com.bhagavatam.app.ui.screens.MeScreen
import com.bhagavatam.app.ui.screens.OnboardingLanguageScreen
import com.bhagavatam.app.ui.screens.OnboardingPaathScreen
import com.bhagavatam.app.ui.screens.PlayerScreen
import com.bhagavatam.app.ui.screens.ReaderScreen
import com.bhagavatam.app.ui.screens.SavedScreen
import com.bhagavatam.app.ui.screens.SearchScreen
import com.bhagavatam.app.ui.theme.BhagavatamTheme
import com.bhagavatam.app.ui.theme.Brand
import com.bhagavatam.app.ui.theme.HindSiliguri
import com.bhagavatam.app.ui.theme.Jakarta
import com.bhagavatam.app.ui.theme.Mukta

object Routes {
    const val ONB_LANG = "onb_lang"
    const val ONB_PAATH = "onb_paath"
    const val ADHYAYAS = "adhyayas/{s}"
    const val READER = "reader/{s}/{a}"
    const val PLAYER = "player"
    const val SAVED = "saved"
    const val GLOSSARY = "glossary"
    const val LANGUAGES = "languages"
    fun adhyayas(s: Int) = "adhyayas/$s"
    fun reader(s: Int, a: Int) = "reader/$s/$a"
}

@Composable
fun AppNav(state: AppState) {
    val uiFont = when (state.uiLang) { Lang.HI -> Mukta; Lang.BN -> HindSiliguri; else -> Jakarta }
    BhagavatamTheme(uiFont = uiFont, reader = state.readerTheme.colors) {
        val nav = rememberNavController()
        val start = if (state.onboarded) Tab.Home.route else Routes.ONB_LANG
        NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize().background(Brand.Paper)) {
            composable(Routes.ONB_LANG) { OnboardingLanguageScreen(state) { nav.navigate(Routes.ONB_PAATH) } }
            composable(Routes.ONB_PAATH) {
                OnboardingPaathScreen(state, onBack = { nav.popBackStack() }) {
                    state.finishOnboarding()
                    nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true } }
                }
            }
            composable(Tab.Home.route) {
                TabScaffold(state, nav, Tab.Home) {
                    HomeScreen(state,
                        onResume = { nav.navigate(Routes.reader(state.lastSkandha, state.lastAdhyaya)) },
                        onSearch = { nav.go(Tab.Search) })
                }
            }
            composable(Tab.Granth.route) {
                TabScaffold(state, nav, Tab.Granth) { GranthScreen(state) { s -> nav.navigate(Routes.adhyayas(s)) } }
            }
            composable(Routes.ADHYAYAS, arguments = listOf(navArgument("s") { type = NavType.IntType })) { e ->
                val s = e.arguments?.getInt("s") ?: 1
                TabScaffold(state, nav, Tab.Granth) {
                    AdhyayasScreen(state, s, onBack = { nav.popBackStack() }) { a -> nav.navigate(Routes.reader(s, a)) }
                }
            }
            composable(
                Routes.READER,
                arguments = listOf(navArgument("s") { type = NavType.IntType }, navArgument("a") { type = NavType.IntType }),
            ) { e ->
                ReaderScreen(state, e.arguments?.getInt("s") ?: 1, e.arguments?.getInt("a") ?: 1,
                    onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) })
            }
            composable(Routes.PLAYER) { PlayerScreen(state) { nav.popBackStack() } }
            composable(Tab.Search.route) {
                TabScaffold(state, nav, Tab.Search) {
                    SearchScreen(state,
                        onOpenVerse = { s, a -> nav.navigate(Routes.reader(s, a)) },
                        onOpenGlossary = { nav.navigate(Routes.GLOSSARY) })
                }
            }
            composable(Tab.Downloads.route) { TabScaffold(state, nav, Tab.Downloads) { DownloadsScreen(state) } }
            composable(Tab.Me.route) {
                TabScaffold(state, nav, Tab.Me) {
                    MeScreen(state,
                        onSaved = { nav.navigate(Routes.SAVED) },
                        onGlossary = { nav.navigate(Routes.GLOSSARY) },
                        onLanguages = { nav.navigate(Routes.LANGUAGES) })
                }
            }
            composable(Routes.SAVED) {
                TabScaffold(state, nav, Tab.Me) { SavedScreen(state, onBack = { nav.popBackStack() }) { s, a -> nav.navigate(Routes.reader(s, a)) } }
            }
            composable(Routes.GLOSSARY) { TabScaffold(state, nav, Tab.Me) { GlossaryScreen(state) { nav.popBackStack() } } }
            composable(Routes.LANGUAGES) { LanguagesScreen(state) { nav.popBackStack() } }
        }
    }
}

private fun NavHostController.go(tab: Tab) = navigate(tab.route) {
    popUpTo(Tab.Home.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** A tab screen with the floating tab bar, and the mini player above it once something has played. */
@Composable
private fun TabScaffold(state: AppState, nav: NavHostController, tab: Tab, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brand.Paper)) {
        content()
        Column(Modifier.align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.hasSession) MiniPlayer(state, onOpen = { nav.navigate(Routes.PLAYER) })
            FloatingTabBar(state, tab, onSelect = { nav.go(it) })
        }
    }
}
