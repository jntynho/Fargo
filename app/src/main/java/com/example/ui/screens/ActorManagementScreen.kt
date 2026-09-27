package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entity.ActorEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current

    val actors by viewModel.allActors.collectAsStateWithLifecycle()
    val links by viewModel.allLinks.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var actorToEdit by remember { mutableStateOf<ActorEntity?>(null) }

    Scaffold(
        containerColor = palette.bg,
        topBar = {
            TopAppBar(
                title = { Text("Actors (${actors.size})", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_actor_button")) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Actor", tint = accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (actors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(54.dp))
                    Text("No actors in vault", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                    Button(onClick = { showAddDialog = true }) {
                        Text("Add Actor")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(actors, key = { it.id }) { actor ->
                    val sceneCount = links.count { it.actorIds.contains(actor.id) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .clickable {
                                // Filter scenes by this actor
                                viewModel.searchQuery.value = actor.name
                                viewModel.navigateTo(ScreenState.Home)
                            },
                        colors = CardDefaults.cardColors(containerColor = palette.cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (actor.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = actor.imageUrl,
                                    contentDescription = actor.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, accent, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(palette.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(40.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = actor.name,
                                color = palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Text(
                                text = "$sceneCount scenes",
                                color = palette.textMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row {
                                IconButton(
                                    onClick = { actorToEdit = actor },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteActor(actor.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Actor Dialog
    if (showAddDialog || actorToEdit != null) {
        val editing = actorToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var imageUrl by remember { mutableStateOf(editing?.imageUrl ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                actorToEdit = null
            },
            title = { Text(if (editing != null) "Edit Actor" else "Add Actor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Actor Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("actor_name_input")
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Profile Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val actor = ActorEntity(
                                id = editing?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                imageUrl = imageUrl.trim()
                            )
                            viewModel.saveActor(actor)
                            showAddDialog = false
                            actorToEdit = null
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    actorToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
