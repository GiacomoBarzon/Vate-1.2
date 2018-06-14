package com.application.handing.vateapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.estimote.coresdk.common.requirements.SystemRequirementsChecker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //Beacon da usare per App Vate
    //array di array -> il primo elemento è il major, gli altri tutti i minor associati a quel major
    int [][] beaconVate = { {1, 1,2,3}, {100, 1}, {200, 1,2,3,4,5}, //Bevilacqua
            {10000, 1,2,4,5,8,10,11,13,14,15,18,19,20,21,24,28} }; //Negozi piazza
            //{5000, 1,2,3,4}}; //Ufficio
    int [] majorInterni = {1, 100, 200};
    int [] majorEsterni = {10000};

    //Funzione che controlla se il beacon è uno di quelli usati per Vate
    private boolean isBeaconVate(int tempMajor, int tempMinor){
        for(int majors = 0; majors<beaconVate.length; majors++){
            if(tempMajor==beaconVate[majors][0]){//tutti i major sono il primo elemento dei vari array
                //minors deve partire da 1 perchè 0 è il major
                for(int minors=1; minors<beaconVate[majors].length; minors++){
                    if(tempMinor==beaconVate[majors][minors])
                        return true;
                }
                return false;
            }
        }
        return false;
    }

    //TextView beacVicino;

    //array di grandezza MEMORIA_FIFO con ultimi majo/mino letti
    //beacon deve essere presente almeno VALORE_MINIMO volte per essere effettivamente il più vicino
    private final static int MEMORIA_FIFO = 4;
    private final static int VALORE_MINIMO = 3;

    private final static String INDIRIZZO_WEB = "https://vateapp.eu/";

    int lastMaio, lastMino;//ultimi valori letti majo/mino del beacon più vicino
    ArrayList prevMaio, prevMino;//array con ultimi major e minor per scegliere NearestBeacon
    Boolean isStarted;//vero se pulsante play è stato schiacciato, deve ritornare falso quando esco dall'app (oppure no, da vedere)
    String lastUrl;
    Boolean firstStart = true;//bool utilizzato per mostrare la richiesta di accensione bluetooth

    //GESTIONE BLUETOOTH
    final private static int BT_REQUEST_ID = 1;
    final  public static BeaconsAdapter mAdapter = new BeaconsAdapter();//dichiarazione oggetto della classe BeaconsAdapter
    final private Handler mHandler = new Handler();//handler per gestire messaggi e runnable
    private BluetoothAdapter mBtAdapter = null;//BtAdapter to communicate with bluetooth
    //Callback
    private BluetoothAdapter.LeScanCallback mLeOldCallback = null;
    private ScanCallback mLeNewCallback = null;

    final private static long VALIDATION_PERIOD = 3000;
    //END GESTIONE BLUETOOTH

    ImageView immagineSfondo;
    FloatingActionButton fabInfo, fabStart;//pulsanti informazioni/play in basso
    ProgressBar webProgress;
    WebView webVista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //beacVicino = (TextView) findViewById(R.id.textView);
        //DICHIARAZIONE CONTENUTI LAYOUT
        immagineSfondo = (ImageView) findViewById(R.id.imgSfondo);
        fabInfo = (FloatingActionButton) findViewById(R.id.fab1);
        fabStart = (FloatingActionButton) findViewById(R.id.fab2);
        webProgress = (ProgressBar) findViewById(R.id.progressWebView);
        webVista = (WebView) findViewById(R.id.vistaWeb);

        prevMaio = new ArrayList();
        prevMino = new ArrayList();
        isStarted = false;
        lastMaio = lastMino = 0;
        lastUrl = "vuoto";

        initializeCallback();

        //GESTIONE WEBVIEW, PROGRESS BAR durante caricamento pagina web
        webVista.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (isStarted && progress < 75) {
                    if (webProgress.getVisibility() != View.VISIBLE)
                        webProgress.setVisibility(View.VISIBLE);
                    webProgress.setProgress(progress);
                } else
                    webProgress.setVisibility(View.INVISIBLE);
            }
        });
        webVista.setWebViewClient(new WebViewClient());
        webVista.getSettings().setJavaScriptEnabled(true);

        //GESTIONE BOTTONI in basso
        fabInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Istruzioni.class);
                startActivity(intent);
            }
        });
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabStart.setVisibility(View.GONE);
                isStarted = true;
                //turnWebOn(lastMaio, lastMino);
                Snackbar.make(view, "E' iniziata la tua avventura con V.A.T.E.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        //END GESTIONE BOTTONI
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Richiesta automatica delle permission Bluetooth, Access Coarse Location
        //controllo run-time di connessione bluetooth e internet eccetera
        //provare metodo .check, che dicono sia meglio
        if(firstStart) {
            SystemRequirementsChecker.checkWithDefaultDialogs(this);
            firstStart = false;
        }

        startScanning();
        startValidating();
    }

    @Override
    protected void onPause() {
        stopScanning();
        stopValidating();

        turnWebOff();//quando si esce dall'activity, ritorna allo stato iniziale e formatta gli array con ultimi majo/mino
        isStarted = false;
        fabStart.setVisibility(View.VISIBLE);
        lastMaio = lastMino = 0;
        mAdapter.nearestMaio = 0;
        mAdapter.nearestMino = 0;
        prevMaio.clear();
        prevMino.clear();

        super.onPause();
    }

    @Override
    public void onBackPressed() {//decidere se lasciare la possibilità di navigare avanti/indietro oppure no
        //ho tolto la possiblità di navigare indietro
        //se si schiaccia la doppia freccia, si torna alla schermata iniziale

        /*if(webVista.canGoBack()) {
            webVista.goBack();
        } else {
            //super.onBackPressed();
            turnWebOff();
            isStarted = false;
            fabStart.setVisibility(View.VISIBLE);
        }*/
        turnWebOff();
        isStarted = false;
        fabStart.setVisibility(View.VISIBLE);
    }

    //GESTIONE MENU IN ALTO A DX
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // creazione menu, oggetti definiti nel file menu_main nella cartella res/menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // gestione tocco su ciascun oggetto del menu
        int id = item.getItemId();
        // apertura activity
        /*if (id == R.id.listaBeacon) {
            Intent intent = new Intent(MainActivity.this, BeacList.class);
            startActivity(intent);
        }*/
        /*else*/ if (id == R.id.info) {
            Intent intent = new Intent(MainActivity.this, Informazioni.class);
            startActivity(intent);
        }
        else if(id == R.id.bevi) {
            Intent intent = new Intent(MainActivity.this, Bevilacqua.class);
            intent.putExtra("sezione", "bevilacqua");
            startActivity(intent);
        }
        else if(id == R.id.negozi) {
            Intent intent = new Intent(MainActivity.this, Bevilacqua.class);
            intent.putExtra("sezione", "negozi");
            startActivity(intent);
        }
        else if(id == R.id.bar) {
            Intent intent = new Intent(MainActivity.this, Bevilacqua.class);
            intent.putExtra("sezione", "bar");
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
    //END GESTIONE MENU

    //GESTIONE VISUALIZZAZIONE/SPEGNIMENTO LAYOUT WEB
    public void turnWebOn(int maio, int mino){
        if(immagineSfondo.getVisibility() == View.VISIBLE)
            immagineSfondo.setVisibility(View.INVISIBLE);
        if(webVista.getVisibility() == View.INVISIBLE)
            webVista.setVisibility(View.VISIBLE);
        /*if(!webVista.getUrl().equals(INDIRIZZO_WEB + maio + "_" + mino + "/")*/
        if(!lastUrl.equals(INDIRIZZO_WEB + maio + "_" + mino + "/")) {
            lastUrl = INDIRIZZO_WEB + maio + "_" + mino + "/";
            //webProgress.setVisibility(View.VISIBLE);//vedere se nel telefono vecchio parte prima
            webVista.loadUrl(INDIRIZZO_WEB + maio + "_" + mino + "/");
        }
    }
    public void turnWebOff(){
        webVista.setVisibility(View.INVISIBLE);
        immagineSfondo.setVisibility(View.VISIBLE);
    }
    //END GESTIONE LAYOUT

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
    }
    //bisogna aggiungere richiesta per accesso a posizione fatta bene
    //tipo richiesta solo per API maggiori di un tot
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
        //isLocationAvailableAndEnabled();
        Log.d("STARTSCANNING", "StartScanning is called");
        System.out.print("StartScanning is called");
        if (!isBluetoothAvailableAndEnabled()) {
            //requestForBluetooth();
            return;
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
            //prima non si usava justFinded, da controllare
            if(!beacon.isEstimoteBeacon() || mAdapter.justFinded(beacon.major, beacon.minor)) return;
            if(!isBeaconVate(beacon.major, beacon.minor)) return;
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
            //if(!isStarted) return;

            mAdapter.nearestBeac();
            int Maio = mAdapter.nearestMaio;
            int Mino = mAdapter.nearestMino;
            fifo(Maio, Mino);
            if(!isStarted) return;
            if ((lastMaio != Maio || lastMino != Mino) && isRealNearest(Maio,Mino)) {
                String bau = "near: major " + String.valueOf(mAdapter.nearestMaio) + " minor " + String.valueOf(mAdapter.nearestMino);
                //beacVicino.setText(bau);
                turnWebOn(Maio, Mino);
                lastMaio = Maio;
                lastMino = Mino;
            }
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
                    /*mAdapter.nearestBeac();
                    int Maio = mAdapter.nearestMaio;
                    int Mino = mAdapter.nearestMino;
                    fifo(Maio, Mino);
                    //beacVicino.setText("near: major " + mAdapter.nearestMaio + " minor " + mAdapter.nearestMino);
                    if ((lastMaio != Maio || lastMino != Mino) && isRealNearest(Maio,Mino)) {
                        turnWebOn(Maio, Mino);
                        lastMaio = Maio;
                        lastMino = Mino;
                    }*/
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

    //FIFO ULTIMI VALORI MAJOR/MINOR
    void fifo(int nowMaio, int nowMino){
        if(prevMaio.size()>MEMORIA_FIFO && prevMino.size()>MEMORIA_FIFO){
            prevMaio.remove(0);
            prevMino.remove(0);
        }
        prevMaio.add(nowMaio);
        prevMino.add(nowMino);
    }
    //VERIFICA BEACON PIU' VICINO
    boolean isRealNearest(int nowMaio, int nowMino){
        if(!isBeaconVate(nowMaio,nowMino)) return false;
        int counter = 0;
        for(int i=0;i<prevMaio.size();i++){
            if(prevMaio.get(i).equals(nowMaio) && prevMino.get(i).equals(nowMino))
                counter ++;
        }
        if(counter >= VALORE_MINIMO)
            return true;
        else
            return false;
    }
}