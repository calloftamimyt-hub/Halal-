package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CircleAlert
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.viewmodel.GlobalLanguage
import com.example.database.TrackerDatabase
import com.example.database.NotificationEntity
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCircleAlertScreen(
    savedLocation: String, // from prayer settings
    onBack: () -> Unit,
    onSubmit: (CircleAlert) -> Unit
) {
    val context = LocalContext.current
    val isEnglish = GlobalLanguage.isEnglish
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(if (isEnglish) "Bangladesh" else "বাংলাদেশ") }
    var location by remember { mutableStateOf(savedLocation) }
    
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("photo") }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            mediaUri = it
            mediaType = "photo"
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            mediaUri = it
            mediaType = "video"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Create Circle Alert" else "সার্কেল অ্যালার্ট তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        containerColor = BgLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isEnglish) "Title (e.g., Lost Child)" else "শিরোনাম (যেমন: হারিয়ে যাওয়া শিশু)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isEnglish) "Full Description" else "সম্পূর্ণ বিবরণ") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )
            
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text(if (isEnglish) "Contact Phone Number" else "যোগাযোগের ফোন নম্বর") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(if (isEnglish) "Location (Auto-filled from settings)" else "অবস্থান (সেটিংস থেকে স্বয়ংক্রিয়ভাবে নেওয়া হয়েছে)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mediaUri != null && mediaType == "photo") PrimaryGreen else Color.Gray)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Photo" else "ছবি")
                }
                Button(
                    onClick = { videoLauncher.launch("video/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (mediaUri != null && mediaType == "video") PrimaryGreen else Color.Gray)
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Video" else "ভিডিও")
                }
            }

            if (mediaUri != null) {
                Text(
                    text = if (isEnglish) "Media selected: ${mediaType.uppercase()}" else "মিডিয়া নির্বাচিত: ${if(mediaType=="photo") "ছবি" else "ভিডিও"}",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotEmpty() && contactNumber.isNotEmpty()) {
                        val docId = UUID.randomUUID().toString()
                        val alert = CircleAlert(
                            docId = docId,
                            title = title,
                            description = description,
                            mediaUri = mediaUri?.toString() ?: "",
                            mediaType = mediaType,
                            contactNumber = contactNumber,
                            country = country,
                            location = location,
                            timestamp = System.currentTimeMillis(),
                            status = "PENDING"
                        )
                        
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        // Show Local Push Notification equivalent
                        coroutineScope.launch {
                            val db = TrackerDatabase.getDatabase(context)
                            db.notificationDao().insertNotification(
                                NotificationEntity(
                                    title = if (isEnglish) "🔴 Circle Alert: $title" else "🔴 সার্কেল অ্যালার্ট: $title",
                                    body = if (isEnglish) "A new alert has been published. Tap to view details." else "একটি নতুন সতর্কতা প্রকাশ করা হয়েছে। বিস্তারিত দেখতে ক্লিক করুন।",
                                    timestamp = System.currentTimeMillis(),
                                    type = "GENERAL",
                                    actorName = currentUser?.displayName ?: "Halal Circle"
                                )
                            )
                        }

                        // Write to Firestore as a UserUploadedVideo but with isCircleAlert = true
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val videoData = hashMapOf(
                            "userId" to (currentUser?.uid ?: "unknown"),
                            "author" to (currentUser?.displayName ?: "Unknown Author"),
                            "title" to title,
                            "description" to description,
                            "timestamp" to System.currentTimeMillis(),
                            "status" to "PENDING",
                            "mediaType" to mediaType,
                            "contactNumber" to contactNumber,
                            "country" to country,
                            "location" to location,
                            "isCircleAlert" to true
                        )
                        firestore.collection("videos").document(docId).set(videoData)
                        
                        if (mediaUri != null) {
                            val intent = android.content.Intent(context, AlertUploadService::class.java).apply {
                                putExtra("mediaUri", mediaUri.toString())
                                putExtra("mediaType", mediaType)
                                putExtra("title", title)
                                putExtra("description", description)
                                putExtra("contactNumber", contactNumber)
                                putExtra("location", location)
                                putExtra("docId", docId)
                            }
                            context.startService(intent)
                        }
                        
                        onSubmit(alert)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(if (isEnglish) "Publish Alert" else "অ্যালার্ট প্রকাশ করুন", fontSize = 16.sp)
            }
        }
    }
}
