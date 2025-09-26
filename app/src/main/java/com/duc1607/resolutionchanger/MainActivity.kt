package com.duc1607.resolutionchanger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duc1607.resolutionchanger.ui.theme.ResolutionChangerTheme
import kotlinx.coroutines.launch
import java.lang.reflect.Method

data class Resolution(
    val width: Int,
    val height: Int
) {
    override fun toString(): String = "${width}x${height}"
}

class MainActivity : ComponentActivity() {

    private fun hasWriteSecureSettingsPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    private fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(this)
    }

    var windowManagerInterface: Any? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Log permission status for debugging
        Log.d("ResolutionChanger", "WRITE_SECURE_SETTINGS: ${hasWriteSecureSettingsPermission()}")
        Log.d("ResolutionChanger", "Can write settings: ${canWriteSettings()}")

        // Initialize IWindowManager using reflection
        initializeWindowManager()

        enableEdgeToEdge()
        setContent {
            ResolutionChangerTheme {
                MainScreen()
            }
        }
    }

    private fun initializeWindowManager() {
        try {
            // Use reflection to access ServiceManager and IWindowManager
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val windowManagerBinder =
                getServiceMethod.invoke(null, Context.WINDOW_SERVICE) as IBinder

            val windowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod =
                windowManagerStubClass.getMethod("asInterface", IBinder::class.java)
            windowManagerInterface = asInterfaceMethod.invoke(null, windowManagerBinder)

            Log.d("ResolutionChanger", "IWindowManager initialized successfully using reflection")
        } catch (e: Exception) {
            Log.e("ResolutionChanger", "Failed to initialize IWindowManager: ${e.message}")
        }
    }

    fun getCurrentResolution(): Resolution {
        return try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getRealSize(size)
            Resolution(size.x, size.y)
        } catch (e: Exception) {
            Log.e("ResolutionChanger", "Failed to get current resolution: ${e.message}")
            Resolution(1080, 1920) // Default fallback
        }
    }
}

