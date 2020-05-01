package com.abel.standgen;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    public void saveLogcatToFile() {
        String fileName = "standGenLog.txt";
        File outputFile = new File(getExternalCacheDir(), fileName);
        try {
            Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath() + " &");
        } catch (IOException e) {
            Toast.makeText(this, "I can't even save the f***ing logs!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG) {
            saveLogcatToFile();
        }
    }
}
