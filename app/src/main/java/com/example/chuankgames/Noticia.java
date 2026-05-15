package com.example.chuankgames;

import com.google.gson.annotations.SerializedName;

public class Noticia {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String titulo;

    @SerializedName("worth")
    private String valor;

    @SerializedName("thumbnail")
    private String thumbnailUrl;

    @SerializedName("description")
    private String descripcion;

    @SerializedName("gamerpower_url")
    private String url;

    @SerializedName("published_date")
    private String fecha;

    @SerializedName("platforms")
    private String plataformas;

    @SerializedName("status")
    private String estado;

    @SerializedName("type")
    private String tipo;

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getValor() { return valor; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getDescripcion() { return descripcion; }
    public String getUrl() { return url; }
    public String getFecha() { return fecha; }
    public String getPlataformas() { return plataformas; }
    public String getEstado() { return estado; }
    public String getTipo() { return tipo; }
}
