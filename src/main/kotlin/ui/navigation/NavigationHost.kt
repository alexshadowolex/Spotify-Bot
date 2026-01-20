package ui.navigation

import androidx.compose.runtime.Composable

class NavigationHost(
    val navController: NavController,
    val contents: @Composable NavigationGraphBuilder.() -> Unit
) {

    @Composable
    fun build() {
        NavigationGraphBuilder().renderContents()
    }

    inner class NavigationGraphBuilder(
        val navController: NavController = this@NavigationHost.navController
    ) {
        @Composable
        fun renderContents() {
            this@NavigationHost.contents(this)
        }
    }
}


/**
 * Registers composable content for a specific navigation route.
 *
 * The provided [content] is rendered only when the current route held by the
 * [NavController] matches the given [route]. This enables simple, declarative
 * screen switching based on the active navigation destination.
 *
 * @param route the navigation route that activates this composable content
 * @param content the composable UI to display when the route is active
 */
@Composable
fun NavigationHost.NavigationGraphBuilder.composable(
    route: String,
    content: @Composable () -> Unit
) {
    if (navController.currentScreen.value == route) {
        content()
    }
}