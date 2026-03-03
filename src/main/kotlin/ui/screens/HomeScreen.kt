package ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import config.TwitchBotConfig
import handler.commands
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ui.hyperlink
import ui.versionAndCreditsRow

@Composable
fun homeScreen() {
    MaterialTheme {
        Scaffold {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {
                val hyperlinkHeight = 2.em

                val issuesHyperlinkText = "Spotify Bot Issues"
                // I don't know what the specific length value is referring to. The divider "2.1" is a hardcoded and
                // guessed number which I don't understand. Since this will only be temporarily visible, I will
                // leave it like this for now and remove it later.
                val issuesHyperlinkWidth = (issuesHyperlinkText.length / 2.1).em
                val issuesHyperLinkId = "issuesHyperLinkId"

                val discordHyperlinkText = "Discord Server"
                val discordHyperlinkWidth = (discordHyperlinkText.length / 2.1).em
                val discordHyperLinkId = "discordHyperLinkId"

                val inlineContentMap = mapOf(
                    issuesHyperLinkId to InlineTextContent(
                        Placeholder(issuesHyperlinkWidth, hyperlinkHeight, PlaceholderVerticalAlign.Center)) {
                            hyperlink(
                                hyperlinkText = issuesHyperlinkText,
                                hyperlinkAddress = "https://github.com/alexshadowolex/Spotify-Bot/issues",
                                useUnderline = false
                            )
                    },
                    discordHyperLinkId to InlineTextContent(
                        Placeholder(discordHyperlinkWidth, hyperlinkHeight, PlaceholderVerticalAlign.Center)) {
                        hyperlink(
                            hyperlinkText = discordHyperlinkText,
                            hyperlinkAddress = "https://discord.gg/sM9B6CbZsy",
                            useUnderline = false
                        )
                    }
                )


                val dateForLiveChanges = Instant.parse("2026-03-03T00:00:00.000Z")
                val today = Clock.System.now()
                Text(
                    text = buildAnnotatedString {
                        append(
                            "Hello ${TwitchBotConfig.channel} and thanks for trusting the Spotify Bot!\n\n" +
                            "!!NEW!!: There's now a Discord server for everything related to the bot - " +
                            "join to stay updated and share feedback: "
                        )

                        appendInlineContent(discordHyperLinkId)

                        append(
                            "\n\nSpotify announced major API changes in February 2026, " +
                                if(today < dateForLiveChanges) {
                                    "going"
                                } else {
                                    "which went"
                                } + " " +
                            "live on March 9th 2026. This version includes necessary adjustments and workarounds " +
                            "to remain compatible.\n" +
                            "Due to the scope of these API changes, some edge cases may still appear. If something " +
                            "doesn’t behave as expected, please tell me on Discord or open an issue on GitHub here: "
                        )

                        appendInlineContent(issuesHyperLinkId)

                        append(
                            "\n\nThanks for your support and patience during this transition - and enjoy! :)"
                        )
                    },
                    inlineContent = inlineContentMap,
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
                        .height(100.dp)
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