package uy.max.guita;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Data {
    static List<String> accounts;
    static String ledger;
    public static List<String> entryCache;

    static {
        accounts = new ArrayList<String>();
        entryCache = new ArrayList<String>();
        updateLedger(getString("ledger"));
    }

    public static List<String> FindAccount(String text) {
        synchronized (accounts) {
            List<String> results = new ArrayList<String>();
            for(String account : accounts) {
                if (account.toLowerCase().contains(text.toLowerCase())) {
                    results.add(account);
                }
            }
            return results;
        }
    }

    public static String getString(String key) {
        return sharedPreferences().getString(key, "");
    }

    public static void setString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences().edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static SharedPreferences sharedPreferences() {
        return MainActivity.AppContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public static void updateLedger(String ledger) {
        synchronized (accounts) {
            setString("ledger", ledger);
            accounts.clear();
            Matcher matcher = Pattern.compile("^[ \\t]+(\\w.*?)([ \\t]{2}.*|$)", Pattern.MULTILINE).matcher(ledger);
            while(matcher.find()) {
                String account = matcher.group(1).trim();
                accounts.remove(account);
                accounts.add(account);
            }
        }
    }

    public static void cacheEntry(String entry) {
        synchronized (entryCache) {
            entryCache.add(entry);
        }
    }

    public static void removeCacheEntry(int position) {
        synchronized (entryCache) {
            entryCache.remove(position);
        }
    }

    public static void clearCacheEntries() {
        synchronized (entryCache) {
            entryCache.clear();
        }
    }

    public static String getEntryCacheString() {
        synchronized (entryCache) {
            StringBuilder builder = new StringBuilder();
            for(String s : entryCache) {
                builder.append(s.trim());
                builder.append("\n\n");
            }
            return builder.toString();
        }
    }
}
