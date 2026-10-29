package dev.nullpointless.canvasmusic.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullpointless.canvasmusic.R
import dev.nullpointless.canvasmusic.api.ApiException
import dev.nullpointless.canvasmusic.data.Repository
import dev.nullpointless.canvasmusic.ui.Bg
import dev.nullpointless.canvasmusic.ui.CmTypography
import dev.nullpointless.canvasmusic.ui.ErrorRed
import dev.nullpointless.canvasmusic.ui.TextSecondary
import dev.nullpointless.canvasmusic.ui.TextTertiary
import dev.nullpointless.canvasmusic.ui.White
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

@Composable
fun LoginScreen(onDone: () -> Unit) {
    var server by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(72.dp))
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(86.dp)
        )
        Spacer(Modifier.height(22.dp))
        Text("Spotune", style = CmTypography.headlineLarge, color = White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sign in to your Navidrome server",
            style = CmTypography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(44.dp))

        LoginField(
            value = server,
            onValueChange = { server = it },
            placeholder = "Server URL (http://…)",
            keyboardType = KeyboardType.Uri
        )
        Spacer(Modifier.height(12.dp))
        LoginField(
            value = user,
            onValueChange = { user = it },
            placeholder = "Username",
            keyboardType = KeyboardType.Text
        )
        Spacer(Modifier.height(12.dp))
        LoginField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            password = true
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                error ?: "",
                style = CmTypography.bodySmall,
                color = ErrorRed,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))
        val enabled = !loading && server.isNotBlank() && user.isNotBlank() && password.isNotBlank()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (enabled) White else Color(0xFF5A5A5A))
                .clickable(enabled = enabled) {
                    scope.launch {
                        loading = true
                        error = null
                        Repository.signIn(server, user, password)
                            .onSuccess {
                                loading = false
                                onDone()
                            }
                            .onFailure { e ->
                                loading = false
                                error = friendlyError(e)
                            }
                    }
                }
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color(0xFF000000),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    "Sign In",
                    style = CmTypography.bodyLarge.copy(fontSize = 16.sp),
                    color = Color(0xFF000000)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Credentials never leave this device.\nSpotune talks to your server over the Subsonic API with salted-token authentication.",
            style = CmTypography.labelSmall.copy(fontSize = 11.sp),
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    password: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle.Default.copy(
            fontFamily = CmTypography.bodyLarge.fontFamily,
            fontSize = 16.sp,
            color = White
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush = SolidColor(White),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = CmTypography.bodyLarge.copy(fontSize = 16.sp),
                    color = TextSecondary
                )
            } else {
                innerTextField()
            }
        }
    )
}

private fun friendlyError(e: Throwable): String = when (e) {
    is ApiException -> when (e.code) {
        40 -> "Wrong username or password"
        10 -> "Missing required parameter"
        else -> "Server error: ${e.message}"
    }
    is UnknownHostException -> "Could not reach the server. Check the URL."
    is ConnectException -> "Could not connect. Check the URL and port."
    else -> "Sign-in failed: ${e.message ?: "unknown error"}"
}
