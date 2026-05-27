package com.example.chuankgames;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class DetalleJuegoActivity extends AppCompatActivity {

    public static final String EXTRA_JUEGO_ID = "juego_id";

    // Views
    private MaterialButton btnVolver;
    private TextView       tvHeaderNombre, tvSaldoDetalle;
    private ImageView      ivPortada;
    private TextView       tvNombre, tvGenero, tvVendedor, tvDescripcion;
    private TextView       tvPrecio, tvPrecioEuro, tvCKReward;
    private MaterialButton btnComprarEuro, btnRevender;
    private ProgressBar    progressBar;

    // Firebase
    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid, displayName;

    // State
    private Videojuego juegoActual;
    private double     saldoEur = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_juego);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        uid         = user.getUid();
        displayName = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";

        dbJuegos   = FirebaseDatabase.getInstance().getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance().getReference("usuarios");

        btnVolver      = findViewById(R.id.btnVolverDetalle);
        tvHeaderNombre = findViewById(R.id.tvHeaderNombre);
        tvSaldoDetalle = findViewById(R.id.tvSaldoDetalle);
        ivPortada      = findViewById(R.id.ivDetallePortada);
        tvNombre       = findViewById(R.id.tvDetalleNombre);
        tvGenero       = findViewById(R.id.tvDetalleGenero);
        tvVendedor     = findViewById(R.id.tvDetalleVendedor);
        tvDescripcion  = findViewById(R.id.tvDetalleDescripcion);
        tvPrecio       = findViewById(R.id.tvDetallePrecio);
        tvPrecioEuro   = findViewById(R.id.tvDetallePrecioEuro);
        tvCKReward     = findViewById(R.id.tvCKReward);
        btnComprarEuro = findViewById(R.id.btnComprarEuro);
        btnRevender    = findViewById(R.id.btnRevender);
        progressBar    = findViewById(R.id.progressDetalle);

        btnVolver.setOnClickListener(v -> finish());
        btnComprarEuro.setOnClickListener(v -> mostrarEleccionPago());
        btnRevender.setOnClickListener(v -> confirmarReventa());

        String juegoId = getIntent().getStringExtra(EXTRA_JUEGO_ID);
        if (juegoId == null) { finish(); return; }

        cargarBalances();
        cargarJuego(juegoId);
    }

    // ── Balances ──────────────────────────────────────────────────────────

    private void cargarBalances() {
        dbUsuarios.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                saldoEur = snapshot.child("dinero").exists()
                        ? safeDouble(snapshot.child("dinero")) : 0;
                tvSaldoDetalle.setText(String.format("💶 %.2f€", saldoEur));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private double safeDouble(DataSnapshot ds) {
        try { return ds.getValue(Double.class); } catch (Exception e) { return 0; }
    }

    // ── Carga de juego ────────────────────────────────────────────────────

    private void cargarJuego(String juegoId) {
        progressBar.setVisibility(View.VISIBLE);
        dbJuegos.child(juegoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                juegoActual = snapshot.getValue(Videojuego.class);
                if (juegoActual == null) { finish(); return; }
                juegoActual.setId(snapshot.getKey());
                mostrarDatos();
                verificarEstado();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { finish(); }
        });
    }

    private void verificarEstado() {
        if (uid.equals(juegoActual.getPublicadoPor())) {
            btnComprarEuro.setVisibility(View.GONE);
            tvCKReward.setVisibility(View.GONE);
            // Si ya está en venta (disponible=true) no mostrar el botón de poner en venta
            btnRevender.setVisibility(juegoActual.isDisponible() ? View.GONE : View.VISIBLE);
            return;
        }
        dbUsuarios.child(uid).child("biblioteca").child(juegoActual.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) setBought();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ── Mostrar datos ─────────────────────────────────────────────────────

    private void mostrarDatos() {
        String nombre = juegoActual.getNombre() != null ? juegoActual.getNombre() : "";
        tvHeaderNombre.setText(nombre);
        tvNombre.setText(nombre);
        tvGenero.setText("🎮 " + (juegoActual.getGenero() != null ? juegoActual.getGenero() : ""));

        // Mostrar nombre del vendedor: primero lo que viene en el objeto,
        // y siempre lo refrescamos consultando Firebase con el UID real
        String nombreGuardado = juegoActual.getNombreVendedor();
        tvVendedor.setText("👤 " + (nombreGuardado != null && !nombreGuardado.isEmpty()
                ? nombreGuardado : "Cargando..."));
        cargarNombreVendedor();
        tvDescripcion.setText(juegoActual.getDescripcion() != null ? juegoActual.getDescripcion() : "");

        int    ck      = (int) juegoActual.getPrecio();
        double eur     = juegoActual.getPrecioEuros();
        int    ckBonus = Math.max(1, (int)(eur * 100));  // precio en € × 100

        tvPrecio.setText(String.format("💶 €%.2f", eur));
        tvPrecioEuro.setText("⚡ " + ck + " CK equivalente");
        tvCKReward.setText("🎁 +" + ckBonus + " CK  tú · vendedor");
        tvCKReward.setVisibility(View.VISIBLE);

        actualizarBotones();

        if (juegoActual.getImagenUrl() != null && !juegoActual.getImagenUrl().isEmpty()) {
            Glide.with(this).load(juegoActual.getImagenUrl()).centerCrop().into(ivPortada);
        }

        if (!juegoActual.isDisponible()) {
            btnComprarEuro.setEnabled(false);
            btnComprarEuro.setText("❌ No disponible");
            tvCKReward.setVisibility(View.GONE);
        }
    }

    /** Consulta el nombre del vendedor en usuarios/{uid}/nombre y lo muestra en tvVendedor. */
    private void cargarNombreVendedor() {
        String vendedorId = juegoActual.getPublicadoPor();
        if (vendedorId == null || vendedorId.isEmpty()) {
            tvVendedor.setText("👤 Desconocido");
            return;
        }
        dbUsuarios.child(vendedorId).child("nombre")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String nombre = snapshot.exists() ? snapshot.getValue(String.class) : null;
                        if (nombre != null && !nombre.isEmpty()) {
                            tvVendedor.setText("👤 " + nombre);
                            // Actualiza el campo en Firebase para futuras cargas rápidas
                            if (juegoActual.getNombreVendedor() == null
                                    || juegoActual.getNombreVendedor().isEmpty()) {
                                dbJuegos.child(juegoActual.getId())
                                        .child("nombreVendedor").setValue(nombre);
                            }
                        } else {
                            tvVendedor.setText("👤 Usuario");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        tvVendedor.setText("👤 Usuario");
                    }
                });
    }

    private void actualizarBotones() {
        if (juegoActual == null) return;
        btnComprarEuro.setText(String.format("🏦 Comprar  €%.2f", juegoActual.getPrecioEuros()));
    }

    private void setBought() {
        btnComprarEuro.setEnabled(false);
        btnComprarEuro.setText("✅ Ya en tu biblioteca");
        tvCKReward.setVisibility(View.GONE);
        btnRevender.setVisibility(View.VISIBLE);
    }

    // ── Elección de método de pago ────────────────────────────────────────

    private void mostrarEleccionPago() {
        double precioEur = juegoActual.getPrecioEuros();
        int    precioCK  = (int) juegoActual.getPrecio();
        int    ckBonus   = Math.max(1, (int)(precioEur * 100));  // igual que lo mostrado

        String[] opciones = {
            "💳  Tarjeta bancaria",
            String.format("💶  Cartera de la app  (€%.2f disponibles)", saldoEur)
        };

        new AlertDialog.Builder(this)
                .setTitle(String.format("🏦 Pagar €%.2f — ¿cómo?", precioEur))
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        mostrarFormularioTarjeta(precioEur, precioCK, ckBonus);
                    } else {
                        confirmarCompraCartera(precioEur, precioCK, ckBonus);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ── Compra con cartera (€ de la app) ─────────────────────────────────

    private void confirmarCompraCartera(double precioEur, int precioCK, int ckBonus) {
        if (saldoEur < precioEur) {
            new AlertDialog.Builder(this)
                    .setTitle("❌ Euros insuficientes")
                    .setMessage(String.format(
                            "Necesitas: €%.2f\nTienes:    €%.2f\n\n" +
                            "Puedes ganar euros en la 🎰 Ruleta.",
                            precioEur, saldoEur))
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("💶 Confirmar pago con cartera")
                .setMessage(
                        "🎮 " + juegoActual.getNombre() + "\n" +
                        "────────────────────────\n" +
                        String.format("Pagarás:       €%.2f\n", precioEur) +
                        "🎁 Tu bonus:   +" + ckBonus + " CK\n" +
                        "🎁 Vendedor:   +" + ckBonus + " CK\n" +
                        "────────────────────────\n" +
                        String.format("Vendedor recibe: €%.2f", precioEur))
                .setPositiveButton("✅ Confirmar",
                        (d, w) -> ejecutarCompraCartera(precioEur, precioCK, ckBonus))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCompraCartera(double precioEur, int precioCK, int ckBonus) {
        mostrarCargando(true);
        String vendedorId = juegoActual.getPublicadoPor();
        String juegoId    = juegoActual.getId();
        double eurAntes   = saldoEur;

        // 1. Descontar € al comprador
        dbUsuarios.child(uid).child("dinero").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double d = data.getValue(Double.class);
                if (d == null) d = 0.0;
                if (d < precioEur) return Transaction.abort();
                data.setValue(round2(d - precioEur));
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (!committed) {
                    mostrarCargando(false);
                    Toast.makeText(DetalleJuegoActivity.this,
                            "Euros insuficientes", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 2. Dar bonus CK al comprador
                dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {
                        Double s = data.getValue(Double.class);
                        if (s == null) s = 0.0;
                        data.setValue(s + ckBonus);
                        return Transaction.success(data);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError e, boolean ok,
                                           @Nullable DataSnapshot snap) {
                        pagarVendedor(vendedorId, precioEur, ckBonus, () ->
                                registrarCompra(juegoId, precioEur, ckBonus,
                                        vendedorId, eurAntes, false));
                    }
                });
            }
        });
    }

    // ── Compra con tarjeta (simulada) ─────────────────────────────────────

    private void mostrarFormularioTarjeta(double precioEur, int precioCK, int ckBonus) {
        View formView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_tarjeta, null);

        TextInputEditText etNum     = formView.findViewById(R.id.etNumTarjeta);
        TextInputEditText etExp     = formView.findViewById(R.id.etCaducidad);
        TextInputEditText etCvv     = formView.findViewById(R.id.etCvv);
        TextInputEditText etNombre  = formView.findViewById(R.id.etNombreTitular);
        TextInputLayout   tilNum    = formView.findViewById(R.id.tilNumTarjeta);
        TextInputLayout   tilExp    = formView.findViewById(R.id.tilCaducidad);
        TextInputLayout   tilCvv    = formView.findViewById(R.id.tilCvv);
        TextInputLayout   tilNombre = formView.findViewById(R.id.tilNombreTitular);
        TextView          tvNumPrev = formView.findViewById(R.id.tvCardNumPreview);
        TextView          tvExpPrev = formView.findViewById(R.id.tvCardExpPreview);
        TextView          tvNomPrev = formView.findViewById(R.id.tvCardNombrePreview);

        // Auto-formateo número: espacio cada 4 dígitos
        etNum.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (editing) return;
                editing = true;
                String digits = s.toString().replace(" ", "");
                if (digits.length() > 16) digits = digits.substring(0, 16);
                StringBuilder fmt = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) fmt.append(' ');
                    fmt.append(digits.charAt(i));
                }
                etNum.setText(fmt.toString());
                etNum.setSelection(fmt.length());
                String preview = fmt.toString().isEmpty() ? "•••• •••• •••• ••••" : fmt.toString();
                tvNumPrev.setText(preview);
                editing = false;
            }
        });

        // Auto-formateo caducidad: auto-slash MM/AA
        etExp.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (editing) return;
                editing = true;
                String digits = s.toString().replace("/", "");
                if (digits.length() > 4) digits = digits.substring(0, 4);
                String fmt = digits.length() > 2
                        ? digits.substring(0, 2) + "/" + digits.substring(2)
                        : digits;
                etExp.setText(fmt);
                etExp.setSelection(fmt.length());
                tvExpPrev.setText(fmt.isEmpty() ? "MM/AA" : fmt);
                editing = false;
            }
        });

        // Nombre → mayúsculas en el preview
        etNombre.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                String up = s.toString().toUpperCase();
                tvNomPrev.setText(up.isEmpty() ? "NOMBRE TITULAR" : up);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(String.format("💳 Pagar €%.2f con tarjeta", precioEur))
                .setView(formView)
                .setPositiveButton("Pagar ahora", null)   // null → sin auto-dismiss
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button btnPagar = ((AlertDialog) dlg).getButton(AlertDialog.BUTTON_POSITIVE);
            btnPagar.setOnClickListener(v -> {
                // Limpiar errores previos
                tilNum.setError(null);
                tilExp.setError(null);
                tilCvv.setError(null);
                tilNombre.setError(null);

                String numRaw = etNum.getText()    != null ? etNum.getText().toString().replace(" ", "") : "";
                String expStr = etExp.getText()    != null ? etExp.getText().toString()  : "";
                String cvvStr = etCvv.getText()    != null ? etCvv.getText().toString()  : "";
                String nomStr = etNombre.getText() != null ? etNombre.getText().toString() : "";

                boolean ok = true;
                if (numRaw.length() != 16) {
                    tilNum.setError("Introduce 16 dígitos");
                    ok = false;
                }
                if (!expStr.matches("\\d{2}/\\d{2}")) {
                    tilExp.setError("Formato MM/AA");
                    ok = false;
                }
                if (cvvStr.length() != 3) {
                    tilCvv.setError("3 dígitos");
                    ok = false;
                }
                if (nomStr.trim().isEmpty()) {
                    tilNombre.setError("Introduce el nombre del titular");
                    ok = false;
                }

                if (ok) {
                    dialog.dismiss();
                    simularProcesandoPago(precioEur, precioCK, ckBonus);
                }
            });
        });

        dialog.show();
    }

    private void simularProcesandoPago(double precioEur, int precioCK, int ckBonus) {
        AlertDialog procesando = new AlertDialog.Builder(this)
                .setTitle("⏳ Procesando pago...")
                .setMessage(String.format(
                        "Conectando con la pasarela de pago...\n\n💳 Cargo: €%.2f", precioEur))
                .setCancelable(false)
                .create();
        procesando.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            procesando.dismiss();
            ejecutarCompraTarjeta(precioEur, precioCK, ckBonus);
        }, 2000);
    }

    private void ejecutarCompraTarjeta(double precioEur, int precioCK, int ckBonus) {
        mostrarCargando(true);
        String vendedorId = juegoActual.getPublicadoPor();
        String juegoId    = juegoActual.getId();
        double eurAntes   = saldoEur;   // la cartera no cambia

        // Con tarjeta: solo dar bonus CK al comprador y pagar al vendedor (sin descontar cartera)
        dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double s = data.getValue(Double.class);
                if (s == null) s = 0.0;
                data.setValue(s + ckBonus);
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError e, boolean ok,
                                   @Nullable DataSnapshot snap) {
                pagarVendedor(vendedorId, precioEur, ckBonus, () ->
                        registrarCompra(juegoId, precioEur, ckBonus,
                                vendedorId, eurAntes, true));
            }
        });
    }

    // ── Pago al vendedor ──────────────────────────────────────────────────

    interface Callback { void run(); }

    /** Acredita € y bonus CK al vendedor. */
    private void pagarVendedor(String vendedorId, double precioEur, int ckBonus,
                                Callback callback) {
        if (vendedorId == null || vendedorId.isEmpty() || vendedorId.equals(uid)) {
            callback.run();
            return;
        }
        dbUsuarios.child(vendedorId).child("dinero").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double v = data.getValue(Double.class);
                if (v == null) v = 0.0;
                data.setValue(round2(v + precioEur));
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError e, boolean ok,
                                   @Nullable DataSnapshot snap) {
                dbUsuarios.child(vendedorId).child("saldo").runTransaction(
                        new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {
                        Double s = data.getValue(Double.class);
                        if (s == null) s = 0.0;
                        data.setValue(s + ckBonus);
                        return Transaction.success(data);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError e2, boolean ok2,
                                           @Nullable DataSnapshot snap2) {
                        callback.run();
                    }
                });
            }
        });
    }

    // ── Registrar compra ──────────────────────────────────────────────────

    private void registrarCompra(String juegoId, double precioEur, int ckBonus,
                                  String vendedorId, double eurAntes, boolean conTarjeta) {
        dbUsuarios.child(uid).child("biblioteca").child(juegoId).setValue(true);

        String compraId = dbUsuarios.child(uid).child("compras").push().getKey();
        if (compraId != null) {
            java.util.HashMap<String, Object> c = new java.util.HashMap<>();
            c.put("juegoId", juegoId);
            c.put("nombre", juegoActual.getNombre());
            c.put("metodoPago", conTarjeta ? "tarjeta" : "cartera");
            c.put("precioEur", precioEur);
            c.put("precioCK", (int) juegoActual.getPrecio());
            c.put("ckBonus", ckBonus);
            c.put("fecha", System.currentTimeMillis());
            dbUsuarios.child(uid).child("compras").child(compraId).setValue(c);
        }

        if (vendedorId != null && !vendedorId.isEmpty()) {
            dbUsuarios.child(vendedorId).child("enVenta").child(juegoId).removeValue();
        }

        java.util.HashMap<String, Object> upd = new java.util.HashMap<>();
        upd.put("disponible", false);
        upd.put("publicadoPor", uid);
        upd.put("nombreVendedor", displayName);
        dbJuegos.child(juegoId).updateChildren(upd);

        mostrarCargando(false);
        mostrarRecibo(precioEur, ckBonus, eurAntes, conTarjeta);
    }

    private void mostrarRecibo(double precioEur, int ckBonus, double eurAntes,
                                boolean conTarjeta) {
        String metodo    = conTarjeta ? "💳 Tarjeta bancaria" : "💶 Cartera de la app";
        String lineaPago = conTarjeta
                ? String.format("💳 Cargado en tarjeta: €%.2f\n", precioEur)
                : String.format("💳 Pagado:             €%.2f\n" +
                                "💶 Cartera restante:  €%.2f\n",
                                precioEur, eurAntes - precioEur);

        String resumen =
                "Método:            " + metodo + "\n" +
                lineaPago +
                "🎁 Tu bonus CK:    +" + ckBonus + " CK\n" +
                "🎁 Bonus vendedor: +" + ckBonus + " CK";

        new AlertDialog.Builder(this)
                .setTitle("✅ ¡Compra completada!")
                .setMessage("🎮 " + juegoActual.getNombre() + "\n\n" + resumen +
                        "\n\n📚 El juego ya está en tu Biblioteca.")
                .setPositiveButton("¡Genial! 🎉", null)
                .setCancelable(false)
                .show();

        setBought();
    }

    // ── Reventa ───────────────────────────────────────────────────────────

    private void confirmarReventa() {
        double eur     = juegoActual.getPrecioEuros();
        int    ck      = (int) juegoActual.getPrecio();
        int    ckBonus = Math.max(1, (int)(eur * 100));  // precio en € × 100

        new AlertDialog.Builder(this)
                .setTitle("🔄 Poner en venta")
                .setMessage("¿Poner \"" + juegoActual.getNombre() + "\" a la venta?\n\n" +
                        String.format(
                                "Recibirás: 💶 €%.2f + 🎁 %d CK de bonus cuando lo compren.\n\n" +
                                "Lo verás en \"Mis juegos en venta\" en tu Biblioteca.",
                                eur, ckBonus))
                .setPositiveButton("Sí, poner en venta", (d, w) -> ejecutarReventa())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarReventa() {
        mostrarCargando(true);
        String juegoId = juegoActual.getId();

        java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
        updates.put("disponible", true);
        updates.put("publicadoPor", uid);
        updates.put("nombreVendedor", displayName);

        dbJuegos.child(juegoId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    dbUsuarios.child(uid).child("biblioteca").child(juegoId).removeValue();

                    java.util.HashMap<String, Object> ev = new java.util.HashMap<>();
                    ev.put("nombre", juegoActual.getNombre());
                    ev.put("precio", juegoActual.getPrecio());
                    ev.put("imagenUrl",
                            juegoActual.getImagenUrl() != null ? juegoActual.getImagenUrl() : "");
                    ev.put("pagoPreferido", "euro");
                    ev.put("fechaPublicacion", System.currentTimeMillis());
                    dbUsuarios.child(uid).child("enVenta").child(juegoId).setValue(ev);

                    mostrarCargando(false);
                    new AlertDialog.Builder(DetalleJuegoActivity.this)
                            .setTitle("🔄 ¡Juego en venta!")
                            .setMessage("\"" + juegoActual.getNombre() +
                                    "\" ya está disponible en el mercado.\n\n" +
                                    "Verás tus juegos en venta en tu Biblioteca.")
                            .setPositiveButton("Aceptar", (d, w) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void mostrarCargando(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnComprarEuro.setEnabled(!on);
        btnRevender.setEnabled(!on);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
