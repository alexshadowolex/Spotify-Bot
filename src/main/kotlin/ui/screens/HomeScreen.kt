package ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import handler.commands
import ui.versionAndCreditsRow

@Composable
fun homeScreen() {
    MaterialTheme {
        Scaffold {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {
                Text(
                    text =  "Hello beautiful person and valued user! Thank you for trusting in my app and using it to " +
                            "enhance your stream's experience with Spotify!\n" +
                            "This Bot has gotten a new glow up so you can manage (almost) all settings in the user interface. " +
                            "I am still looking for ideas and improvements since planning and building an UI is not my " +
                            "strength.\n" +
                            "This text and the home screen in general are only temporary, what information would you like " +
                            "to see here? Let me know!\n\n" +
                            "With that said - have fun and enjoy :)",
                    modifier = Modifier
                        .padding(top = 10.dp)
                )

                Text(
                    text = "Available Twitch Commands: ",
                    modifier = Modifier
                        .padding(top = 10.dp)
                )

                val scrollState = rememberScrollState(0)
                val scrollBarAdapter = rememberScrollbarAdapter(scrollState)
                Column (
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                        .border(1.dp, Color(30, 30, 30), RoundedCornerShape(4.dp))
                ) {
                    Row (
                        modifier = Modifier
                            .padding(all = 5.dp)
                    ) {
                        Column (
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .verticalScroll(scrollState)
                        ) {
                            commands.forEach { command ->
                                Column (
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .padding(top = 3.dp)
                                ) {
                                    Text(
                                        text = command.commandDisplayName + ": " + command.names.joinToString(", ")
                                    )

                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 1.dp),
                                        color = MaterialTheme.colors.primary,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }

                        Column (
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            VerticalScrollbar(
                                adapter = scrollBarAdapter,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(start = 2.dp)
                                    .padding(end = 1.dp)
                            )
                        }
                    }
                }

                versionAndCreditsRow()
            }
        }
    }
}