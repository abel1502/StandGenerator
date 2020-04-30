package com.abel.standgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


class PowerInfoResult {
    @SerializedName("id")
    private int id;

    @SerializedName("abstract")
    private String description;

    @SerializedName("title")
    private String title;

    @SerializedName("type")
    private String type;
    
    @SerializedName("url")
    private String url;

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getTitle(){
        return title;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getUrl(){
        return url;
    }
}

class PowerInfoDeserializer implements JsonDeserializer<PowerInfoResult> {
    @Override
    public PowerInfoResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonElement value = json.getAsJsonObject().get("items");
        String key = value.getAsJsonObject().keySet().iterator().next();
        JsonElement inner = value.getAsJsonObject().get(key);
        return new Gson().fromJson(inner, PowerInfoResult.class);
    }
}

class PowerRandomResult {
    @SerializedName("ns")
    private int ns;

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    public void setNs(int ns){
        this.ns = ns;
    }

    public int getNs(){
        return ns;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getTitle(){
        return title;
    }
}

class PowerRandomDeserializer implements JsonDeserializer<PowerRandomResult> {
    @Override
    public PowerRandomResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonElement value = json.getAsJsonObject().get("query").getAsJsonObject().get("random").getAsJsonArray().get(0);
        return new Gson().fromJson(value, PowerRandomResult.class);
    }
}

class ItunesResult {
    static final Type listOfT = new TypeToken<List<ItunesResult>>(){}.getType();

    @SerializedName("trackName")
    private String trackName;

    @SerializedName("wrapperType")
    private String wrapperType;

    @SerializedName("trackCensoredName")
    private String trackCensoredName;

    @SerializedName("artistName")
    private String artistName;

    public void setTrackName(String trackName){
        this.trackName = trackName;
    }

    public String getTrackName(){
        return trackName;
    }

    public void setWrapperType(String wrapperType){
        this.wrapperType = wrapperType;
    }

    public String getWrapperType(){
        return wrapperType;
    }

    public void setTrackCensoredName(String trackCensoredName){
        this.trackCensoredName = trackCensoredName;
    }

    public String getTrackCensoredName(){
        return trackCensoredName;
    }

    public void setArtistName(String artistName){
        this.artistName = artistName;
    }

    public String getArtistName(){
        return artistName;
    }
}

class ItunesDeserializer implements JsonDeserializer<List<ItunesResult>> {
    @Override
    public List<ItunesResult> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonElement value = json.getAsJsonObject().get("results");
        return new Gson().fromJson(value, ItunesResult.listOfT);
    }
}

class NetworkManager {
    private static Retrofit itunesRetrofit;
    private static Retrofit powerRetrofit;

    static Retrofit getItunesRetrofit() {
        if (itunesRetrofit == null) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ItunesResult.listOfT, new ItunesDeserializer())
                    .create();
            itunesRetrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl("http://itunes.apple.com")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return itunesRetrofit;
    }

    static Retrofit getPowerRetrofit() {
        if (powerRetrofit == null) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(PowerRandomResult.class, new PowerRandomDeserializer())
                    .registerTypeAdapter(PowerInfoResult.class, new PowerInfoDeserializer())
                    .create();
            powerRetrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl("https://powerlisting.fandom.com")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return powerRetrofit;
    }
}

interface ItunesService {
    @GET("/search?media=music")
    Call<List<ItunesResult>> search(@Query("term") String term);
}

interface PowerService {
    @GET("/api.php?action=query&format=json&list=random&rnlimit=1&rnnamespace=0")
    Call<PowerRandomResult> search();

    @GET("/api/v1/Articles/Details")
    Call<PowerInfoResult> getInfo(@Query("ids") int id, @Query("abstract") int descrLength);
}

