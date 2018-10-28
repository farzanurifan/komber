package com.example.farzanurifan.detectwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    WifiManager wifi;
    ListView list;
    Button scanUlang;
    boolean flag = true;
    int index = 0;
    int counter = 0;
    double meter;

    List<ScanResult> results;
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<>();
    HashMap<String, ArrayList<String>> arrayid = new HashMap<>();
    SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.list);
        scanUlang = findViewById(R.id.scanUlang);

        wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        assert wifi != null;
        if(!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
        }
        scanwifi();

        String[] from = {"name", "megahertz", "distance"};
        int[] to = {R.id.list_value, R.id.megahertz,R.id.meter};

        this.adapter = new SimpleAdapter(MainActivity.this, arraylist, R.layout.row, from, to);
        list.setAdapter(adapter);

        scanUlang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                arrayid.clear();
                scanwifi();
            }
        });

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                counter += 1;
                results = wifi.getScanResults();
                index = results.size();

                getWifi();

                if (counter == 5) {
                    printWifi();
                    flag = false;
                    counter = 0;
                }
                else if(flag) {
                    scanwifi();
                }
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void scanwifi() {
        toastMessage("Scanning, mohon tunggu....");
        wifi.startScan();
    }

    private void printWifi() {
        arraylist.clear();
        for (Map.Entry<String, ArrayList<String>> entry : arrayid.entrySet()) {
            HashMap<String, String> item = new HashMap<>();
            ArrayList<String> isi = entry.getValue();

            String[] res = (rerata(isi, isi.size())).split(";");
            item.put("name", res[0]);
            item.put("megahertz", res[1] + " MHz | " +  res[2] + " dBm ");
            item.put("distance", df2.format(Double.valueOf(res[3])) + " meter");

            arraylist.add(item);
        }

        class MapComparator implements Comparator<HashMap<String, String>>{
            private final String key;

            private MapComparator(String key){
                this.key = key;
            }

            public int compare(HashMap<String, String> first,
                               HashMap<String, String> second){
                String firstValue = first.get(key);
                String secondValue = second.get(key);
                return firstValue.compareTo(secondValue);
            }
        }

        Collections.sort(arraylist, new MapComparator("distance"));
        adapter.notifyDataSetChanged();
    }

    private void getWifi() {
        try {
            for (int i = 0; i < index; i++) {
                meter = calculateDistance(results.get(i).level, results.get(i).frequency);
                String dbm = String.valueOf(results.get(i).level);
                String megahertz = String.valueOf(results.get(i).frequency);

                ArrayList<String> value = new ArrayList<>();
                if (arrayid.get(results.get(i).BSSID) != null) {
                    value = arrayid.get(results.get(i).BSSID);
                }
                value.add(results.get(i).SSID + ";" + megahertz + ";" + dbm + ";" + meter);

                arrayid.put(results.get(i).BSSID, value);
            }
        }
        catch (Exception e) {
            toastMessage(e.getMessage());
        }
    }

    private String rerata(ArrayList<String> data, int size) {
        String[] firstData = data.get(0).split(";");
        double meter = 0;

        for(int i = 0; i < size; i++) {
            meter += Double.valueOf(data.get(i).split(";")[3]);
        }
        String finalMeter = String.valueOf(meter/size);

        return firstData[0] + ";" + firstData[1] + ";" + firstData[2] + ";" + finalMeter;
    }

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    public double calculateDistance(double levelInDb, double freqInMHz) {
        // FSPL = Free-Space Path Loss adapted average constant for home Wifi routers and following units
        double FSPL = 27.55;
        double exp = (FSPL - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }
}