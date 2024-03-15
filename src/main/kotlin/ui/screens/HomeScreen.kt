package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                            "This text and the home screen in general is only temporary, what information would you like " +
                            "to see here? Let me know!\n" +
                            "I have done some improvements in general on everything, make sure to check the change log " +
                            "in the release!\n\n" +
                            "With that said - have fun and enjoy :)"
                )

                versionAndCreditsRow()
            }
        }
    }
}