package com.memeitizer.kushstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlinx.coroutines.delay

data class App(
    val id: Int,
    val name: String,
    val subtitle: String,
    val icon: String,
    val screenshots: List<String>,
    val description: String,
    val rating: Double,
    val reviews: Int,
    val size: String,
    val version: String,
    @SerializedName("apk_url") val apkUrl: String,
    val featured: Boolean = false,
    val category: String
)

data class ApiResponse(val success: Boolean, val apps: List<App>)

interface ApiService {
    @GET("index.php")
    suspend fun getApps(): ApiResponse
}

val retrofit = Retrofit.Builder()
    .baseUrl("https://memeitizer.com/appstore/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KushStoreTheme { KushStoreApp() }
        }
    }
}

@Composable
fun KushStoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00FF88),
            secondary = Color(0xFF8A2BE2),
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF1A1A1A)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter("https://memeitizer.com/appstore/bg_psychedelic.jpg"),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.3f,
                modifier = Modifier.fillMaxSize()
            )
            content()
        }
    }
}

@Composable
fun KushStoreApp() {
    var apps by remember { mutableStateOf<List<App>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<App?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(800)
        try {
            val response = apiService.getApps()
            if (response.success) apps = response.apps
        } catch (e: Exception) {}
        loading = false
    }

    if (selectedApp != null) {
        AppDetailScreen(app = selectedApp!!, onBack = { selectedApp = null }, onInstall = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(selectedApp!!.apkUrl), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.Black.copy(alpha = 0.95f)) {
                    listOf("Today", "Games", "Apps", "Search").forEach {
                        NavigationBarItem(selected = false, onClick = {}, icon = { Text(it, color = Color(0xFF00FF88)) }, label = { Text(it, color = Color.Gray) })
                    }
                }
            }
        ) { padding ->
            if (loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00FF88), strokeWidth = 6.dp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(apps.size) { i -> AppCard(apps[i]) { selectedApp = apps[i] } }
                }
            }
        }
    }
}

@Composable
fun AppCard(app: App, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(0.9f)), shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(4.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = rememberAsyncImagePainter(app.icon), null, Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)).border(2.dp, Color(0xFF00FF88), RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp))
            Text(app.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
            Text(app.subtitle, fontSize = 12.sp, color = Color(0xFF00FF88))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${app.rating}", fontWeight = FontWeight.Bold, color = Color.Yellow)
                Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                Text(" (${app.reviews})", color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)), shape = RoundedCornerShape(16.dp), modifier = Modifier.height(40.dp).fillMaxWidth()) {
                Text("GET", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDetailScreen(app: App, onBack: () -> Unit, onInstall: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { app.screenshots.size })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            HorizontalPager(state = pagerState, modifier = Modifier.height(400.dp)) { page ->
                Image(painter = rememberAsyncImagePainter(app.screenshots[page]), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(400.dp))
            }
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).align(Alignment.TopStart)) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = rememberAsyncImagePainter(app.icon), null, Modifier.size(90.dp).clip(RoundedCornerShape(22.dp)))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(app.subtitle, color = Color(0xFF00FF88), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onInstall, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))) {
                Text("GET", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("Version \( {app.version} • \){app.size}", color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text(app.description, fontSize = 16.sp, color = Color(0xFFDDDDDD), lineHeight = 24.sp)
        }
    }
}