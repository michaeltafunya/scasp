package zm.scasp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

/** One screen: loads the course list over TCP (background thread), caches it, opens a campus place in a map. */
public class MainActivity extends Activity {
    // 10.0.2.2 is the host machine from the Android emulator. Gateway port as started by the server.
    private static final String HOST = "10.0.2.2";
    private static final int PORT = 9090;
    private TextView courses;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        courses = findViewById(R.id.courses);
        SharedPreferences prefs = getSharedPreferences("cache", MODE_PRIVATE);
        courses.setText(b != null ? b.getString("courses") : prefs.getString("courses", "No data yet"));
        findViewById(R.id.refresh).setOnClickListener(v -> refresh(prefs));
        findViewById(R.id.map).setOnClickListener(v -> openMap());
    }

    private void refresh(SharedPreferences prefs) {
        Executors.newSingleThreadExecutor().execute(() -> {           // never block the main thread
            String text;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(HOST, PORT), 3000);
                s.setSoTimeout(3000);
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("COURSES");
                text = new BufferedReader(new InputStreamReader(s.getInputStream())).readLine();
                prefs.edit().putString("courses", text).apply();     // cache for offline use
            } catch (IOException e) {
                text = "Offline - showing cached: " + prefs.getString("courses", "nothing cached");
            }
            String shown = text;
            runOnUiThread(() -> courses.setText(shown));
        });
    }

    /** Implicit intent; the location permission is requested only when this feature is used. */
    private void openMap() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:-15.3875,28.3228?q=Library")));
    }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) openMap();
        else Toast.makeText(this, "Location denied - map needs it", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onSaveInstanceState(Bundle b) {          // survive rotation
        super.onSaveInstanceState(b);
        b.putString("courses", courses.getText().toString());
    }
}
