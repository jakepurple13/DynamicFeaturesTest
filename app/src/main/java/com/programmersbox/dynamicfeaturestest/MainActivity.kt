package com.programmersbox.dynamicfeaturestest

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.ktx.bytesDownloaded
import com.google.android.play.core.ktx.totalBytesToDownload
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.android.play.core.tasks.Task
import com.google.android.play.core.tasks.Tasks
import com.programmersbox.dynamicfeaturestest.ui.theme.DynamicFeaturesTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val manager by lazy { SplitInstallManagerFactory.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynamicFeaturesTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Features(viewModel { DynamicFeaturesViewModel(manager, this@MainActivity) })
                }
            }
        }
    }
}

val moduleName = "dynamicfeature_one"
val dynamicFeatureActivity = "com.programmersbox.dynamicfeature_one.FeatureActivity"

enum class Status {
    Downloading, Installing, None, Downloaded, Installed
}

class DynamicFeaturesViewModel(
    val manager: SplitInstallManager,
    context: Context
) : ViewModel() {

    var progress by mutableStateOf(0L)
    var status by mutableStateOf(Status.None)

    init {
        manager.registerListener {
            when (it.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    status = Status.Downloading
                    progress = it.bytesDownloaded / it.totalBytesToDownload
                }
                SplitInstallSessionStatus.CANCELED -> {

                }
                SplitInstallSessionStatus.CANCELING -> {

                }
                SplitInstallSessionStatus.DOWNLOADED -> {
                    status = Status.Downloaded
                }
                SplitInstallSessionStatus.FAILED -> {

                }
                SplitInstallSessionStatus.INSTALLED -> {
                    status = Status.Installed
                    SplitInstallHelper.updateAppInfo(context)
                }
                SplitInstallSessionStatus.INSTALLING -> {
                    status = Status.Installing
                }
                SplitInstallSessionStatus.PENDING -> {

                }
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    try {
                        context.startIntentSender(it.resolutionIntent()?.intentSender, null, 0, 0, 0)
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
                SplitInstallSessionStatus.UNKNOWN -> {

                }
            }
        }
    }

    fun startOrOpen(context: Context) {
        if (manager.installedModules.contains(moduleName)) {
            val i = Intent(Intent.ACTION_VIEW)
            i.setClassName(context.packageName, dynamicFeatureActivity)
            context.startActivity(i)
            return
        }

        val s = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()
        manager.startInstall(s).addOnSuccessListener { println("$it success") }
        println("Starting install for $moduleName")
    }

    fun uninstall() {
        viewModelScope.launch(Dispatchers.IO) {
            manager.deferredUninstall(manager.installedModules.toList()).addOnSuccessListener {
                status = Status.None
                println(manager.installedModules.toString())
            }.await()
        }
    }
}

private fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Features(vm: DynamicFeaturesViewModel = viewModel()) {
    val context = LocalContext.current
    Scaffold { p ->
        Crossfade(targetState = vm.status) {
            Column(
                modifier = Modifier.padding(p)
            ) {
                when (it) {
                    Status.Downloading -> {
                        Text("Downloading")
                        Text(vm.progress.toString())
                    }
                    Status.Installing -> {
                        CircularProgressIndicator()
                        Text("Installing")
                    }
                    Status.None -> {
                        Text(vm.manager.installedModules.toString())
                        Button(onClick = { vm.startOrOpen(context) }) {
                            Text("Start it!")
                        }
                        Button(onClick = { vm.uninstall() }) {
                            Text("Uninstall")
                        }
                    }
                    Status.Downloaded -> {
                        Text("Downloaded!")
                    }
                    Status.Installed -> {
                        Button(onClick = { vm.startOrOpen(context) }) {
                            Text("Start it!")
                        }
                        Button(onClick = { vm.uninstall() }) {
                            Text("Uninstall")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DynamicFeaturesTestTheme {

    }
}