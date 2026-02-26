package com.duc1607.resolutionchanger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duc1607.resolutionchanger.ui.theme.ResolutionChangerTheme
import java.io.InputStream
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import rikka.shizuku.Shizuku

data class Resolution(
    val width: Int,
    val height: Int,
    val description: String = ""
) {
    override fun toString(): String = "${width}x${height}"
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1000
    }

    // Track Shizuku permission usability (true = binder alive & permission granted)
    var shizukuPermissionGranted by mutableStateOf(false)
        private set

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d("ResolutionChanger", "Shizuku binder received")
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            shizukuPermissionGranted = true
            Log.d("ResolutionChanger", "Shizuku permission already granted on binder receipt")
        } else {
            Log.d("ResolutionChanger", "Requesting Shizuku permission after binder receipt")
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w("ResolutionChanger", "Shizuku binder dead")
        shizukuPermissionGranted = false
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQUEST_CODE_SHIZUKU) {
            val granted = result == PackageManager.PERMISSION_GRANTED
            shizukuPermissionGranted = granted
            Log.d("ResolutionChanger", "Shizuku permission result: granted=$granted")
        }
    }

    private fun hasWriteSecureSettingsPermission(): Boolean =
        checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    private fun canWriteSettings(): Boolean = Settings.System.canWrite(this)

    var windowManagerInterface: Any? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Log permission status for debugging
        Log.d("ResolutionChanger", "WRITE_SECURE_SETTINGS: ${hasWriteSecureSettingsPermission()}")
        Log.d("ResolutionChanger", "Can write settings: ${canWriteSettings()}")

        // Initialize IWindowManager using reflection
        initializeWindowManager()

        // Register Shizuku listeners
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        // Initial Shizuku state check
        if (Shizuku.pingBinder()) {
            Log.d("ResolutionChanger", "Shizuku binder ping success in onCreate")
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                shizukuPermissionGranted = true
                Log.d("ResolutionChanger", "Shizuku permission already granted at startup")
            } else {
                Log.d("ResolutionChanger", "Requesting Shizuku permission at startup")
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
        } else {
            Log.d("ResolutionChanger", "Shizuku binder not available at startup")
        }

        enableEdgeToEdge()
        setContent { ResolutionChangerTheme { MainScreen() } }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
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

    fun getCurrentResolution(): Resolution = try {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        Resolution(size.x, size.y)
    } catch (e: Exception) {
        Log.e("ResolutionChanger", "Failed to get current resolution: ${e.message}")
        Resolution(1080, 1920)
    }
}

private fun runElevatedShell(vararg args: String): Boolean {
    val joined = args.joinToString(" ")
    // Try Shizuku via reflection (avoid direct call to private newProcess)
    try {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Log.d("ResolutionChanger", "Attempt Shizuku exec: $joined")
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val processObj = method.invoke(null, args, null, null)
            val procClass = processObj.javaClass
            val waitForMethod = procClass.getMethod("waitFor")
            val exitCode = waitForMethod.invoke(processObj) as Int
            val getInputStream = procClass.getMethod("getInputStream")
            val getErrorStream = procClass.getMethod("getErrorStream")
            val stdout =
                (getInputStream.invoke(processObj) as InputStream).bufferedReader().readText()
            val stderr =
                (getErrorStream.invoke(processObj) as InputStream).bufferedReader().readText()
            Log.d("ResolutionChanger", "Shizuku stdout: $stdout")
            if (stderr.isNotBlank()) Log.w("ResolutionChanger", "Shizuku stderr: $stderr")
            if (exitCode == 0) return true else Log.w(
                "ResolutionChanger",
                "Shizuku exitCode=$exitCode; fallback to root"
            )
        } else {
            Log.d(
                "ResolutionChanger",
                "Shizuku not usable; binder=${Shizuku.pingBinder()} perm=${Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED}"
            )
        }
    } catch (e: Exception) {
        Log.w("ResolutionChanger", "Shizuku reflection exec failed: ${e.message}; fallback to root")
    }

    // Root fallback
    return try {
        Log.d("ResolutionChanger", "Root exec: $joined")
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", joined))
        val exitCode = process.waitFor()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        Log.d("ResolutionChanger", "Root stdout: $stdout")
        if (stderr.isNotBlank()) Log.w("ResolutionChanger", "Root stderr: $stderr")
        exitCode == 0
    } catch (e: Exception) {
        Log.w("ResolutionChanger", "Root exec failed: ${e.message}")
        false
    }
}

fun changeResolutionWithIWindowManager(
    windowManager: Any?,
    width: Int,
    height: Int
) {
    if (windowManager == null) {
        Log.w("ResolutionChanger", "windowManagerInterface is null; aborting resolution change")
        return
    }
    val success = runElevatedShell("wm", "size", "${width}x${height}")
    Log.d("ResolutionChanger", "Resolution change command success=$success")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var resolutions by remember { mutableStateOf(DefaultResolutions.all) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? MainActivity

    // Tile-set state – which resolutions are checked for the quick tile
    var tileSet by remember { mutableStateOf(ResolutionControl.getTileSet(context)) }

    // Show snackbar when Shizuku availability changes
    LaunchedEffect(activity?.shizukuPermissionGranted) {
        activity?.let {
            if (it.shizukuPermissionGranted) snackbarHostState.showSnackbar("Shizuku ready: elevated commands will use Shizuku")
            else snackbarHostState.showSnackbar("Shizuku unavailable or permission denied; using root fallback")
        }
    }

    fun changeResolution(resolution: Resolution) {
        coroutineScope.launch {
            val act = context as MainActivity
            changeResolutionWithIWindowManager(
                act.windowManagerInterface,
                resolution.width,
                resolution.height
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resolutions) { resolution ->
                    ResolutionItem(
                        resolution = resolution,
                        checkedForTile = tileSet.contains("${resolution.width}x${resolution.height}"),
                        onCheckedChange = { checked ->
                            ResolutionControl.setCheckedForTile(context, resolution, checked)
                            tileSet = ResolutionControl.getTileSet(context)
                        },
                        onClick = { changeResolution(resolution) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Made with ❤️ by duc1607",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
fun ResolutionItem(
    resolution: Resolution,
    checkedForTile: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checkedForTile,
                onCheckedChange = onCheckedChange
            )
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)) {
                Text(
                    text = resolution.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (resolution.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resolution.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CreateResolutionDialog(onDismiss: () -> Unit, onSave: (Resolution) -> Unit) {
    var widthText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var widthError by remember { mutableStateOf(false) }
    var heightError by remember { mutableStateOf(false) }

    fun validateAndSave() {
        val width = widthText.toIntOrNull()
        val height = heightText.toIntOrNull()
        widthError = width == null || width <= 0
        heightError = height == null || height <= 0
        if (!widthError && !heightError && width != null && height != null) onSave(
            Resolution(
                width,
                height
            )
        )
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
                    onValueChange = { widthText = it; widthError = false },
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
                    onValueChange = { heightText = it; heightError = false },
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
        confirmButton = { TextButton(onClick = { validateAndSave() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ResolutionChangerTheme { MainScreen() }
}
