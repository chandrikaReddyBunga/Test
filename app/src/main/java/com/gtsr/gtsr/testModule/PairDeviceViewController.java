package com.gtsr.gtsr.testModule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gtsr.gtsr.R;
import com.gtsr.gtsr.RefreshShowingDialog;
import com.skyfishjy.library.RippleBackground;

import com.spectrochips.spectrumsdk.FRAMEWORK.SCConnectionHelper;
import com.spectrochips.spectrumsdk.FRAMEWORK.SCTestAnalysis;
import com.spectrochips.spectrumsdk.FRAMEWORK.SpectroCareSDK;

import java.util.ArrayList;
public class PairDeviceViewController extends AppCompatActivity {
    RelativeLayout relativeScanning, relativeConnect;
    RippleBackground rippleBackground;
    RefreshShowingDialog refreshShowingDialog;
    BluetoothDevice bluetoothDevice;
    private ArrayList<BluetoothDevice> devicesArray = new ArrayList<BluetoothDevice>();
    int selectedPosition = -1;
    devicesAdapter adapter;
    RecyclerView recyclerView;
    Button btnNext;
    boolean isConnected = false;
    BluetoothAdapter bluetoothAdapter;
    int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairdevice);
        refreshShowingDialog = new RefreshShowingDialog(PairDeviceViewController.this);

        SpectroCareSDK.getInstance().fillContext(getApplicationContext());
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            SCConnectionHelper.getInstance().initializeAdapterAndServcie();
            bluetoothAdapter = SCConnectionHelper.getInstance().mBluetoothAdapter;
        }
        SCTestAnalysis.getInstance().initializeService();
        loadRecyclerView();
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeNotifications();
        if(SCConnectionHelper.getInstance().mBluetoothAdapter == null) {
            finish();
        } else if(!SCConnectionHelper.getInstance().mBluetoothAdapter.isEnabled()) {
            // Request for BLE turn on
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        } else {
            startScan();
        }
        ///////////////////
        SCConnectionHelper.getInstance().disconnectWithPeripheral();
        if(bluetoothDevice != null) {
            bluetoothDevice = null;
        }
        devicesArray.clear();
        selectedPosition = -1;
        adapter.notifyDataSetChanged();
        btnNext.setVisibility(View.VISIBLE);
        SCConnectionHelper.getInstance().startScan(true);
        init();
    }
    protected void onPause() {
        super.onPause();
        SCConnectionHelper.getInstance().scanDeviceInterface = null;
        SCConnectionHelper.getInstance().startScan(false);

    }

    private void init() {
        // iv = (ImageView) findViewById(R.id.animation);
        relativeScanning = (RelativeLayout) findViewById(R.id.relativeScanning);
        relativeScanning.setVisibility(View.VISIBLE);
        relativeConnect = (RelativeLayout) findViewById(R.id.relativeConnect);
        rippleBackground = (RippleBackground) findViewById(R.id.content);
        rippleBackground.startRippleAnimation();
        btnNext = findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothDevice = null;
                isConnected = false;
                startActivity(new Intent(PairDeviceViewController.this, DownloadStripViewController.class));
            }
        });
        RelativeLayout back = findViewById(R.id.home);
        RelativeLayout refresh = findViewById(R.id.refresh);
        RelativeLayout back1 = findViewById(R.id.home1);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isConnected = false;
                SCConnectionHelper.getInstance().disconnectWithPeripheral();
                bluetoothDevice = null;
                devicesArray.clear();
                selectedPosition = -1;
                adapter.notifyDataSetChanged();
                btnNext.setVisibility(View.GONE);
                SCConnectionHelper.getInstance().startScan(true);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SCConnectionHelper.getInstance().startScan(false);
                finish();
            }
        });
        back1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SCConnectionHelper.getInstance().startScan(false);
                finish();
            }
        });
    }

    private void loadRecyclerView() {
        adapter = new devicesAdapter(this);
        recyclerView = (RecyclerView) findViewById(R.id.list);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(horizontalLayoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }


    private  void  activeNotifications(){
        SCConnectionHelper.getInstance().activateScanNotification(new SCConnectionHelper.ScanDeviceInterface() {
            @Override
            public void onSuccessForConnection(String msg) {
                Log.e("onSuccessForConnection", "call");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getApplicationContext(), DownloadStripViewController.class));
                            }
                        }, 2000 * 1);
                    }
                });
            }
            @Override
            public void onSuccessForScanning(final ArrayList<BluetoothDevice> devcies, boolean msg) {
                Log.e("onSuccessForScanning", "size" + devcies.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        devicesArray = devcies;
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailureForConnection(String error) {
                Log.e("onFailureForConnection", "call");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void uartServiceClose(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onBLEStatusChange(int state) {
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //Indicates the local Bluetooth adapter is off.
                        //   setEmptyText("<bluetooth is disabled>");
                        devicesArray.clear();
                        adapter.notifyDataSetChanged();
                        // menu.findItem(R.id.ble_scan).setEnabled(false);
                        Log.e("BLE_Status", "OFF");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Indicates the local Bluetooth adapter is turning on. However local clients should wait for STATE_ON before attempting to use the adapter.
                        break;

                    case BluetoothAdapter.STATE_ON:
                        //Indicates the local Bluetooth adapter is on, and ready for use.
                        Log.e("BLE Status", "ON");
                        // setEmptyText("<use SCAN to refresh devices>");
//                        if (menu != null)
//                            menu.findItem(R.id.ble_scan).setEnabled(true);
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.
                        break;
                }
            }
        });
    }
   /* public void activateNotification() {
        SCConnectionHelper.getInstance().activateScanNotification(new SCConnectionHelper.ScanDeviceInterface() {
            @Override
            public void onSuccessForConnection(String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("onSuccessForConnection", "call");
                        refreshShowingDialog.hideRefreshDialog();
                        isConnected=true;
                        btnNext.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onSuccessForScanning(final ArrayList<BluetoothDevice> devcies, boolean msg) {
                Log.e("onSuccessForScanning", "call" + devcies.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (devcies.size() > 0) {
                            devicesArray = devcies;
                            relativeScanning.setVisibility(View.GONE);
                            relativeConnect.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        } else {
                            btnNext.setVisibility(View.GONE);
                        }
                    }
                });


            }

            @Override
            public void onFailureForConnection(String error) {
                refreshShowingDialog.hideRefreshDialog();
                Log.e("onFailureForConnection", "call");
                isConnected = false;
                deviceStatus = "Connect";
                btnNext.setVisibility(View.GONE);
            }

            @Override
            public void uartServiceClose(String error) {
            }
        });
    }
*/

    @SuppressLint("StaticFieldLeak") // AsyncTask needs reference to this fragment
    private void startScan() {
        if(SCConnectionHelper.getInstance().getScanState() != SCConnectionHelper.ScanState.NONE)
            return;
        SCConnectionHelper.getInstance().setScanState(SCConnectionHelper.ScanState.LESCAN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationEnableStatus();
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                SCConnectionHelper.getInstance().setScanState(SCConnectionHelper.ScanState.NONE);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.location_permission_title);
                builder.setMessage(R.string.location_permission_message);
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0));
                builder.show();
                return;
            }
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            boolean         locationEnabled = false;
            try {
                locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch(Exception ignored) {}
            try {
                locationEnabled |= locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch(Exception ignored) {}
            if(!locationEnabled)
                SCConnectionHelper.getInstance().setScanState(SCConnectionHelper.ScanState.DISCOVERY);
        }
        devicesArray.clear();
        adapter.notifyDataSetChanged();
        if(SCConnectionHelper.getInstance().getScanState() == SCConnectionHelper.ScanState.LESCAN) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void[] params) {
                    SCConnectionHelper.getInstance().startScan(true);
                    return null;
                }
            }.execute();
        } else {
            bluetoothAdapter.startDiscovery();
        }
    }

    private  void  checkLocationEnableStatus(){
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage("Please enable location services to scan bluetooth devices")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel",null)
                    .show();
        }else{

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // ignore requestCode as there is only one in this fragment
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Handler(Looper.getMainLooper()).postDelayed(this::startScan,1); // run after onResume to avoid wrong empty-text
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.location_denied_title));
            builder.setMessage(getText(R.string.location_denied_message));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }
    public class devicesAdapter extends RecyclerView.Adapter<devicesAdapter.ViewHolder> {
        Context ctx;

        // ArrayList<BluetoothDevice> devicesArray;
        public devicesAdapter(Context ctx/*,ArrayList<BluetoothDevice> devcies*/) {
            this.ctx = ctx;
            //  this.devicesArray=devcies;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView name, id;
            ImageView image;
            Button btnConnect;

            public ViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.testName);
                image = (ImageView) itemView.findViewById(R.id.image);
                id = itemView.findViewById(R.id.txt_id);
                btnConnect = itemView.findViewById(R.id.btn_connect);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {

            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.scanlist, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            BluetoothDevice device = devicesArray.get(position);
            holder.name.setText(device.getName());
            holder.id.setText(device.getAddress());
            holder.btnConnect.setBackgroundResource(R.drawable.btn_gray);
            holder.btnConnect.setText("Connect");
            if (isConnected) {
                Log.e("aaaaaaaaaa", "call" + bluetoothDevice.getAddress());
                selectedPosition=-1;
                if (devicesArray.get(position).getAddress().equals(bluetoothDevice.getAddress())) {
                    holder.btnConnect.setBackgroundResource(R.drawable.btn_gradient);
                    holder.btnConnect.setText("Connected");
                    btnNext.setVisibility(View.VISIBLE);
                }
            } else {
                holder.btnConnect.setBackgroundResource(R.drawable.btn_gray);
                holder.btnConnect.setText("Connect");
                btnNext.setVisibility(View.GONE);
                SCConnectionHelper.getInstance().disconnectWithPeripheral();
            }
            if (selectedPosition == position) {
                selectedPosition = position;
                refreshShowingDialog.showAlert();
                Log.e("selectedpos", "call" + holder.getAdapterPosition());
                bluetoothDevice = devicesArray.get(position);
                SCTestAnalysis.getInstance().mService.connect(bluetoothDevice);
                SCConnectionHelper.getInstance().startScan(false);
            }
            holder.btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isConnected = false;
                    if (selectedPosition != position) {
                        selectedPosition = position;
                        notifyDataSetChanged();
                    } else {
                        selectedPosition = -1;
                        notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            if (devicesArray.size() > 0) {
                return devicesArray.size();
            } else {
                return 0;
            }
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        // if(SCConnectionHelper.getInstance().scanner!=null)
        SCConnectionHelper.getInstance().stopScan();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SCConnectionHelper.getInstance().stopScan();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SCConnectionHelper.getInstance().startScan(false);
        finish();
    }
}