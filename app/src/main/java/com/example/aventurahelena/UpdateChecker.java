package com.example.aventurahelena;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends AsyncTask<Void, Void, JSONObject> {

    private static final String TAG = "UpdateChecker";
    private static final String VERSION_URL =
        "https://raw.githubusercontent.com/organizakids28-maker/aventura-helena/main/version.json";

    private final Activity activity;

    public UpdateChecker(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
        try {
            URL url = new URL(VERSION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status != 200) return null;

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();

            return new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao verificar atualização: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(JSONObject json) {
        if (json == null || activity.isFinishing()) return;

        try {
            int latestCode    = json.getInt("versionCode");
            String latestName = json.getString("versionName");
            final String apkUrl = json.getString("apkUrl");

            int currentCode = activity.getPackageManager()
                .getPackageInfo(activity.getPackageName(), 0).versionCode;

            if (latestCode <= currentCode) return;

            new AlertDialog.Builder(activity)
                .setTitle("Atualização disponível!")
                .setMessage("Nova versão " + latestName + " disponível.\nDeseja atualizar agora?")
                .setCancelable(true)
                .setPositiveButton("Atualizar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("Agora não", null)
                .show();

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Erro ao ler versão atual: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar atualização: " + e.getMessage());
        }
    }
}