fun changeResolutionWithIWindowManager(
    windowManager: Any?,
    width: Int,
    height: Int
): Pair<Boolean, String> {
    return try {
        if (windowManager == null) {
            return Pair(false, "IWindowManager not available")
        }

        Log.d(
            "ResolutionChanger",
            "Attempting to change resolution to ${width}x${height} using IWindowManager"
        )
        Log.d("ResolutionChanger", "WindowManager class: ${windowManager.javaClass.name}")

        // Method 1: Try setForcedDisplaySize using reflection with detailed logging
        try {
            val setForcedDisplaySizeMethod: Method = windowManager.javaClass.getMethod(
                "setForcedDisplaySize",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            Log.d(
                "ResolutionChanger",
                "Found setForcedDisplaySize method: $setForcedDisplaySizeMethod"
            )
            Log.d(
                "ResolutionChanger",
                "Invoking with params: displayId=${Display.DEFAULT_DISPLAY}, width=$width, height=$height"
            )

            val result = setForcedDisplaySizeMethod.invoke(
                windowManager,
                Display.DEFAULT_DISPLAY,
                width,
                height
            )
            Log.d("ResolutionChanger", "Method invocation result: $result")
            Log.d("ResolutionChanger", "Successfully changed resolution using setForcedDisplaySize")
            return Pair(true, "Resolution changed to ${width}x${height} using IWindowManager")
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "setForcedDisplaySize failed: ${e.message}")
            Log.w("ResolutionChanger", "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }

        // Method 2: Try with different parameter types (some Android versions use different signatures)
        try {
            Log.d("ResolutionChanger", "Trying alternative parameter types...")
            val setForcedDisplaySizeMethod: Method = windowManager.javaClass.getMethod(
                "setForcedDisplaySize",
                Int::class.java,
                Int::class.java,
                Int::class.java
            )
            Log.d("ResolutionChanger", "Found alternative setForcedDisplaySize method")
            val result = setForcedDisplaySizeMethod.invoke(
                windowManager,
                Display.DEFAULT_DISPLAY,
                width,
                height
            )
            Log.d("ResolutionChanger", "Alternative method result: $result")
            return Pair(
                true,
                "Resolution changed to ${width}x${height} using alternative IWindowManager method"
            )
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "Alternative setForcedDisplaySize failed: ${e.message}")
        }

        // Method 3: Try overrideDisplayInfo (newer Android versions)
        try {
            Log.d("ResolutionChanger", "Trying overrideDisplayInfo method...")
            val overrideDisplayInfoMethod: Method = windowManager.javaClass.getMethod(
                "overrideDisplayInfo",
                Int::class.javaPrimitiveType,
                Any::class.java  // DisplayInfo object
            )
            Log.d("ResolutionChanger", "Found overrideDisplayInfo method")
            // This requires creating a DisplayInfo object, which is complex
            Log.d(
                "ResolutionChanger",
                "overrideDisplayInfo requires DisplayInfo object - skipping for now"
            )
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "overrideDisplayInfo not available: ${e.message}")
        }

        // Method 4: Try setDisplayOverrideConfiguration
        try {
            Log.d("ResolutionChanger", "Trying setDisplayOverrideConfiguration...")
            val setDisplayOverrideConfigMethod: Method = windowManager.javaClass.getMethod(
                "setDisplayOverrideConfiguration",
                Any::class.java,  // Configuration
                Int::class.javaPrimitiveType
            )
            Log.d("ResolutionChanger", "Found setDisplayOverrideConfiguration method")
            // This also requires creating a Configuration object
            Log.d(
                "ResolutionChanger",
                "setDisplayOverrideConfiguration requires Configuration object - skipping for now"
            )
        } catch (e: Exception) {
            Log.w(
                "ResolutionChanger",
                "setDisplayOverrideConfiguration not available: ${e.message}"
            )
        }

        // Method 5: List all available methods for debugging
        Log.d("ResolutionChanger", "Available methods in IWindowManager:")
        val methods = windowManager.javaClass.methods
        for (method in methods) {
            if (method.name.contains("Display") || method.name.contains("Size") || method.name.contains(
                    "Resolution"
                )
            ) {
                Log.d(
                    "ResolutionChanger",
                    "Method: ${method.name}, Parameters: ${method.parameterTypes.contentToString()}"
                )
            }
        }

        // Method 6: Try alternative method names using reflection
        try {
            Log.d("ResolutionChanger", "Searching for alternative methods...")
            for (method in methods) {
                if ((method.name.contains("setDisplaySize") || method.name.contains("setForcedSize") ||
                            method.name.contains("setOverrideDisplayModeSize")) && method.parameterTypes.size == 3
                ) {
                    try {
                        Log.d("ResolutionChanger", "Trying method: ${method.name}")
                        val result =
                            method.invoke(windowManager, Display.DEFAULT_DISPLAY, width, height)
                        Log.d("ResolutionChanger", "Method ${method.name} result: $result")
                        Log.d(
                            "ResolutionChanger",
                            "Successfully changed resolution using ${method.name}"
                        )
                        return Pair(
                            true,
                            "Resolution changed to ${width}x${height} using ${method.name}"
                        )
                    } catch (e: Exception) {
                        Log.w("ResolutionChanger", "Method ${method.name} failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "Alternative method search failed: ${e.message}")
        }


        // Method 7: Use Runtime.exec() to run wm size
        try {
            Log.d("ResolutionChanger", "Trying shell command: wm size ${width}x${height}")

            // Split the command properly - wm is the command, size and resolution are arguments
            val process =
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "wm size ${width}x${height}"))
            val exitCode = process.waitFor()

            // Read any output for debugging
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            Log.d("ResolutionChanger", "Shell command output: $output")
            if (errorOutput.isNotEmpty()) {
                Log.d("ResolutionChanger", "Shell command error output: $errorOutput")
            }

            if (exitCode == 0) {
                Log.d("ResolutionChanger", "Shell command succeeded")
                return Pair(true, "Resolution changed to ${width}x${height} using shell command")
            } else {
                Log.w("ResolutionChanger", "Shell command failed with exit code $exitCode")

                // Try alternative approach - direct wm command
                try {
                    Log.d("ResolutionChanger", "Trying direct wm command...")
                    val processAlt =
                        Runtime.getRuntime().exec(arrayOf("wm", "size", "${width}x${height}"))
                    val exitCodeAlt = processAlt.waitFor()

                    if (exitCodeAlt == 0) {
                        Log.d("ResolutionChanger", "Direct wm command succeeded")
                        return Pair(
                            true,
                            "Resolution changed to ${width}x${height} using direct wm command"
                        )
                    } else {
                        Log.w(
                            "ResolutionChanger",
                            "Direct wm command also failed with exit code $exitCodeAlt"
                        )
                    }
                } catch (e: Exception) {
                    Log.w("ResolutionChanger", "Direct wm command failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "Shell command failed: ${e.message}")
        }

        // Method 9: use root
        try {
            Log.d("ResolutionChanger", "Trying root shell command: wm size ${width}x${height}")
            val process =
                Runtime.getRuntime().exec(arrayOf("su", "-c", "wm size ${width}x${height}"))
            val exitCode = process.waitFor()

            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()

            Log.d("ResolutionChanger", "Root shell command output: $output")
            if (errorOutput.isNotEmpty()) {
                Log.d("ResolutionChanger", "Root shell command error output: $errorOutput")
            }

            if (exitCode == 0) {
                Log.d("ResolutionChanger", "Root shell command succeeded")
                return Pair(
                    true,
                    "Resolution changed to ${width}x${height} using root shell command"
                )
            } else {
                Log.w("ResolutionChanger", "Root shell command failed with exit code $exitCode")
            }
        } catch (e: Exception) {
            Log.w("ResolutionChanger", "Root shell command failed: ${e.message}")
        }

        Pair(
            false,
            "All IWindowManager methods failed. This may require system-level permissions, rooted device, or the device doesn't support forced display size changes."
        )

    } catch (e: Exception) {
        Log.e("ResolutionChanger", "IWindowManager resolution change failed: ${e.message}")
        e.printStackTrace()
        Pair(false, "Error using IWindowManager: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var resolutions by remember {
        mutableStateOf(
            DefaultResolutions.all
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun changeResolution(resolution: Resolution) {
        coroutineScope.launch {
            val activity = context as MainActivity
            val (success, message) = changeResolutionWithIWindowManager(
                activity.windowManagerInterface,
                resolution.width,
                resolution.height
            )

            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "OK"
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Resolution Changer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resolutions) { resolution ->
                    ResolutionItem(
                        resolution = resolution,
                        onClick = { changeResolution(resolution) }
                    )
                }
            }
        }

        if (showDialog) {
            CreateResolutionDialog(
                onDismiss = { showDialog = false },
                onSave = { resolution ->
                    resolutions = resolutions + resolution
                    showDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Resolution $resolution added",
                            actionLabel = "OK"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun ResolutionItem(resolution: Resolution, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = resolution.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Width: ${resolution.width}px • Height: ${resolution.height}px",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateResolutionDialog(
    onDismiss: () -> Unit,
    onSave: (Resolution) -> Unit
) {
    var widthText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var widthError by remember { mutableStateOf(false) }
    var heightError by remember { mutableStateOf(false) }

    fun validateAndSave() {
        val width = widthText.toIntOrNull()
        val height = heightText.toIntOrNull()

        widthError = width == null || width <= 0
        heightError = height == null || height <= 0

        if (!widthError && !heightError && width != null && height != null) {
            onSave(Resolution(width, height))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Resolution",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        widthText = it
                        widthError = false
                    },
                    label = { Text("Width") },
                    placeholder = { Text("e.g. 1920") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = widthError,
                    supportingText = if (widthError) {
                        { Text("Width must be greater than 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it
                        heightError = false
                    },
                    label = { Text("Height") },
                    placeholder = { Text("e.g. 1080") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = heightError,
                    supportingText = if (heightError) {
                        { Text("Height must be greater than 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { validateAndSave() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ResolutionChangerTheme {
        MainScreen()
    }
}
