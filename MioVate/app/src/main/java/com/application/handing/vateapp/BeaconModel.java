package com.application.handing.vateapp;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.Date;

public class BeaconModel
{
    final private static String estimoteUuid = "b9407f30-f5f8-466e-aff9-25556b57fe6d";//Uuid dei beacon Estimote

    public final static int DIM_FIFO = 20;
    public final static int DEV_STD = 15;

    //VARIABILI
    public String uuid;         // UUID of beacon
    public int major;           // major of beacon
    public int minor;           // minor of beacon
    public int txPower;         // reference power
    public int istantRssi;      // current RSSI, non usato
    public double rssi;         // average RSSI

    public long timestamp;      // timestamp when this beacon was last time scanned
    public String id;           // ID of the beacon, in case of android it will be BT MAC address
    public long durate;

    public final ArrayList<Integer> rssi_list = new ArrayList<>();//lista per immagazzinare i valori rssi misurati

    //STANDARD iBEACON
    //prefisso 9 byte, uuid 16 byte, major 2 byte, minor 2 byte, txPower 1 byte
    private static final int UUID_START_INDEX = 9;
    private static final int UUID_STOP_INDEX = UUID_START_INDEX + 15;//16 byte UUID, 32 caratteri
    private static final int MAJOR_START_INDEX = UUID_STOP_INDEX + 1;//4 byte major, minor
    private static final int TXPOWER_INDEX = MAJOR_START_INDEX + 4;//1 byte TxPower

    //funzione che tramite il minor controlla se è un nostro beacon
    public boolean isEstimoteBeacon(){
        if(uuid.equals(estimoteUuid))
            return true;
        else
            return false;
    }

    // MEDIA_RSSI: --------------------------------------
    //MEDIA, REIEZIONE DATI RSSI  (NB: torna 0 se errore)
    public double media_rssi(){
        if (rssi_list.size()<=0)
            return 0;//se torna 0, significa che errore
        double sum = 0.0;
        for(int i:rssi_list)
            sum += i;
        return sum / rssi_list.size(); // sempre diverso da zero size (perchè chiamata se =10)
    }

    // REAL_RSSI: --------------------------------------
    // MEDIA FATTA ESCLDENDO I CAMPIONI FUORI DALLA DEVIAZIONE STANDARD (2 volte dev) (NB: torna 0 se errore)
    public double real_rssi(){
        double sum = 0.0;
        int counter = 0;
        double media_semplice=media_rssi(); // tengo double come media degli rssi interi
        if( media_semplice==0)
            return 0; // 0=errore

        for(int i=0;i<rssi_list.size();++i) {
            if(Math.abs(rssi_list.get(i) - media_semplice) < DEV_STD) {
                sum += rssi_list.get(i);
                counter += 1;
            }
        }
        if (counter<=0 || sum <=0)
            return media_semplice;
        else
            return sum / counter;
    }

    // DATAENTRY:-----------------------------------------
    public void dataEntry(final BluetoothDevice device,
                           final int rssi,
                           final byte[] advertisement) {
        this.rssi = rssi;
        if(rssi != 0){
            rssi_list.clear(); // RECH
            for (int j=0;j<DIM_FIFO;j++)  //al primo colpo lo aggiungo DIM_FIFO VOLTE per riempire e fare media decente
                rssi_list.add(rssi);
        }
        this.id = device.getAddress();
        this.durate = new Date().getTime() - timestamp;
        this.timestamp = new Date().getTime();
        this.major = ((advertisement[MAJOR_START_INDEX] & 0xff) << 8) | (advertisement[MAJOR_START_INDEX+1] & 0xff);
        this.minor = ((advertisement[MAJOR_START_INDEX+2] & 0xff) << 8) | (advertisement[MAJOR_START_INDEX+3] & 0xff);
        this.txPower = (int) advertisement[TXPOWER_INDEX];
        //costruzione UUID
        StringBuilder sb = new StringBuilder();
        for(int i = UUID_START_INDEX, offset = 0; i <= UUID_STOP_INDEX; ++i, ++offset) {
            sb.append(String.format("%02x", (int)(advertisement[i] & 0xff)));
            if (offset == 3 || offset == 5 || offset == 7 || offset == 9) {
                sb.append("-");
            }
        }
        this.uuid = sb.toString();
    }
}