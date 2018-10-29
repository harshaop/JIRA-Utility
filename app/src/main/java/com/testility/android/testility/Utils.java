package com.testility.android.testility;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

/*import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;*/

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public final class Utils {

    private static final String LOG_TAG = "Utils";

    public static String fetchJiraAPIResponse(String requestUrl, String username, String password) {
        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jiraJsonResponse = null;
        try {
            // testConnection(requestUrl);
            jiraJsonResponse = makeHttpRequest(url, username, password);
        } catch (IOException e) {
            Log.e(LOG_TAG + "-fetchJiraAPIResp", "Error closing input stream", e);
        }

        String jiraTitle = extractFeatureFromJiraJson(jiraJsonResponse);

        return jiraTitle;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {

            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error with creating URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url, String username, String password) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;

        InputStream inputStream = null;
        try {

            String auth = new String(username + ":" + password);
            byte[] data1 = new byte[0];
            {
                try {
                    data1 = auth.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            String base64 = Base64.encodeToString(data1, Base64.NO_WRAP);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + base64);
            //    urlConnection.setRequestProperty(username,auth);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            //  urlConnection.setRequestProperty("X-Atlassian-Token", "no-check");
            //   urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            //  urlConnection.setDoOutput(true);
            urlConnection.connect();
             int code = urlConnection.getResponseCode();

            inputStream = urlConnection.getInputStream();
            jsonResponse = readFromStream(inputStream);
            Log.i("RESCode:", "" + code);
            Log.i("RES:", jsonResponse);
            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the JIRA JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
            reader.close();
            inputStreamReader.close();
        }

        return output.toString();
    }

    private static String extractFeatureFromJiraJson(String jiraJsonResponse) {
        // If the JSON string is empty or null, then return early.
        String returnStr = null;
        if (TextUtils.isEmpty(jiraJsonResponse)) {
            return returnStr;
        }
        try {
            JSONObject baseJsonResponse = new JSONObject(jiraJsonResponse);
            JSONObject featureObject = baseJsonResponse.getJSONObject("fields");

            // If there are results in the features array
            if (featureObject != null) {
                returnStr = featureObject.getString("summary");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the JIRA JSON results", e);
        }
        return returnStr;
    }


    public static String uploadToJira(String requestUrl,String username, String password, String fileToUpload, String fileName) throws IOException {
        /*String username = "tempharop";
        String password = "Start10";*/


        String auth = new String(username + ":" + password);
        byte[] data1 = new byte[0];
        {
            try {
                data1 = auth.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        String base64 = Base64.encodeToString(data1, Base64.NO_WRAP);

        URL url = createUrl(requestUrl);

        File file = new File(fileToUpload);
        final MediaType MEDIA_TYPE = fileToUpload.endsWith("png") ?
                MediaType.parse("image/png") : MediaType.parse("image/jpeg");

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, RequestBody.create(MEDIA_TYPE, file))
                .build();

        Headers headers = new Headers.Builder()
                .set("Authorization", "Basic " + base64)
                .set("X-Atlassian-Token", "no-check")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        Log.i(LOG_TAG,response.toString());
        if (!response.isSuccessful()) throw new IOException("Unexpected code" + response);

        return response.message();
    }

    public static String uploadImage(String requestUrl, String fileToUpload, String fileName) throws IOException {

        HttpURLConnection urlConnection = null;
        String response1 = null;
        try {
            String username = "tempharop";
            String password = "Start10";
            urlConnection = null;
            URL url = createUrl(requestUrl);
            response1 = null;
            String auth = new String(username + ":" + password);
            byte[] data1 = new byte[0];
            {
                try {
                    data1 = auth.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            String base64 = Base64.encodeToString(data1, Base64.NO_WRAP);
            int maxBufferSize = 1024 * 1024;

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            File file = new File(fileToUpload);
            FileInputStream fileInputStream = new FileInputStream(file);
            DataOutputStream dataOutputStream = new DataOutputStream(urlConnection.getOutputStream());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + base64);
            urlConnection.setRequestProperty("X-Atlassian-Token", "no-check");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);


            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            dataOutputStream.flush();
            dataOutputStream.close();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuilder builder = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    builder.append(inputLine);
                }
                response1 = builder.toString();
                bufferedReader.close();
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Upload filed.", e);

        }
        finally {
            urlConnection.disconnect();
            return response1;
        }
    }

}




