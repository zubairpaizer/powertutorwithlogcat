/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package com.example.powertutor.ui;

import com.example.powertutor.R;
import com.example.powertutor.phone.PhoneSelector;
import com.example.powertutor.service.ICounterService;
import com.example.powertutor.service.PowerEstimator;
import com.example.powertutor.service.UMLoggerService;
import com.example.powertutor.util.Counter;
import com.example.powertutor.util.BatteryStats;
import com.example.powertutor.util.SystemInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.io.File;

public class Logcat extends Activity {
    private static final String TAG = "Logcat";

    private SharedPreferences prefs;
    private int uid;

    private Runnable collector;

    private Intent serviceIntent;
    private CounterServiceConnection conn;
    private ICounterService counterService;
    private Handler handler;

    private BatteryStats batteryStats;

    private String[] componentNames;

    public void refreshView() {
        final ListView listView = new ListView(this);

        ArrayAdapter adapter = new ArrayAdapter(this, 0) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView = getLayoutInflater()
                        .inflate(R.layout.activity_logcat, listView, false);
                TextView title = (TextView) itemView.findViewById(R.id.title);
                TextView summary = (TextView) itemView.findViewById(R.id.summary);
                LinearLayout widgetGroup =
                        (LinearLayout) itemView.findViewById(R.id.widget_frame);
                InfoItem item = (InfoItem) getItem(position);
                item.initViews(title, summary, widgetGroup);
                item.setupView();
                return itemView;
            }
        };

        final ArrayList<InfoItem> allItems = new ArrayList<InfoItem>();
        //allItems.add(new UidItem());
        allItems.add(new PackageItem());
        allItems.add(new LogcatView());
        //allItems.add(new OLEDItem());
        //allItems.add(new InstantPowerItem());
        //allItems.add(new AveragePowerItem());
        //allItems.add(new CurrentItem());
        //allItems.add(new ChargeItem());
        //allItems.add(new VoltageItem());
        //allItems.add(new TempItem());

        for (InfoItem inf : allItems) {
            if (inf.available()) {
                adapter.add(inf);
            }
        }

        listView.setAdapter(adapter);
        setContentView(listView);

        collector = new Runnable() {
            public void run() {
                for (InfoItem inf : allItems) {
                    if (inf.available()) {
                        inf.setupView();
                    }
                }
                if (handler != null) {
                    handler.postDelayed(this, 2 * PowerEstimator.ITERATION_INTERVAL);
                }
            }
        };
        if (handler != null) {
            handler.post(collector);
        }
    }

    class CounterServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,
                                       IBinder boundService) {
            counterService = ICounterService.Stub.asInterface((IBinder) boundService);
            try {
                componentNames = counterService.getComponents();
            } catch (RemoteException e) {
                componentNames = new String[0];
            }
            refreshView();
        }

        public void onServiceDisconnected(ComponentName className) {
            counterService = null;
            getApplicationContext().unbindService(conn);
            getApplicationContext().bindService(serviceIntent, conn, 0);
            Log.w(TAG, "Unexpectedly lost connection to service");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        uid = getIntent().getIntExtra("uid", SystemInfo.AID_ALL);
        if (savedInstanceState != null) {
            componentNames = savedInstanceState.getStringArray("componentNames");
        }
        batteryStats = BatteryStats.getInstance();
        serviceIntent = new Intent(this, UMLoggerService.class);
        conn = new CounterServiceConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new Handler();
        getApplicationContext().bindService(serviceIntent, conn, 0);
        refreshView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getApplicationContext().unbindService(conn);
        if (collector != null) {
            handler.removeCallbacks(collector);
            collector = null;
            handler = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("componentNames", componentNames);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return null;
    }

    private abstract class InfoItem {
        protected TextView title;
        protected TextView summary;
        protected TextView txt;

        public void initViews(TextView title, TextView summary,
                              LinearLayout widgetGroup) {
            this.title = title;
            this.summary = summary;
            txt = new TextView(Logcat.this);
            widgetGroup.addView(txt);
        }

        public abstract boolean available();

        public abstract void setupView();
    }

    private class PackageItem extends InfoItem {
        public boolean available() {
            return uid >= SystemInfo.AID_APP;
        }

        public void setupView() {
            if (txt == null) return;
            txt.setText("");

            title.setText("Packages");

            PackageManager pm = getApplicationContext().getPackageManager();
            String[] packages = pm.getPackagesForUid(uid);
            if (packages != null) {
                StringBuilder buf = new StringBuilder();
                for (String packageName : packages) {
                    if (buf.length() != 0) buf.append("\n");
                    buf.append(packageName);
                }
                summary.setText(buf.toString());
            } else {
                summary.setText("(None)");
            }
        }
    }

    private class LogcatView extends InfoItem {
        public boolean available() {
            return uid >= SystemInfo.AID_APP;
        }

        public void setupView() {
            if (txt == null) return;
            txt.setText("");

            title.setText("Logcat");

            PackageManager pm = getApplicationContext().getPackageManager();
            String[] packages = pm.getPackagesForUid(uid);
            if (packages != null) {
                StringBuilder buf = new StringBuilder();
                for (String packageName : packages) {
                    if (buf.length() != 0) buf.append("\n");
                    buf.append(packageName);
                    try {
                        Process process = Runtime.getRuntime().exec("logcat -d | grep " + packageName);
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));
                        StringBuilder log = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            log.append(line);
                        }
                        try {
                            File myDir = new File(getFilesDir().getAbsolutePath());
                            FileWriter fi = new FileWriter(myDir + "/" + packageName.replace(".","_"));
                            FileWriter fw = new FileWriter("/sdcard/download/" + packageName.replace(".","_"));
                            fi.close();
                            fw.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        summary.setText(log.toString());
                    } catch (IOException e) {
                    }
                }
                }else{
                    summary.setText("(None)");
                }
        }
    }
}
