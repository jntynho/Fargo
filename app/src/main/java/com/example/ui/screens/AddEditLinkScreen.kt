package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.LinkEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditLinkScreen(
    viewModel: MainViewModel,
    linkId: String?,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val links by viewModel.allLinks.collectAsStateWithLifecycle()
    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val studios by viewModel.allStudios.collectAsStateWithLifecycle()

    val existingLink = remember(linkId, links) {
        links.firstOrNull { it.id == linkId }
    }

    var title by remember { mutableStateOf(existingLink?.title ?: "") }
    var coverImage by remember { mutableStateOf(existingLink?.coverImage ?: "") }
    var coverOffset by remember { mutableFloatStateOf(existingLink?.coverOffset ?: 50f) }
    var aspectRatio by remember { mutableStateOf(existingLink?.aspectRatio ?: "16:9") }
    var urlHD by remember { mutableStateOf(existingLink?.urlHD ?: "") }
    var url4K by remember { mutableStateOf(existingLink?.url4K ?: "") }
    var magnetHD by remember { mutableStateOf(existingLink?.magnet ?: "") }
    var magnet4K by remember { mutableStateOf(existingLink?.magnet4K ?: "") }
    var galleryUrls by remember { mutableStateOf(existingLink?.galleryUrls ?: emptyList()) }
    var selectedActorIds by remember { mutableStateOf(existingLink?.actorIds ?: emptyList()) }
    var selectedStudioIds by remember { mutableStateOf(existingLink?.studioIds ?: emptyList()) }

    // Date state & DatePicker dialog
    var assignedDate by remember {
        mutableStateOf(existingLink?.assignedDate ?: existingLink?.createdAt ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedAssignedDate = remember(assignedDate) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(assignedDate))
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = assignedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            assignedDate = selected
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Fully rounded corner shape for all text fields
    val roundedFieldShape = RoundedCornerShape(24.dp)

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            TopAppBar(
                title = { Text(if (existingLink != null) "Edit Scene" else "Add Scene", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val newLink = LinkEntity(
                                    id = existingLink?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    coverImage = coverImage.trim(),
                                    coverOffset = coverOffset,
                                    aspectRatio = aspectRatio,
                                    urlHD = urlHD.trim().ifEmpty { null },
                                    url4K = url4K.trim().ifEmpty { null },
                                    magnet = magnetHD.trim().ifEmpty { null },
                                    magnet4K = magnet4K.trim().ifEmpty { null },
                                    galleryScraperUrl = null,
                                    galleryUrls = galleryUrls,
                                    actorIds = selectedActorIds,
                                    studioIds = selectedStudioIds,
                                    assignedDate = assignedDate
                                )
                                viewModel.saveLink(newLink)
                                viewModel.navigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(20.dp),
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_scene_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Scene Title Input (Rounded Corners)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Scene Title *") },
                shape = roundedFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scene_title_input")
            )

            // 2. Native Date Picker Field (Rounded Corners + Calendar Icon)
            OutlinedTextField(
                value = formattedAssignedDate,
                onValueChange = { },
                readOnly = true,
                label = { Text("Release Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = accent
                        )
                    }
                },
                shape = roundedFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("scene_date_input")
            )

            // 3. Cover Image URL & Live Preview (Rounded Corners)
            OutlinedTextField(
                value = coverImage,
                onValueChange = { coverImage = it },
                label = { Text("Cover Image URL") },
                trailingIcon = {
                    if (coverImage.isNotEmpty()) {
                        IconButton(onClick = { coverImage = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = roundedFieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cover_image_input")
            )

            if (coverImage.isNotEmpty()) {
                Text("Cover Preview & Offset", color = palette.textSecondary, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                ) {
                    AsyncImage(
                        model = coverImage,
                        contentDescription = "Cover Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Offset slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Offset: ${coverOffset.toInt()}%", color = palette.textMuted, fontSize = 12.sp)
                    Slider(
                        value = coverOffset,
                        onValueChange = { coverOffset = it },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }

                // Aspect Ratio Selector
                Text("Aspect Ratio", color = palette.textSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("16:9", "3:2", "5:7", "Adaptive").forEach { ratio ->
                        FilterChip(
                            selected = aspectRatio == ratio,
                            onClick = { aspectRatio = ratio },
                            label = { Text(ratio) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border)

            // 4. Video Stream & Magnet URLs (All with Rounded Corners)
            Text("Stream & Media Sources", color = palette.textPrimary, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = urlHD,
                onValueChange = { urlHD = it },
                label = { Text("HD Stream (1080p URL / HLS / Web)") },
                shape = roundedFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = url4K,
                onValueChange = { url4K = it },
                label = { Text("4K Stream (2160p URL / DASH)") },
                shape = roundedFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = magnetHD,
                onValueChange = { magnetHD = it },
                label = { Text("Magnet / Torbox Link (HD)") },
                shape = roundedFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = magnet4K,
                onValueChange = { magnet4K = it },
                label = { Text("Magnet / Torbox Link (4K)") },
                shape = roundedFieldShape,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = palette.border)

            // 5. Studio Tagging (Tag Studio)
            Text("Tag Studio", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            if (studios.isEmpty()) {
                Text("No studios created yet. Add studios from the Studios menu.", color = palette.textMuted, fontSize = 13.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studios.forEach { studio ->
                        val isSelected = selectedStudioIds.contains(studio.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedStudioIds = if (isSelected) {
                                    selectedStudioIds - studio.id
                                } else {
                                    selectedStudioIds + studio.id
                                }
                            },
                            label = { Text(studio.name) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border)

            // 6. Actor Tagging (Tag Actors)
            Text("Tag Actors", color = palette.textPrimary, fontWeight = FontWeight.Bold)
            if (actors.isEmpty()) {
                Text("No actors added yet. Go to Actors in menu to create actors.", color = palette.textMuted, fontSize = 13.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actors.forEach { actor ->
                        val isSelected = selectedActorIds.contains(actor.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedActorIds = if (isSelected) {
                                    selectedActorIds - actor.id
                                } else {
                                    selectedActorIds + actor.id
                                }
                            },
                            label = { Text(actor.name) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
