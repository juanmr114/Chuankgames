package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private EditText etNombre, etEmail, etPassword, etConfirmarPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_view);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios");

        etNombre            = findViewById(R.id.etNombre);
        etEmail             = findViewById(R.id.etEmail);
        etPassword          = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        Button btnRegistrar = findViewById(R.id.btnRegistrar);
        TextView tvVolverLogin = findViewById(R.id.tvVolverLogin);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        tvVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre       = etNombre.getText().toString().trim();
        String email        = etEmail.getText().toString().trim();
        String pass         = etPassword.getText().toString().trim();
        String confirmPass  = etConfirmarPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) guardarUsuarioEnDB(user, nombre);
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarUsuarioEnDB(FirebaseUser usuario, String nombre) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("email", usuario.getEmail());
        datos.put("fechaRegistro", System.currentTimeMillis());
        datos.put("saldo", 0.0);
        datos.put("dinero", 50.0);   // 50€ de bienvenida (simulado)

        dbRef.child(usuario.getUid()).setValue(datos).addOnSuccessListener(unused -> {
            // Actualizar displayName en Firebase Auth
            com.google.firebase.auth.UserProfileChangeRequest profile =
                    new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();
            usuario.updateProfile(profile);

            Intent intent = new Intent(this, PaginaPrincipalActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
