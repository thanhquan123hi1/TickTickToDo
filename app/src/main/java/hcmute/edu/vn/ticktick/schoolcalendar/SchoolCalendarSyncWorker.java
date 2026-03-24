package hcmute.edu.vn.ticktick.schoolcalendar;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.SchoolCalendarDao;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class SchoolCalendarSyncWorker extends Worker {

    private final SchoolCalendarPreferences preferences;

    public SchoolCalendarSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        preferences = new SchoolCalendarPreferences(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        String sourceUrl = preferences.getSourceUrl();
        if (!SchoolCalendarPreferences.isValidHttpUrl(sourceUrl)) {
            preferences.setLastSyncError(getApplicationContext().getString(
                    hcmute.edu.vn.ticktick.R.string.school_calendar_error_invalid_url));
            return Result.failure();
        }

        try {
            String body = downloadIcs(sourceUrl);
            long now = System.currentTimeMillis();
            IcsParser parser = new IcsParser();
            List<SchoolCalendarEventEntity> events = parser.parse(body, sourceUrl, now);

            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            SchoolCalendarDao dao = db.schoolCalendarDao();
            db.runInTransaction(() -> {
                dao.deleteAll();
                if (!events.isEmpty()) {
                    dao.insertAll(events);
                }
            });

            preferences.setLastSyncTime(now);
            preferences.setLastSyncError("");
            return Result.success();
        } catch (Exception e) {
            preferences.setLastSyncError(e.getMessage() == null
                    ? getApplicationContext().getString(hcmute.edu.vn.ticktick.R.string.school_calendar_error_sync_failed)
                    : e.getMessage());
            return Result.failure();
        }
    }

    private String downloadIcs(String sourceUrl) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(sourceUrl);
            connection = (HttpURLConnection) url.openConnection();

            if (connection instanceof HttpsURLConnection) {
                try {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        }
                    };
                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection https = (HttpsURLConnection) connection;
                    https.setSSLSocketFactory(sc.getSocketFactory());
                    https.setHostnameVerifier((hostname, session) -> true);
                } catch (Exception ignored) {
                }
            }

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }

            inputStream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
