package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    // Requisitos de contraseña
    private static final int    MIN_LENGTH        = 8;
    private static final Pattern HAS_UPPER        = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT        = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL      = Pattern.compile("[^a-zA-Z0-9]");

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private EditText        etNombre, etEmail, etPassword, etConfirmarPassword;
    private TextInputLayout tilPassword, tilConfirmarPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_view);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios");

        etNombre             = findViewById(R.id.etNombre);
        etEmail              = findViewById(R.id.etEmail);
        etPassword           = findViewById(R.id.etPassword);
        etConfirmarPassword  = findViewById(R.id.etConfirmarPassword);
        tilPassword          = findViewById(R.id.tilPassword);
        tilConfirmarPassword = findViewById(R.id.tilConfirmarPassword);

        Button   btnRegistrar  = findViewById(R.id.btnRegistrar);
        TextView tvVolverLogin = findViewById(R.id.tvVolverLogin);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());
        tvVolverLogin.setOnClickListener(v -> finish());

        // Limpiar errores en tiempo real cuando el usuario escribe
        etPassword.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                tilPassword.setError(null);
                // Validar en vivo si el campo de confirmación ya tiene texto
                if (etConfirmarPassword.length() > 0) {
                    validarCoincidencia();
                }
            }
        });
        etConfirmarPassword.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                tilConfirmarPassword.setError(null);
            }
        });
    }

    // ── Validación ──────────────────────────────────────────────────────────

    private void registrarUsuario() {
        String nombre      = etNombre.getText().toString().trim();
        String email       = etEmail.getText().toString().trim();
        String pass        = etPassword.getText().toString();
        String confirmPass = etConfirmarPassword.getText().toString();

        // Limpiar errores previos
        tilPassword.setError(null);
        tilConfirmarPassword.setError(null);

        // Campos básicos vacíos
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar requisitos de contraseña
        String errorPass = validarRequisitosPassword(pass);
        if (errorPass != null) {
            tilPassword.setError(errorPass);
            etPassword.requestFocus();
            return;
        }

        // Validar coincidencia
        if (!pass.equals(confirmPass)) {
            tilConfirmarPassword.setError("Las contraseñas no coinciden");
            etConfirmarPassword.requestFocus();
            return;
        }

        // Todo correcto → crear cuenta
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) guardarUsuarioEnDB(user, nombre);
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage()
                        : "Error desconocido";
                Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Devuelve null si la contraseña cumple todos los requisitos,
     * o un mensaje de error si falla alguno.
     */
    private String validarRequisitosPassword(String pass) {
        if (pass.length() < MIN_LENGTH) {
            return "Mínimo " + MIN_LENGTH + " caracteres";
        }
        if (!HAS_UPPER.matcher(pass).find()) {
            return "Debe contener al menos una letra mayúscula";
        }
        if (!HAS_DIGIT.matcher(pass).find()) {
            return "Debe contener al menos un número";
        }
        if (!HAS_SPECIAL.matcher(pass).find()) {
            return "Debe contener al menos un símbolo (ej: @, #, !)";
        }
        return null;
    }

    /** Comprueba en tiempo real si las contraseñas coinciden. */
    private void validarCoincidencia() {
        String pass    = etPassword.getText().toString();
        String confirm = etConfirmarPassword.getText().toString();
        if (!pass.equals(confirm)) {
            tilConfirmarPassword.setError("Las contraseñas no coinciden");
        } else {
            tilConfirmarPassword.setError(null);
        }
    }

    // ── Guardar en Firebase ──────────────────────────────────────────────────

    private void guardarUsuarioEnDB(FirebaseUser usuario, String nombre) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("email", usuario.getEmail());
        datos.put("fechaRegistro", System.currentTimeMillis());
        datos.put("saldo", 0.0);
        datos.put("dinero", 50.0);   // 50€ de bienvenida (simulado)

        dbRef.child(usuario.getUid()).setValue(datos).addOnSuccessListener(unused -> {
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

    // ── Helper TextWatcher ───────────────────────────────────────────────────

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
