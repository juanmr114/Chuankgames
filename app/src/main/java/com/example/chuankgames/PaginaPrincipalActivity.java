package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import java.util.stream.Collectors;

public class PaginaPrincipalActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView navigationView;

    private RecyclerView recyclerTodos, recyclerRecomendados;
    private VideojuegoAdapter adapterTodos, adapterRecomendados;

    private final List<Videojuego> todosLosJuegos     = new ArrayList<>();
    private final List<Videojuego> juegosRecomendados = new ArrayList<>();

    private DatabaseReference dbRef;
    private String generoFavorito = "Acción"; // En el futuro vendrá del perfil del usuario

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_principal);

        dbRef = FirebaseDatabase.getInstance().getReference("videojuegos");

        inicializarVistas();
        configurarToolbar();
        configurarDrawer();
        configurarBottomNav();
        configurarRecyclers();
        cargarJuegosDesdeFirebase();
    }

    private void inicializarVistas() {
        drawerLayout         = findViewById(R.id.drawerLayout);
        bottomNav            = findViewById(R.id.bottomNav);
        navigationView       = findViewById(R.id.navigationView);
        recyclerTodos        = findViewById(R.id.recyclerTodos);
        recyclerRecomendados = findViewById(R.id.recyclerRecomendados);
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
                Toast.makeText(this, "Buscar (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_añadir) {
                // Abrir pantalla de publicar juego
                startActivity(new Intent(this, PublicarJuegoActivity.class));
                return true;
            } else if (id == R.id.nav_carrito) {
                Toast.makeText(this, "Carrito (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_inicio);
    }

    private void configurarRecyclers() {
        adapterTodos = new VideojuegoAdapter(todosLosJuegos, juego ->
                Toast.makeText(this, "Abriendo: " + juego.getNombre(), Toast.LENGTH_SHORT).show()
        );
        recyclerTodos.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerTodos.setAdapter(adapterTodos);
        recyclerTodos.setNestedScrollingEnabled(false);

        adapterRecomendados = new VideojuegoAdapter(juegosRecomendados, juego ->
                Toast.makeText(this, "Abriendo: " + juego.getNombre(), Toast.LENGTH_SHORT).show()
        );
        recyclerRecomendados.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerRecomendados.setAdapter(adapterRecomendados);
        recyclerRecomendados.setNestedScrollingEnabled(false);
    }

    private void cargarJuegosDesdeFirebase() {
        // ValueEventListener actualiza la lista en tiempo real
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                todosLosJuegos.clear();
                juegosRecomendados.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Videojuego juego = ds.getValue(Videojuego.class);
                    if (juego != null) {
                        juego.setId(ds.getKey());
                        todosLosJuegos.add(juego);
                        if (juego.getGenero() != null &&
                                juego.getGenero().equalsIgnoreCase(generoFavorito)) {
                            juegosRecomendados.add(juego);
                        }
                    }
                }

                adapterTodos.actualizarLista(todosLosJuegos);
                adapterRecomendados.actualizarLista(juegosRecomendados);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaginaPrincipalActivity.this,
                        "Error cargando juegos: " + error.getMessage(),
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
            Toast.makeText(this, "Mis compras (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.lateral_ajustes) {
            Toast.makeText(this, "Ajustes (próximamente)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.lateral_cerrar_sesion) {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
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