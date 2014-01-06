package uy.max.guita;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public static Context AppContext;
    NewEntryFragment newEntryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext = getApplicationContext();

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            newEntryFragment = new NewEntryFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, newEntryFragment)
                    .commit();
        }

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Data.setString("cookies", intent.getData().getEncodedFragment());
            finish();
            Intent newIntent = new Intent(this, MainActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newIntent);
        }

        updateLedger();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_upload:
                item.setIcon(android.R.drawable.stat_notify_sync);
                item.setEnabled(false);
                uploadEntries(item);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void uploadEntries(final MenuItem item) {
        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... is) {
                synchronized (Data.entryCache) {
                    if (Data.entryCache.isEmpty()) return 0;
                    try {
                        URL url = new URL("http://ledger.outboxlabs.com/max/append");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.addRequestProperty("Cookie", Data.getString("cookies"));
                        conn.setReadTimeout(10000);
                        conn.setConnectTimeout(15000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);

                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair("append", Data.getEntryCacheString()));
                        params.add(new BasicNameValuePair("message", ""));

                        OutputStream os = conn.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(getQuery(params));
                        writer.flush();
                        writer.close();
                        os.close();

                        conn.connect();
                        if (conn.getResponseCode() == 200) {
                            toastFromBackground("Upload complete", Toast.LENGTH_LONG);
                            Data.clearCacheEntries();
                            newEntryFragment.updateEntryListFromBackground();
                        } else {
                            toastFromBackground("Server error " + conn.getResponseCode(), Toast.LENGTH_LONG);
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        toastFromBackground("Can't reach server", Toast.LENGTH_LONG);
                        e.printStackTrace();
                    }
                    return 0;
                }
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                item.setIcon(android.R.drawable.ic_menu_upload);
                item.setEnabled(true);
            }
        }.execute();
    }

    void toastFromBackground(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, length);
            }
        });
    }

    void updateLedger() {
        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    URL url = new URL("http://ledger.outboxlabs.com/max/raw");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.addRequestProperty("Cookie", Data.getString("cookies"));

                    if (con.getResponseCode() == 200) {
                        Log.i("GUITA", "Update ledger from server");
                        toastFromBackground("Updated ledger", Toast.LENGTH_SHORT);
                        Data.updateLedger(readResponse(con.getInputStream()));
                    } else {
                        toastFromBackground("Server error " + con.getResponseCode(), Toast.LENGTH_LONG);
                        Log.e("GUITA", "Server code " + con.getResponseCode());
                    }
                    con.disconnect();
                } catch (Exception e) {
                    toastFromBackground("Can't reach server", Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
                return 0;
            }
        }.execute();
    }

    String readResponse(InputStream inputStream) {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[10000];
        try {
            int read = 0;
            while((read = r.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
