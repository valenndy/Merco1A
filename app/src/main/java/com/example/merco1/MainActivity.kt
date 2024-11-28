package com.example.merco1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.merco1.ui.theme.MERCO1Theme
import com.example.merco1.viewmodel.SignupViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val signupViewModel: SignupViewModel by viewModels()
    private var selectedImageUri: Uri? = null // Variable para almacenar la URI seleccionada

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val selectedImage = data?.data
            selectedImageUri = selectedImage // Asignamos el valor a la variable
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MERCO1Theme {
                App()
            }
        }

        signupViewModel.authState.observe(this) { authState ->
            when (authState) {
                1 -> { /* Show progress */ }
                2 -> Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                3 -> { /* Navigate to profile */ }
            }
        }

        signupViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }


}

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("user_selection") { UserSelectionScreen(navController) }
        composable("login?userType={userType}") { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "buyer"
            LoginScreen(navController, initialUserType = userType)
        }
        composable("signup") { SignupScreen(navController) }
        composable("buyer_dashboard") { BuyerDashboard(navController) }
        composable("seller_dashboard") { SellerDashboard(navController) }
        composable("create_product/{sellerId}") { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            CreateProductScreen(navController, sellerId)
        }
        composable("reserve_product/{productId}/{buyerId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val buyerId = backStackEntry.arguments?.getString("buyerId") ?: ""
            ReserveProductScreen(navController = navController, productId = productId, buyerId = buyerId)
        }
        composable("seller_reservations/{sellerId}") { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            SellerReservationsScreen(navController, sellerId)
        }
    }




}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds
        navController.navigate("user_selection")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

    }
}

@Composable
fun UserSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Quien eres?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { navController.navigate("login?userType=buyer") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff33ffc7)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp)
        ) {
            Text(text = "COMPRADOR", color = Color.White)
        }

        Button(
            onClick = { navController.navigate("login?userType=seller") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff33ffc7)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp)
        ) {
            Text(text = "VENDEDOR", color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón para registrar usuarios
        Button(
            onClick = { navController.navigate("signup") }, // Navega a la pantalla de registro
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff33ffc7)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Crear una cuenta", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}



@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: SignupViewModel = viewModel(),
    initialUserType: String = "buyer" // Pasar el tipo de usuario inicial
) {
    val authState by authViewModel.authState.observeAsState()
    val errorMessage by authViewModel.errorMessage.observeAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf(initialUserType) } // Controlar el tipo de usuario

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), fontSize = 24.sp)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { userType = "buyer" },
                colors = ButtonDefaults.buttonColors(containerColor = if (userType == "buyer") Color.Green else Color.Gray)
            ) {
                Text("Buyer")
            }
            Button(
                onClick = { userType = "seller" },
                colors = ButtonDefaults.buttonColors(containerColor = if (userType == "seller") Color.Green else Color.Gray)
            ) {
                Text("Seller")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.loginUser(email, password, userType) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xff33ffc7)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Iniciar sesión", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (authState) {
            1 -> CircularProgressIndicator() // Estado de carga
            2 -> Text(text = errorMessage ?: "Error desconocido", color = Color.Red) // Error
            3 -> {
                // Navegar dependiendo del tipo de usuario
                if (userType == "buyer") {
                    navController.navigate("buyer_dashboard")
                } else {
                    navController.navigate("seller_dashboard")
                }
            }
        }
    }
}



