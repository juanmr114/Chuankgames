package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BibliotecaActivity extends AppCompatActivity {

    private RecyclerView recyclerBiblioteca;
    private VideojuegoAdapter adapter;
    private TextView tvVacia, tvSaldo;
    private final List<Videojuego> juegosComprados = new ArrayList<>();

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
        tvVacia            = findViewById(R.id.tvBibliotecaVacia);
        tvSaldo            = findViewById(R.id.tvSaldoBiblioteca);

        adapter = new VideojuegoAdapter(juegosComprados, juego -> {
            Intent intent = new Intent(this, DetalleJuegoActivity.class);
            intent.putExtra(DetalleJuegoActivity.EXTRA_JUEGO_ID, juego.getId());
            startActivity(intent);
        });
        recyclerBiblioteca.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerBiblioteca.setAdapter(adapter);

        configurarBottomNav();
        cargarSaldo();
        cargarBiblioteca();
    }

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
            }
            return false;
        });
    }

    private void cargarSaldo() {
        dbUsuarios.child(uid).child("saldo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double saldo = snapshot.exists() ? snapshot.getValue(Double.class) : 0;
                tvSaldo.setText("💰 " + (int) saldo + " monedas");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarBiblioteca() {
        dbUsuarios.child(uid).child("biblioteca").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                juegosComprados.clear();
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    tvVacia.setVisibility(View.VISIBLE);
                    adapter.actualizarLista(juegosComprados);
                    return;
                }
                tvVacia.setVisibility(View.GONE);
                long total = snapshot.getChildrenCount();
                final long[] cargados = {0};

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String juegoId = ds.getKey();
                    dbJuegos.child(juegoId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot juegoSnap) {
                            Videojuego juego = juegoSnap.getValue(Videojuego.class);
                            if (juego != null) {
                                juego.setId(juegoSnap.getKey());
                                juegosComprados.add(juego);
                            }
                            cargados[0]++;
                            if (cargados[0] == total) {
                                adapter.actualizarLista(juegosComprados);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            cargados[0]++;
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
