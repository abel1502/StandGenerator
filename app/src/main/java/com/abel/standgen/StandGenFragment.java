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
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

//        void setUrl() {
//            if (abilityName == null)
//                return;
//            abilityUrl = getString(R.string.fmt_power_page, abilityName.replace(" ", "_"));
//        }
    }

    private Random rnd = new Random();
    private ArrayList<String> curNames = new ArrayList<>();
    private String curName = "";
    private String curBandName = "";
    private Boolean lastSearchSucceeded = false;
    private Boolean nameGenInProgress = false;
    private StandData standData = new StandData();
    private Boolean standGenInProgress = false;

    private EditText inputName;
    private EditText outputName;
    private EditText outputStand;
    private ProgressBar spinnerGenName;
    private ProgressBar spinnerGenStand;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stand_gen, container, false);

        inputName = view.findViewById(R.id.input_name);
        outputName = view.findViewById(R.id.output_name);
        outputStand = view.findViewById(R.id.output_stand);
        spinnerGenStand = view.findViewById(R.id.spinner_gen_stand);
        spinnerGenName = view.findViewById(R.id.spinner_gen_name);

        view.findViewById(R.id.btn_gen_name).setOnClickListener(this::startGenerateName);
        view.findViewById(R.id.btn_gen_stand).setOnClickListener(this::startGenerateStand);
        view.findViewById(R.id.btn_copy).setOnClickListener(this::copyStand);
        view.findViewById(R.id.label_ability_link).setOnClickListener(this::openAbilityPage);
        view.findViewById(R.id.btn_info).setOnClickListener(this::showAboutDialog);

        return view;
    }

    private void handleError(int errId) {
        //Log.d("HandleError", err);
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getActivity());
        dlgAlert.setMessage(getString(errId));
        dlgAlert.setTitle(getString(R.string.dlg_error_name));
        dlgAlert.setPositiveButton(getString(android.R.string.ok), null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    private void debugHandleError(String error) {
        debugHandleError(error, null);
    }

    private void debugHandleError(String error, @Nullable Throwable thr) {
        String data = error;
        if (thr != null) {
            data += " - " + thr.getMessage();
        }
        Log.d("DEBUG_ERR", data);
        Toast.makeText(getActivity(), data, Toast.LENGTH_LONG).show();
    }

    public void startGenerateName(View v) {
        if (nameGenInProgress) {
            return;
        }
        nameGenInProgress = true;
        spinnerGenName.setVisibility(View.VISIBLE);
        String newBandName = inputName.getText().toString();
        if (newBandName.isEmpty()) {
            debugHandleError("Empty name query");
            finishGenerateName(false, R.string.err_empty_query);
        } else if (curBandName.equals(newBandName) && lastSearchSucceeded) {
            finishGenerateName(true, 0);
        } else {
            curBandName = newBandName;
            startItunesSearch();
        }
    }

    private void startItunesSearch() {
        lastSearchSucceeded = false;
        String url = getString(R.string.url_itunes_search, Uri.encode(curBandName));
        JsonObjectRequest req = new JsonObjectRequest(url, this::finishItunesSearch, (error) -> {debugHandleError("Name search", error); finishGenerateName(false, R.string.err_network);});
        NetworkProvider.Instance.performRequest(req);
    }

    private void finishItunesSearch(JSONObject data) {
        try {
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
            if (curNames.size() == 0)
                throw new StandGenException();
            lastSearchSucceeded = true;
        } catch (StandGenException e) {
            debugHandleError("No songs found");
            finishGenerateName(false, R.string.err_empty_response);
            return;
        } catch (JSONException e) {
            debugHandleError("Name search", e);
            finishGenerateName(false, R.string.err_network);
            return;
        }
        finishGenerateName(true, 0);
    }

    private void finishGenerateName(Boolean success, int errId) {
        if (success) {
            curName = curNames.get(rnd.nextInt(curNames.size()));
            outputName.setText(curName);
        } else {
            handleError(errId);
        }
        nameGenInProgress = false;
        spinnerGenName.setVisibility(View.INVISIBLE);
    }

    public void startGenerateStand(View v) {
        if (standGenInProgress) {
            return;
        }
        standGenInProgress = true;
        spinnerGenStand.setVisibility(View.VISIBLE);
        StandData newStandData = new StandData();
        if (curName.isEmpty()) {
            debugHandleError("Empty name");
            finishGenerateStand(false, R.string.err_empty_name);
        } else {
            newStandData.standName = curName;
            generateStandStats(newStandData);
            startGenerateAbility(newStandData);
        }
    }

    private void startGenerateAbility(StandData newStandData) {
        String url = getString(R.string.url_random_power);
        JsonObjectRequest req = new JsonObjectRequest(url, (data) -> finishGenerateAbility(newStandData, data), (error) -> {debugHandleError("Ability search 1", error); finishGenerateStand(false, R.string.err_network);});
        NetworkProvider.Instance.performRequest(req);
    }

    private void finishGenerateAbility(StandData newStandData, JSONObject data) {
        int pageId;
        try {
            pageId = data.getJSONObject("query").getJSONArray("random").getJSONObject(0).getInt("id");
        } catch (JSONException e) {
            debugHandleError("Ability search 1", e);
            finishGenerateStand(false, R.string.err_network);
            return;
        }
        startQueryAbility(newStandData, pageId);
    }

    private void startQueryAbility(StandData newStandData, int pageId) {
        String url = getString(R.string.url_power_info, pageId, 256);
        JsonObjectRequest req = new JsonObjectRequest(url, (data) -> finishQueryAbility(newStandData, data, pageId), (error) -> {debugHandleError("Ability Search 2", error); finishGenerateStand(false, R.string.err_network);});
        NetworkProvider.Instance.performRequest(req);
    }

    private void finishQueryAbility(StandData newStandData, JSONObject data, int pageId) {
        try {
            JSONObject innerData = data.getJSONObject("items").getJSONObject(Integer.toString(pageId));

            // TODO: Exclude "List of "
            newStandData.abilityName = innerData.getString("title");
            newStandData.abilityUrl = getString(R.string.url_power_base) + innerData.getString("url");
            newStandData.abilityDescription = innerData.getString("abstract");
        } catch (JSONException e) {
            debugHandleError("Ability Search 2", e);
            finishGenerateStand(false, R.string.err_network);
            return;
        }

        standData = newStandData;
        finishGenerateStand(true, 0);

    }

    private void finishGenerateStand(Boolean success, int errId) {
        if (success) {
            outputStand.setText(standData.getRepresentation());
        } else {
            handleError(errId);
        }
        standGenInProgress = false;
        spinnerGenStand.setVisibility(View.INVISIBLE);
    }

    private void generateStandStats(StandData newStandData) {
        for (int i = 0; i < 6; i++) {
            newStandData.stats[i] = "EDCBA".charAt(rnd.nextInt(5));
        }
    }

    public void openAbilityPage(View v) {
        String url = standData.abilityUrl;
        if (url == null)
            return;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void copyStand(View v) {
        String text = outputStand.getText().toString();
        if (text.isEmpty())
            return;
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(text, text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), R.string.toast_copy_success, Toast.LENGTH_SHORT).show();
    }

    public void showAboutDialog(View v) {
        final SpannableString data = new SpannableString(getString(R.string.dlg_about));
        Linkify.addLinks(data, Linkify.WEB_URLS);
        AlertDialog dlgAlert  = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.dlg_about_name))
                .setMessage(data)
                .setPositiveButton(getString(android.R.string.ok), null)
                .setCancelable(false)
                .create();

        dlgAlert.show();
        ((TextView) dlgAlert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
