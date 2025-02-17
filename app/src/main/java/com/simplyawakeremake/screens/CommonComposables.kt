package com.simplyawakeremake.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplyawakeremake.R
import com.simplyawakeremake.extensions.isOnline
import kotlinx.coroutines.launch

@Composable
fun <T> ItemList(
    items: List<T>,
    keySelector: ((index: Int) -> Any) = {},
    onclick: (T) -> Unit,
    divider : @Composable () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    var showNoInternetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            count = items.size,
            key = { keySelector(it) },
            itemContent = { index ->
                val item = items[index]
                Surface(Modifier.wrapContentSize().clickable {
                    showNoInternetDialog = !context.isOnline()
                    if (!showNoInternetDialog) {
                        onclick(item)
                    }
                }) {
                    itemContent(item)
                }
                divider()
                if (index < items.lastIndex) divider()
            }
        )
    }
    if (showNoInternetDialog) {
        QuickDismissAlertDialog(
            onDismissRequest = { showNoInternetDialog = false },
            dialogTitle = "Whoops",
            dialogText = "The content of the meditation cannot be loaded because you're device seems to be offline."
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = Color.White,
            strokeWidth = 5.dp
        )
    }
}

@Composable
fun CommonErrorView(throwable: Throwable) {
    Text(
        text = throwable.message ?: "An error has happened",
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun NoInternetScreen(tryAgainAction: () -> Unit) {

    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.baseline_wifi_off_24),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),

            )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Whoops!!",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No Internet connection was found. Check your connection or try again.",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp, start = 25.dp, end = 25.dp)
                .fillMaxWidth(),
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp),
            onClick = { scope.launch { tryAgainAction() } },
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text(
                text = "Try again",
                fontSize = 20.sp,
                color = Color.White
            )
        }

    }
}

@Composable
fun QuickDismissAlertDialog(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    AlertDialog(
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }
            ) {
                Text("Dismiss")
            }
        }
    )
}
