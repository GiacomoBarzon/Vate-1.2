package com.application.handing.vateapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BeacList extends AppCompatActivity {
    //GESTIONE LAYOUT
    private ListView beaconList;
    ArrayAdapter<String> listAdapter;
    private ProgressBar progressBar;
    private ObjectAnimator progressAnimator;

    //GESTIONE BLUETOOTH
    final private static int BT_REQUEST_ID = 1;
    private static final int REQUEST_CODE_LOCATION = 2;
    final  public static BeaconsAdapter mAdapter = new BeaconsAdapter();//dichiarazione oggetto della classe BeaconsAdapter
    final private Handler mHandler = new Handler();//handler per gestire messaggi e runnable
    private BluetoothAdapter mBtAdapter = null;//BtAdapter to communicate with bluetooth
    //Callback
    private BluetoothAdapter.LeScanCallback mLeOldCallback = null;
    private ScanCallback mLeNewCallback = null;

    final private static long VALIDATION_PERIOD = 3000;
    //END GESTIONE BLUETOOTH

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beac_list);
        beaconList = (ListView) findViewById(R.id.listaBeacon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initializeCallback();

        //GESTIONE CIRCOLETTO per visualizzare il caricamento
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressAnimator = ObjectAnimator.ofInt (progressBar, "progress", 0, 500);
        progressAnimator.setDuration (5000);
        progressAnimator.setInterpolator (new DecelerateInterpolator());
        circleOn();
        //END GESTIONE CIRCOLETTO
    }

    @Override
    protected void onResume() {
        super.onResume();

        startScanning();
        startValidating();

        circleOn();
    }

    @Override
    protected void onPause() {
        stopScanning();
        stopValidating();

        circleOff();
        super.onPause();
    }

    //GESTIONE LISTVIEW
    private void gestioneLista(){
        ArrayList<String> graficaList = new ArrayList<String>();
        //GESTIONE LISTVIEW
        graficaList.clear();
        for(int i=0;i<mAdapter.mBeacons.size(); ++i){
            int majo=mAdapter.mBeacons.get(i).major;
            int mino=mAdapter.mBeacons.get(i).minor;

            graficaList.add(" Beacon: major " + majo
                    + ", minor " + mino + ", rssi " + Math.round(mAdapter.mBeacons.get(i).rssi));
        }
        circleOff();
        listAdapter = new ArrayAdapter<String>(BeacList.this, R.layout.list_row, graficaList);//crea adapter
        beaconList.setAdapter(listAdapter);
    }

    //GESTIONE VISUALIZZAZIONE/SPEGNIMENTO PROGRESS CIRCLE
    private void circleOn(){
        if(progressBar.getVisibility() != View.VISIBLE)
            progressBar.setVisibility(View.VISIBLE);
        if(!progressAnimator.isRunning())
            progressAnimator.start();
    }
    private void circleOff(){
        progressAnimator.cancel();
        if(progressBar.getVisibility() == View.VISIBLE)
            progressBar.setVisibility(View.GONE);
    }
    //END GESTIONE PROGRESS CIRCLE

    //BLUETOOTH CONTROL
    //control if Bt is enabled, create an instance of a BluetoothAdapter, into startScanning
    private boolean isBluetoothAvailableAndEnabled() {
        //obtaining an instance of a BluetoothAdapter
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBtAdapter = btManager.getAdapter();

        return mBtAdapter != null && mBtAdapter.isEnabled();
    }
    //new activity to enable Bluetooth hardware, into startScanning
    private void requestForBluetooth() {
        Intent request = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(request, BT_REQUEST_ID);//ritorna onActivityResult quando viene creata l'activity

        /*if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request missing location permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION);
        }*/
    }
    public void getPermissionToLoc() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION);
        }
    }
    //bisogna aggiungere richiesta per accesso a posizione fatta bene
    //tipo richiesta solo per API maggiori di un tot
    //private void requestForPos(){}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BT_REQUEST_ID) {
            if (isBluetoothAvailableAndEnabled()) {
                startScanning();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    //END BLUETOOTH CONTROL

    //CALLBACK CREATION
    //crea gli oggetti di callback e relative funzioni, utilizzate durante startScanning
    private void initializeCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mLeOldCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    handleNewBeaconDiscovered(device, rssi, scanRecord);
                }

            };
        } else {
            mLeNewCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (result.getScanRecord() == null ||
                                result.getScanRecord().getBytes() == null) {
                            return;
                        }
                        handleNewBeaconDiscovered(
                                result.getDevice(),
                                result.getRssi(),
                                result.getScanRecord().getBytes());
                    }
                }

                @Override
                //return information about more than one device
                public void onBatchScanResults(List<ScanResult> results) {
                    for (final ScanResult result : results) {
                        onScanResult(0, result);
                    }
                }
            };
        }
    }
    //END CALLBACK CREATION

    //INIZIO SCANSIONE
    private void startScanning() {//scan result riportati come callback
        if (!isBluetoothAvailableAndEnabled()) {
            requestForBluetooth();
            //return;
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBtAdapter.startLeScan(mLeOldCallback);//*esite funzione che filtra gli uuid
            } else {
                BluetoothLeScanner scanner = mBtAdapter.getBluetoothLeScanner();//return a BtLeScanner instance
                if (scanner != null) {
                    //defining settings for the scan
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//SCAN_MODE_LOW_LATENCY
                            // LOW_LATENCY, LOW_ENERGY,BALANCE ...vari tipi di scan possibili
                            //.setReportDelay()//milliseconds
                            .build();
                    scanner.startScan(null, settings, mLeNewCallback);//List<ScanFilter> filters, ScanSettings settings, ScanCallback callback
                }
            }
        }
    }
    //STOP SCANSIONE
    private void stopScanning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBtAdapter.stopLeScan(mLeOldCallback);
        } else {
            BluetoothLeScanner scanner = mBtAdapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(mLeNewCallback);
            }
        }
    }

    //HANDLE NEW BEACON DISCOVERED
    // Funzione di risposta alla ricezione di ogni Handle da bluethoot LB
    //se nuovo Beacon, aggiungi alla lista
    //se vecchio Beacon, aggiorna i valori e medie
    private void handleNewBeaconDiscovered(final BluetoothDevice device,
                                           final int rssi,
                                           final byte[] advertisement) {
        final BeaconModel beaconToAdd;
        BeaconModel beacon = mAdapter.findBeaconWithId(device.getAddress());

        if (beacon == null) {//se nuovo beacon, aggiungi alla lista
            // new item
            beacon = new BeaconModel();
            beacon.dataEntry(device, rssi, advertisement);
            //controllo se effettivamente non era già presente
            //perchè a volte sbaglia
            if(!beacon.isEstimoteBeacon() /*|| mAdapter.justFinded(beacon.minor)*/) return;
            beaconToAdd = beacon;
        }
        else {//se vecchio Beacon, aggiorna i valori
            beaconToAdd = null;

            if(rssi < 0 && rssi >-150){  // rssi deve essere minore di zero e non troppo piccolo (>=0 non ok)
                beacon.istantRssi = rssi;
                beacon.rssi_list.add(rssi);//aggiungi valore rssi alla lista
                beacon.durate = new Date().getTime() - beacon.timestamp;
                beacon.timestamp = new Date().getTime();
                mAdapter.updating(beacon); // Aggiorno rssi, medie etc
            }
            gestioneLista();
        }
        //controlla se è un nostro beacon
        //if(!beacon.isDemoBeacon()) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (beaconToAdd != null) {
                    mAdapter.addNewBeacon(beaconToAdd);
                } else {
                    // just notify about changes in underlying data
                }
            }
        });
    }
    //END UPDATING BEACON MEASURES

    //BEACON VALIDATION
    //se dopo un tot non sento un dispositivo, toglilo dalla lista
    private void startValidating() {
        mHandler.postDelayed(periodicValidationTask, VALIDATION_PERIOD);
    }

    private void stopValidating() {
        mHandler.removeCallbacks(periodicValidationTask);
    }

    private final Runnable periodicValidationTask = new Runnable() {
        @Override
        public void run() {
            /*if (mAdapter.validateAllBeacons()) {
                mAdapter.notifyDataSetChanged();
            }*/
            mAdapter.validateAllBeacons();

            //ricomincia il giro
            startValidating();
        }
    };
    //END BEACON VALIDATION

}
