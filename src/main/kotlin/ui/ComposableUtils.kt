package ui

import addQuotationMarks
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backgroundCoroutineScope
import config.BuildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import java.awt.Desktop
import java.net.URI

/**
 * Displays a horizontal divider used to visually separate UI sections.
 *
 * The divider spans the full available width, uses the primary theme color,
 * and applies a small top padding to improve visual spacing.
 */
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

/**
 * Displays a vertical divider used to visually separate columns or side-by-side content.
 *
 * The divider fills the available height, applies vertical padding,
 * and uses the primary theme color.
 */
@Composable
fun columnDivider() {
    Divider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 5.dp, bottom = 5.dp)
            .width(2.dp),
        color = MaterialTheme.colors.primary
    )
}

/**
 * Displays a labeled toggle row for enabling or disabling a specific functionality.
 *
 * The label text may dynamically reflect the current enabled state, and the toggle
 * itself can be conditionally disabled based on an external state.
 *
 * @param labelPrefixText the text displayed before the optional enabled/disabled suffix
 * @param showLabelSuffixText whether to append the current state ("Enabled"/"Disabled") to the label
 * @param conditionClickable optional state controlling whether the toggle is interactable
 * @param functionalityFlag the mutable state representing the functionality’s enabled status
 */
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


/**
 * Displays the application version information and author credits.
 *
 * The version is retrieved from build configuration and shown alongside
 * a clickable hyperlink to the author’s page, aligned to the bottom-right
 * of the layout.
 */
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

                hyperlink(
                    hyperlinkText = "alexshadowolex",
                    hyperlinkAddress = "https://www.twitch.tv/alexshadowolex",
                    fontSize = 12.sp,
                    useUnderline = false
                )
            }
        }
    }
}


/**
 * Displays clickable, underlined text that opens a web link when pressed.
 *
 * The link is opened asynchronously using the system browser, and failures
 * are logged gracefully. A hand cursor is shown on hover to indicate interactivity.
 *
 * @param hyperlinkText the visible text of the hyperlink
 * @param hyperlinkAddress the URL to open when clicked
 * @param fontSize optional font size override for the hyperlink text
 */
@Composable
fun hyperlink(
    hyperlinkText: String,
    hyperlinkAddress: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    useUnderline: Boolean
) {
    Text(
        style = MaterialTheme.typography.body1,
        text = hyperlinkText,
        modifier = Modifier
            .clickable {
                logger.info("Clicked on $hyperlinkText link")
                backgroundCoroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            Desktop.getDesktop().browse(
                                URI.create(hyperlinkAddress)
                            )
                        } catch (e: java.io.IOException) {
                            logger.error("Couldn't open $hyperlinkText with address $hyperlinkAddress")
                            logger.error(e.stackTraceToString())
                        }
                    }
                }
            }
            .pointerHoverIcon(PointerIcon.Hand),
        textDecoration = if(useUnderline) TextDecoration.Underline else TextDecoration.None,
        color = MaterialTheme.colors.primary,
        fontSize = fontSize
    )
}


/**
 * Displays an editable dropdown list for managing a list of string properties.
 *
 * Users can view, add, edit, or delete entries from the list. Changes are applied
 * immediately to the provided [SnapshotStateList], and user feedback is shown
 * using snackbars. Optional lowercase normalization can be applied to inputs.
 *
 * The optional parameter `getHeaderContent` defines a function that dynamically
 * generates the header for each list entry.
 *
 * @param entries the mutable list of string entries to manage
 * @param textFieldTitle the label shown for the text field and dropdown
 * @param scaffoldState the scaffold state used to display snackbars
 * @param lowercaseInput whether newly entered values should be converted to lowercase
 * @param getHeaderContent
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun dropDownStringPropertiesList(
    entries: SnapshotStateList<String>,
    textFieldTitle: String,
    scaffoldState: ScaffoldState,
    lowercaseInput: Boolean,
    getHeaderContent: ((String) -> String)? = null
) {
    val defaultIndex = -1
    var editIndex by remember { mutableStateOf(defaultIndex) }
    var expanded by remember { mutableStateOf(false) }
    val textFieldContent = remember { mutableStateOf(entries.joinToString(",")) }
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
                                val newContent = if(lowercaseInput) {
                                    textFieldContent.value.lowercase()
                                } else {
                                    textFieldContent.value
                                }
                                val message = if (isEntryGettingAdded) {
                                    entries.add(newContent)
                                    "Successfully added entry"
                                } else {
                                    entries.removeAt(editIndex)
                                    entries.add(editIndex, newContent)
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
                    Column {
                        if(getHeaderContent != null) {
                            Row {
                                Text(
                                    text = getHeaderContent(customGroupEntry)
                                )
                            }
                        }

                        Row {
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
                                                    message = "Removed entry ${entry.addQuotationMarks()}"
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


/**
 * Displays a simple single-line text field for editing a configurable property.
 *
 * The field shows a title label and propagates value changes through the provided
 * callback.
 *
 * @param textFieldTitle the label displayed above the text field
 * @param textFieldContent the mutable state holding the current field value
 * @param onValueChange callback invoked when the text value changes
 */
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


/**
 * Displays a modal alert dialog with customizable title, message, and confirmation action.
 *
 * The dialog is conditionally shown based on visibility state and provides
 * "Cancel" and "Ok" actions. The OK action executes a caller-supplied callback.
 *
 * @param isVisible controls whether the dialog is currently displayed
 * @param title the dialog title text
 * @param message the dialog body message
 * @param onOkClick the action executed when the OK button is pressed
 */
@Composable
fun alertDialogSurface(
    isVisible: MutableState<Boolean>,
    title: MutableState<String>,
    message: MutableState<String>,
    onOkClick: MutableState<() -> Unit>
) {
    if(isVisible.value) {
        AlertDialog(
            onDismissRequest = {
                isVisible.value = false
            },
            title = {
                Text(text = title.value)
            },
            text = {
                Text(text = message.value)
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column {
                        Button(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand),
                            onClick = { isVisible.value = false }
                        ) {
                            Text("Cancel")
                        }
                    }

                    Column {
                        Button(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand),
                            onClick = onOkClick.value
                        ) {
                            Text("Ok")
                        }
                    }
                }
            }
        )
    }
}