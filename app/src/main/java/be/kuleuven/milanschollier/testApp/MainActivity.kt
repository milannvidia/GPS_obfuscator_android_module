package be.kuleuven.milanschollier.testApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import be.kuleuven.milanschollier.safegps.LocationManager
import be.kuleuven.milanschollier.safegps.LocationObfuscatorV1
import be.kuleuven.milanschollier.testApp.ui.theme.TestAppTheme
import com.google.android.gms.location.Priority


class MainActivity : ComponentActivity() {
    private val locationManager= LocationManager.getInstance(this)
    private val componentActivity:ComponentActivity=this

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager.locationObfuscator=LocationObfuscatorV1.getInstance(this.filesDir)
        enableEdgeToEdge()
        setContent {
            val priorities= listOf(Priority.PRIORITY_HIGH_ACCURACY,Priority.PRIORITY_BALANCED_POWER_ACCURACY,Priority.PRIORITY_LOW_POWER,Priority.PRIORITY_PASSIVE)
            var selectedIndex by remember { mutableIntStateOf(0) }
            TestAppTheme  {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)){
                        Text(
                            text = "Dit is een app voor mijn thesis het stuurt continue locatie gegevens door. Bij privacy concerns stuur me zeker een vraag",
                            modifier = Modifier.padding(10.dp)
                        )
                        val names= listOf("High Accuracy","Balanced Power Accuracy","Low Power","Passive")
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Column {
                                OutlinedTextField(
                                    value = names[selectedIndex],
                                    onValueChange = {},
                                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = "Priority") }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    names.forEachIndexed { index, priority ->
                                        DropdownMenuItem(
                                            text = { Text(text = priority) },
                                            onClick = {
                                                selectedIndex = index
                                                expanded = false
                                            })
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Transparent)
                                    .padding(10.dp)
                                    .clickable(onClick = {
                                        expanded = !expanded
                                    })
                            )
                        }
                        OutlinedTextField(
                            value = "5",
                            onValueChange = { },
                            trailingIcon = {Icon(Icons.Outlined.ArrowDropDown,null)},
                            modifier= Modifier.fillMaxWidth(),
                            label = { Text(text = "Priority") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )


                        Row {
                            FlowRow (modifier = Modifier.fillMaxWidth() ) {
                                Button(onClick = {
                                    locationManager.startTracking()
                                }) {
                                    Text(text = if(false) "Stop tracking" else "Start tracking")
                                }
                                Button(onClick ={
                                    locationManager.getCoarsePermission( componentActivity,true)
                                }) {
                                    Text(text = "Ask Coarse Permission")
                                }
                                Button(onClick ={
                                    locationManager.getFinePermission( componentActivity,true)
                                }) {
                                    Text(text = "Ask Fine Permission")
                                }
                            }

                        }
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
fun PrioritySpinner(selectedIndex: Int, onOptionSelected:(Int)->Unit) {

    val names= listOf("High Accuracy","Balanced Power Accuracy","Low Power","Passive")
    var expanded by remember { mutableStateOf(false) }
    Box{
        Column {
            OutlinedTextField(
                value = names[selectedIndex],
                onValueChange = {},
                trailingIcon = {Icon(Icons.Outlined.ArrowDropDown,null)},
                readOnly = true,
                modifier= Modifier.fillMaxWidth(),
                label = { Text(text = "Priority") }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {expanded=false},
                modifier = Modifier.fillMaxWidth()
            ) {
                names.forEachIndexed { index, priority ->
                    DropdownMenuItem(text = { Text(text = priority) }, onClick = {
                        onOptionSelected(index)
                        expanded=false
                    })
                }
            }
        }
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .padding(10.dp)
                .clickable(onClick = {
                    expanded=!expanded
                })
        )
    }

}
