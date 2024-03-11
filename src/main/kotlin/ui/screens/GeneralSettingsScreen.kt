package ui.screens

import CustomCommandPermissions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import config.BuildInfo
import isSongRequestEnabledAsRedeem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import ui.*
import java.awt.Desktop
import java.net.URI

@Composable
fun generalSettingsScreen() {
    MaterialTheme {
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

                commandSecurityMultiToggleButton(
                    currentSelection = addSongCommandSecurityLevel.value,
                    toggleStates = listOf(
                        CustomCommandPermissions.BROADCASTER,
                        CustomCommandPermissions.MODERATOR,
                        CustomCommandPermissions.CUSTOM
                    ),
                    conditionClickable = isAddSongCommandEnabled,
                    functionalityDisplayName = "Add Song Command",
                    onToggleChange = {
                        addSongCommandSecurityLevel.value = CustomCommandPermissions.valueOf(it)
                    }
                )

                sectionDivider()

                toggleFunctionalityRow(
                    "Skip Song Command ",
                    true,
                    null,
                    isSkipSongCommandEnabled
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
                            text = "Security Level of Skip Song Command: ",
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }
                }

                commandSecurityMultiToggleButton(
                    currentSelection = skipSongCommandSecurityLevel.value,
                    toggleStates = listOf(
                        CustomCommandPermissions.BROADCASTER,
                        CustomCommandPermissions.MODERATOR,
                        CustomCommandPermissions.CUSTOM
                    ),
                    conditionClickable = isSkipSongCommandEnabled,
                    functionalityDisplayName = "Skip Song Command",
                    onToggleChange = {
                        skipSongCommandSecurityLevel.value = CustomCommandPermissions.valueOf(it)
                    }
                )
                sectionDivider()

                toggleFunctionalityRow(
                    "Remove Song From Queue Command ",
                    true,
                    null,
                    isRemoveSongFromQueueCommandEnabled
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
                            text = "Security Level of Remove Song From Queue Command: ",
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }
                }

                commandSecurityMultiToggleButton(
                    currentSelection = removeSongFromQueueCommandSecurityLevel.value,
                    toggleStates = listOf(
                        CustomCommandPermissions.BROADCASTER,
                        CustomCommandPermissions.MODERATOR,
                        CustomCommandPermissions.CUSTOM
                    ),
                    conditionClickable = isRemoveSongFromQueueCommandEnabled,
                    functionalityDisplayName = "Remove Song From Queue Command",
                    onToggleChange = {
                        removeSongFromQueueCommandSecurityLevel.value = CustomCommandPermissions.valueOf(it)
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
                            isSongRequestEnabledAsCommand.value = it
                            logger.info("Changed song request type to " + if(isSongRequestEnabledAsCommand.value) {
                                "Command"
                            } else {
                                "Redeem"
                            }
                            )
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
fun commandSecurityMultiToggleButton(
    currentSelection: Any,
    toggleStates: List<Any>,
    conditionClickable: MutableState<Boolean>,
    functionalityDisplayName: String,
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
                        enabled = conditionClickable.value,
                        onValueChange = { selected ->
                            if (selected) {
                                logger.info("clicked on multi selection entry $toggleState for $functionalityDisplayName")
                                onToggleChange(toggleState.toString())
                            }
                        }
                    )
                    .pointerHoverIcon(
                        if(conditionClickable.value) {
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
