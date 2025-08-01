package com.example.expensemanager.ui.screens // Or your preferred package for screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensemanager.R // Assuming your R file is here

@Composable
fun SplashScreen() {
    val alpha = remember { Animatable(0f) } // Initial alpha is 0 (fully transparent)

    LaunchedEffect(key1 = true) { // Runs once when the Composable enters the composition
        alpha.animateTo(
            targetValue = 1f, // Animate to fully opaque
            animationSpec = tween(durationMillis = 1500) // Animation duration
        )
        // Note: The navigation to the next screen will be handled by
        // the AuthViewModel's state change and the LaunchedEffect in your AppNavigation.
        // This SplashScreen's job is just to display itself.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Use your app's background color
            .alpha(alpha.value), // Apply the animated alpha
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with your logo
            contentDescription = stringResource(id = R.string.app_logo_content_description), // Add to strings.xml
            modifier = Modifier.size(120.dp) // Adjust size as needed
        )

        Spacer(modifier = Modifier.height(24.dp))

        // App Name
        Text(
            text = stringResource(id = R.string.app_name), // Your app's name from strings.xml
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground // Text color appropriate for the background
        )

        // Optional: A subtle tagline or version number
        // Spacer(modifier = Modifier.height(8.dp))
        // Text(
        // text = "Track your expenses effortlessly",
        // fontSize = 16.sp,
        // color = MaterialTheme.colorScheme.onSurfaceVariant
        // )
    }
}

// Add these to your res/values/strings.xml:
// <string name="app_name">Expense Manager</string>
// <string name="app_logo_content_description">App Logo</string>

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    // You might need to wrap with your app's theme for accurate preview
    // YourAppTheme {
    SplashScreen()
    // }
}
