package uy.max.guita;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuggestionsTextEditor extends EditText {
    public LinearLayout suggestionsContainer;
    LayoutInflater inflater;

    public SuggestionsTextEditor(Context context) {
        super(context);
    }

    public SuggestionsTextEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuggestionsTextEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        UpdateSuggestions();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (lengthBefore == 0 && lengthAfter == 1 && text.charAt(start) == '\n') {
            getText().insert(getSelectionStart(), "  ");
        }
        UpdateSuggestions();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        UpdateSuggestions();
    }

    public void UpdateSuggestions() {
        if (suggestionsContainer == null) return;
        suggestionsContainer.setVisibility(isFocused() ? VISIBLE : GONE);
        if (!isFocused()) return;
        List<Suggestion> suggestions = getSuggestions(getText().toString(), getSelectionEnd());
        if (suggestions.isEmpty()) {
            suggestionsContainer.setVisibility(GONE);
            return;
        }
        suggestionsContainer.removeAllViews();
        if (inflater == null) inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for(Suggestion suggestion : suggestions) {
            Button button = (Button)inflater.inflate(R.layout.suggestion_view, suggestionsContainer, false);
            suggestionsContainer.addView(button);
            button.setText(suggestion.display);

            final String replacement = suggestion.replacement;
            final int start = suggestion.start;
            final int end = suggestion.end;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getText().replace(start, end, replacement);
                }
            });
        }
    }

    static class Suggestion {
        public Suggestion(String replacement, int start, int end, String display) {
            this.replacement = replacement;
            this.display = display;
            this.start = start;
            this.end = end;
        }
        public Suggestion(String replacement, int start, int end) {
            this.replacement = replacement;
            this.display = replacement;
            this.start = start;
            this.end = end;
        }
        public String replacement;
        public int start, end;
        public String display;
    }

    List<Suggestion> getSuggestions(String text, int cursor) {
        int lineStart = cursor;
        int lineEnd = cursor;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') --lineStart;
        while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') ++lineEnd;

        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        String line = text.substring(lineStart, lineEnd);
        String typed = text.substring(lineStart, cursor);

        if (line.length() == 0) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
            String date = df.format(new Date());
            suggestions.add(new Suggestion(date, lineStart, lineEnd));
        } else if (typed.matches("\\s+\\w[\\w\\s:]*\\s\\s+")) {
            suggestions.add(new Suggestion("$", cursor, cursor));
            suggestions.add(new Suggestion("US$", cursor, cursor));
        } else if (typed.matches("\\s+\\w.*") && !typed.matches("\\s+\\w.*?\\s\\s.*")) {
            String account = MatchGroup("\\s+(\\w.*?)(\\s\\s.*|$)", typed, 1);
            List<String> accounts = Data.FindAccount(account);
            int maxAccounts = 3;
            if (accounts.size() > maxAccounts) {
                accounts = accounts.subList(accounts.size() - maxAccounts, accounts.size());
            }
            Collections.reverse(accounts);
            int start = lineStart + MatchGroup("(\\s+)\\w.*", typed, 1).length();
            int end = lineStart + MatchGroup("(\\s+\\w.*?)(\\s\\s.*|$)", typed, 1).length();
            for(String acc : accounts) {
                String display = acc.replace(":", ":\u200B");
                suggestions.add(new Suggestion(acc + "  ", start, end, display));
            }
        } else if(typed.matches("\\s+\\w.*?\\s\\s.*\\d")) {
            Matcher matcher = Pattern.compile("\\s+\\w.*?\\s\\s[^\\d]*(\\d+(\\.\\d+)?)").matcher(typed);
            if (matcher.matches()) {
                String numString = matcher.group(1);
                BigDecimal value = new BigDecimal(numString);
                BigDecimal alpha = value.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_DOWN);
                alpha = value.add(alpha.add(alpha.multiply(new BigDecimal("0.22")).setScale(2, RoundingMode.HALF_DOWN)));
                BigDecimal half = value.divide(new BigDecimal(2));

                int start = lineStart + matcher.start(1);
                int end = lineStart + matcher.end(1);
                suggestions.add(new Suggestion(alpha.toString(), start, end, "\u03B1BROU"));
                suggestions.add(new Suggestion(half.toString(), start, end, "half"));
            }
        }

        return suggestions;
    }

    static String MatchGroup(String pattern, String input, int group) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.matches() && group <= matcher.groupCount()) {
            return matcher.group(group);
        }
        return "";
    }
}
