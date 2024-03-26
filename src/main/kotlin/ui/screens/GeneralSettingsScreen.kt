package ui.screens

import CustomCommandPermissions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import config.BotConfig
import isSongRequestEnabledAsRedeem
import logger
import ui.*


private var isSongRequestEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongRequestEnabled = value.value
    }
private var isSongRequestEnabledAsCommand = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongRequestCommandEnabled = value.value
    }
private var isSpotifySongNameGetterEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSpotifySongNameGetterEnabled = value.value
    }
private var isSongInfoCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongInfoCommandEnabled = value.value
    }
private var isEmptySongDisplayFilesOnPauseEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isEmptySongDisplayFilesOnPauseEnabled = value.value
    }
private var isAddSongCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isAddSongCommandEnabled = value.value
    }
private var addSongCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.addSongCommandSecurityLevel = value.value
    }
private var customGroupUserNamesAddSongCommand = mutableStateListOf("")
    set(value) {
        field = value
        BotConfig.customGroupUserNamesAddSongCommand = value
    }
private var isSkipSongCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSkipSongCommandEnabled = value.value
    }
private var skipSongCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.skipSongCommandSecurityLevel = value.value
    }
private var customGroupUserNamesSkipSongCommand = mutableStateListOf("")
    set(value) {
        field = value
        BotConfig.customGroupUserNamesSkipSongCommand = value
    }
private var isRemoveSongFromQueueCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isRemoveSongFromQueueCommandEnabled = value.value
    }
private var removeSongFromQueueCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.removeSongFromQueueCommandSecurityLevel = value.value
    }
private var customGroupUserNamesRemoveSongFromQueueCommand = mutableStateListOf("")
    set(value) {
        field = value
        BotConfig.customGroupUserNamesRemoveSongFromQueueCommand = value
    }
private var blacklistedUsers = mutableStateListOf("")
    set(value) {
        field = value
        BotConfig.blacklistedUsers = value
    }
private var isNewVersionCheckEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isNewVersionCheckEnabled = value.value
    }
private var isBlockSongCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isBlockSongCommandEnabled = value.value
    }
private var blockSongCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.blockSongCommandSecurityLevel = value.value
    }
private var customGroupUserNamesBlockSongCommand = mutableStateListOf("")
    set(value) {
        field = value
        BotConfig.customGroupUserNamesBlockSongCommand = value
    }

@Composable
fun generalSettingsScreen() {
    val scaffoldState = rememberScaffoldState()

    MaterialTheme {
        Scaffold (
            scaffoldState = scaffoldState
        ) {
            Column (
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
                    .fillMaxSize()
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.49f)
                            .padding(end = 10.dp)
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

                        dropDownStringPropertiesList(
                            entries = blacklistedUsers,
                            textFieldTitle = "Blacklisted Users",
                            scaffoldState = scaffoldState
                        )

                        sectionDivider()

                        toggleFunctionalityRow(
                            "New Version Check on Start-up ",
                            true,
                            null,
                            isNewVersionCheckEnabled
                        )

                        sectionDivider()

                        toggleFunctionalityRow(
                            "Block Song Command ",
                            true,
                            null,
                            isBlockSongCommandEnabled
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
                                    text = "Security Level of Block Song Command: ",
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                )
                            }
                        }
                        commandSecurityMultiToggleButton(
                            currentSelection = blockSongCommandSecurityLevel.value,
                            toggleStates = listOf(
                                CustomCommandPermissions.BROADCASTER,
                                CustomCommandPermissions.MODERATOR,
                                CustomCommandPermissions.CUSTOM
                            ),
                            conditionClickable = isBlockSongCommandEnabled,
                            functionalityDisplayName = "Block Song Command",
                            onToggleChange = {
                                blockSongCommandSecurityLevel.value = CustomCommandPermissions.valueOf(it)
                            }
                        )

                        dropDownStringPropertiesList(
                            entries = customGroupUserNamesBlockSongCommand,
                            textFieldTitle = "Custom Group Block Song Command",
                            scaffoldState = scaffoldState
                        )

                        sectionDivider()
                    }


                    columnDivider()


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp)
                    ) {

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

                        dropDownStringPropertiesList(
                            entries = customGroupUserNamesAddSongCommand,
                            textFieldTitle = "Custom Group Add Song Command",
                            scaffoldState = scaffoldState
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

                        dropDownStringPropertiesList(
                            entries = customGroupUserNamesSkipSongCommand,
                            textFieldTitle = "Custom Group Skip Song Command",
                            scaffoldState = scaffoldState
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

                        dropDownStringPropertiesList(
                            entries = customGroupUserNamesRemoveSongFromQueueCommand,
                            textFieldTitle = "Custom Group Skip Song Command",
                            scaffoldState = scaffoldState
                        )

                        sectionDivider()

                        versionAndCreditsRow()
                    }
                }
            }
        }
    }
}


@Composable
private fun songRequestRow() {
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
                            colors.primary
                        } else {
                            colors.onBackground
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
                            colors.primary
                        } else {
                            colors.onBackground
                        }
                    )
                }
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
    val selectedTint = colors.primary
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
                colors.onPrimary
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


@Composable
fun initializeGeneralFlagVariables() {
    isSongRequestEnabled = remember { mutableStateOf(BotConfig.isSongRequestEnabled) }
    isSongRequestEnabledAsCommand = remember { mutableStateOf(BotConfig.isSongRequestCommandEnabled) }
    isSpotifySongNameGetterEnabled = remember { mutableStateOf(BotConfig.isSpotifySongNameGetterEnabled) }
    isSongInfoCommandEnabled = remember { mutableStateOf(BotConfig.isSongInfoCommandEnabled) }
    isEmptySongDisplayFilesOnPauseEnabled = remember { mutableStateOf(BotConfig.isEmptySongDisplayFilesOnPauseEnabled) }
    isAddSongCommandEnabled = remember { mutableStateOf(BotConfig.isAddSongCommandEnabled) }
    addSongCommandSecurityLevel = remember { mutableStateOf(BotConfig.addSongCommandSecurityLevel) }
    isSkipSongCommandEnabled = remember { mutableStateOf(BotConfig.isSkipSongCommandEnabled) }
    skipSongCommandSecurityLevel = remember { mutableStateOf(BotConfig.skipSongCommandSecurityLevel) }
    isRemoveSongFromQueueCommandEnabled = remember { mutableStateOf(BotConfig.isRemoveSongFromQueueCommandEnabled) }
    removeSongFromQueueCommandSecurityLevel = remember { mutableStateOf(BotConfig.removeSongFromQueueCommandSecurityLevel) }
    customGroupUserNamesAddSongCommand = remember { BotConfig.customGroupUserNamesAddSongCommand.toMutableStateList() }
    customGroupUserNamesSkipSongCommand = remember { BotConfig.customGroupUserNamesSkipSongCommand.toMutableStateList() }
    customGroupUserNamesRemoveSongFromQueueCommand = remember { BotConfig.customGroupUserNamesRemoveSongFromQueueCommand.toMutableStateList() }
    blacklistedUsers = remember { BotConfig.blacklistedUsers.toMutableStateList() }
    isNewVersionCheckEnabled = remember { mutableStateOf(BotConfig.isNewVersionCheckEnabled) }
    isBlockSongCommandEnabled = remember { mutableStateOf(BotConfig.isBlockSongCommandEnabled) }
    blockSongCommandSecurityLevel = remember { mutableStateOf(BotConfig.blockSongCommandSecurityLevel) }
    customGroupUserNamesBlockSongCommand = remember { BotConfig.customGroupUserNamesBlockSongCommand.toMutableStateList() }
}