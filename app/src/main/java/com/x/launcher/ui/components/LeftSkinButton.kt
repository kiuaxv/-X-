import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x.launcher.ui.theme.XButterDeep
import com.x.launcher.ui.theme.XButterYellow
import com.x.launcher.ui.theme.XCardDark
import com.x.launcher.ui.theme.XWhite

@Composable
fun LeftSkinButton(modifier: Modifier = Modifier) {
 var expanded by remember { mutableStateOf(false) }

 Box(modifier = modifier) {
 IconButton(
 onClick = { expanded = true },
 modifier = Modifier
 .size(46.dp)
 .background(XCardDark, CircleShape)
 .border(1.dp, XButterDeep.copy(alpha = 0.22f), CircleShape)
 ) {
 Icon(
 imageVector = Icons.Default.Folder,
 contentDescription = "Skin Menu",
 tint = XButterYellow,
 modifier = Modifier.size(20.dp)
 )
 }

 DropdownMenu(
 expanded = expanded,
 onDismissRequest = { expanded = false },
 modifier = Modifier.background(XCardDark)
 ) {
 DropdownMenuItem(
 text = { Text("Change Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 DropdownMenuItem(
 text = { Text("View Current Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 DropdownMenuItem(
 text = { Text("Import Skin", color = XWhite) },
 onClick = { expanded = false }
 )
 }
 }
}
