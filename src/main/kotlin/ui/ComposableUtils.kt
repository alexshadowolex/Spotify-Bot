package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backgroundCoroutineScope
import config.BuildInfo
import joinToLowercasePropertiesString
import joinToPropertiesString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import java.awt.Desktop
import java.net.URI
import java.util.Locale


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



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun dropDownStringPropertiesList(
    entries: SnapshotStateList<String>,
    textFieldTitle: String,
    scaffoldState: ScaffoldState
) {
    val defaultIndex = -1
    var editIndex by remember { mutableStateOf(defaultIndex) }
    var expanded by remember { mutableStateOf(false) }
    val textFieldContent = remember { mutableStateOf(entries.toList().joinToString(",")) }
    var isEditModeEnabled by remember { mutableStateOf(false) }
    val borderColor = Color(46,46,46)
    val widthPercentage = 0.8F
    val tooltipModifier = Modifier
        .background(
            color = MaterialTheme.colors.onSecondary,
            shape = RoundedCornerShape(5.dp)
        )
        .shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(8.dp)
        )
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))

    Row (
        modifier = Modifier
            .padding(top = 5.dp, bottom = 5.dp)
    ) {
        val isEntryGettingAdded = editIndex > entries.lastIndex && isEditModeEnabled
        val isEntryGettingEdited = editIndex != defaultIndex && isEditModeEnabled
        TextField(
            value = textFieldContent.value,
            onValueChange = {
                textFieldContent.value = it
            },
            label = {
                Text(
                    color = MaterialTheme.colors.onPrimary,
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
                                    entries.add(textFieldContent.value)
                                    "Successfully added entry"
                                } else {
                                    entries.removeAt(editIndex)
                                    entries.add(editIndex, textFieldContent.value)
                                    "Successfully saved changes"
                                }
                                logger.info("$message ${entries[editIndex]} to $textFieldTitle")

                                isEditModeEnabled = false
                                editIndex = defaultIndex
                                textFieldContent.value = entries.toList().joinToString(",")
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
                                textFieldContent.value = entries.toList().joinToString(",")
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
            .background(MaterialTheme.colors.background)
            .fillMaxWidth(widthPercentage)
            .height(400.dp)
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
        if(entries.isEmpty()) {
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
            entries.forEachIndexed { index, customGroupEntry ->
                DropdownMenuItem(
                    onClick = {
                        // edit entry
                        editIndex = index
                        isEditModeEnabled = true
                        expanded = false
                        textFieldContent.value = entries[index]
                        logger.info("Started editing entry ${entries[index]} of $textFieldTitle")
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
                                    textFieldContent.value = entries[index]
                                    logger.info("Started editing entry ${entries[index]} of $textFieldTitle")
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
                                    val entry = entries[index]
                                    backgroundCoroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            message = "Removed entry \"$entry\""
                                        )
                                    }
                                    entries.removeAt(index)
                                    textFieldContent.value = entries.toList().joinToString(",")
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
                editIndex = entries.lastIndex + 1
                textFieldContent.value = ""
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
                            editIndex = entries.lastIndex + 1
                            textFieldContent.value = ""
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
fun propertiesTextField(
    textFieldTitle: String,
    textFieldContent: MutableState<String>,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = textFieldContent.value,
        onValueChange = onValueChange,
        label = {
            Text(
                color = MaterialTheme.colors.onPrimary,
                text = textFieldTitle
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 5.dp)
    )
}