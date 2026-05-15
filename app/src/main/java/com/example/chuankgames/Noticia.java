package com.example.chuankgames;

public class Noticia {
    private String titulo;
    private String descripcion;
    private String urlImagen;
    private String url;
    private String fuente;
    private String fecha;

    public Noticia(String titulo, String descripcion, String urlImagen,
                   String url, String fuente, String fecha) {
        this.titulo      = titulo;
        this.descripcion = descripcion;
        this.urlImagen   = urlImagen;
        this.url         = url;
        this.fuente      = fuente;
        this.fecha       = fecha;
    }

    public String getTitulo()      { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getUrlImagen()   { return urlImagen; }
    public String getUrl()         { return url; }
    public String getFuente()      { return fuente; }
    public String getFecha()       { return fecha; }
}