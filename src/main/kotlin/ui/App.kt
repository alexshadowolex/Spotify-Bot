package ui

import SpotifyConfig
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backgroundCoroutineScope
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import com.github.twitch4j.common.enums.CommandPermission
import config.BuildInfo
import config.TwitchBotConfig
import isSongRequestEnabledAsRedeem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import java.awt.Desktop
import java.net.URI
import kotlin.time.Duration.Companion.seconds

val darkColorPalette = darkColors(
    primary = Color(0xff5bbbfe),
    onPrimary = Color.White,
    secondary = Color(0xff2244bb),
    background = Color.DarkGray,
    onBackground = Color.White,
)

val lightColorPalette = lightColors(
    primary = Color(0xff4466ff),
    onPrimary = Color.White,
    secondary = Color(0xff0b5b8e),
    background = Color.White,
    onBackground = Color.Black,
)

lateinit var isSongRequestEnabled: MutableState<Boolean>
lateinit var isSongRequestEnabledAsCommand: MutableState<Boolean>
lateinit var isSpotifySongNameGetterEnabled: MutableState<Boolean>
lateinit var isSongInfoCommandEnabled: MutableState<Boolean>
lateinit var isEmptySongDisplayFilesOnPauseEnabled: MutableState<Boolean>
lateinit var isAddSongCommandEnabled: MutableState<Boolean>
lateinit var addSongCommandSecurityLevel: MutableState<CommandPermission>

@Composable
@Preview
fun app() {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val windowsRegistryPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
        val windowsRegistryLightThemeParameter = "AppsUseLightTheme"
        while (true) {
            isInDarkMode = if (NativeParameterStoreAccess.IS_WINDOWS) {
                WindowsRegistry.getWindowsRegistryEntry(
                    windowsRegistryPath,
                    windowsRegistryLightThemeParameter
                ) == 0x0
            } else {
                false
            }

            delay(1.seconds)
        }
    }

    initializeFlagVariables()

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        Scaffold {
            Column (
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {
                songRequestRow()

                sectionDivider()

                toggleFunctionalityRow(
                    "Song Name Getter ",
                    true,
                    null,
                    isSpotifySongNameGetterEnabled
                )

                toggleFunctionalityRow(
                    "Empty Song Display Files on Pause ",
                    false,
                    isSpotifySongNameGetterEnabled,
                    isEmptySongDisplayFilesOnPauseEnabled
                )

                sectionDivider()

                toggleFunctionalityRow(
                    "Song Info Command ",
                    true,
                    null,
                    isSongInfoCommandEnabled
                )

                sectionDivider()

                toggleFunctionalityRow(
                    "Add Song Command ",
                    true,
                    null,
                    isAddSongCommandEnabled
                )

                Row(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Security Level of Add Song Command: ",
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }
                }

                addSongSecurityMultiToggleButton(
                    currentSelection = addSongCommandSecurityLevel.value,
                    toggleStates = listOf(
                        CommandPermission.BROADCASTER,
                        CommandPermission.MODERATOR,
                        CommandPermission.EVERYONE
                    ),
                    onToggleChange = {
                        addSongCommandSecurityLevel.value = CommandPermission.valueOf(it)
                    }
                )

                sectionDivider()

                versionAndCreditsRow()
            }
        }
    }
}

@Composable
fun sectionDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        color = MaterialTheme.colors.primary,
        thickness = 2.dp
    )
}

@Composable
fun toggleFunctionalityRow(
    labelPrefixText: String,
    showLabelSuffixText: Boolean,
    conditionClickable: MutableState<Boolean>?,
    functionalityFlag: MutableState<Boolean>
) {
    val switchLabelWidthPercentage = 0.9F

    Row {
        Column(
            modifier = Modifier
                .fillMaxWidth(switchLabelWidthPercentage)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = labelPrefixText +
                        if(showLabelSuffixText) {
                            if (functionalityFlag.value) {
                                "Enabled"
                            } else {
                                "Disabled"
                            }
                        } else "",
                modifier = Modifier
                    .align(Alignment.Start)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
        ) {
            Switch(
                checked = functionalityFlag.value,
                onCheckedChange = {
                    functionalityFlag.value = it
                },
                modifier = Modifier
                    .align(Alignment.End),
                enabled = conditionClickable?.value ?: true
            )
        }
    }
}

