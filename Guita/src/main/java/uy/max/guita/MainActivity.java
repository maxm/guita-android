package uy.max.guita;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public static Context AppContext;
    NewEntryFragment newEntryFragment;
    QueryFragment queryFragment;

    ProgressBar progressBar;
    int progressBarStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext = getApplicationContext();

        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar)findViewById(R.id.progress);

        newEntryFragment = (NewEntryFragment) getFragmentManager().findFragmentByTag("new-entry");
        queryFragment = (QueryFragment) getFragmentManager().findFragmentByTag("query");

        if (newEntryFragment == null) {
            newEntryFragment = new NewEntryFragment();
            queryFragment = new QueryFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, queryFragment, "query")
                    .add(R.id.container, newEntryFragment, "new-entry")
                    .commit();
        }

        getFragmentManager().beginTransaction().hide(queryFragment).show(newEntryFragment).commit();

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Log.i("GUITA", "Found intent data " + intent.getData().getFragment());
            Data.setString("cookies", intent.getData().getFragment());
            finish();
            Intent newIntent = new Intent(this, MainActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newIntent);
        }

        updateLedger();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uploadEntries();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint("Query");
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            boolean visible = false;

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.isEmpty()) return false;
                queryFragment.doQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!visible && !newText.isEmpty()) {
                    getFragmentManager().beginTransaction().hide(newEntryFragment).show(queryFragment).commit();
                    visible = true;
                }
                if (visible && newText.isEmpty()) {
                    getFragmentManager().beginTransaction().hide(queryFragment).show(newEntryFragment).commit();
                    queryFragment.clear();
                    visible = false;
                    return true;
                }
                return false;
            }
        });

        return true;
    }

    public void uploadEntries() {
        if (Data.entryCache.isEmpty()) return;
        showProgressBar();
        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... is) {
                synchronized (Data.entryCache) {
                    if (Data.entryCache.isEmpty()) return 0;
                    try {
                        URL url = new URL(Data.BaseURL  + "append");
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
                            Data.updateLedger(Util.readStream(conn.getInputStream()));
                            newEntryFragment.updateEntryListFromBackground();
                        } else {
                            toastFromBackground("Server error " + conn.getResponseCode(), Toast.LENGTH_LONG);
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        toastFromBackground("Can't reach server", Toast.LENGTH_LONG);
                        e.printStackTrace();
                    }
                    hideProgressBar();
                    return 0;
                }
            }
        }.execute();
    }

    void toastFromBackground(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, length).show();
            }
        });
    }

    void updateLedger() {
        showProgressBar();
        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    URL url = new URL(Data.BaseURL  + "raw");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.addRequestProperty("Cookie", Data.getString("cookies"));

                    if (con.getResponseCode() == 200) {
                        Log.i("GUITA", "Update ledger from server");
                        Data.updateLedger(Util.readStream(con.getInputStream()));
                        newEntryFragment.updateEntryListFromBackground();
                    } else if(con.getResponseCode() == 302) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Data.BaseURL + "app_auth"));
                        startActivity(browserIntent);
                    } else {
                        toastFromBackground("Server error " + con.getResponseCode(), Toast.LENGTH_LONG);
                        Log.e("GUITA", "Server code " + con.getResponseCode());
                    }
                    con.disconnect();
                } catch (Exception e) {
                    toastFromBackground("Can't reach server", Toast.LENGTH_LONG);
                    e.printStackTrace();
                }
                hideProgressBar();
                return 0;
            }
        }.execute();
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

    void showProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ++progressBarStack;
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    void hideProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                --progressBarStack;
                if (progressBarStack == 0) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
