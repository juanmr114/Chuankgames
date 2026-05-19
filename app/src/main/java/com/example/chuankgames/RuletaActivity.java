package com.example.chuankgames;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.Random;

public class RuletaActivity extends AppCompatActivity {

    // Coste por tirada
    private static final int COSTE_CK = 50;

    // Premios  |  probabilidad acumulada 0-100  |  emojis de cada tier
    private static final double[] PREMIOS_EUR = { 0.20,  0.50,  1.00,  5.00, 10.00, 20.00 };
    private static final int[]    PROB_ACUM   = {   40,    65,    85,    95,    99,   100  };
    private static final String[] EMOJIS      = { "🥉",  "🥈",  "🥇",  "💎",  "👑",  "🌟" };

    // Emojis que desfilan durante el giro (efecto slot)
    private static final String[] POOL_GIRO = {
        "🎰", "🥉", "🥈", "🥇", "💎", "👑", "🌟",
        "🎲", "🎯", "🃏", "⭐", "🔥", "💫", "✨"
    };

    // Tiempos (ms) entre frames del slot — empieza rápido, va frenando
    private static final long[] FRAMES_MS = {
        50, 55, 60, 65, 70, 80, 95, 115, 140, 175, 215, 265, 330, 410, 520
    };

    // Views
    private TextView       tvSaldo, tvResultado, tvEmoji, tvHistorial;
    private MaterialButton btnGirar;
    private ImageView      ivRuleta;

    // Firebase
    private DatabaseReference dbUsuarios;
    private String uid;