@Composable
fun songRequestRow() {
    Row (
        modifier = Modifier
            .padding(top = 5.dp)
    ) {
        Column {
            toggleFunctionalityRow(
                "Song Request ",
                true,
                null,
                isSongRequestEnabled
            )

            Row(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Song Request as...",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }

            Row (
                modifier = Modifier
                    .padding(top = 5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.33F)
                        .align(Alignment.CenterVertically)
                        .padding(start = 15.dp)
                ) {
                    Text(
                        text = "Redeem",
                        modifier = Modifier
                            .align(Alignment.Start),
                        color = if(isSongRequestEnabledAsRedeem()) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onBackground
                        }
                    )
                }

                Column (
                    modifier = Modifier
                        .fillMaxWidth(0.5F)
                        .align(Alignment.CenterVertically)
                ) {
                    Switch(
                        checked = isSongRequestEnabledAsCommand.value,
                        onCheckedChange = {
                            logger.info("Changed song request type to " + if(isSongRequestEnabledAsCommand.value) {
                                    "Command"
                                } else {
                                    "Redeem"
                                }
                            )
                            isSongRequestEnabledAsCommand.value = it
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .scale(1.5F),
                        enabled = isSongRequestEnabled.value
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(end = 15.dp)
                ) {
                    Text(
                        text = "Command",
                        modifier = Modifier
                            .align(Alignment.End),
                        color = if(isSongRequestEnabledAsCommand.value) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onBackground
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun versionAndCreditsRow() {
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Bottom)
        ) {
            Row (
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Text(
                    text = "Bot Version v${BuildInfo.version} by ",
                    fontSize = 12.sp
                )

                Text(
                    style = MaterialTheme.typography.body1,
                    text = "alexshadowolex",
                    modifier = Modifier
                        .clickable {
                            logger.info("Clicked on alexshadowolex Link")
                            backgroundCoroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    Desktop.getDesktop()
                                        .browse(URI.create("https://www.twitch.tv/alexshadowolex"))
                                }
                            }
                        }
                        .pointerHoverIcon(PointerIcon.Hand),
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.primary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun addSongSecurityMultiToggleButton(
    currentSelection: Any,
    toggleStates: List<Any>,
    onToggleChange: (String) -> Unit
) {
    val selectedTint = MaterialTheme.colors.primary
    val unselectedTint = Color.Unspecified
    val widthPerColumn = listOf(0.33F, 0.5F, 1F)

    Row(
        modifier = Modifier
            .padding(top = 5.dp, bottom = 5.dp)
            .clip(shape = RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(20.dp))
    ) {
        toggleStates.forEachIndexed { index, toggleState ->
            val isSelected = currentSelection == toggleState
            val backgroundTint = if (isSelected) {
                selectedTint
            } else {
                unselectedTint
            }

            val textColor = if (isSelected) {
                MaterialTheme.colors.onPrimary
            } else {
                Color.Unspecified
            }

            Column (
                modifier = Modifier
                    .background(backgroundTint)
                    .fillMaxWidth(widthPerColumn[index])
                    .toggleable(
                        value = isSelected,
                        enabled = isAddSongCommandEnabled.value,
                        onValueChange = { selected ->
                            if (selected) {
                                logger.info("clicked on multi selection entry $toggleState")
                                onToggleChange(toggleState.toString())
                            }
                        }
                    )
                    .pointerHoverIcon(
                        if(isAddSongCommandEnabled.value) {
                            PointerIcon.Hand
                        } else {
                            PointerIcon.Default
                        }
                    )
            ) {
                Text(
                    toggleState.toString(),
                    color = textColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun initializeFlagVariables() {
    isSongRequestEnabled = remember { mutableStateOf(TwitchBotConfig.isSongRequestEnabledByDefault) }
    isSongRequestEnabledAsCommand = remember { mutableStateOf(TwitchBotConfig.isSongRequestCommandEnabledByDefault) }
    isSpotifySongNameGetterEnabled = remember { mutableStateOf(TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault) }
    isSongInfoCommandEnabled = remember { mutableStateOf(TwitchBotConfig.isSongInfoCommandEnabledByDefault) }
    isEmptySongDisplayFilesOnPauseEnabled = remember { mutableStateOf(TwitchBotConfig.isEmptySongDisplayFilesOnPauseEnabledByDefault) }
    isAddSongCommandEnabled = remember { mutableStateOf(TwitchBotConfig.isAddSongCommandEnabledByDefault) }
    addSongCommandSecurityLevel = remember { mutableStateOf(SpotifyConfig.addSongCommandSecurityLevelOnStartUp) }
}