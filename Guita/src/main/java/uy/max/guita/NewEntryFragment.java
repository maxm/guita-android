package uy.max.guita;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NewEntryFragment extends Fragment {

    LinearLayout suggestions;
    SuggestionsTextEditor newEntry;
    ListView entryList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        suggestions = (LinearLayout)rootView.findViewById(R.id.suggestions);
        newEntry = (SuggestionsTextEditor)rootView.findViewById(R.id.new_entry_editor);

        newEntry.suggestionsContainer = suggestions;
        newEntry.UpdateSuggestions();

        Button done = (Button)rootView.findViewById(R.id.done_button);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Data.cacheEntry(newEntry.getText().toString().trim());
                resetEntry();
                updateEntryList();
                ((MainActivity)getActivity()).uploadEntries();
            }
        });

        entryList = (ListView)rootView.findViewById(R.id.entry_list);
        entryList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        entryList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Data.removeCacheEntry(position - 1);
                    updateEntryList();
                }
                return true;
            }
        });

        resetEntry();
        updateEntryList();

        newEntry.requestFocus();

        return rootView;
    }

    void resetEntry() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        newEntry.setText(df.format(new Date())+ " ");
        newEntry.setSelection(newEntry.getText().length());
    }

    public void updateEntryList() {
        List<String> entries = new ArrayList<String>(Data.entryCache);
        entries.add(0, Data.ledger.substring(Data.ledger.length() - 1000, Data.ledger.length()));
        entryList.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.entry_list_item, R.id.entry, entries));
    }

    public void updateEntryListFromBackground() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateEntryList();
            }
        });
    }
}
