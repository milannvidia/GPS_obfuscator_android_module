package be.kuleuven.milanschollier.testApp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.kuleuven.milanschollier.testApp.ui.theme.TestAppTheme
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {
    private var _permissionString = MutableLiveData("Huidige permissie:\n")
    private val permissionString: LiveData<String> = _permissionString
    private var foregroundService: Intent? =null
    private var checkedPerrmissions=MutableLiveData(false)
    private val running=MutableLiveData(false)

    private fun checkPermissions() {
        val permissionArray = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)!= PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        val permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            val hasFine = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val hasForeground = ContextCompat.checkSelfPermission(this,Manifest.permission.FOREGROUND_SERVICE)==PackageManager.PERMISSION_GRANTED
            if (hasFine) {
                _permissionString.value += "Fine Location\n"
            }
            if (hasCoarse) {
                _permissionString.value += "Coarse Location\n"
            }
            if (hasForeground) {
                _permissionString.value += "Foreground\n"
            }
            checkedPerrmissions.value=(hasCoarse||hasFine) && hasForeground
        }

        if(permissionArray.isNotEmpty()){
            permissionRequest.launch(permissionArray.toTypedArray())
        }else{
            val hasFine = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
            val hasForeground = ContextCompat.checkSelfPermission(this,Manifest.permission.FOREGROUND_SERVICE)==PackageManager.PERMISSION_GRANTED
            if (hasFine) {
                _permissionString.value += "Fine Location\n"
            }
            if (hasCoarse) {
                _permissionString.value += "Coarse Location\n"
            }
            if (hasForeground) {
                _permissionString.value += "Foreground\n"
            }

            checkedPerrmissions.value= (hasCoarse||hasFine) && hasForeground
        }


    }

    private fun startTrackingService(priority: Int) {
        println("startTrackingService with priority: $priority")
        if(foregroundService!=null)endTrackingService()
        val fine=ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        foregroundService= Intent(this, LocationTrackingService::class.java)
        foregroundService!!.putExtra("Priority", priority)
        val interval=(if(fine) 30L else 120L)
        foregroundService!!.putExtra("interval", interval)
        foregroundService!!.putExtra("FinePermission", ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        ContextCompat.startForegroundService(this, foregroundService!!)
        running.value=true

    }
    private fun endTrackingService() {
        println("endTrackingService")
        stopService(foregroundService)
        foregroundService=null
        running.value=false
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "locationTrackingChannel",
            "Location Tracking Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            val priorities= listOf(Priority.PRIORITY_HIGH_ACCURACY,Priority.PRIORITY_BALANCED_POWER_ACCURACY,Priority.PRIORITY_LOW_POWER,Priority.PRIORITY_PASSIVE)
            var selectedIndex by remember { mutableIntStateOf(0) }
            TestAppTheme  {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)){
                        Text(
                            text = "Dit is een app voor mijn thesis het stuurt continue locatie gegevens door. Bij privacy concerns stuur me zeker een vraag",
                            modifier = Modifier.padding(innerPadding)
                        )
                        Spinner(
                            selectedIndex=selectedIndex,
                            onOptionSelected = {newIndex->
                                run {
                                    selectedIndex = newIndex
                                }
                            }
                        )
                        TrackButton(running){
                            println("Button clicked")
                            if(foregroundService!=null) endTrackingService()
                            else if(checkedPerrmissions.value==true){
                                startTrackingService(priorities[selectedIndex])
                            }
                        }
                        PermissionsText(
                            permissionString,
                            Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TrackButton(running:LiveData<Boolean>, onClick:() -> Unit){
    val isRunning by running.observeAsState(false)
    Button(onClick = { onClick() }) {
        Text(text = if(isRunning) "Stop tracking" else "Start tracking")
    }
}
@Composable
fun PermissionsText(permissionString: LiveData<String>, modifier: Modifier = Modifier) {
    val text by permissionString.observeAsState("hii")
    Text(
        text = text,
        modifier = modifier
    )
}
@Composable
fun Spinner(selectedIndex: Int, onOptionSelected:(Int)->Unit) {

    val names= listOf("High Accuracy","Balanced Power Accuracy","Low Power","Passive")
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = names[selectedIndex],
            modifier= Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = {expanded=false}) {
            names.forEachIndexed { index, priority ->
                DropdownMenuItem(text = { Text(text = priority) }, onClick = {
                    onOptionSelected(index)
                    expanded=false
                })
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TestAppTheme {
//        Greeting("Android")
//    }
//}