package com.devunitec.plantbio;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final UUID MODULE_BT_UUID = UUID.fromString("2e051fd6-c749-11e7-abc4-cec278b6b50a");
    private static final int MESSAGE_READ = 0;
    private final int REQUEST_ENABLE_BT = 1;

    private ConnectedThread mConnectedThread;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket = null;

    private Boolean findDevice = false;

    private Handler bluetoothIn;

    private ImageView imageView;
    private TextView textView;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals("00:21:13:00:A3:FB")) {
                    findDevice = true;
                    mBluetoothDevice = device;
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (findDevice) {
                    if (mBluetoothDevice != null) {
                        try {
                            mBluetoothSocket = createBluetoothSocket(mBluetoothDevice);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            mBluetoothSocket.connect();
                        } catch (IOException e) {
                            try {
                                mBluetoothSocket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            e.printStackTrace();
                        }
                        mConnectedThread = new ConnectedThread(mBluetoothSocket);
                        mConnectedThread.start();
                    }
                } else {
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkStateBt(mBluetoothAdapter);

        bluetoothIn = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    String inputMessage = (String) msg.obj;
                    Log.e(LOG_TAG, inputMessage);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck();
            }
            if (mBluetoothAdapter.startDiscovery()) {
            } else {

            }
        }

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);
    }


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer); //read bytes from input buffer
                    //String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    String readMessage = new String(ByteStreams.toByteArray(mmInStream));
                    Log.e(LOG_TAG, readMessage);
                    bluetoothIn.obtainMessage(MESSAGE_READ, bytes, -1, readMessage).sendToTarget();
                    Log.e(LOG_TAG + " 2", readMessage);
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException {
        return bluetoothDevice.createRfcommSocketToServiceRecord(bluetoothDevice.getUuids()[0].getUuid());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void permissionCheck() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
    }

    private void checkStateBt(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntentBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntentBt, REQUEST_ENABLE_BT);
            }
        } else {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
        }
    }
}
