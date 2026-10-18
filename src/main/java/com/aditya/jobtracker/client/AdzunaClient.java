package com.aditya.jobtracker.client;

import com.aditya.jobtracker.exception.ApiException;
import com.aditya.jobtracker.model.FullTimeJob;
import com.aditya.jobtracker.model.InternshipJob;
import com.aditya.jobtracker.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// Talks to the Adzuna job API and turns its JSON answer into Job objects.
// @Component = Spring creates ONE object of this class (a bean) and can inject it anywhere.
@Component
public class AdzunaClient {

    // "in" in the URL means India
    private static final String BASE_URL = "https://api.adzuna.com/v1/api/jobs/in/search/1";

    private final String appId;
    private final String appKey;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // @Value reads one setting from config.properties (Spring finds it for us).
    // The keys are never written in the code. After the colon is the default if missing.
    public AdzunaClient(@Value("${adzuna.app_id:}") String appId,
                        @Value("${adzuna.app_key:}") String appKey) {
        this.appId = appId;
        this.appKey = appKey;
    }

    public List<Job> searchJobs(String what, String where, int count) throws ApiException {
        if (appId.isBlank() || appKey.isBlank()) {
            throw new ApiException("The Adzuna keys are missing in config.properties.");
        }
        // URLEncoder turns spaces and special characters into a safe URL form
        String url = BASE_URL
                + "?app_id=" + encode(appId)
                + "&app_key=" + encode(appKey)
                + "&results_per_page=" + count
                + "&what=" + encode(what);
        if (!where.isEmpty()) {
            url += "&where=" + encode(where);
        }
        url += "&content-type=application/json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // no internet, or the request timed out
            throw new ApiException("Couldn't reach Adzuna. Check your internet connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("The request was interrupted.");
        }

        checkStatus(response.statusCode());
        return parseJobs(response.body());
    }

    // The status code tells us if the request worked. 200 = OK.
    private void checkStatus(int code) throws ApiException {
        if (code == 200) {
            return;
        }
        if (code == 401 || code == 403) {
            throw new ApiException("Adzuna rejected your App ID or App Key. Check config.properties.");
        }
        if (code == 429) {
            throw new ApiException("Adzuna request limit reached. Wait a while and try again.");
        }
        throw new ApiException("Adzuna returned error code " + code + ".");
    }

    private List<Job> parseJobs(String body) throws ApiException {
        List<Job> jobs = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");

            for (JsonElement item : results) {
                JsonObject o = item.getAsJsonObject();

                String title = clean(text(o, "title"));
                String company = clean(nestedName(o, "company"));
                String location = clean(nestedName(o, "location"));
                Double salary = number(o, "salary_min");

                if (company.isEmpty()) company = "Unknown company";
                if (location.isEmpty()) location = "India";

                // Adzuna has no "internship" flag, so we look for the word in the title.
                // Experience and duration aren't given, so we pass -1 and 0 for "not listed".
                Job job;
                if (title.toLowerCase().contains("intern")) {
                    job = new InternshipJob(title, company, location, salary, 0);
                } else {
                    job = new FullTimeJob(title, company, location, salary, -1);
                }
                job.setApplyLink(text(o, "redirect_url"));
                jobs.add(job);
            }
        } catch (JsonSyntaxException | IllegalStateException | NullPointerException e) {
            throw new ApiException("Adzuna sent data in a format we didn't expect.");
        } catch (IllegalArgumentException e) {
            // thrown by Job if a title is blank; we skip nothing silently, we report it
            throw new ApiException("Adzuna sent a job without a title.");
        }
        return jobs;
    }

    // ---- small helper methods so the code above stays readable ----

    private String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? "" : e.getAsString();
    }

    // company and location look like  "company": {"display_name": "Infosys"}
    private String nestedName(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonObject()) {
            return "";
        }
        return text(e.getAsJsonObject(), "display_name");
    }

    private Double number(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? null : e.getAsDouble();
    }

    // Adzuna wraps search words in <strong> tags. This removes any HTML tag.
    private String clean(String s) {
        return s.replaceAll("<[^>]*>", "").trim();
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
