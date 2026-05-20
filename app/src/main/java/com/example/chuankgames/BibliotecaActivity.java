package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BibliotecaActivity extends AppCompatActivity {

    // ── Modelo para juegos en venta ───────────────────────────────────────

    static class JuegoEnVenta {
        String id, nombre, imagenUrl;
        double precio;

        JuegoEnVenta(String id, String nombre, double precio, String imagenUrl) {
            this.id        = id;
            this.nombre    = nombre != null ? nombre : "";
            this.precio    = precio;
            this.imagenUrl = imagenUrl != null ? imagenUrl : "";
        }
    }

    // ── Adapter para juegos en venta ──────────────────────────────────────

    class EnVentaAdapter extends RecyclerView.Adapter<EnVentaAdapter.VH> {
        final List<JuegoEnVenta> lista;

        EnVentaAdapter(List<JuegoEnVenta> lista) { this.lista = lista; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_juego_en_venta, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            JuegoEnVenta j = lista.get(pos);
            h.tvNombre.setText(j.nombre);

            int    ck  = (int) j.precio;
            double eur = j.precio / 10.0;
            h.tvPrecio.setText("💶 " + String.format("€%.2f", eur) + "  ·  ⚡ " + ck + " CK");
            h.tvPago.setText("Cobro: 💶 Euros");

            if (!j.imagenUrl.isEmpty()) {
                Glide.with(BibliotecaActivity.this)
                        .load(j.imagenUrl).centerCrop().into(h.imgJuego);
            } else {
                h.imgJuego.setImageResource(R.mipmap.ic_launcher);
            }
        }

        @Override public int getItemCount() { return lista.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView imgJuego;
            TextView  tvNombre, tvPrecio, tvPago;

            VH(@NonNull View v) {
                super(v);
                imgJuego = v.findViewById(R.id.imgEnVenta);
                tvNombre = v.findViewById(R.id.tvEnVentaNombre);
                tvPrecio = v.findViewById(R.id.tvEnVentaPrecio);
                tvPago   = v.findViewById(R.id.tvEnVentaPago);
            }
        }
    }

    // ── Activity ──────────────────────────────────────────────────────────

    private RecyclerView      recyclerBiblioteca, recyclerEnVenta;
    private VideojuegoAdapter adapterBiblioteca;
    private EnVentaAdapter    adapterEnVenta;
    private TextView          tvSaldo, tvDinero, tvContador;
    private LinearLayout      layoutVacia, sectionEnVenta;

    private final List<Videojuego>   juegosComprados = new ArrayList<>();
    private final List<JuegoEnVenta> juegosEnVenta   = new ArrayList<>();

    private DatabaseReference dbUsuarios, dbJuegos;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biblioteca);

        uid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbUsuarios = FirebaseDatabase.getInstance().getReference("usuarios");
        dbJuegos   = FirebaseDatabase.getInstance().getReference("videojuegos");

        recyclerBiblioteca = findViewById(R.id.recyclerBiblioteca);
        recyclerEnVenta    = findViewById(R.id.recyclerEnVenta);
        layoutVacia        = findViewById(R.id.layoutBibliotecaVacia);
        sectionEnVenta     = findViewById(R.id.sectionEnVenta);
        tvSaldo            = findViewById(R.id.tvSaldoBiblioteca);
        tvDinero           = findViewById(R.id.tvDineroBiblioteca);
        tvContador         = findViewById(R.id.tvContadorEnVenta);

        // Adapter biblioteca (grid 2 col)
        adapterBiblioteca = new VideojuegoAdapter(juegosComprados, juego -> {
            Intent i = new Intent(this, DetalleJuegoActivity.class);
            i.putExtra(DetalleJuegoActivity.EXTRA_JUEGO_ID, juego.getId());
            startActivity(i);
        });
        adapterBiblioteca.setOnCodigoClickListener(this::mostrarCodigo);
        recyclerBiblioteca.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerBiblioteca.setAdapter(adapterBiblioteca);
        recyclerBiblioteca.setNestedScrollingEnabled(false);

        // Adapter en-venta (lista)
        adapterEnVenta = new EnVentaAdapter(juegosEnVenta);
        recyclerEnVenta.setLayoutManager(new LinearLayoutManager(this));
        recyclerEnVenta.setAdapter(adapterEnVenta);
        recyclerEnVenta.setNestedScrollingEnabled(false);

        configurarBottomNav();
        cargarBalances();
        cargarBiblioteca();
        cargarEnVenta();
    }

    // ── Navegación ────────────────────────────────────────────────────────

    private void configurarBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_carrito);
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
                return true;
            } else if (id == R.id.nav_ruleta) {
                startActivity(new Intent(this, RuletaActivity.class));
                return true;
            }
            return false;
        });
    }

    // ── Firebase ──────────────────────────────────────────────────────────

    private void cargarBalances() {
        dbUsuarios.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double ck  = snapshot.child("saldo").exists()
                        ? safeD(snapshot.child("saldo"))  : 0;
                double eur = snapshot.child("dinero").exists()
                        ? safeD(snapshot.child("dinero")) : 0;
                tvSaldo.setText("⚡ " + (int) ck + " CK");
                tvDinero.setText(String.format("💶 %.2f€", eur));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarBiblioteca() {
        dbUsuarios.child(uid).child("biblioteca").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                juegosComprados.clear();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    layoutVacia.setVisibility(View.VISIBLE);
                    recyclerBiblioteca.setVisibility(View.GONE);
                    adapterBiblioteca.actualizarLista(juegosComprados);
                    return;
                }
                layoutVacia.setVisibility(View.GONE);
                recyclerBiblioteca.setVisibility(View.VISIBLE);

                long total = snapshot.getChildrenCount();
                final long[] cargados = {0};
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String juegoId = ds.getKey();
                    dbJuegos.child(juegoId).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot js) {
                                    Videojuego j = js.getValue(Videojuego.class);
                                    if (j != null) {
                                        j.setId(js.getKey());
                                        juegosComprados.add(j);
                                    }
                                    if (++cargados[0] == total)
                                        adapterBiblioteca.actualizarLista(juegosComprados);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError e) {
                                    if (++cargados[0] == total)
                                        adapterBiblioteca.actualizarLista(juegosComprados);
                                }
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarEnVenta() {
        dbUsuarios.child(uid).child("enVenta").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                juegosEnVenta.clear();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    sectionEnVenta.setVisibility(View.GONE);
                    adapterEnVenta.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id     = ds.getKey();
                    String nombre = ds.child("nombre").exists()
                            ? ds.child("nombre").getValue(String.class) : "?";
                    double precio = ds.child("precio").exists()
                            ? safeD(ds.child("precio")) : 0;
                    String img    = ds.child("imagenUrl").exists()
                            ? ds.child("imagenUrl").getValue(String.class) : "";
                    juegosEnVenta.add(new JuegoEnVenta(id, nombre, precio, img));
                }
                sectionEnVenta.setVisibility(View.VISIBLE);
                tvContador.setText(juegosEnVenta.size() + " juego(s)");
                adapterEnVenta.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ── Código de activación ──────────────────────────────────────────────

    private void mostrarCodigo(Videojuego juego) {
        String juegoId = juego.getId();
        dbUsuarios.child(uid).child("codigos").child(juegoId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String codigo;
                        if (snapshot.exists() && snapshot.getValue(String.class) != null) {
                            codigo = snapshot.getValue(String.class);
                        } else {
                            codigo = generarCodigo();
                            dbUsuarios.child(uid).child("codigos").child(juegoId).setValue(codigo);
                        }
                        mostrarDialogoCodigo(juego.getNombre() != null ? juego.getNombre() : "?", codigo);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(BibliotecaActivity.this,
                                "Error al obtener código", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generarCodigo() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString(); // ejemplo: A3BX-9KPM-2YHD-7WQN
    }

    private void mostrarDialogoCodigo(String nombre, String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("🔑 Código de activación")
                .setMessage("🎮 " + nombre + "\n\n" +
                        "Tu código de activación:\n\n" +
                        "  " + codigo + "\n\n" +
                        "Guarda este código en un lugar seguro.\n" +
                        "Es único para este juego en tu cuenta.")
                .setPositiveButton("📋 Copiar", (d, w) -> {
                    android.content.ClipboardManager cm =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(
                            android.content.ClipData.newPlainText("Código", codigo));
                    Toast.makeText(this, "✅ Código copiado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private double safeD(DataSnapshot ds) {
        try { return ds.getValue(Double.class); } catch (Exception e) { return 0; }
    }
}
