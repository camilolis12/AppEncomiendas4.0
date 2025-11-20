package co.edu.appencomiendas.network;

import co.edu.appencomiendas.network.models.PaqueteRequest;
import co.edu.appencomiendas.network.models.PaqueteResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @GET("paquetes")
    Call<List<PaqueteResponse>> getPaquetes();

    @GET("paquetes/{id}")
    Call<PaqueteResponse> getPaqueteById(@Path("id") int id);

    @POST("paquetes")
    Call<PaqueteResponse> crearPaquete(@Body PaqueteRequest request);
}
