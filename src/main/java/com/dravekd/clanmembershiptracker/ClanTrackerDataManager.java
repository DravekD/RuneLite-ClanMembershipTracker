package com.dravekd.clanmembershiptracker;

import com.google.gson.*;
import okhttp3.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Singleton
public class ClanTrackerDataManager {
    //private final String baseUrl = "https://localhost:7072/api/v1/ClanTracker";
    private final String baseUrl = "https://clanactivitytracker20230112113751.azurewebsites.net/api/v1/ClanTracker";
    private static final MediaType JSON = MediaType.parse("application/json;");

    @Inject
    private OkHttpClient okHttpClient;

    //@Inject
    //private Gson gson;

    protected void updateClanMembership(List<ClanTracker> clans)
    {
        String url = baseUrl.concat("/UpdateClanMembers");
        try
        {
            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create();
            String json = gson.toJson(clans);

            Request r = new Request
                    .Builder()
                    .url(url)
                    .put(RequestBody.create(JSON, json))
                    .build();

            okHttpClient.newCall(r).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    log.debug("Error sending post data", e);
                }

                @Override
                public void onResponse(Call call, Response response)
                {
                    if (response.isSuccessful())
                    {
                        log.debug("Successfully sent clan membership data.");
                        response.close();
                    }
                    else
                    {
                        log.debug("Put request failed");
                        response.close();
                    }
                }
            });
        }
        catch (Exception ex)
        {
            log.error("Bad URL: " + ex.getLocalizedMessage());
        }
    }
}
