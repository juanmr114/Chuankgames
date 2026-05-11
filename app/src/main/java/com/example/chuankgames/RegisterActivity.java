package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private DatabaseReference dbRef;

    private EditText etNombre, etEmail, etPassword, etConfirmarPassword;
    private Button btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_view);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios");

        // Vincular vistas
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        TextView tvVolverLogin = findViewById(R.id.tvVolverLogin);
        SignInButton btnGoogle = findViewById(R.id.btnGoogleRegister);

        // 1. Registro Manual
        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        // 2. Configurar Google Sign-In con protección de cierre
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id)) // Si esto falla, revisa el google-services.json
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
            btnGoogle.setOnClickListener(v -> iniciarSesionConGoogle());
        } catch (Exception e) {
            Toast.makeText(this, "Error config Google: Asegúrate de tener el archivo google-services.json", Toast.LENGTH_LONG).show();
        }

        // 3. Volver al Login
        tvVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmarPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) guardarUsuarioEnDB(user, nombre);
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void iniciarSesionConGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount cuenta = task.getResult(ApiException.class);
                autenticarConFirebase(cuenta.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Error Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void autenticarConFirebase(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) guardarUsuarioEnDB(user, user.getDisplayName());
            }
        });
    }

    private void guardarUsuarioEnDB(FirebaseUser usuario, String nombre) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre != null ? nombre : "Usuario");
        datos.put("email", usuario.getEmail());
        datos.put("fechaRegistro", System.currentTimeMillis());

        dbRef.child(usuario.getUid()).setValue(datos).addOnSuccessListener(unused -> {
            irAPaginaPrincipal();
        });
    }

    private void irAPaginaPrincipal() {
        Intent intent = new Intent(RegisterActivity.this, PaginaPrincipalActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}