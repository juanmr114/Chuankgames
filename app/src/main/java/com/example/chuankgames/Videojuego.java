package com.example.chuankgames;

public class Videojuego {
    private String id;
    private String nombre;
    private String genero;
    private String descripcion;
    private double precio;
    private String imagenUrl;
    private String publicadoPor;
    private String nombreVendedor;
    private long fechaPublicacion;
    private boolean disponible;

    public Videojuego() {}

    public Videojuego(String id, String nombre, String genero, String descripcion,
                      double precio, String imagenUrl) {
        this.id = id;
        this.nombre = nombre;
        this.genero = genero;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.disponible = true;
    }

    public String getId()                               { return id; }
    public void setId(String id)                        { this.id = id; }
    public String getNombre()                           { return nombre; }
    public void setNombre(String nombre)                { this.nombre = nombre; }
    public String getGenero()                           { return genero; }
    public void setGenero(String genero)                { this.genero = genero; }
    public String getDescripcion()                      { return descripcion; }
    public void setDescripcion(String descripcion)      { this.descripcion = descripcion; }
    public double getPrecio()                           { return precio; }
    public void setPrecio(double precio)                { this.precio = precio; }
    public String getImagenUrl()                        { return imagenUrl; }
    public void setImagenUrl(String imagenUrl)          { this.imagenUrl = imagenUrl; }
    public String getPublicadoPor()                     { return publicadoPor; }
    public void setPublicadoPor(String publicadoPor)    { this.publicadoPor = publicadoPor; }
    public String getNombreVendedor()                   { return nombreVendedor; }
    public void setNombreVendedor(String n)             { this.nombreVendedor = n; }
    public long getFechaPublicacion()                   { return fechaPublicacion; }
    public void setFechaPublicacion(long f)             { this.fechaPublicacion = f; }
    public boolean isDisponible()                       { return disponible; }
    public void setDisponible(boolean disponible)       { this.disponible = disponible; }
}