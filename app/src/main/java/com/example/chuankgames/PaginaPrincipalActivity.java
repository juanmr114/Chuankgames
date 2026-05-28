package com.example.chuankgames;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private DrawerLayout       drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView     navigationView;

    private RecyclerView       recyclerTodos, recyclerRecomendados;
    private VideojuegoAdapter  adapterTodos, adapterRecomendados;
    private EditText           etBuscar;
    private TextView           tvSaldoPrincipal, tvEmptyTodos, tvEmptyPropios;
    private LinearLayout       layoutChipsGenero, layoutChipsPrecio;

    // Listas fuente de verdad
    private final List<Videojuego> todosCompleto   = new ArrayList<>();
    private final List<Videojuego> propiosCompleto = new ArrayList<>();
    // Listas que muestra el adaptador
    private final List<Videojuego> todosLosJuegos    = new ArrayList<>();
    private final List<Videojuego> juegosRecomendados = new ArrayList<>();

    private DatabaseReference dbJuegos, dbUsuarios;
    private String uid;

    // Estado de filtros
    private String generoFiltro    = "";   // "" = todos
    private double precioMaxFiltro = -1;   // -1 = sin límite
    // Chip activo label (para resaltar el seleccionado)
    private String chipGeneroActivo  = "Todos";
    private String chipPrecioActivo  = "Todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pagina_principal);

        uid        = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        dbJuegos   = FirebaseDatabase.getInstance(DB_URL).getReference("videojuegos");
        dbUsuarios = FirebaseDatabase.getInstance(DB_URL).getReference("usuarios");

        inicializarVistas();
        configurarToolbar();
        configurarDrawer();
        configurarBottomNav();
        configurarRecyclers();
        configurarBuscador();
        setupFiltros();
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
        layoutChipsGenero    = findViewById(R.id.layoutChipsGenero);
        layoutChipsPrecio    = findViewById(R.id.layoutChipsPrecio);

        // Botón cerrar sesión (esquina superior derecha)
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("👋 ¡Hasta luego!")
                        .setMessage("Gracias Oscar")
                        .setPositiveButton("Salir", (d, w) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show()
        );
    }

    // ── Filtros ───────────────────────────────────────────────────────────

    private void setupFiltros() {
        // Géneros disponibles
        String[] generos = {
                "Todos", "Acción", "Aventura", "RPG", "Deportes",
                "Simulación", "Estrategia", "Puzzle", "Terror",
                "Multijugador", "Sandbox", "Plataformas", "Carreras",
                "Lucha", "Shooter", "Indie"
        };
        for (String genero : generos) {
            boolean activo = genero.equals("Todos");
            TextView chip  = crearChip(genero, activo);
            chip.setOnClickListener(v -> {
                generoFiltro       = genero.equals("Todos") ? "" : genero;
                chipGeneroActivo   = genero;
                actualizarChips(layoutChipsGenero, genero);
                filtrarJuegos(queryActual());
            });
            layoutChipsGenero.addView(chip);
        }

        // Opciones de precio
        double[] precios     = { -1,   1.0,  5.0, 10.0, 20.0, 50.0 };
        String[] labPrecio   = {"Todos", "< €1", "< €5", "< €10", "< €20", "< €50"};
        for (int i = 0; i < precios.length; i++) {
            final double precio = precios[i];
            final String label  = labPrecio[i];
            boolean activo = label.equals("Todos");
            TextView chip  = crearChip(label, activo);
            chip.setOnClickListener(v -> {
                precioMaxFiltro  = precio;
                chipPrecioActivo = label;
                actualizarChips(layoutChipsPrecio, label);
                filtrarJuegos(queryActual());
            });
            layoutChipsPrecio.addView(chip);
        }
    }

    /** Crea un TextView con estilo de chip. */
    private TextView crearChip(String texto, boolean activo) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        chip.setText(texto);
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(12), dp(5), dp(12), dp(5));
        aplicarEstiloChip(chip, activo);
        return chip;
    }

    private void aplicarEstiloChip(TextView chip, boolean activo) {
        if (activo) {
            chip.setBackground(getDrawable(R.drawable.bg_chip_ck));
            chip.setTextColor(getColor(R.color.principal));
        } else {
            chip.setBackground(getDrawable(R.drawable.bg_btn_outline));
            chip.setTextColor(getColor(R.color.secundario));
        }
    }

    private void actualizarChips(LinearLayout container, String seleccionado) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView chip = (TextView) container.getChildAt(i);
            aplicarEstiloChip(chip, chip.getText().toString().equals(seleccionado));
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String queryActual() {
        return etBuscar != null ? etBuscar.getText().toString().trim().toLowerCase() : "";
    }

    // ── Navegación ────────────────────────────────────────────────────────

    private void asegurarSaldoInicial() {
        if (uid.isEmpty()) return;
        dbUsuarios.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.child("saldo").exists())
                    dbUsuarios.child(uid).child("saldo").setValue(0.0);
                if (!snapshot.child("dinero").exists())
                    dbUsuarios.child(uid).child("dinero").setValue(50.0);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void configurarToolbar() {
        View btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START);
            else
                drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void configurarDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        TextView tvNombre = headerView.findViewById(R.id.navHeaderNombre);
        TextView tvEmail  = headerView.findViewById(R.id.navHeaderEmail);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            // Email siempre disponible en Auth
            tvEmail.setText(usuario.getEmail() != null ? usuario.getEmail() : "");

            // Nombre: primero intentamos Auth displayName; si no está, leemos Firebase DB
            String authName = usuario.getDisplayName();
            if (authName != null && !authName.isEmpty()) {
                tvNombre.setText(authName);
            } else {
                tvNombre.setText("Cargando...");
                dbUsuarios.child(uid).child("nombre")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String nombre = snapshot.exists()
                                        ? snapshot.getValue(String.class) : null;
                                tvNombre.setText(nombre != null && !nombre.isEmpty()
                                        ? nombre : "Usuario");
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {
                                tvNombre.setText("Usuario");
                            }
                        });
            }
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
        dbUsuarios.child(uid).child("dinero").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double dinero = snapshot.exists() ? snapshot.getValue(Double.class) : 0;
                tvSaldoPrincipal.setText(String.format("💶 %.2f€", dinero));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        tvSaldoPrincipal.setOnClickListener(v ->
                startActivity(new Intent(this, RuletaActivity.class)));
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filtrarJuegos(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Filtrado ──────────────────────────────────────────────────────────

    private void filtrarJuegos(String query) {
        List<Videojuego> filtTodos = new ArrayList<>();
        for (Videojuego j : todosCompleto)
            if (pasaFiltros(j, query)) filtTodos.add(j);

        List<Videojuego> filtPropios = new ArrayList<>();
        for (Videojuego j : propiosCompleto)
            if (pasaFiltros(j, query)) filtPropios.add(j);

        adapterTodos.actualizarLista(filtTodos);
        adapterRecomendados.actualizarLista(filtPropios);

        if (tvEmptyTodos != null)
            tvEmptyTodos.setVisibility(filtTodos.isEmpty() ? View.VISIBLE : View.GONE);
        if (tvEmptyPropios != null)
            tvEmptyPropios.setVisibility(filtPropios.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean pasaFiltros(Videojuego j, String query) {
        // Búsqueda de texto
        if (!query.isEmpty() && !coincideConBusqueda(j, query)) return false;
        // Filtro de género
        if (!generoFiltro.isEmpty()) {
            String g = j.getGenero() != null ? j.getGenero() : "";
            if (!generoFiltro.equalsIgnoreCase(g)) return false;
        }
        // Filtro de precio máximo
        if (precioMaxFiltro > 0 && j.getPrecioEuros() >= precioMaxFiltro) return false;
        return true;
    }

    private boolean coincideConBusqueda(Videojuego j, String query) {
        return (j.getNombre()      != null && j.getNombre().toLowerCase().contains(query))
            || (j.getGenero()      != null && j.getGenero().toLowerCase().contains(query))
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
                            // Solo mostrar si está en venta; los comprados van a Biblioteca
                            if (juego.isDisponible()) {
                                propiosCompleto.add(juego);
                            }
                        } else if (juego.isDisponible()) {
                            todosCompleto.add(juego);
                        }
                    }
                }

                recyclerTodos.post(() -> {
                    filtrarJuegos(queryActual());
                    recyclerTodos.requestLayout();
                    recyclerRecomendados.requestLayout();
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PaginaPrincipalActivity.this,
                        "Error al sincronizar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            drawerLayout.closeDrawer(GravityCompat.START);
            new AlertDialog.Builder(this)
                    .setTitle("👋 ¡Hasta luego!")
                    .setMessage("Gracias Oscar")
                    .setPositiveButton("Salir", (d, w) -> {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}
