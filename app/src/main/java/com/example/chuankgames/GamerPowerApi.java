package com.example.chuankgames;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface GamerPowerApi {

    String BASE_URL = "https://www.gamerpower.com/api/";

    @GET("giveaways")
    Call<List<Noticia>> obtenerNoticias();
}
