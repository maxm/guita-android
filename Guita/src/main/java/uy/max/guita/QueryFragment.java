package uy.max.guita;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class QueryFragment extends Fragment {
    ProgressBar progressBar;
    TextView textView;
    ScrollView scrollView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_query, container, false);
        progressBar = (ProgressBar)rootView.findViewById(R.id.progress);
        textView = (TextView)rootView.findViewById(R.id.textView);
        scrollView = (ScrollView)rootView.findViewById(R.id.scrollView);
        return rootView;
    }

    public void doQuery(final String query) {
        progressBar.setVisibility(View.VISIBLE);
        clear();
        new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    URL url = new URL(Data.BaseURL + "query_text?query=" + URLEncoder.encode(query, "utf-8"));
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.addRequestProperty("Cookie", Data.getString("cookies"));

                    if (con.getResponseCode() == 200) {
                        String result = Util.readStream(con.getInputStream());
                        textView.setText(result);
                        scrollView.forceLayout();
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    } else {
                        Log.e("GUITA", "Query server error " + con.getResponseMessage());
                    }
                    con.disconnect();
                } catch (Exception e) {
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                progressBar.setVisibility(View.GONE);
            }
        }.execute();
    }

    public void clear() {
        textView.setText("");
    }
}