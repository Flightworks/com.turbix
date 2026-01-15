package io.sensify.sensor.ui.pages.turbix

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurbixPage(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: TurbixViewModel = viewModel(
        factory = TurbixViewModel.Factory(application)
    )

    val state by viewModel.uiState.collectAsState()

    // Slider state
    var sliderPosition by remember { mutableStateOf(1f) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("HeliVibe Indicator") },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Scales Row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Absolute Scale
                TurbulenceScale(
                    label = "Absolue",
                    value = state.absoluteTurbulence,
                    max = 5f,
                    color = Color.Red
                )

                // Relative Scale
                TurbulenceScale(
                    label = "Relative",
                    value = state.relativeTurbulence,
                    max = 5f,
                    color = Color.Green
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tare Button
            Button(
                onClick = { viewModel.performTare() },
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("TARE")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sampling Duration Slider
            Text(
                text = "Durée d'échantillonnage: ${String.format("%.1f", sliderPosition)} s",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    viewModel.setSamplingDuration(it)
                },
                valueRange = 0.1f..5f,
                steps = 48, // 0.1 increments
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TurbulenceScale(
    label: String,
    value: Float,
    max: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Value Text
        Text(
            text = String.format("%.2f", value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bar
        Box(
            modifier = Modifier
                .width(60.dp)
                .weight(1f)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val fillFraction = min(value / max, 1f).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(color, RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
