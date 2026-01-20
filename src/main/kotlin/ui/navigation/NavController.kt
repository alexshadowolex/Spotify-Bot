package ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable


class NavController(
    private val startDestination: String,
    private var backStackScreens: MutableSet<String> = mutableSetOf()
) {
    var currentScreen: MutableState<String> = mutableStateOf(startDestination)

    fun navigate(route: String) {
        if (route != currentScreen.value) {
            if (backStackScreens.contains(currentScreen.value) && currentScreen.value != startDestination) {
                backStackScreens.remove(currentScreen.value)
            }

            if (route == startDestination) {
                backStackScreens = mutableSetOf()
            } else {
                backStackScreens.add(currentScreen.value)
            }

            currentScreen.value = route
        }
    }
}


/**
 * Creates and remembers a [NavController] instance across recompositions and
 * process recreation.
 *
 * The controller is stored using [rememberSaveable], ensuring that navigation
 * state survives configuration changes such as screen rotations. The initial
 * navigation stack is defined by the provided start destination and an optional
 * set of back stack screens.
 *
 * @param startDestination the route used as the initial destination of the navigation stack
 * @param backStackScreens a mutable set of additional routes that should already
 * exist in the back stack when the controller is created
 * @return a [MutableState] holding the remembered [NavController] instance
 */
@Composable
fun rememberNavController(
    startDestination: String,
    backStackScreens: MutableSet<String> = mutableSetOf()
): MutableState<NavController> = rememberSaveable {
    mutableStateOf(NavController(startDestination, backStackScreens))
}