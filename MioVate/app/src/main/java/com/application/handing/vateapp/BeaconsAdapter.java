package com.application.handing.vateapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;

public class BeaconsAdapter //extends BaseAdapter
{
    private static final int NUMERO_RSSI_SAMPLE = 10;
    private static final long BEACON_LIFE_DURATION = 10000;
    public final ArrayList<BeaconModel> mBeacons = new ArrayList<>();

    int nearestMaio =0;
    int nearestMino =0;

    //aggiungi alla lista un nuovo beacon
    //viene chiamata nel main, nella funzione HandleNewBeacon
    public void addNewBeacon(final BeaconModel beacon) {
        mBeacons.add(beacon);
        //mBeacons.sort();
        //notifyDataSetChanged();
    }

    // UPDATING: ------------------------------------------------------------
    //aggiorna i valori di rssi medio se ho almeno N misurazioni
    public void updating(BeaconModel beacon) {
        double temp_new_media_rssi;

        if(beacon.rssi_list.size()>=NUMERO_RSSI_SAMPLE) // ho superato il numero dei campioni
        {
            temp_new_media_rssi=beacon.real_rssi(); // RSSI faccio media tra campioni esclusi quelli fuori deviazione ???
            //dopo aver aggiornato rssi, formatta lista rssi
            beacon.rssi_list.clear();
            beacon.durate = new Date().getTime() - beacon.timestamp;//aggiorno durata lettura
            beacon.timestamp = new Date().getTime();//aggiorno ultimo istante lettura

            if (temp_new_media_rssi<0 && temp_new_media_rssi>-500) // raggio non ok: segnalo solo errore sopra ma non vario le medie precedenti
            {
                beacon.rssi=temp_new_media_rssi; //salvo valore  temporaneo come nuova media rssi
                beacon.rssi_list.clear();
            }
        }
    }

    // NEARESTBEAC:----------------------------------------------------------
    //calcolo beacon più vicino, se ritorna 10 non c'è un beacon vicino (o non ci sono abbastanza valori rssi)
    public void nearestBeac(){
        int position = 1000;
        double min_rssi = -1000.0;
        for(int i=0;i<mBeacons.size();++i){
            if(mBeacons.get(i).rssi<0 && mBeacons.get(i).rssi>-200) {
                if(mBeacons.get(i).rssi > min_rssi) {
                    min_rssi = mBeacons.get(i).rssi;
                    position = i;
                }
            }
        }
        nearestMaio = mBeacons.get(position).major;
        nearestMino = mBeacons.get(position).minor;
    }

    public BeaconModel findBeaconWithId(final String id) {
        for(final BeaconModel beacon : mBeacons) {
            if(beacon.id.equals(id)) return beacon;
        }
        return null;
    }

    public boolean justFinded(final int major, final int minor){
        for(final BeaconModel beacon : mBeacons){
            if(beacon.minor == minor && beacon.major == major ) return true;
        }
        return false;
    }

    //controllo ciclico se legge o meno un beacon
    public boolean validateAllBeacons() {
        boolean anythingChanged = false;

        final long oldestTimestampAllowed = new Date().getTime() - BEACON_LIFE_DURATION;
        ListIterator<BeaconModel> iterator = mBeacons.listIterator();
        while (iterator.hasNext()) {
            final BeaconModel beacon = iterator.next();
            if (beacon.timestamp < oldestTimestampAllowed) {
                iterator.remove();
                anythingChanged = true;
            }
        }
        return anythingChanged;
    }
}