    // Estado
    private double  saldoCK = 0;
    private boolean girando = false;
    private final StringBuilder historial = new StringBuilder();
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ruleta);

        uid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbUsuarios = FirebaseDatabase.getInstance().getReference("usuarios");

        tvSaldo     = findViewById(R.id.tvSaldoRuleta);
        tvResultado = findViewById(R.id.tvResultadoRuleta);
        tvEmoji     = findViewById(R.id.tvRuletaEmoji);
        btnGirar    = findViewById(R.id.btnGirar);
        ivRuleta    = findViewById(R.id.ivRuleta);
        tvHistorial = findViewById(R.id.tvHistorialRuleta);

        btnGirar.setOnClickListener(v -> intentarGirar());

        configurarBottomNav();
        cargarSaldo();
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────

    private void configurarBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_ruleta);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                Intent i = new Intent(this, PaginaPrincipalActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_buscar) {
                startActivity(new Intent(this, NoticiasActivity.class));
                return true;
            } else if (id == R.id.nav_añadir) {
                startActivity(new Intent(this, PublicarJuegoActivity.class));
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, BibliotecaActivity.class));
                return true;
            } else if (id == R.id.nav_ruleta) {
                return true; // Ya estamos aquí
            }
            return false;
        });
    }

    // ── Saldo ─────────────────────────────────────────────────────────────

    private void cargarSaldo() {
        dbUsuarios.child(uid).child("saldo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                saldoCK = snapshot.exists() ? safeD(snapshot) : 0;
                tvSaldo.setText("⚡ " + (int) saldoCK + " CK");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ── Lógica de tirada ──────────────────────────────────────────────────

    private void intentarGirar() {
        if (girando) return;

        if (saldoCK < COSTE_CK) {
            new AlertDialog.Builder(this)
                    .setTitle("❌ CK insuficientes")
                    .setMessage("Necesitas ⚡ " + COSTE_CK + " CK para girar.\n" +
                            "Tienes: ⚡ " + (int) saldoCK + " CK\n\n" +
                            "Compra juegos con € para ganar CK de bonus.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        girando = true;
        btnGirar.setEnabled(false);

        // Descontar CK
        dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double s = data.getValue(Double.class);
                if (s == null) s = 0.0;
                if (s < COSTE_CK) return Transaction.abort();
                data.setValue(s - COSTE_CK);
                return Transaction.success(data);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (!committed) {
                    runOnUiThread(() -> {
                        girando = false;
                        btnGirar.setEnabled(true);
                        Toast.makeText(RuletaActivity.this,
                                "CK insuficientes", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Sortear premio
                double premio = sortearPremio();

                // Acreditar euros al usuario
                dbUsuarios.child(uid).child("dinero").runTransaction(new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData data) {
                        Double d = data.getValue(Double.class);
                        if (d == null) d = 0.0;
                        data.setValue(Math.round((d + premio) * 100.0) / 100.0);
                        return Transaction.success(data);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError e, boolean ok,
                                           @Nullable DataSnapshot snap) {
                        // Registrar tirada en Firebase
                        String tiradaId = dbUsuarios.child(uid).child("ruleta").push().getKey();
                        if (tiradaId != null) {
                            java.util.HashMap<String, Object> t = new java.util.HashMap<>();
                            t.put("premio", premio);
                            t.put("coste", COSTE_CK);
                            t.put("fecha", System.currentTimeMillis());
                            dbUsuarios.child(uid).child("ruleta").child(tiradaId).setValue(t);
                        }
                        // Lanzar animación en UI thread
                        runOnUiThread(() -> iniciarAnimacionSlot(premio));
                    }
                });
            }
        });
    }

    private double sortearPremio() {
        int rand = rnd.nextInt(100);
        for (int i = 0; i < PROB_ACUM.length; i++) {
            if (rand < PROB_ACUM[i]) return PREMIOS_EUR[i];
        }
        return PREMIOS_EUR[0];
    }

    // ── Animación slot machine ────────────────────────────────────────────

    private void iniciarAnimacionSlot(double premio) {
        // Buscar emoji del premio
        String emoji = "🎉";
        for (int i = 0; i < PREMIOS_EUR.length; i++) {
            if (Math.abs(PREMIOS_EUR[i] - premio) < 0.001) {
                emoji = EMOJIS[i];
                break;
            }
        }
        final String emojiFinal = emoji;

        // Calcular duración total de la animación slot
        long duracionTotal = 0;
        for (long ms : FRAMES_MS) duracionTotal += ms;

        // Rueda girando: 5 vueltas, frena al final
        ObjectAnimator rotAnim = ObjectAnimator.ofFloat(ivRuleta, "rotation", 0f, 1800f);
        rotAnim.setDuration(duracionTotal + 200);
        rotAnim.setInterpolator(new DecelerateInterpolator(2.5f));
        rotAnim.start();

        tvResultado.setText("...");
        tvResultado.setAlpha(0.5f);

        // Ciclo de emojis frame a frame
        int[] frameRef = {0};
        Runnable[] runnableRef = {null};
        runnableRef[0] = () -> {
            int frame = frameRef[0];
            if (frame < FRAMES_MS.length) {
                tvEmoji.setText(POOL_GIRO[rnd.nextInt(POOL_GIRO.length)]);
                tvEmoji.setScaleX(0.75f);
                tvEmoji.setScaleY(0.75f);
                tvEmoji.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(FRAMES_MS[frame] - 10)
                        .start();
                frameRef[0]++;
                handler.postDelayed(runnableRef[0], FRAMES_MS[frame]);
            } else {
                revelarPremio(emojiFinal, premio);
            }
        };
        handler.post(runnableRef[0]);
    }

    private void revelarPremio(String emoji, double premio) {
        // Emoji ganador con efecto bounce
        tvEmoji.setText(emoji);
        tvEmoji.setScaleX(0.2f);
        tvEmoji.setScaleY(0.2f);
        tvEmoji.animate()
                .scaleX(1.35f).scaleY(1.35f)
                .setDuration(220)
                .withEndAction(() ->
                        tvEmoji.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(160)
                                .start())
                .start();

        // Resultado con fade-in
        tvResultado.setText(String.format(Locale.getDefault(), "+€%.2f", premio));
        tvResultado.setAlpha(0f);
        tvResultado.animate().alpha(1f).setDuration(300).start();

        // Historial
        String entrada = emoji + " €" + String.format(Locale.getDefault(), "%.2f", premio) + "  ";
        historial.insert(0, entrada);
        if (historial.length() > 100) historial.setLength(100);
        tvHistorial.setText("Últimas tiradas:  " + historial.toString().trim());

        // Diálogo con retardo para ver la animación
        handler.postDelayed(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(emoji + "  ¡Premio!")
                    .setMessage(String.format(
                            "¡Has ganado €%.2f!\n\nYa está en tu saldo de euros.\n\nSe han gastado ⚡ %d CK.",
                            premio, COSTE_CK))
                    .setPositiveButton("¡Genial! 🎉", null)
                    .show();

            girando = false;
            btnGirar.setEnabled(true);
        }, 450);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private double safeD(DataSnapshot ds) {
        try { return ds.getValue(Double.class); } catch (Exception e) { return 0; }
    }
}