/*

Recordar que al momento de registrarse se cierra la app porque no se dirige
a una pantalla que no existe debe dirigirse a signupScreen!!!!

*/
@Composable
fun SignupScreen(navController: NavController, signupViewModel: SignupViewModel = viewModel()) {
    val authState by signupViewModel.authState.observeAsState()
    var name by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var celphone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf("buyer") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign Up", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))

        TextField(value = name, onValueChange = { name = it }, label = { Text("First Name") })
        TextField(value = lastname, onValueChange = { lastname = it }, label = { Text("Last Name") })
        TextField(value = celphone, onValueChange = { celphone = it }, label = { Text("Cellphone") })
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())

        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { selectedUserType = "buyer" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedUserType == "buyer") Color.Green else Color.Gray)) {
                Text(text = "Buyer")
            }
            Button(onClick = { selectedUserType = "seller" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedUserType == "seller") Color.Green else Color.Gray)) {
                Text(text = "Seller")
            }
        }

        Button(onClick = {
            signupViewModel.registerUser(
                com.example.merco1.domain.model.User(
                    "",
                    name,
                    lastname,
                    celphone,
                    email
                ), password, selectedUserType)
        }) {
            Text(text = "Register")
        }

        when (authState) {
            1 -> CircularProgressIndicator()
            2 -> Text(text = "Error occurred", color = Color.Red)
            3 -> {
                // Navegar dependiendo del tipo de usuario registrado
                if (selectedUserType == "buyer") {
                    navController.navigate("buyer_dashboard")
                } else if (selectedUserType == "seller") {
                    navController.navigate("seller_dashboard")
                }
            }
        }
    }
}



@Composable
fun BuyerDashboard(navController: NavController) {
    val context = LocalContext.current
    val products = remember { mutableStateListOf<Map<String, Any>>() }

    // Cargar productos desde Firestore
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("products").get()
            .addOnSuccessListener { result ->
                products.clear()
                for (document in result) {
                    document.data.let { products.add(it) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Productos Disponibles", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                ProductCard(product, navController)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Firebase.auth.signOut()
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar Sesión")
        }
    }
}

@Composable
fun ProductCard(product: Map<String, Any>, navController: NavController) {
    val name = product["name"] as? String ?: "Sin nombre"
    val price = product["price"] as? Double ?: 0.0
    val quantity = product["quantity"] as? Int ?: 0
    val description = product["description"] as? String ?: "Sin descripción"
    val productId = product["id"] as? String ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("Precio: $price", style = MaterialTheme.typography.bodyMedium)
            Text("Cantidad: $quantity", style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // Navegar a la pantalla de reservación
                    navController.navigate("reserve_product/$productId/${Firebase.auth.currentUser?.uid}")
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Reservar")
            }
        }
    }
}



