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
                val issuesHyperlinkText = "Spotify Bot Issues"
                // I don't know what the specific length value is referring to. The divider "2.1" is a hardcoded and
                // guessed number which I don't understand. Since this will only be temporarily visible, I will
                // leave it like this for now and remove it later.
                val issuesHyperlinkWidth = (issuesHyperlinkText.length / 2.1).em
                val issuesHyperlinkHeight = 1.2.em
                val issuesHyperLinkId = "issuesHyperLinkId"

                val inlineContentMap = mapOf(
                    issuesHyperLinkId to InlineTextContent(
                        Placeholder(issuesHyperlinkWidth, issuesHyperlinkHeight, PlaceholderVerticalAlign.Center)) {
                            hyperlink(
                                hyperlinkText = issuesHyperlinkText,
                                hyperlinkAddress = "https://github.com/alexshadowolex/Spotify-Bot/issues"
                            )
                    }
                )

                Text(
                    text = buildAnnotatedString {
                        append(
                            "Hello ${TwitchBotConfig.channel} you beautiful person and valued user! " +
                            "Thank you for trusting in my app and using it to enhance your stream's experience with Spotify!\n\n" +
                            "There is a small update on how to communicate bugs/features: Since this project's binaries are hosted " +
                            "and distributed over Github, why not also use more of this site's features? On Github, you can " +
                            "create issues and label them as an \"enhancement\" or a \"bug\". It is available under this link: "
                        )

                        appendInlineContent(issuesHyperLinkId)

                        append(
                            ". Please use it to tell me new feature ideas and bugs. You only need a (free) Github account :)\n" +
                            "Of course you can still message me and then I will do that. But it would be nice to see that feature get used.\n\n" +
                            "With that said - have fun and enjoy :)"
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