package com.example.merco1

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
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
        //composable("profile") { ProfileScreen(navController) }
        composable("buyer_dashboard") { BuyerDashboard(navController) }
        composable("seller_dashboard") { SellerDashboard(navController) }
        //v1
        composable("create_product/{sellerId}") { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            CreateProductScreen(navController, sellerId)
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

        Text(text = "O inicia sesión con:", color = Color.Black, fontSize = 16.sp)

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
            3 -> navController.navigate("profile")
        }
    }
}



@Composable
fun BuyerDashboard(navController: NavController) {
    val context = LocalContext.current
    val userId = Firebase.auth.currentUser?.uid
    var buyerName by remember { mutableStateOf("") }
    var buyerEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val snapshot = Firebase.firestore
                    .collection("buyers")
                    .document(userId)
                    .get()
                    .await()
                val data = snapshot.data
                if (data != null) {
                    buyerName = data["name"] as? String ?: "Sin nombre"
                    buyerEmail = data["email"] as? String ?: "Sin correo"
                } else {
                    errorMessage = "No se encontró información del comprador."
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar datos: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Usuario no autenticado."
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
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            errorMessage.isNotEmpty() -> {
                Text(text = errorMessage, color = Color.Red)
            }
            else -> {
                // Mostrar información del comprador
                Text(
                    text = "Welcome, $buyerName",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email: $buyerEmail",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            Firebase.auth.signOut()
            navController.navigate("login")
        }) {
            Text(text = "Log Out")
        }
    }
}


@Composable
fun SellerDashboard(navController: NavController) {
    val context = LocalContext.current
    val userId = Firebase.auth.currentUser?.uid
    var sellerName by remember { mutableStateOf("") }
    var sellerEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Cargar datos del vendedor desde Firestore
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val snapshot = Firebase.firestore
                    .collection("sellers")
                    .document(userId)
                    .get()
                    .await()
                val data = snapshot.data
                if (data != null) {
                    sellerName = data["name"] as? String ?: "Sin nombre"
                    sellerEmail = data["email"] as? String ?: "Sin correo"
                } else {
                    errorMessage = "No se encontró información del vendedor."
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar datos: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Usuario no autenticado."
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
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            errorMessage.isNotEmpty() -> {
                Text(text = errorMessage, color = Color.Red)
            }
            else -> {
                // Mostrar información del vendedor
                Text(
                    text = "Welcome, $sellerName",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Email: $sellerEmail",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            Firebase.auth.signOut()
            navController.navigate("login")
        }) {
            Text(text = "Log Out")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para crear un producto
        Button(onClick = {
            navController.navigate("create_product/${Firebase.auth.currentUser?.uid}")
        }) {
            Text("Crear Producto")
        }
    }
}

//v1
@Composable
fun CreateProductScreen(navController: NavController, sellerId: String) {
    val context = LocalContext.current

    // Variables de estado para los campos
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productQuantity by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Lista de categorías (estado dinámico)
    val categories = remember { mutableStateListOf<String>() }

    // Selector de imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    // Cargar categorías desde Firestore
    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        try {
            val snapshot = db.collection("categories").get().await()
            categories.clear()
            categories.addAll(snapshot.documents.map { it.getString("name") ?: "" })
        } catch (e: Exception) {
            Toast.makeText(context, "Error cargando categorías: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Diseño de la pantalla
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Habilita desplazamiento
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text("Crear Producto", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Campos del formulario
        TextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("Nombre del producto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = productPrice,
            onValueChange = { productPrice = it },
            label = { Text("Precio del producto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = productQuantity,
            onValueChange = { productQuantity = it },
            label = { Text("Cantidad disponible") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = productDescription,
            onValueChange = { productDescription = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seleccionar categoría
        DropdownMenu(
            expanded = true,
            onDismissRequest = { /* Manejar cierre */ }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    onClick = { selectedCategory = category },
                    text = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para seleccionar imagen
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Seleccionar Imagen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar la imagen seleccionada (si existe)
        selectedImageUri?.let { uri ->
            Text("Imagen seleccionada: $uri", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar producto
        Button(onClick = {
            uploadProduct(
                productName,
                productPrice.toDoubleOrNull() ?: 0.0,
                productQuantity.toIntOrNull() ?: 0,
                productDescription,
                selectedCategory,
                selectedImageUri,
                sellerId,
                context,
                navController
            )
        }) {
            Text("Guardar Producto")
        }
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
    val uploadTask = storageRef.putFile(imageUri)

    uploadTask.addOnSuccessListener {
        storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
            val product = hashMapOf(
                "name" to name,
                "price" to price,
                "quantity" to quantity,
                "description" to description,
                "category_id" to category,
                "image_url" to imageUrl.toString(),
                "seller_id" to sellerId
            )

            Firebase.firestore.collection("products").add(product).addOnSuccessListener {
                Toast.makeText(context, "Producto creado exitosamente", Toast.LENGTH_SHORT).show()
                // Navegar de regreso al dashboard del vendedor
                if (context is MainActivity) {
                    context.runOnUiThread {
                        navController.navigate("seller_dashboard")
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Error al guardar el producto.", Toast.LENGTH_SHORT).show()
            }
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Error al subir la imagen.", Toast.LENGTH_SHORT).show()
    }


}


