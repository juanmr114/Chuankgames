package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PaginaPrincipalActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DB_URL = "https://chuankgames-default-rtdb.europe-west1.firebasedatabase.app";

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView navigationView;
    private EditText etBuscarJuego;

    private RecyclerView recyclerTodos, recyclerRecomendados;
    private VideojuegoAdapter adapterTodos, adapterRecomendados;

    private final List<Videojuego> todosLosJuegos     = new ArrayList<>();
    private final List<Videojuego> todosLosJuegosFull = new ArrayList<>();
    private final List<Videojuego> juegosRecomendados = new ArrayList<>();

    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_principal);

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        dbJuegos   = FirebaseDatabase.getInstance(DB_URL).getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance(DB_URL).getReference("usuarios");

        inicializarVistas();
        configurarToolbar();
        configurarDrawer();
        configurarBottomNav();
        configurarRecyclers();
        configurarBuscador();
        cargarJuegosDesdeFirebase();
        asegurarSaldoInicial();
    }

    private void inicializarVistas() {
        drawerLayout         = findViewById(R.id.drawerLayout);
        bottomNav            = findViewById(R.id.bottomNav);
        navigationView       = findViewById(R.id.navigationView);
        recyclerTodos        = findViewById(R.id.recyclerTodos);
        recyclerRecomendados = findViewById(R.id.recyclerRecomendados);
        etBuscarJuego        = findViewById(R.id.etBuscarJuego);
    }

    private void asegurarSaldoInicial() {
        if (uid.isEmpty()) return;
        dbUsuarios.child(uid).child("saldo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    dbUsuarios.child(uid).child("saldo").setValue(500.0);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void configurarToolbar() {
        View btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void configurarDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        TextView tvNombre = headerView.findViewById(R.id.navHeaderNombre);
        TextView tvEmail  = headerView.findViewById(R.id.navHeaderEmail);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            String nombre = usuario.getDisplayName();
            tvNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : "Usuario");
            tvEmail.setText(usuario.getEmail() != null ? usuario.getEmail() : "");
        }
    }

    private void configurarBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                return true;
            } else if (id == R.id.nav_noticias) {
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
        bottomNav.setSelectedItemId(R.id.nav_inicio);
    }

    private void configurarRecyclers() {
        adapterTodos = new VideojuegoAdapter(todosLosJuegos, juego -> abrirDetalle(juego));
        recyclerTodos.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerTodos.setAdapter(adapterTodos);
        recyclerTodos.setNestedScrollingEnabled(false);
        recyclerTodos.setHasFixedSize(false);

        adapterRecomendados = new VideojuegoAdapter(juegosRecomendados, juego -> abrirDetalle(juego));
        recyclerRecomendados.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerRecomendados.setAdapter(adapterRecomendados);
        recyclerRecomendados.setNestedScrollingEnabled(false);
        recyclerRecomendados.setHasFixedSize(false);
    }

    private void configurarBuscador() {
        etBuscarJuego.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarJuegos(s.toString().trim());
            }
        });
    }

    private void filtrarJuegos(String query) {
        List<Videojuego> filtrados = new ArrayList<>();
        if (query.isEmpty()) {
            filtrados.addAll(todosLosJuegosFull);
        } else {
            String q = query.toLowerCase();
            for (Videojuego j : todosLosJuegosFull) {
                if (j.getNombre() != null && j.getNombre().toLowerCase().contains(q)) {
                    filtrados.add(j);
                }
            }
        }
        adapterTodos.actualizarLista(filtrados);
    }

    private void abrirDetalle(Videojuego juego) {
        Intent intent = new Intent(this, DetalleJuegoActivity.class);
        intent.putExtra(DetalleJuegoActivity.EXTRA_JUEGO_ID, juego.getId());
        startActivity(intent);
    }

    private void cargarJuegosDesdeFirebase() {
        dbJuegos.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todosLosJuegos.clear();
                todosLosJuegosFull.clear();
                juegosRecomendados.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Videojuego juego = ds.getValue(Videojuego.class);
                    if (juego != null) {
                        juego.setId(ds.getKey());

                        if (juego.getPublicadoPor() != null && juego.getPublicadoPor().equals(uid)) {
                            juegosRecomendados.add(juego);
                        } else if (juego.isDisponible()) {
                            todosLosJuegos.add(juego);
                            todosLosJuegosFull.add(juego);
                        }
                    }
                }

                // Reaplicar filtro si hay texto en el buscador
                String queryActual = etBuscarJuego.getText().toString().trim();
                List<Videojuego> mostrar = queryActual.isEmpty()
                        ? new ArrayList<>(todosLosJuegos)
                        : filtradosPor(queryActual);

                recyclerTodos.post(() -> {
                    adapterTodos.actualizarLista(mostrar);
                    recyclerTodos.requestLayout();
                });
                recyclerRecomendados.post(() -> {
                    adapterRecomendados.actualizarLista(new ArrayList<>(juegosRecomendados));
                    recyclerRecomendados.requestLayout();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaginaPrincipalActivity.this,
                        "Error al sincronizar: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Videojuego> filtradosPor(String query) {
        List<Videojuego> resultado = new ArrayList<>();
        String q = query.toLowerCase();
        for (Videojuego j : todosLosJuegosFull) {
            if (j.getNombre() != null && j.getNombre().toLowerCase().contains(q)) {
                resultado.add(j);
            }
        }
        return resultado;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.lateral_perfil) {
            Toast.makeText(this, "Mi perfil (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.lateral_compras) {
            startActivity(new Intent(this, BibliotecaActivity.class));
        } else if (id == R.id.lateral_ajustes) {
            Toast.makeText(this, "Ajustes (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.lateral_cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
