package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

    private RecyclerView recyclerTodos, recyclerRecomendados;
    private VideojuegoAdapter adapterTodos, adapterRecomendados;
    private EditText etBuscar;
    private TextView tvSaldoPrincipal, tvEmptyTodos, tvEmptyPropios;

    // Listas completas (fuente de verdad desde Firebase)
    private final List<Videojuego> todosCompleto    = new ArrayList<>();
    private final List<Videojuego> propiosCompleto  = new ArrayList<>();
    // Listas que muestran los adaptadores (pueden estar filtradas)
    private final List<Videojuego> todosLosJuegos     = new ArrayList<>();
    private final List<Videojuego> juegosRecomendados = new ArrayList<>();

    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid;
    private String generoFavorito = "Acción";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_principal);
        android.util.Log.d("PAGINA", "onCreate ejecutado");

        uid        = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        // Busca donde inicializas dbJuegos y dbUsuarios y cámbialo por:
        dbJuegos = FirebaseDatabase.getInstance(DB_URL).getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance(DB_URL).getReference("usuarios");

        inicializarVistas();
        configurarToolbar();
        configurarDrawer();
        configurarBottomNav();
        configurarRecyclers();
        configurarBuscador();
        configurarSaldo();
        cargarJuegosDesdeFirebase();
        asegurarSaldoInicial();
    }

    private void inicializarVistas() {
        drawerLayout         = findViewById(R.id.drawerLayout);
        bottomNav            = findViewById(R.id.bottomNav);
        navigationView       = findViewById(R.id.navigationView);
        recyclerTodos        = findViewById(R.id.recyclerTodos);
        recyclerRecomendados = findViewById(R.id.recyclerRecomendados);
        etBuscar             = findViewById(R.id.etBuscar);
        tvSaldoPrincipal     = findViewById(R.id.tvSaldoPrincipal);
        tvEmptyTodos         = findViewById(R.id.tvEmptyTodos);
        tvEmptyPropios       = findViewById(R.id.tvEmptyPropios);
    }

    private void asegurarSaldoInicial() {
        if (uid.isEmpty()) return;
        dbUsuarios.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Migración: si no existe saldo, inicializar a 0
                if (!snapshot.child("saldo").exists()) {
                    dbUsuarios.child(uid).child("saldo").setValue(0.0);
                }
                // Migración: si no existe dinero, dar 50€ de bienvenida
                if (!snapshot.child("dinero").exists()) {
                    dbUsuarios.child(uid).child("dinero").setValue(50.0);
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
                startActivity(new Intent(this, RuletaActivity.class));
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

    private void configurarSaldo() {
        // Mostrar euros en el chip principal (más relevante para compras)
        dbUsuarios.child(uid).child("dinero").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double dinero = snapshot.exists() ? snapshot.getValue(Double.class) : 0;
                tvSaldoPrincipal.setText(String.format("💶 %.2f€", dinero));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        // Tap en el chip → Ruleta
        tvSaldoPrincipal.setOnClickListener(v ->
                startActivity(new Intent(this, RuletaActivity.class)));
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarJuegos(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filtrarJuegos(String query) {
        if (query.isEmpty()) {
            adapterTodos.actualizarLista(new ArrayList<>(todosCompleto));
            adapterRecomendados.actualizarLista(new ArrayList<>(propiosCompleto));
            return;
        }
        List<Videojuego> filtradosTodos = new ArrayList<>();
        for (Videojuego j : todosCompleto) {
            if (coincideConBusqueda(j, query)) filtradosTodos.add(j);
        }
        List<Videojuego> filtradosPropios = new ArrayList<>();
        for (Videojuego j : propiosCompleto) {
            if (coincideConBusqueda(j, query)) filtradosPropios.add(j);
        }
        adapterTodos.actualizarLista(filtradosTodos);
        adapterRecomendados.actualizarLista(filtradosPropios);
    }

    private boolean coincideConBusqueda(Videojuego j, String query) {
        return (j.getNombre() != null && j.getNombre().toLowerCase().contains(query))
                || (j.getGenero() != null && j.getGenero().toLowerCase().contains(query))
                || (j.getDescripcion() != null && j.getDescripcion().toLowerCase().contains(query));
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
                todosCompleto.clear();
                propiosCompleto.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Videojuego juego = ds.getValue(Videojuego.class);
                    if (juego != null) {
                        juego.setId(ds.getKey());
                        if (juego.getPublicadoPor() != null && juego.getPublicadoPor().equals(uid)) {
                            propiosCompleto.add(juego);
                        } else if (juego.isDisponible()) {
                            todosCompleto.add(juego);
                        }
                    }
                }

                android.util.Log.d("PAGINA", "Carga finalizada. Propios: " +
                        propiosCompleto.size() + " | Otros: " + todosCompleto.size());

                String queryActual = etBuscar != null
                        ? etBuscar.getText().toString().trim().toLowerCase() : "";
                recyclerTodos.post(() -> {
                    filtrarJuegos(queryActual);
                    recyclerTodos.requestLayout();
                    recyclerRecomendados.requestLayout();
                    // Empty states
                    if (tvEmptyTodos != null)
                        tvEmptyTodos.setVisibility(todosCompleto.isEmpty() ? View.VISIBLE : View.GONE);
                    if (tvEmptyPropios != null)
                        tvEmptyPropios.setVisibility(propiosCompleto.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaginaPrincipalActivity.this,
                        "Error al sincronizar con la base de datos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.lateral_perfil) {
            Toast.makeText(this, "Mi perfil (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.lateral_compras) {
            startActivity(new Intent(this, BibliotecaActivity.class));
        } else if (id == R.id.lateral_ruleta) {
            startActivity(new Intent(this, RuletaActivity.class));
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