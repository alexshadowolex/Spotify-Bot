package ui.screens

import CustomCommandPermissions
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backgroundCoroutineScope
import config.BotConfig
import config.BuildInfo
import isSongRequestEnabledAsRedeem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import java.awt.Desktop
import java.net.URI


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

                dropDownListCustomGroup(
                    customGroup = customGroupUserNamesAddSongCommand,
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

                dropDownListCustomGroup(
                    customGroup = customGroupUserNamesSkipSongCommand,
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

                dropDownListCustomGroup(
                    customGroup = customGroupUserNamesRemoveSongFromQueueCommand,
                    textFieldTitle = "Custom Group Skip Song Command",
                    scaffoldState = scaffoldState
                )

                sectionDivider()

                dropDownListCustomGroup(
                    customGroup = blacklistedUsers,
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
        color = colors.primary,
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
                    style = typography.body1,
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
                    color = colors.primary,
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun dropDownListCustomGroup(
    customGroup: SnapshotStateList<String>,
    textFieldTitle: String,
    scaffoldState: ScaffoldState
) {
    val defaultIndex = -1
    var editIndex by remember { mutableStateOf(defaultIndex) }
    var expanded by remember { mutableStateOf(false) }
    val textFieldContent = remember { mutableStateOf("") }
    var isEditModeEnabled by remember { mutableStateOf(false) }
    val borderColor = Color(46,46,46)
    val widthPercentage = 0.8F
    val tooltipModifier = Modifier
        .background(
            color = colors.onSecondary,
            shape = RoundedCornerShape(5.dp)
        )
        .shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(8.dp)
        )
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))

    Row (
        modifier = Modifier
            .padding(top = 5.dp)
    ) {
        val isEntryGettingAdded = editIndex > customGroup.lastIndex && isEditModeEnabled
        val isEntryGettingEdited = editIndex != defaultIndex && isEditModeEnabled
        TextField(
            value = textFieldContent.value,
            onValueChange = {
                textFieldContent.value = it
            },
            label = {
                Text(
                    color = colors.onPrimary,
                    text = if (isEntryGettingAdded) {
                        "Add entry for $textFieldTitle"
                    } else if(isEntryGettingEdited) {
                        "Edit entry for $textFieldTitle"
                    } else {
                        textFieldTitle
                    }
                )
            },
            enabled = isEditModeEnabled,
            singleLine = true,
            modifier = Modifier
                .clickable {
                    expanded = true
                }
                .fillMaxWidth(widthPercentage)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
        ) {
            if (isEditModeEnabled) {
                Row (
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                ) {
                    TooltipArea(
                        tooltip = {
                            Box (
                                modifier = tooltipModifier
                            ) {
                                Text(
                                    text = "Save current changes",
                                    modifier = Modifier
                                        .padding(all = 3.dp)
                                )
                            }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                // save changes
                                val message = if (isEntryGettingAdded) {
                                    customGroup.add(textFieldContent.value)
                                    "Successfully added entry"
                                } else {
                                    customGroup.removeAt(editIndex)
                                    customGroup.add(editIndex, textFieldContent.value)
                                    "Successfully saved changes"
                                }
                                logger.info("$message ${customGroup[editIndex]} to $textFieldTitle")

                                isEditModeEnabled = false
                                editIndex = defaultIndex
                                textFieldContent.value = ""
                                backgroundCoroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = message
                                    )
                                }

                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "save_icon",
                                tint = Color.Unspecified
                            )
                        }
                    }
                    TooltipArea(
                        tooltip = {
                            Box (
                                modifier = tooltipModifier
                            ) {
                                Text(
                                    text = "Discard current changes",
                                    modifier = Modifier
                                        .padding(all = 3.dp)
                                )
                            }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                // discard changes
                                editIndex = defaultIndex
                                isEditModeEnabled = false
                                textFieldContent.value = ""
                                backgroundCoroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Discarded changes"
                                    )
                                }
                                logger.info("Discarded changes of $textFieldTitle")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "cancel_icon",
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        },
        modifier = Modifier
            .background(colors.background)
            .fillMaxWidth(widthPercentage)
            .height(500.dp)
            .border(2.dp, borderColor)
    ) {
        Column (
            modifier = Modifier
                .padding(2.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = textFieldTitle,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }
        if(customGroup.isEmpty()) {
            Column (
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "No entries found",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }
        } else {
            customGroup.forEachIndexed { index, customGroupEntry ->
                DropdownMenuItem(
                    onClick = {
                        // edit entry
                        editIndex = index
                        isEditModeEnabled = true
                        expanded = false
                        textFieldContent.value = customGroup[index]
                        logger.info("Started editing entry ${customGroup[index]} of $textFieldTitle")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(widthPercentage)
                    ) {
                        Text(
                            text = customGroupEntry,
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                        ) {
                            IconButton(
                                onClick = {
                                    // edit entry
                                    editIndex = index
                                    isEditModeEnabled = true
                                    expanded = false
                                    textFieldContent.value = customGroup[index]
                                    logger.info("Started editing entry ${customGroup[index]} of $textFieldTitle")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "edit_icon",
                                    tint = Color.Unspecified
                                )
                            }

                            IconButton(
                                onClick = {
                                    // delete entry
                                    val entry = customGroup[index]
                                    backgroundCoroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            message = "Removed entry \"$entry\""
                                        )
                                    }
                                    customGroup.removeAt(index)
                                    logger.info("Removed entry $entry of $textFieldTitle")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "delete_icon",
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }

                sectionDivider()
            }
        }

        DropdownMenuItem(
            onClick = {
                // add new entry
                editIndex = customGroup.lastIndex + 1
                isEditModeEnabled = true
                expanded = false
                logger.info("Started adding new entry to $textFieldTitle")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Add new entry",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                    IconButton(
                        onClick = {
                            // add new entry
                            editIndex = customGroup.lastIndex + 1
                            isEditModeEnabled = true
                            expanded = false
                            logger.info("Started adding new entry to $textFieldTitle")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "add_icon",
                            tint = Color.Unspecified
                        )
                    }
                }
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
}