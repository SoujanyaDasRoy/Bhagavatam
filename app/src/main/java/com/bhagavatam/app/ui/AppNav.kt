package com.bhagavatam.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import com.bhagavatam.app.ui.screens.AppearanceSettings
import com.bhagavatam.app.ui.screens.ListeningSettings
import com.bhagavatam.app.ui.screens.ReadingSettings
import com.bhagavatam.app.ui.theme.Motion
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
import com.bhagavatam.app.ui.components.BottomScrim
import com.bhagavatam.app.ui.components.FloatingTabBar
import com.bhagavatam.app.ui.components.MiniPlayer
import com.bhagavatam.app.ui.components.Tab
import com.bhagavatam.app.ui.screens.AdhyayasScreen
import com.bhagavatam.app.ui.screens.DownloadsScreen
import com.bhagavatam.app.ui.screens.GlossaryScreen
import com.bhagavatam.app.ui.screens.GranthScreen
import com.bhagavatam.app.ui.screens.HomeScreen
import com.bhagavatam.app.ui.screens.LanguagesScreen
import com.bhagavatam.app.ui.screens.SettingsScreen
import com.bhagavatam.app.ui.screens.OnboardingLanguageScreen
import com.bhagavatam.app.ui.screens.OnboardingPaathScreen
import com.bhagavatam.app.ui.screens.PlayerScreen
import com.bhagavatam.app.ui.screens.ReaderScreen
import com.bhagavatam.app.ui.screens.SavedScreen
import com.bhagavatam.app.ui.screens.SearchScreen
import com.bhagavatam.app.ui.theme.BhagavatamTheme
import com.bhagavatam.app.ui.theme.appColorsFor
import androidx.compose.foundation.isSystemInDarkTheme
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
    const val SET_APPEARANCE = "settings/appearance"
    const val SET_READING = "settings/reading"
    const val SET_LISTENING = "settings/listening"
    fun adhyayas(s: Int) = "adhyayas/$s"
    fun reader(s: Int, a: Int) = "reader/$s/$a"
}

