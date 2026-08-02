package com.example.autoresponder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autoresponder.data.AppDatabase
import com.example.autoresponder.data.AppSettings
import com.example.autoresponder.data.ReplyType
import com.example.autoresponder.data.Rule
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).ruleDao() }
    val rules by dao.getAllRules().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var serviceEnabled by remember { mutableStateOf(AppSettings.isServiceEnabled(context)) }
    var apiKey by remember { mutableStateOf(AppSettings.getApiKey(context)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AutoResponder Clone") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = serviceEnabled, onCheckedChange = {
                    serviceEnabled = it
                    AppSettings.setServiceEnabled(context, it)
                })
                Spacer(Modifier.width(8.dp))
                Text(if (serviceEnabled) "الخدمة شغالة" else "الخدمة متوقفة")
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }) { Text("فعّل صلاحية قراءة الإشعارات") }

            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) { Text("فعّل خدمة Accessibility") }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    AppSettings.setApiKey(context, it)
                },
                label = { Text("Claude API Key (اختياري للرد الذكي)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("القواعد الحالية:", style = MaterialTheme.typography.titleMedium)

            LazyColumn {
                items(rules) { rule ->
                    RuleCard(rule = rule, onDelete = {
                        scope.launch { dao.delete(rule) }
                    })
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { rule ->
                scope.launch { dao.insert(rule) }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleCard(rule: Rule, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("المرسل: ${if (rule.senderName == "*") "الكل (افتراضي)" else rule.senderName}")
            Text("النوع: ${if (rule.replyType == ReplyType.FIXED_TEXT) "نص ثابت" else "رد ذكي AI"}")
            if (rule.replyType == ReplyType.FIXED_TEXT) {
                Text("الرد: ${rule.fixedReplyText}")
            }
            Text("الوقت: من ${rule.activeFromHour}:00 إلى ${rule.activeToHour}:00")
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDelete) { Text("حذف") }
        }
    }
}

@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onSave: (Rule) -> Unit) {
    var senderName by remember { mutableStateOf("*") }
    var replyText by remember { mutableStateOf("") }
    var isAiReply by remember { mutableStateOf(false) }
    var fromHour by remember { mutableStateOf("0") }
    var toHour by remember { mutableStateOf("24") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قاعدة جديدة") },
        text = {
            Column {
                OutlinedTextField(
                    value = senderName, onValueChange = { senderName = it },
                    label = { Text("اسم الشخص (أو * للكل)") }
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAiReply, onCheckedChange = { isAiReply = it })
                    Text("رد ذكي (AI) بدل النص الثابت")
                }
                OutlinedTextField(
                    value = replyText, onValueChange = { replyText = it },
                    label = { Text("نص الرد (أو الرد الاحتياطي لو AI فشل)") }
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = fromHour, onValueChange = { fromHour = it },
                        label = { Text("من ساعة") }, modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = toHour, onValueChange = { toHour = it },
                        label = { Text("لحد ساعة") }, modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Rule(
                        senderName = senderName.ifBlank { "*" },
                        replyType = if (isAiReply) ReplyType.AI_SMART else ReplyType.FIXED_TEXT,
                        fixedReplyText = replyText,
                        activeFromHour = fromHour.toIntOrNull() ?: 0,
                        activeToHour = toHour.toIntOrNull() ?: 24
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
