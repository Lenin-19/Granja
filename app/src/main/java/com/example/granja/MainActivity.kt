package com.example.granja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.granja.ui.theme.GranjaTheme

// capacidad máxima del recinto (es una constante, no un estado)
private const val CAPACIDAD = 8

// modelo de datos: el id es un Int que genero yo, la edad va en meses (Int)
data class Animal(
    val id: Int,
    val nombre: String,
    val especie: String,
    val edadMeses: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GranjaTheme {
                // el Scaffold me da innerPadding para no quedar bajo las barras del sistema
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaGranja(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Saver para guardar la lista al girar la pantalla.
// Convierte cada Animal en una lista plana (id, nombre, especie, edad) que el Bundle sí admite
fun animalesSaver(): Saver<SnapshotStateList<Animal>, Any> = listSaver(
    save = { lista ->
        lista.flatMap { listOf<Any>(it.id, it.nombre, it.especie, it.edadMeses) }
    },
    restore = { datos ->
        // cada animal ocupa 4 posiciones, así que los vuelvo a montar de 4 en 4
        datos.chunked(4).map {
            Animal(it[0] as Int, it[1] as String, it[2] as String, it[3] as Int)
        }.toMutableStateList()
    }
)

@Composable
fun PantallaGranja(modifier: Modifier = Modifier) {
    // estado: todo con rememberSaveable para que sobreviva a la recreación de la actividad
    val animales = rememberSaveable(saver = animalesSaver()) { mutableStateListOf<Animal>() }
    var siguienteId by rememberSaveable { mutableIntStateOf(1) } // solo sube, nunca se reutiliza
    var nombre by rememberSaveable { mutableStateOf("") }
    var especie by rememberSaveable { mutableStateOf("") }
    var edad by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    // el contador y el límite salen del tamaño de la lista, no hay contador aparte
    val recintoCompleto = animales.size >= CAPACIDAD

    fun registrar() {
        if (animales.size >= CAPACIDAD) return // por seguridad, aunque el botón ya está deshabilitado

        val nombreLimpio = nombre.trim()
        val especieLimpia = especie.trim()
        // toIntOrNull devuelve null con letras, decimales o vacío, así que no peta
        val meses = edad.trim().toIntOrNull()

        // solo enseño el primer error que encuentro
        when {
            nombreLimpio.isEmpty() -> error = "El nombre no puede estar vacío"
            especieLimpia.isEmpty() -> error = "La especie no puede estar vacía"
            meses == null -> error = "La edad tiene que ser un número entero de meses"
            meses !in 0..360 -> error = "La edad tiene que estar entre 0 y 360 meses"
            else -> {
                animales.add(Animal(siguienteId, nombreLimpio, especieLimpia, meses))
                siguienteId++
                // limpio el formulario y el error
                nombre = ""
                especie = ""
                edad = ""
                error = null
            }
        }
    }

    // todo dentro de la LazyColumn para que en horizontal se pueda hacer scroll y no se tape nada
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "cabecera") {
            Cabecera(total = animales.size, completo = recintoCompleto)
        }
        item(key = "formulario") {
            Formulario(
                nombre = nombre,
                especie = especie,
                edad = edad,
                error = error,
                botonActivo = !recintoCompleto,
                onNombreChange = { nombre = it },
                onEspecieChange = { especie = it },
                onEdadChange = { edad = it },
                onRegistrar = { registrar() }
            )
        }
        if (animales.isEmpty()) {
            item(key = "vacio") { MensajeVacio() }
        } else {
            // la key es el id, así Compose distingue cada tarjeta aunque haya nombres repetidos
            items(animales, key = { it.id }) { animal ->
                TarjetaAnimal(
                    animal = animal,
                    // borro por id, no por nombre, para no tocar a otro "Luna"
                    onEliminar = { animales.removeAll { a -> a.id == animal.id } }
                )
            }
        }
    }
}

@Composable
fun Cabecera(total: Int, completo: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Mi granja", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Animales registrados: $total / $CAPACIDAD")
        if (completo) {
            Text(
                text = "Recinto completo",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun Formulario(
    nombre: String,
    especie: String,
    edad: String,
    error: String?,
    botonActivo: Boolean,
    onNombreChange: (String) -> Unit,
    onEspecieChange: (String) -> Unit,
    onEdadChange: (String) -> Unit,
    onRegistrar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = especie,
            onValueChange = onEspecieChange,
            label = { Text("Especie") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = edad,
            onValueChange = onEdadChange,
            label = { Text("Edad (meses)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        // mensaje de error, solo sale si hay uno
        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onRegistrar,
            enabled = botonActivo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar animal")
        }
    }
}

@Composable
fun TarjetaAnimal(animal: Animal, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = animal.nombre, style = MaterialTheme.typography.titleMedium)
                Text(text = animal.especie)
                Text(text = "${animal.edadMeses} meses")
            }
            Button(onClick = onEliminar) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
fun MensajeVacio() {
    Text(
        text = "No hay animales registrados",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PantallaGranjaPreview() {
    GranjaTheme {
        PantallaGranja()
    }
}