@Composable
fun SellerDashboard(navController: NavController) {
    val sellerId = Firebase.auth.currentUser?.uid
    var sellerName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(sellerId) {
        if (sellerId != null) {
            try {
                val snapshot = Firebase.firestore
                    .collection("sellers")
                    .document(sellerId)
                    .get()
                    .await()
                val data = snapshot.data
                sellerName = data?.get("name") as? String ?: "Sin nombre"
            } catch (e: Exception) {
                errorMessage = "Error al cargar datos del vendedor: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Usuario no autenticado"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        } else {
            Text("Bienvenido, $sellerName", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("create_product/${Firebase.auth.currentUser?.uid}") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Producto")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!sellerId.isNullOrEmpty()) {
                Button(
                    onClick = {
                        navController.navigate("seller_reservations/$sellerId")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver Reservaciones")
                }
            } else {
                Text("Usuario no autenticado", color = Color.Red)
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    Firebase.auth.signOut()
                    navController.navigate("login")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}

//NO SE VISUALIZAN LAS RESERVACIONES CREADAS POR EL VENDEDOR!!!!!!!!!!!
@Composable
fun SellerReservationsScreen(navController: NavController, sellerId: String) {
    val context = LocalContext.current
    val reservations = remember { mutableStateListOf<Map<String, Any>>() }


    // Verifica que el sellerId no esté vacío
    LaunchedEffect(sellerId) {
        if (sellerId.isNotBlank()) {
            try {
                Firebase.firestore.collection("reservations")
                    .whereEqualTo("seller_id", sellerId) // Filtro por sellerId
                    .get()
                    .addOnSuccessListener { result ->
                        Log.d("SellerReservations", "Número de reservaciones: ${result.size()}")

                        // Limpiar la lista antes de agregar los nuevos resultados
                        reservations.clear()

                        if (result.isEmpty) {
                            Log.d("SellerReservations", "No hay reservaciones para este vendedor")
                        } else {
                            // Agregar los documentos recuperados
                            for (document in result) {
                                reservations.add(document.data) // Agregar el dato completo del documento
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error al cargar reservaciones: ${e.message}")
                        Toast.makeText(context, "Error al cargar reservaciones: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "ID del vendedor no válido.", Toast.LENGTH_SHORT).show()
        }
        Log.d("seller_id",sellerId )

    }

    // Componente de la interfaz de usuario
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Reservaciones de Productos", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Verificar si no hay reservas y mostrar el mensaje correspondiente
        if (reservations.isEmpty()) {
            Text("No hay reservaciones disponibles", style = MaterialTheme.typography.bodyLarge)
        } else {
            // Mostrar las reservas en una lista
            LazyColumn {
                items(reservations) { reservation ->
                    ReservationCard(reservation) // un composable para mostrar la reservación
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para regresar al panel del vendedor
        Button(
            onClick = { navController.navigate("seller_dashboard") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al Panel")
        }
    }
}



@Composable
fun ReservationCard(reservation: Map<String, Any>) {
    val productId = reservation["product_id"] as? String ?: "Sin ID"
    val buyerId = reservation["buyer_id"] as? String ?: "Sin ID"
    val reservedAt = reservation["reserved_at"] as? Long ?: 0L
    val date = if (reservedAt > 0L) {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(java.util.Date(reservedAt))
    } else {
        "Fecha no disponible"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Producto ID: $productId", style = MaterialTheme.typography.bodyMedium)
            Text("Reservado por: $buyerId", style = MaterialTheme.typography.bodyMedium)
            Text("Fecha: $date", style = MaterialTheme.typography.bodySmall)
        }
    }
}




@Composable
fun CategoryDropdownMenu(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    // Estado para controlar si el menú desplegable está expandido
    var isExpanded by remember { mutableStateOf(false) }

    // Botón para abrir el menú
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (selectedCategory.isEmpty()) "Seleccionar Categoría" else selectedCategory)
        }

        // Menú desplegable
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onCategorySelected(category) // Callback para actualizar la categoría
                        isExpanded = false // Cierra el menú
                    },
                    text = { Text(category) }
                )
            }
        }
    }
}


//v1
@Composable
fun CreateProductScreen(navController: NavController, sellerId: String) {
    val context = LocalContext.current

    // Variables de estado
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    val categories = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar categorías desde Firestore
    LaunchedEffect(Unit) {
        try {
            val snapshot = Firebase.firestore.collection("categories").get().await()
            categories.clear()
            categories.addAll(snapshot.documents.map { it.getString("name") ?: "" })
        } catch (e: Exception) {
            Log.e("FirestoreError", "Error al cargar categorías: ${e.message}")
            Toast.makeText(context, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crear Producto", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Nombre del producto") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Precio del producto") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        TextField(
            value = productQuantity,
            onValueChange = { productQuantity = it },
            label = { Text("Cantidad disponible") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        TextField(
            value = productDescription,
            onValueChange = { productDescription = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown para seleccionar categoría
        CategoryDropdownMenu(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (productName.isNotBlank() && productPrice.isNotBlank() && productQuantity.isNotBlank() && selectedCategory.isNotBlank()) {
                    isLoading = true
                    saveProductToFirestore(
                        name = productName,
                        price = productPrice.toDoubleOrNull() ?: 0.0,
                        quantity = productQuantity.toIntOrNull() ?: 0,
                        description = productDescription,
                        category = selectedCategory,
                        sellerId = sellerId,
                        context = context,
                        navController = navController,
                        onComplete = { isLoading = false }
                    )
                } else {
                    Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Cargando..." else "Guardar Producto")
        }
    }
}



@Composable
fun BuyerProductListScreen(navController: NavController) {
    val context = LocalContext.current
    val products = remember { mutableStateListOf<Map<String, Any>>() }

    // Cargar productos desde Firestore
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("products").get()
            .addOnSuccessListener { result ->
                products.clear()
                for (document in result) {
                    document.data.let { products.add(it) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            }
    }

    // Mostrar lista de productos
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lista de Productos", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(products) { product ->
                ProductCard(product, navController)
            }
        }
    }
}


@Composable
fun ReserveProductScreen(navController: NavController, productId: String, buyerId: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reservar Producto", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            reserveProduct(productId, buyerId, context, navController) // Parámetros corregidos
        }) {
            Text("Confirmar Reservación")
        }
    }
}


fun reserveProduct(productId: String, buyerId: String, context: Context, navController: NavController) {
    // Obtener el seller_id del producto
    Firebase.firestore.collection("products")
        .document(productId)
        .get()
        .addOnSuccessListener { productDocument ->
            if (productDocument.exists()) {
                // Extraer el seller_id del documento del producto
                val sellerId = productDocument.getString("seller_id")

                // Verificar que el seller_id exista
                if (sellerId != null) {
                    // Crear el documento de la reservación con el seller_id correcto
                    val reservationData = mapOf(
                        "buyer_id" to buyerId,
                        "product_id" to productId,
                        "seller_id" to sellerId, // Aquí se usa el seller_id obtenido del producto
                        "reservation_time" to System.currentTimeMillis()
                    )

                    // Guardar la reservación en la colección "reservations"
                    Firebase.firestore.collection("reservations")
                        .add(reservationData)
                        .addOnSuccessListener { reservationDocument ->
                            // La reserva se ha creado con éxito
                            Log.d("ReserveProduct", "Reserva creada con éxito: ${reservationDocument.id}")
                            navController.navigate("buyer_dashboard")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReserveProductError", "Error al crear la reserva: ${e.message}")
                            Toast.makeText(context, "Error al crear la reserva: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Si no se encuentra el seller_id en el producto
                    Toast.makeText(context, "No se encontró el vendedor para este producto.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // El producto no existe
                Toast.makeText(context, "Producto no encontrado.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            // Error al obtener el producto
            Toast.makeText(context, "Error al obtener el producto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}




fun saveProductToFirestore(
    name: String,
    price: Double,
    quantity: Int,
    description: String,
    category: String, // Asegúrate de incluir este parámetro
    sellerId: String,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit
) {
    val productId = UUID.randomUUID().toString()

    val product = hashMapOf(
        "id" to productId,
        "name" to name,
        "price" to price,
        "quantity" to quantity,
        "description" to description,
        "category" to category, // Guardar la categoría seleccionada
        "seller_id" to sellerId,
        "created_at" to System.currentTimeMillis()
    )

    Firebase.firestore.collection("products").document(productId).set(product)
        .addOnSuccessListener {
            onComplete()
            Toast.makeText(context, "Producto creado exitosamente", Toast.LENGTH_SHORT).show()
            navController.navigate("seller_dashboard")
        }
        .addOnFailureListener { e ->
            onComplete()
            Toast.makeText(context, "Error al guardar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}





// Lógica para subir producto a Firestore
fun uploadProduct(
    name: String,
    price: Double,
    quantity: Int,
    description: String,
    category: String,
    imageUri: Uri?,
    sellerId: String,
    context: Context,
    navController: NavController
) {
    if (name.isBlank() || category.isBlank() || imageUri == null) {
        Toast.makeText(context, "Todos los campos son obligatorios, incluida la imagen.", Toast.LENGTH_SHORT).show()
        return
    }

    val storageRef = Firebase.storage.reference.child("products/${UUID.randomUUID()}.jpg")

    try {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "tempImage_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        val inputStream = contentResolver.openInputStream(imageUri)

        if (inputStream != null) {
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            // Subir archivo usando URI desde el archivo temporal
            val tempUri = Uri.fromFile(tempFile)
            storageRef.putFile(tempUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        // Guardar los detalles del producto en Firestore
                        val product = hashMapOf(
                            "name" to name,
                            "price" to price,
                            "quantity" to quantity,
                            "description" to description,
                            "category_id" to category,
                            "image_url" to imageUrl.toString(),
                            "seller_id" to sellerId
                        )

                        Firebase.firestore.collection("products").add(product)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Producto creado exitosamente", Toast.LENGTH_SHORT).show()
                                navController.navigate("seller_dashboard")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreError", "Error al guardar en Firestore: ${e.message}")
                                Toast.makeText(context, "Error al guardar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StorageError", "Error al subir la imagen: ${e.message}")
                    Toast.makeText(context, "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "No se pudo abrir la imagen seleccionada.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("UploadError", "Error procesando la imagen: ${e.message}")
        Toast.makeText(context, "Error procesando la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}












