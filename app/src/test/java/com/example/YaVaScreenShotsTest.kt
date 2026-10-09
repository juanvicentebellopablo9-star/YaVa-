package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.AuthGatewayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MandatoryTermsGateScreen
import com.example.ui.theme.YaVaTheme
import com.example.ui.viewmodel.YaVaViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class YaVaScreenShotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun terms_gate_light() {
        composeTestRule.setContent {
            YaVaTheme(darkTheme = false) {
                MandatoryTermsGateScreen(onAccept = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/01_terms_gate_light.png"
        )
    }

    @Test
    fun auth_gateway_light() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = YaVaViewModel(app)
        composeTestRule.setContent {
            YaVaTheme(darkTheme = false) {
                AuthGatewayScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/02_auth_gateway_light.png"
        )
    }

    @Test
    fun home_screen_light() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = YaVaViewModel(app)
        composeTestRule.setContent {
            YaVaTheme(darkTheme = false) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRequest = {},
                    onNavigateToTracking = {},
                    onNavigateToDriverPortal = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/03_home_screen_light.png"
        )
    }
}
