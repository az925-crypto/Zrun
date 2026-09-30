package com.zaaam.Zmusic.ui.zrun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
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
        containerColor = ZR.S1,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Playlist baru", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx) },
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
        dismissButton = {
            Text(
                "Batal",
                style = zStyle(13.sp, FontWeight.SemiBold),
                color = ZR.Mut,
                modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp)
            )
        }
    )
}

/** Dialog "tambah ke playlist": pilih yang ada, atau buat baru + langsung tambah. */
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
        containerColor = ZR.S1,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Tambah ke playlist", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx) },
        text = {
            Column {
                if (creating) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Nama playlist baru") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { creating = true }
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZR.Ember),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", style = zStyle(20.sp, FontWeight.Bold), color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Buat playlist baru",
                            style = zStyle(14.sp, FontWeight.SemiBold),
                            color = ZR.Tx
                        )
                    }
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        playlists.forEach { pl ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPick(pl.id); onDismiss() }
                                    .padding(vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZRArt(seed = pl.name, size = 38.dp, radius = 10.dp)
                                Text(pl.name, style = zStyle(14.sp, FontWeight.Normal), color = ZR.Tx)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                DialogAction("Buat dan tambah", enabled = name.isNotBlank()) {
                    onCreateNew(name.trim()); onDismiss()
                }
            }
        },
        dismissButton = {
            Text(
                "Tutup",
                style = zStyle(13.sp, FontWeight.SemiBold),
                color = ZR.Mut,
                modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp)
            )
        }
    )
}

@Composable
private fun DialogAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) ZR.Ember else ZR.S2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            style = zStyle(14.sp, FontWeight.Bold),
            color = if (enabled) Color.White else ZR.Faint
        )
    }
}
