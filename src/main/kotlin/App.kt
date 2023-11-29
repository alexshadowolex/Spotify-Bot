import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import config.TwitchBotConfig

val darkColorPalette = darkColors(
    primary = Color(0xff2244bb),
    onPrimary = Color.White,
    secondary = Color(0xff5bbbfe),
    background = Color.DarkGray,
    onBackground = Color.White,
)

var isSongRequestRedeemEnabled = !TwitchBotConfig.isSongRequestCommandEnabledByDefault
var isSongRequestCommandEnabled = TwitchBotConfig.isSongRequestCommandEnabledByDefault
var isSpotifySongNameGetterEnabled = TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault

@Composable
@Preview
fun app() {
    val isSongRequestChecked = remember { mutableStateOf(TwitchBotConfig.isSongRequestCommandEnabledByDefault) }
    val isSpotifySongNameGetterChecked = remember { mutableStateOf(TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault) }

    MaterialTheme(colors = darkColorPalette) {
        Scaffold {
            Column {
                Row {
                    Column {
                        Row(
                            modifier = Modifier
                                .padding(top = 20.dp)
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

                        Row(
                            modifier = Modifier
                                .padding(top = 10.dp, start = 15.dp, end = 15.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                            ) {
                                Text(
                                    text = "Redeem",
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
                            ) {
                                Text(
                                    text = "Command",
                                    modifier = Modifier
                                        .align(Alignment.End)
                                )
                            }
                        }

                        Row {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Switch(
                                    checked = isSongRequestChecked.value,
                                    onCheckedChange = {
                                        isSongRequestChecked.value = it
                                        isSongRequestRedeemEnabled = !it
                                        isSongRequestCommandEnabled = it
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                        .scale(2F)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.5F)
                    ) {
                        Text(
                            text = "Song Name Getter " +
                                    if (isSpotifySongNameGetterChecked.value) {
                                        "Enabled"
                                    } else {
                                        "Disabled"
                                    },
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Switch(
                            checked = isSpotifySongNameGetterChecked.value,
                            onCheckedChange = {
                                isSpotifySongNameGetterChecked.value = it
                                isSpotifySongNameGetterEnabled = it
                            },
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }
                }
            }
        }
    }
}