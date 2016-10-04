package org.xdty.lancamera.data;

import org.xdty.lancamera.module.History;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface HistoryService {
    @GET("{path}/")
    Call<List<History>> getHistory(@Header("Authorization") String basic,
            @Path("path") String path);
}
