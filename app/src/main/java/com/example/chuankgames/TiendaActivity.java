package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;

public class TiendaActivity extends AppCompatActivity {

    // Paquetes: emoji, nombre, CK base, bonus CK, precio €
    private static final Object[][] PAQUETES_DATA = {
            {"🌱", "Starter",   100,    0,   0.99},
            {"⚡", "Gamer",     500,   50,   3.99},
            {"🏆", "Pro",      1200,  200,   9.99},
            {"💎", "Elite",    3000,  700,  19.99},
            {"🚀", "Ultimate", 8000, 2000,  49.99},
    };

    private TextView tvSaldoTienda;
    private DatabaseReference dbUsuarios;
    private String uid;
    private double saldoActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tienda);

        uid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbUsuarios = FirebaseDatabase.getInstance(
                "https://chuankgames-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("usuarios");

        tvSaldoTienda = findViewById(R.id.tvSaldoTienda);
        findViewById(R.id.btnVolverTienda).setOnClickListener(v -> finish());

        cargarSaldo();
        configurarRecycler();
        configurarBottomNav();
    }

    private void cargarSaldo() {
        dbUsuarios.child(uid).child("saldo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                saldoActual = snapshot.exists() ? snapshot.getValue(Double.class) : 0;
                tvSaldoTienda.setText("⚡ " + (int) saldoActual + " CK");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void configurarRecycler() {
        List<Paquete> paquetes = new ArrayList<>();
        for (Object[] d : PAQUETES_DATA) {
            paquetes.add(new Paquete(
                    (String) d[0], (String) d[1],
                    (int) d[2], (int) d[3], (double) d[4]));
        }
        RecyclerView recycler = findViewById(R.id.recyclerPaquetes);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new PaqueteAdapter(paquetes));
    }

    private void mostrarConfirmacion(Paquete p) {
        int totalCK = p.ckBase + p.ckBonus;
        String bonusLinea = p.ckBonus > 0
                ? "\n🎁 Bonus incluido: +" + p.ckBonus + " CK"
                : "";
        new AlertDialog.Builder(this)
                .setTitle("💳 Pago simulado")
                .setMessage(
                        "Paquete: " + p.emoji + " " + p.nombre + "\n" +
                        "Recibirás: ⚡ " + totalCK + " CK" + bonusLinea + "\n" +
                        "Precio: " + String.format("%.2f€", p.precio) + "\n\n" +
                        "─────────────────────────\n" +
                        "Saldo actual:   " + (int) saldoActual + " CK\n" +
                        "Saldo tras compra: " + ((int) saldoActual + totalCK) + " CK\n\n" +
                        "⚠️ Pago simulado — sin cargo real")
                .setPositiveButton("✅ Confirmar compra", (d, w) -> ejecutarCompra(p, totalCK))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCompra(Paquete p, int totalCK) {
        dbUsuarios.child(uid).child("saldo").runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData data) {
                Double s = data.getValue(Double.class);
                if (s == null) s = 0.0;
                data.setValue(s + totalCK);
                return Transaction.success(data);
            }
            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed) {
                    registrarTransaccion(p, totalCK);
                    mostrarRecibo(p, totalCK);
                }
            }
        });
    }

    private void registrarTransaccion(Paquete p, int totalCK) {
        String txId = dbUsuarios.child(uid).child("recargas").push().getKey();
        if (txId == null) return;
        java.util.HashMap<String, Object> tx = new java.util.HashMap<>();
        tx.put("paquete",  p.nombre);
        tx.put("ckTotal",  totalCK);
        tx.put("precio",   p.precio);
        tx.put("fecha",    System.currentTimeMillis());
        dbUsuarios.child(uid).child("recargas").child(txId).setValue(tx);
    }

    private void mostrarRecibo(Paquete p, int totalCK) {
        new AlertDialog.Builder(this)
                .setTitle("✅ ¡Recarga completada!")
                .setMessage(
                        p.emoji + " Paquete " + p.nombre + "\n\n" +
                        "CK añadidos:  +" + totalCK + " CK\n" +
                        "Nuevo saldo:  " + ((int) saldoActual + totalCK) + " CK\n\n" +
                        "Pagado:  " + String.format("%.2f€", p.precio) + " (simulado)")
                .setPositiveButton("¡Genial! 🎉", null)
                .setCancelable(false)
                .show();
    }

    private void configurarBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
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
            }
            return false;
        });
    }

    // ── Modelo interno ───────────────────────────────────────────────────

    static class Paquete {
        final String emoji, nombre;
        final int ckBase, ckBonus;
        final double precio;
        Paquete(String emoji, String nombre, int ckBase, int ckBonus, double precio) {
            this.emoji = emoji; this.nombre = nombre;
            this.ckBase = ckBase; this.ckBonus = ckBonus; this.precio = precio;
        }
    }

    // ── Adapter interno ──────────────────────────────────────────────────

    class PaqueteAdapter extends RecyclerView.Adapter<PaqueteAdapter.VH> {
        private final List<Paquete> lista;
        PaqueteAdapter(List<Paquete> lista) { this.lista = lista; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paquete_ck, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Paquete p = lista.get(pos);
            h.tvIcono.setText(p.emoji);
            h.tvNombre.setText(p.nombre);
            h.tvCK.setText(String.valueOf(p.ckBase + p.ckBonus));
            h.tvPrecio.setText(String.format("%.2f€", p.precio));

            if (p.ckBonus > 0) {
                h.tvBonus.setVisibility(View.VISIBLE);
                h.tvBonus.setText("🎁 +" + p.ckBonus + " CK de bonus");
            } else {
                h.tvBonus.setVisibility(View.GONE);
            }
            h.btnComprar.setOnClickListener(v -> mostrarConfirmacion(p));
        }

        @Override public int getItemCount() { return lista.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIcono, tvNombre, tvCK, tvBonus, tvPrecio;
            MaterialButton btnComprar;
            VH(@NonNull View v) {
                super(v);
                tvIcono   = v.findViewById(R.id.tvPaqueteIcono);
                tvNombre  = v.findViewById(R.id.tvPaqueteNombre);
                tvCK      = v.findViewById(R.id.tvPaqueteCK);
                tvBonus   = v.findViewById(R.id.tvPaqueteBonus);
                tvPrecio  = v.findViewById(R.id.tvPaquetePrecio);
                btnComprar = v.findViewById(R.id.btnComprarPaquete);
            }
        }
    }
}
