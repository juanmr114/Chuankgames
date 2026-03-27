package com.example.chuankgames;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    private List<Videojuego> todosLosJuegos = new ArrayList<>();

    // Género favorito del usuario (en el futuro vendrá de Firebase/SharedPreferences)
    private String generoFavorito = "Acción";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_principal);

        inicializarVistas();
        configurarToolbar();
        configurarDrawer();
        configurarBottomNav();
        cargarDatosDePrueba();
        configurarRecyclers();
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

        // Actualizar cabecera con datos del usuario de Firebase
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
                Toast.makeText(this, "Añadir juego (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_carrito) {
                Toast.makeText(this, "Carrito (próximamente)", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_inicio);
    }

    private void cargarDatosDePrueba() {
        // Datos de ejemplo — aquí conectarás con Firebase Realtime Database
        todosLosJuegos.add(new Videojuego("1", "God of War",      "Acción",     "Épica aventura nórdica con Kratos y Atreus",       29.99, ""));
        todosLosJuegos.add(new Videojuego("2", "The Witcher 3",   "RPG",        "Explora el mundo abierto de Geralt de Rivia",       9.99,  ""));
        todosLosJuegos.add(new Videojuego("3", "Minecraft",       "Sandbox",    "Construye y sobrevive en un mundo de bloques",      19.99, ""));
        todosLosJuegos.add(new Videojuego("4", "FIFA 25",         "Deportes",   "El simulador de fútbol más popular del mundo",      59.99, ""));
        todosLosJuegos.add(new Videojuego("5", "Cyberpunk 2077",  "Acción",     "Aventura futurista en Night City",                  14.99, ""));
        todosLosJuegos.add(new Videojuego("6", "Stardew Valley",  "Simulación", "Cultiva tu granja y vive en el campo",              13.99, ""));
        todosLosJuegos.add(new Videojuego("7", "Elden Ring",      "Acción",     "RPG de acción desafiante en un mundo abierto",      39.99, ""));
        todosLosJuegos.add(new Videojuego("8", "Among Us",        "Multijugador","Descubre quién es el impostor en la nave", 3.99,  ""));
        todosLosJuegos.add(new Videojuego("9", "Sex with hitler",        "Historia","La historia de un pintor Austriaco", 5.99,  ""));
    }

    private void configurarRecyclers() {
        // Todos los juegos — 2 columnas
        adapterTodos = new VideojuegoAdapter(todosLosJuegos, juego ->
                Toast.makeText(this, "Abriendo: " + juego.getNombre(), Toast.LENGTH_SHORT).show()
        );
        recyclerTodos.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerTodos.setAdapter(adapterTodos);
        recyclerTodos.setNestedScrollingEnabled(false);

        // Recomendados — filtrar por género favorito, 2 columnas
        List<Videojuego> recomendados = todosLosJuegos.stream()
                .filter(j -> j.getGenero().equalsIgnoreCase(generoFavorito))
                .collect(Collectors.toList());

        adapterRecomendados = new VideojuegoAdapter(recomendados, juego ->
                Toast.makeText(this, "Abriendo: " + juego.getNombre(), Toast.LENGTH_SHORT).show()
        );
        recyclerRecomendados.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerRecomendados.setAdapter(adapterRecomendados);
        recyclerRecomendados.setNestedScrollingEnabled(false);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
