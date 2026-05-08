package com.example.sqlaris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionsActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()
        
        setContent {
            SqlarisTheme {
                val sessions by tokenManager.sessions.collectAsState(initial = emptyList())
                
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Sqlaris-conecciones") }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva conexión")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ConnectionsScreen(
                        sessions = sessions,
                        modifier = Modifier.padding(innerPadding),
                        onSessionClick = { session ->
                            lifecycleScope.launch {
                                tokenManager.setSelectedSession(session.name)
                                startActivity(Intent(this@ConnectionsActivity, SessionDetailActivity::class.java))
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ConnectionsScreen(
        sessions: List<UserSession>,
        modifier: Modifier = Modifier,
        onSessionClick: (UserSession) -> Unit
    ) {
        if (sessions.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay conexiones guardadas. Pulsa + para agregar una.")
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    ConnectionCard(session = session, onClick = { onSessionClick(session) })
                }
            }
        }
    }

    @Composable
    fun ConnectionCard(session: UserSession, onClick: () -> Unit) {
        var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000 * 30) // Actualizar cada 30 seg para más fluidez
                currentTime = System.currentTimeMillis()
            }
        }

        val millisPassed = currentTime - session.issuedAt
        val secondsPassed = millisPassed / 1000
        val secondsRemaining = session.expiresIn - secondsPassed
        val isActive = secondsRemaining > 0

        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.dbName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusText = if (isActive) "Sesión activa" else "Sesión inactiva"
                    val statusColor = if (isActive) Color(0xFF4CAF50) else Color.Red
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isActive) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall
                        )
                        val minutesRemaining = secondsRemaining / 60
                        Text(
                            text = "Expira en $minutesRemaining min",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
