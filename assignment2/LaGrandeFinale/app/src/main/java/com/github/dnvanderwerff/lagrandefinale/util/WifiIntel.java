package com.github.dnvanderwerff.lagrandefinale.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.dnvanderwerff.lagrandefinale.MapActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jeroen on 20/06/2016.
 */
public class WifiIntel {
    private static final String host = "52.58.85.127";
    private static final int port = 10000;
    private static final SocketAddress addr = new InetSocketAddress(host, port);

    private class Measurement {
        public String bssid;
        public int strength;

        public Measurement(String bssid, int strength) {
            this.bssid = bssid;
            this.strength = strength;
        }
    }

    private class MessageParam {
        public Handler handler;
        public int handler_msg_id;
        public String message;

        MessageParam(String message) {
            this.message = message;
        }

        MessageParam(String message, Handler handler, int handler_msg_id) {
            this.message = message;
            this.handler = handler;
            this.handler_msg_id = handler_msg_id;
        }
    }

    private class SendMessageTask extends AsyncTask<MessageParam, Void, String> {
        private String result;
        private Handler handler;
        private int handler_msg_id;

        @Override
        protected String doInBackground(MessageParam... msgs) {
            MessageParam p = msgs[0];
            String msg = p.message;
            handler = p.handler;
            handler_msg_id = p.handler_msg_id;

            result = sendMessage(msg);
            return result;
        }

        @Override
        protected void onPostExecute(String r) {
            if (handler != null) {
                handler.obtainMessage(handler_msg_id, result).sendToTarget();
            }
        }
    }

    private MapActivity parent;

    /* Socket */
    private Socket sock;
    private InputStream in;
    private OutputStream out;

    /* Wifi */
    private WifiManager mWifiManager;
    private List<Measurement> measurements; // Holds latest measurement results
    private Timer t;

    /* Prediction handling */
    private boolean awaitingPrediction = false;
    private Handler handler;
    private int handler_id;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();

                measurements.clear();
                List<String> handledIds = new ArrayList<>();
                for (ScanResult sr : mScanResults) {
                    // Don't add duplicate MAC addresses
                    if (handledIds.contains(sr.BSSID)) continue;

                    measurements.add(new Measurement(sr.BSSID, sr.level));

                    handledIds.add(sr.BSSID);
                }

                // If awaiting prediction go
                if (awaitingPrediction) {
                    int[] cells = {};
                    String msg = createMessage("predict", cells, measurements);
                    new SendMessageTask().execute(new MessageParam(msg, handler, handler_id));
                    awaitingPrediction = false;
                    return;
                }

                // Only store if activity says so
                if (parent.storeWifi && parent.storeCells.length > 0) {
                    int[] cells = parent.storeCells;
                    String msg = createMessage("store", cells, measurements);
                    Log.d("TheMessage", msg);
                    new SendMessageTask().execute(new MessageParam(msg));
                }
            }
        }
    };

    public WifiIntel (MapActivity parent, Handler predictionHandler, int prediction_handler_id) {
        this.parent = parent;
        this.measurements = new ArrayList<>();
        sock = new Socket();
        this.handler = predictionHandler;
        this.handler_id = prediction_handler_id;
    }

    public void start() {
    }

    public void requestPrediction() {
        int[] cells = {};

        if (measurements.size() > 0) {
            String msg = createMessage("predict", cells, measurements);
            new SendMessageTask().execute(new MessageParam(msg, handler, handler_id));
        } else {
            awaitingPrediction = true;
        }
    }

    private void connect() {
        try {
            sock.connect(addr);
            in = sock.getInputStream();
            out = sock.getOutputStream();
        } catch (IOException e) {
            Log.d("butWhy", e.getMessage());
            e.printStackTrace();

            parent.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(parent, "Could not connect to cloud service", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private String createMessage(String type, int[] cells, List<Measurement> measurements) {
        JSONObject obj = new JSONObject();

        try {
            obj.put("type", type);
            JSONArray c = new JSONArray();
            for (int cell : cells ) {
                c.put(cell);
            }
            obj.put("cell", c);

            JSONArray m = new JSONArray();
            for (Measurement meas : measurements) {
                m.put(new JSONObject()
                    .put("bssid", meas.bssid)
                    .put("strength", meas.strength));
            }
            obj.put("data", m);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";
    }

    /* Use protocol to send and receive a message */
    private String sendMessage(String message) {
        // Ensure tcp connection is live?
        if (!sock.isConnected())
            connect();
        if (!sock.isConnected())
            return "";

        byte[] msg = message.getBytes();

        // First send the length of the message
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(msg.length);

        try {
            // First send the length of the message
            System.out.println("Sending msg: " + bb.array().toString());
            out.write(bb.array());

            // Send the message
            System.out.println("Sending msg: " + msg);
            out.write(msg);


            // Receive the return message
            byte[] return_length = new byte[4];
            in.read(return_length, 0, 4);
            int msg_size = ByteBuffer.wrap(return_length).getInt();
            System.out.println("Received msg size: " + msg_size);

            // Read returned message
            byte[] input = new byte[msg_size];
            String message_return = "";
            int bytes_read;
            boolean end = false;
            while (!end) {
                bytes_read = in.read(input);
                message_return += new String(input, 0, bytes_read);
                if (message_return.length() == msg_size) {
                    end = true;
                }
            }
            return message_return;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void startWifi() {
        mWifiManager = (WifiManager) parent.getSystemService(Context.WIFI_SERVICE);

        // Frequently scan Wifi networks
        if (t != null)
            t.cancel();

        t = new Timer();

        class ScanTask extends TimerTask {
            @Override
            public void run() {
                mWifiManager.startScan();
            }
        }

        // Scan every second
        t.schedule(new ScanTask(), 1000, 3000);

        if (mWifiManager != null) {
            parent.registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
    }

    /* Register listeners */
    public void onResume() {
        parent.registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        startWifi();
    }

    /* Unregister listeners */
    public void onPause() {
        parent.unregisterReceiver(mWifiScanReceiver);
        if (t != null)
            t.cancel();
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
