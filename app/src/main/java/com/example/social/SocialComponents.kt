package com.example.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Supabase
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import io.github.jan.supabase.auth.auth
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch

@Composable
fun WhatsOnYourMindSection() {
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("User") }
    var currentUserAvatar by remember { mutableStateOf<String?>(null) }
    
    val auth = remember { Supabase.client.auth }

    LaunchedEffect(Unit) {
        val user = auth.currentUserOrNull()
        isUserLoggedIn = user != null
        if (user != null) {
            currentUserName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
            currentUserAvatar = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "")
        }
    }

    if (!isUserLoggedIn) {
        return
    }

    var textInput by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedMediaUri = uri }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedMediaUri = uri }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile Logo Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(currentUserName.take(1).uppercase(), color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What's on your mind?", color = TextGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }
        
        if (selectedMediaUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Media attached: *1 media*", color = PrimaryGreen, fontSize = 12.sp, modifier = Modifier.padding(start=52.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.LightGray.copy(alpha=0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = "Photo", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photo", color = TextDark, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { videoPickerLauncher.launch("video/*") }) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "Video", tint = Color(0xFFF44336))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Video", color = TextDark, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* Handle Upload logic later */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Post")
        }
    }
    
    // Video Feed below What's on your mind
    VideoFeedSection()
}

@Composable
fun VideoFeedSection() {
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val response: io.ktor.client.statement.HttpResponse = com.example.network.ApiClient.ktor.get("${com.example.network.ApiConfig.BASE_URL}api/videos")
            val data = response.body<VideoResponse>()
            videos = data.videos
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text("Recent Posts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (videos.isEmpty()) {
            Text("No posts available right now.", color = TextGray, fontSize = 14.sp)
        } else {
            videos.forEach { video ->
                VideoPostCard(video)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VideoPostCard(video: VideoItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Mock user details
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("User", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("Just now", fontSize = 12.sp, color = TextGray)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Uploaded video file: ${video.filename}", color = TextDark, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(12.dp))
        // Video Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.VideoLibrary, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(48.dp))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Like", color = TextGray, fontSize = 14.sp)
            Text("Comment", color = TextGray, fontSize = 14.sp)
            Text("Share", color = TextGray, fontSize = 14.sp)
            Text("Report", color = Color.Red, fontSize = 14.sp)
        }
    }
}
