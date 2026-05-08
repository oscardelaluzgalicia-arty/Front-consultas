package com.example.sqlaris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import com.example.sqlaris.ui.theme.GreenSuccess
import com.example.sqlaris.ui.theme.RedError
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
                            title = { 
                                Text(
                                    "Mis Conexiones", 
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this, LoginActivity::class.java))
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva conexión")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Dns, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay conexiones guardadas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
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
                delay(30000)
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = session.dbName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isActive) GreenSuccess else RedError)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "Sesión Activa" else "Sesión Inactiva",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) GreenSuccess else RedError,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        if (isActive) {
                            Text(
                                text = " • Expira en ${secondsRemaining / 60} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
