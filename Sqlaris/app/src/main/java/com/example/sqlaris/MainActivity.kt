package com.example.sqlaris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sqlaris.ui.theme.SqlarisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SqlarisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WelcomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartClicked = {
                            // Ahora navegamos a la pantalla de conexiones
                            startActivity(Intent(this, ConnectionsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onStartClicked: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Bienvenido a Sqlaris",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tu cliente móvil de consulta y exportación de datos SQL.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Sqlaris está diseñado para conectarse dinámicamente a múltiples bases de datos SQL remotas de forma segura. Permite explorar tablas, ejecutar consultas dinámicas y exportar resultados directamente desde tu dispositivo móvil.",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Funciones principales:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        val features = listOf(
            "Conexión dinámica a múltiples motores SQL",
            "Gestión segura de sesiones mediante JWT",
            "Exploración de tablas y columnas",
            "Ejecución de consultas dinámicas de solo lectura",
            "Exportación de datos a archivos Excel",
            "Arquitectura segura con backend FastAPI"
        )
        
        features.forEach { feature ->
            Text(
                text = "• $feature",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comenzar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    SqlarisTheme {
        WelcomeScreen()
    }
}
