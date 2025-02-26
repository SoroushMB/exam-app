import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.d1am.R
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Main Activity: Sets up the TripSearcher interface as the core view
class TripSearcherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the TripSearcherScreen as the content view
        setContent {
            TripSearcherScreen()
        }
    }
}

// Data model for API response items
data class TripData(
    val type: String,
    val title: String,
    val content: String
)

// ViewModel handles asset loading, API data fetching, and error handling.
class TripSearcherViewModel : ViewModel() {
    // UI state: list of trip data and additional assets from assets.txt
    var tripDataList by mutableStateOf(listOf<TripData>())
    var assetsList by mutableStateOf(listOf<String>())

    // Load additional assets defined in assets.txt
    fun loadAssets(context: Context) {
        try {
            val inputStream = context.assets.open("assets.txt")
            val content = inputStream.bufferedReader().use { it.readText() }
            assetsList = content.lines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e("TripSearcherViewModel", "Error loading assets", e)
        }
    }

    // Fetch JSON data from the API endpoint and parse into TripData objects
    fun fetchTripData() {
        viewModelScope.launch {
            try {
                val url = URL("https://test-iws.s3.ir-thr-at1.arvanstorage.ir/iws1402_api/home.json")
                (url.openConnection() as? HttpURLConnection)?.let { connection ->
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        tripDataList = parseTripData(response)
                    } else {
                        Log.e("TripSearcherViewModel", "Error fetching data: ${connection.responseCode}")
                    }
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("TripSearcherViewModel", "Exception during fetching trip data", e)
            }
        }
    }

    // Parse the JSON response to extract a list of TripData objects.
    fun parseTripData(jsonString: String): List<TripData> {
        val list = mutableListOf<TripData>()
        try {
            val jsonObject = JSONObject(jsonString)
            val dataArray = jsonObject.getJSONArray("data")
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val type = item.getString("type")
                val title = item.getString("title")
                val content = item.getString("content")
                list.add(TripData(type, title, content))
            }
        } catch (e: Exception) {
            Log.e("TripSearcherViewModel", "Error parsing JSON", e)
        }
        return list
    }

    // Action triggered by the interactive “action_link” component.
    fun handleActionLink() {
        // For example, re-fetch API data or navigate to a detailed view.
        fetchTripData()
    }
}

// Main UI composable for TripSearcher
@Composable
fun TripSearcherScreen(viewModel: TripSearcherViewModel = viewModel()) {
    val context = LocalContext.current

    // On first composition, load assets and fetch API data.
    LaunchedEffect(Unit) {
        viewModel.loadAssets(context)
        viewModel.fetchTripData()
    }

    // Scaffold provides a top-level layout with a bottom navigation bar.
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Navigation: Home action */ },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Navigation: Info action */ },
                    icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Info") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            // Load and display the application logo from assets (logo.jpg should be in the drawable folder)
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "Application Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Display session context information
            Text(text = "Session: 3.5 Hours – Module 1 – D1AM", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            // Render fetched content based on its type ("slide" or "story")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.tripDataList.size) { index ->
                    val data = viewModel.tripDataList[index]
                    when (data.type) {
                        "slide" -> SlideContent(data)
                        "story" -> StoryContent(data)
                        else -> {} // Handle unexpected types if needed.
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            // Interactive action_link component triggering API behaviors.
            Button(onClick = { viewModel.handleActionLink() }) {
                Text("Action Link")
            }
        }
    }
}

// Composable to render "slide" type content
@Composable
fun SlideContent(data: TripData) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = data.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = data.content)
        }
    }
}

// Composable to render "story" type content
@Composable
fun StoryContent(data: TripData) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = data.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = data.content)
        }
    }
}
