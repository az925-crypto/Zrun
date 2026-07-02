package com.zaaam.Zmusic.ui.zrun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.model.entity.PlaylistEntity

/** Dialog buat playlist baru (kosong). */
@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZR.S2,
        title = { Text("Playlist baru", color = ZR.Tx, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nama playlist") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            DialogAction("Buat", enabled = name.isNotBlank()) { onCreate(name.trim()); onDismiss() }
        },
        dismissButton = { Text("Batal", color = ZR.Mut, modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp)) }
    )
}

/**
 * Dialog "tambah ke playlist": pilih playlist yang ada, atau buat baru + langsung tambah.
 */
@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZR.S2,
        title = { Text("Tambah ke playlist", color = ZR.Tx, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (creating) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nama playlist baru") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // baris "buat baru"
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { creating = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(ZR.Ember),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Buat playlist baru", color = ZR.Tx, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        playlists.forEach { pl ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .clickable { onPick(pl.id); onDismiss() }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(ZR.artBrush(pl.name)))
                                Spacer(Modifier.width(12.dp))
                                Text(pl.name, color = ZR.Tx, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                DialogAction("Buat & tambah", enabled = name.isNotBlank()) { onCreateNew(name.trim()); onDismiss() }
            }
        },
        dismissButton = { Text("Tutup", color = ZR.Mut, modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp)) }
    )
}

@Composable
private fun DialogAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) Modifier.background(ZR.Ember) else Modifier.background(ZR.S3)
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).then(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, color = if (enabled) Color.White else ZR.Faint, fontWeight = FontWeight.Bold)
    }
}
