package com.example.chuankgames;

public class Videojuego {
    private String id;
    private String nombre;
    private String genero;
    private String descripcion;
    private double precio;          // precio en CK
    private String imagenUrl;
    private String publicadoPor;
    private String nombreVendedor;
    private long   fechaPublicacion;
    private boolean disponible;
    private String pagoPreferido;   // "euro" o "ck" (preferencia del vendedor)

    public Videojuego() {}

    public Videojuego(String id, String nombre, String genero, String descripcion,
                      double precio, String imagenUrl) {
        this.id = id; this.nombre = nombre; this.genero = genero;
        this.descripcion = descripcion; this.precio = precio;
        this.imagenUrl = imagenUrl; this.disponible = true;
        this.pagoPreferido = "euro";
    }

    public String  getId()                             { return id; }
    public void    setId(String id)                    { this.id = id; }
    public String  getNombre()                         { return nombre; }
    public void    setNombre(String n)                 { this.nombre = n; }
    public String  getGenero()                         { return genero; }
    public void    setGenero(String g)                 { this.genero = g; }
    public String  getDescripcion()                    { return descripcion; }
    public void    setDescripcion(String d)            { this.descripcion = d; }
    public double  getPrecio()                         { return precio; }
    public void    setPrecio(double p)                 { this.precio = p; }
    public String  getImagenUrl()                      { return imagenUrl; }
    public void    setImagenUrl(String u)              { this.imagenUrl = u; }
    public String  getPublicadoPor()                   { return publicadoPor; }
    public void    setPublicadoPor(String uid)         { this.publicadoPor = uid; }
    public String  getNombreVendedor()                 { return nombreVendedor; }
    public void    setNombreVendedor(String n)         { this.nombreVendedor = n; }
    public long    getFechaPublicacion()               { return fechaPublicacion; }
    public void    setFechaPublicacion(long f)         { this.fechaPublicacion = f; }
    public boolean isDisponible()                      { return disponible; }
    public void    setDisponible(boolean d)            { this.disponible = d; }
    public String  getPagoPreferido()                  { return pagoPreferido != null ? pagoPreferido : "euro"; }
    public void    setPagoPreferido(String p)          { this.pagoPreferido = p; }

    /** Precio en euros (100 CK = 1 €) */
    public double getPrecioEuros() { return precio / 100.0; }
}
