import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestWorker {
    public static void main(String[] args) {
        String url = "https://utexlms.hcmute.edu.vn/calendar/export_execute.php?userid=61450&authtoken=f4b12686cc29289a5ec8cd7b969ed429f5ef56a0&preset_what=all&preset_time=recentupcoming";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                System.out.println("HTTP " + code);
                return;
            }

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
                count++;
            }
            System.out.println("Success, downloaded " + count + " lines");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