@Composable
fun AppNav(state: AppState) {
    val uiFont = when (state.uiLang) { Lang.HI -> Mukta; Lang.BN -> HindSiliguri; else -> Jakarta }
    val sysDark = isSystemInDarkTheme()
    val theme = if (state.followSystem) (if (sysDark) state.darkTheme else state.lightTheme) else state.readerTheme
    val app = appColorsFor(theme)
    // The playing verse is tinted with the chosen accent, the same colour as the play button.
    val reader = theme.colors.copy(accent = app.accent, teal = app.accent, playing = app.accent.copy(alpha = if (app.isDark) 0.16f else 0.09f))
    BhagavatamTheme(uiFont = uiFont, reader = reader, app = app) {
        val nav = rememberNavController()
        val start = if (state.onboarded) Tab.Home.route else Routes.ONB_LANG
        NavHost(
            nav, startDestination = start, modifier = Modifier.fillMaxSize().background(Brand.Paper),
            // Pages slide in a little and fade; tabs only cross-fade; the player rises from the bottom. All follow the system animation scale.
            enterTransition = { fadeIn(tween(Motion.screen, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(Motion.screen, easing = FastOutSlowInEasing)) { it / 10 } },
            exitTransition = { fadeOut(tween(Motion.sheet)) },
            popEnterTransition = { fadeIn(tween(Motion.screen, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(Motion.sheet)) + slideOutHorizontally(tween(Motion.screen, easing = FastOutSlowInEasing)) { it / 10 } },
        ) {
            composable(Routes.ONB_LANG) { OnboardingLanguageScreen(state) { nav.navigate(Routes.ONB_PAATH) } }
            composable(Routes.ONB_PAATH) {
                OnboardingPaathScreen(state, onBack = { nav.popBackStack() }) {
                    state.finishOnboarding()
                    nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true } }
                }
            }
            tabComposable(Tab.Home.route) {
                TabScaffold(state, nav, Tab.Home) {
                    HomeScreen(state,
                        onResume = { nav.navigate(Routes.reader(state.lastSkandha, state.lastAdhyaya)) },
                        onSearch = { nav.go(Tab.Search) },
                        onOpenSkandha = { sk -> nav.navigate(Routes.adhyayas(sk)) },
                        onOpenGranth = { nav.go(Tab.Granth) },
                        onOpenChapter = { s, a -> nav.navigate(Routes.reader(s, a)) },
                        onSaved = { nav.navigate(Routes.SAVED) },
                        onGlossary = { nav.navigate(Routes.GLOSSARY) })
                }
            }
            tabComposable(Tab.Granth.route) {
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
                val rs = e.arguments?.getInt("s") ?: 1
                ReaderScreen(state, rs, e.arguments?.getInt("a") ?: 1,
                    onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) },
                    onNextChapter = { s, a -> nav.navigate(Routes.reader(s, a)) { popUpTo(Routes.READER) { inclusive = true } } },
                    onPrevChapter = { s, a -> nav.navigate(Routes.reader(s, a)) { popUpTo(Routes.READER) { inclusive = true } } })
            }
            composable(
                Routes.PLAYER,
                enterTransition = { slideInVertically(tween(Motion.screen, easing = FastOutSlowInEasing)) { it } },
                exitTransition = { fadeOut(tween(Motion.sheet)) },
                popEnterTransition = { fadeIn(tween(Motion.sheet)) },
                popExitTransition = { slideOutVertically(tween(Motion.screen, easing = FastOutSlowInEasing)) { it } },
            ) { PlayerScreen(state) { nav.popBackStack() } }
            tabComposable(Tab.Search.route) {
                TabScaffold(state, nav, Tab.Search) {
                    SearchScreen(state,
                        onOpenVerse = { s, a -> nav.navigate(Routes.reader(s, a)) },
                        onOpenGlossary = { nav.navigate(Routes.GLOSSARY) })
                }
            }
            tabComposable(Tab.Downloads.route) { TabScaffold(state, nav, Tab.Downloads) { DownloadsScreen(state) } }
            tabComposable(Tab.Me.route) {
                TabScaffold(state, nav, Tab.Me) {
                    SettingsScreen(state,
                        onSaved = { nav.navigate(Routes.SAVED) },
                        onGlossary = { nav.navigate(Routes.GLOSSARY) },
                        onLanguages = { nav.navigate(Routes.LANGUAGES) },
                        onAppearance = { nav.navigate(Routes.SET_APPEARANCE) },
                        onReading = { nav.navigate(Routes.SET_READING) },
                        onListening = { nav.navigate(Routes.SET_LISTENING) })
                }
            }
            composable(Routes.SET_APPEARANCE) { TabScaffold(state, nav, Tab.Me) { AppearanceSettings(state) { nav.popBackStack() } } }
            composable(Routes.SET_READING) { TabScaffold(state, nav, Tab.Me) { ReadingSettings(state) { nav.popBackStack() } } }
            composable(Routes.SET_LISTENING) { TabScaffold(state, nav, Tab.Me) { ListeningSettings(state) { nav.popBackStack() } } }
            composable(Routes.SAVED) {
                TabScaffold(state, nav, Tab.Me) { SavedScreen(state, onBack = { nav.popBackStack() }) { s, a -> nav.navigate(Routes.reader(s, a)) } }
            }
            composable(Routes.GLOSSARY) { TabScaffold(state, nav, Tab.Me) { GlossaryScreen(state) { nav.popBackStack() } } }
            composable(Routes.LANGUAGES) { LanguagesScreen(state) { nav.popBackStack() } }
        }
    }
}

/** A tab page: switching tabs cross-fades instead of sliding. */
private fun NavGraphBuilder.tabComposable(route: String, content: @Composable () -> Unit) = composable(
    route,
    enterTransition = { fadeIn(tween(Motion.sheet)) },
    exitTransition = { fadeOut(tween(Motion.press)) },
    popEnterTransition = { fadeIn(tween(Motion.sheet)) },
    popExitTransition = { fadeOut(tween(Motion.press)) },
) { content() }

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
        val scrim by animateDpAsState(if (state.hasSession) 200.dp else 130.dp, tween(Motion.sheet), label = "scrim")
        BottomScrim(Modifier.align(Alignment.BottomCenter), height = scrim)
        Column(Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(
                visible = state.hasSession,
                enter = fadeIn(tween(Motion.sheet)) + expandVertically(tween(Motion.sheet), expandFrom = Alignment.Bottom),
                exit = fadeOut(tween(Motion.press)) + shrinkVertically(tween(Motion.sheet), shrinkTowards = Alignment.Bottom),
            ) { Box(Modifier.padding(bottom = 10.dp)) { MiniPlayer(state, onOpen = { nav.navigate(Routes.PLAYER) }) } }
            FloatingTabBar(state, tab, onSelect = { nav.go(it) })
        }
    }
}
