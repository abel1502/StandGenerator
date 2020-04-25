package com.abel.standgen;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class StandGenFragment extends Fragment {
    class StandData {
        String standName;
        String abilityName;
        String abilityUrl;
        char[] stats = new char[6];
        String abilityDescription;

        String getRepresentation() {
            return getString(R.string.fmt_stand_info,
                    standName,
                    abilityUrl,
                    abilityName,
                    stats[0], stats[1], stats[2], stats[3], stats[4], stats[5],
                    abilityDescription);
        }

        void setUrl() {
            if (abilityName == null)
                return;
            abilityUrl = getString(R.string.fmt_power_page, abilityName.replace(" ", "_"));
        }
    }

    private ArrayList<String> curNames = new ArrayList<>();
    private String curName = "";
    private String curBandName = "";
    private Boolean lastSearchSucceeded = false;
    private StandData standData = new StandData();
    private Random rnd = new Random();

    private EditText inputName;
    private EditText outputName;
    private EditText outputStand;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stand_gen, container, false);

        inputName = (EditText) view.findViewById(R.id.input_name);
        outputName = (EditText) view.findViewById(R.id.output_name);
        outputStand = (EditText) view.findViewById(R.id.output_stand);


        view.findViewById(R.id.btn_gen_name).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                generateName(v);
            }
        });

        view.findViewById(R.id.btn_gen_stand).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                generateStand(v);
            }
        });

        view.findViewById(R.id.btn_copy).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                copyStand(v);
            }
        });

        view.findViewById(R.id.label_ability_link).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openAbilityPage(v);
            }
        });

        return view;
    }

    public void openAbilityPage(View v) {
        String url = standData.abilityUrl;
        if (url == null)
            return;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void handleError(String err) {
        Log.d("HandleError", err);
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getActivity());
        dlgAlert.setMessage(err);
        dlgAlert.setTitle(getString(R.string.err_dialog_name));
        dlgAlert.setPositiveButton(getString(android.R.string.ok), null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    public void generateName(View v) {
        try {
            String _curBandName = inputName.getText().toString();
            if (!curBandName.equals(_curBandName) || !lastSearchSucceeded) {
                curBandName = _curBandName;
                performItunesSearch();
            }
            if (curNames.size() == 0) {
                lastSearchSucceeded = false;
                handleError(getString(R.string.err_name_not_found));
            } else {
                lastSearchSucceeded = true;
                curName = curNames.get(rnd.nextInt(curNames.size()));
                outputName.setText(curName);
            }
        } catch (StandGenException e) {
            lastSearchSucceeded = false;
            handleError(getString(R.string.err_name_not_generated));
        }
    }

    @Nullable
    private String getWebPage(String targetAddr) throws IOException {
        URL target = new URL(targetAddr);
        HttpURLConnection con;
        con = (HttpURLConnection) target.openConnection();
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            StringBuffer stringBuffer = new StringBuffer();  // TODO: Replace with stringbuilder, unless it breaks something
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
            return stringBuffer.toString();
        } finally {
            con.disconnect();
        }
    }

    private JSONObject getJSON(String targetAddr) throws IOException, JSONException {
        return new JSONObject(getWebPage(targetAddr));
    }

    private void performItunesSearch() throws StandGenException {
        try {
            JSONObject data = getJSON(getString(R.string.fmt_itunes_search, Uri.encode(curBandName)));
            if (data.length() == 0)
                throw new StandGenException();
            JSONArray results = data.getJSONArray("results");
            curNames.clear();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                if (!item.getString("wrapperType").equals("track"))
                    continue;
                String _curName = item.getString("trackCensoredName");
                int tmp = _curName.indexOf("(");
                if (tmp != -1) {
                    _curName = _curName.substring(0, tmp);
                }
                if (_curName.equals("Intro") || _curName.isEmpty()) {
                    continue;
                }
                curNames.add(_curName);
            }
        } catch (IOException | JSONException e) {
            Log.e("MainActivity", "performItunesSearch", e);
            throw new StandGenException();
        }
    }

    public void generateStand(View v) {
        StandData prevStandData = standData;
        try {
            standData = new StandData();
            if (curName.isEmpty())
                throw new StandGenException();
            standData.standName = curName;
            generateStandAbility();
            generateStandStats();
            outputStand.setText(standData.getRepresentation());
        } catch (StandGenException e) {
            standData = prevStandData;
            handleError(getString(R.string.err_ability_not_generated));
        }
    }

    private void _assert(boolean statement) throws StandGenException {
        if (!statement)
            throw new StandGenException();
    }

    private void generateStandAbility() throws StandGenException {
        try {
            String pageData = getWebPage(getString(R.string.fmt_power_page, getString(R.string.random_power_page)));
            int metaTagStart = pageData.indexOf("og:description");
            _assert(metaTagStart >= 0);
            int contentStart = pageData.indexOf("content", metaTagStart);
            _assert(contentStart >= 0);
            int descriptionStart = pageData.indexOf("\"", contentStart) + 1;
            _assert(descriptionStart >= 1);
            int metaTagEnd = pageData.indexOf("\" />", descriptionStart);
            _assert(metaTagEnd >= 0);
            standData.abilityDescription = pageData.substring(descriptionStart, metaTagEnd).replace("&quot;", "\"").replace("&amp;", "&");

            // This part differs from the original, but seems to work
            // TODO: Maybe perform the same checks as for the ability description?
            int headerStart = pageData.indexOf(">", pageData.indexOf("<h1")) + 1;
            _assert(headerStart >= 1);
            int headerEnd = pageData.indexOf("</h1>", headerStart);
            _assert(headerEnd >= 0);
            standData.abilityName = pageData.substring(headerStart, headerEnd).replace("&quot;", "\"").replace("&amp;", "&");
            _assert(!standData.abilityName.contains("Admins"));

            standData.setUrl();
        } catch (IOException e) {
            throw new StandGenException();
        }
    }

    private void generateStandStats() {
        for (int i = 0; i < 6; i++) {
            standData.stats[i] = getString(R.string.stat_chars).charAt(rnd.nextInt(5));
        }
    }

    public void copyStand(View v) {
        String text = outputStand.getText().toString();
        if (text.isEmpty())
            return;
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(text, text);
        clipboard.setPrimaryClip(clip);
    }

    public void openPreferences(View v) {
        //startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }
}
