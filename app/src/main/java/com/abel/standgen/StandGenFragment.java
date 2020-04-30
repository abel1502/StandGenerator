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

//import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        view.findViewById(R.id.btn_gen_name).setOnClickListener(this::generateName);
        view.findViewById(R.id.btn_gen_stand).setOnClickListener(this::generateStand);
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
        if (BuildConfig.DEBUG) {
            Toast.makeText(getActivity(), data, Toast.LENGTH_LONG).show();
        }
    }

    public void generateName(View v) {
        if (nameGenInProgress) {
            return;
        }
        nameGenInProgress = true;
        spinnerGenName.setVisibility(View.VISIBLE);
        String newBandName = inputName.getText().toString();
        if (newBandName.isEmpty()) {
            debugHandleError("Empty name query");
            finishGenerateName(false, R.string.err_empty_query);
            return;
        }
        if (curBandName.equals(newBandName) && lastSearchSucceeded) {
            finishGenerateName(true, 0);
        } else {
            lastSearchSucceeded = false;
            curBandName = newBandName;
            Call<List<ItunesResult>> call = NetworkManager.getItunesService().getSearch(curBandName);
            call.enqueue(new Callback<List<ItunesResult>>() {
                @Override
                public void onResponse(@NonNull Call<List<ItunesResult>> call, @NonNull Response<List<ItunesResult>> response) {
                    curNames.clear();
                    List<ItunesResult> results = response.body();
                    if (results == null) {
                        debugHandleError("Name search - empty body");
                        finishGenerateName(false, R.string.err_network);
                        return;
                    }
                    for (ItunesResult item : results) {
                        String _curName = item.getTrackCensoredName();
                        int tmp = _curName.indexOf("(");
                        if (tmp != -1) {
                            _curName = _curName.substring(0, tmp);
                        }
                        if (_curName.equals("Intro") || _curName.isEmpty()) {
                            continue;
                        }
                        curNames.add(_curName);
                    }
                    if (curNames.isEmpty()) {
                        debugHandleError("No songs found");
                        finishGenerateName(false, R.string.err_empty_response);
                        return;
                    }
                    lastSearchSucceeded = true;
                    finishGenerateName(true, 0);
                }

                @Override
                public void onFailure(@NonNull Call<List<ItunesResult>> call, @NonNull Throwable t) {
                    debugHandleError("Name search", t);
                    finishGenerateName(false, R.string.err_network);
                }
            });
        }
    }

    public void finishGenerateName(Boolean success, int errId) {
        if (success) {
            curName = curNames.get(rnd.nextInt(curNames.size()));
            outputName.setText(curName);
        } else {
            handleError(errId);
        }
        nameGenInProgress = false;
        spinnerGenName.setVisibility(View.INVISIBLE);
    }

    public void generateStand(View v) {
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

            Call<PowerRandomResult> call = NetworkManager.getPowerService().getRandom();
            call.enqueue(new Callback<PowerRandomResult>() {
                @Override
                public void onResponse(@NonNull Call<PowerRandomResult> call, @NonNull Response<PowerRandomResult> response) {
                    PowerRandomResult result = response.body();
                    if (result == null) {
                        debugHandleError("Ability search 1 - empty body");
                        finishGenerateStand(false, R.string.err_network);
                        return;
                    }
                    continueGenerateStand(newStandData, result.getId());
                }

                @Override
                public void onFailure(@NonNull Call<PowerRandomResult> call, @NonNull Throwable t) {
                    debugHandleError("Ability search 1", t);
                    finishGenerateStand(false, R.string.err_network);
                }
            });
        }
    }

    private void continueGenerateStand(StandData newStandData, int pageId) {
        Call<PowerInfoResult> call = NetworkManager.getPowerService().getInfo(pageId, 300);
        call.enqueue(new Callback<PowerInfoResult>() {
            @Override
            public void onResponse(@NonNull Call<PowerInfoResult> call, @NonNull Response<PowerInfoResult> response) {
                PowerInfoResult result = response.body();
                if (result == null) {
                    debugHandleError("Ability search 2 - empty body");
                    finishGenerateStand(false, R.string.err_network);
                    return;
                }
                newStandData.abilityName = result.getTitle();
                if (newStandData.abilityName.contains("List of")) {
                    debugHandleError("List instead of ability");
                    finishGenerateStand(false, R.string.err_bad_ability);
                    return;
                }
                newStandData.abilityUrl = NetworkManager.powerBaseUrl + result.getUrl();
                newStandData.abilityDescription = result.getDescription();

                standData = newStandData;
                finishGenerateStand(true, 0);
            }

            @Override
            public void onFailure(@NonNull Call<PowerInfoResult> call, @NonNull Throwable t) {
                debugHandleError("Ability search 2", t);
                finishGenerateStand(false, R.string.err_network);
            }
        });
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
