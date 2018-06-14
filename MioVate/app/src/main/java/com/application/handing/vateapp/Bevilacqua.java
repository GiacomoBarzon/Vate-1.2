package com.application.handing.vateapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Bevilacqua extends AppCompatActivity {
    //String sale[] = {"Ingresso","Sala 1","Sala 2","Giroscala","Sala 3","Sala 4","Sala 5","Sala 6" };
    //String beacon[] = {"1_1/","1_2/","1_3/","100_1/","200_1/","200_2/","200_3/","200_4/"};
    List<String> luoghi = new ArrayList<>();
    List<String> id_beacon = new ArrayList<>();
    private final static String INDIRIZZO_WEB = "https://vateapp.eu/";
    int numero_sala, sala_precedente;
    //static int SALA_MAX=7;//numero di sale -1
    int SALA_MAX;
    Button btnSala;
    Boolean isStarted;//vero se pulsante play è stato schiacciato, deve ritornare falso quando esco dall'app
    //ImageView immagineSfondo;
    WebView webVista;
    ProgressBar webProgress;
    TextView istruzioni;
    String lastUrl = " ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bevilacqua);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        numero_sala = 0;
        sala_precedente = 100;//era 10
        isStarted = false;

        String value= getIntent().getStringExtra("sezione");
        if(value.equals("bevilacqua")){
            getSupportActionBar().setTitle("Bevilacqua La Masa");

            luoghi.add("Ingresso");
            luoghi.add("Sala 1");
            luoghi.add("Sala 2");
            luoghi.add("Giroscala");
            luoghi.add("Sala 3");
            luoghi.add("Sala 4");
            luoghi.add("Sala 5");
            luoghi.add("Sala 6");

            id_beacon.add("1_1/");
            id_beacon.add("1_2/");
            id_beacon.add("1_3/");
            id_beacon.add("100_1/");
            id_beacon.add("200_1/");
            id_beacon.add("200_2/");
            id_beacon.add("200_3/");
            id_beacon.add("200_4/");
        }
        else if(value.equals("negozi")){
            getSupportActionBar().setTitle("Negozi");

            //luoghi.add("Todaro");
            //luoghi.add("Chioggia");
            luoghi.add("Tokatzian");
            luoghi.add("Cenedese");
            luoghi.add("Rolex");
            luoghi.add("Panerai");
            luoghi.add("Ravagnan");
            luoghi.add("Frey Wille");
            //luoghi.add("Florian");
            luoghi.add("Arcadia");
            luoghi.add("66");
            luoghi.add("Salvadori");
            luoghi.add("Nardi");
            luoghi.add("Pauly");

            //id_beacon.add("10000_1/");
            //id_beacon.add("10000_4/");
            id_beacon.add("10000_5/");
            id_beacon.add("10000_8/");
            id_beacon.add("10000_10/");
            id_beacon.add("10000_11/");
            id_beacon.add("10000_13/");
            id_beacon.add("10000_14/");
            //id_beacon.add("10000_15/");
            id_beacon.add("10000_18/");
            id_beacon.add("10000_19/");
            id_beacon.add("10000_20/");
            id_beacon.add("10000_21/");
            id_beacon.add("10000_24/");
        }
        else if(value.equals("bar")){
            getSupportActionBar().setTitle("Caffè e Gourmet");

            luoghi.add("Todaro");
            luoghi.add("Todaro");
            luoghi.add("Chioggia");
            /*luoghi.add("Tokatzian");
            luoghi.add("Cenedese");
            luoghi.add("Rolex");
            luoghi.add("Panerai");
            luoghi.add("Ravagnan");
            luoghi.add("Frey Wille");*/
            luoghi.add("Florian");
            /*luoghi.add("Arcadia");
            luoghi.add("66");
            luoghi.add("Salvadori");
            luoghi.add("Nardi");
            luoghi.add("Pauly");*/

            id_beacon.add("10000_1/");
            id_beacon.add("10000_2/");
            id_beacon.add("10000_4/");
            /*id_beacon.add("10000_5/");
            id_beacon.add("10000_8/");
            id_beacon.add("10000_10/");
            id_beacon.add("10000_11/");
            id_beacon.add("10000_13/");
            id_beacon.add("10000_14/");*/
            id_beacon.add("10000_15/");
            /*id_beacon.add("10000_18/");
            id_beacon.add("10000_19/");
            id_beacon.add("10000_20/");
            id_beacon.add("10000_21/");
            id_beacon.add("10000_24/");*/
        }
        else if(value.equals("piazza")){
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");
            luoghi.add("Piazza");

            id_beacon.add("10000_2/");
            id_beacon.add("10000_3/");
            id_beacon.add("10000_6/");
            id_beacon.add("10000_7/");
            id_beacon.add("10000_9/");
            id_beacon.add("10000_12/");
            id_beacon.add("10000_16/");
            id_beacon.add("10000_17/");
            id_beacon.add("10000_21/");
            id_beacon.add("10000_24/");
            id_beacon.add("10000_25/");
            id_beacon.add("10000_26/");
        }

        SALA_MAX = luoghi.size()-1;//poi si può mettere meglio questa cosa

        //immagineSfondo = (ImageView) findViewById(R.id.imageBevi);
        istruzioni = (TextView) findViewById(R.id.testoIstruzioni);
        webProgress = (ProgressBar) findViewById(R.id.progressWebView);
        webVista = (WebView) findViewById(R.id.vistaWeb);
        btnSala = (Button) findViewById(R.id.btnNomeSala);

        FloatingActionButton fabDestra = (FloatingActionButton) findViewById(R.id.fabDex);
        FloatingActionButton fabSinistra = (FloatingActionButton) findViewById(R.id.fabSix);

        //GESTIONE WEBVIEW, PROGRESS BAR durante caricamento pagina web
        webVista.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 75 && progress >0) {
                    if (webProgress.getVisibility() != View.VISIBLE)
                        webProgress.setVisibility(View.VISIBLE);
                    webProgress.setProgress(progress);
                } else
                    webProgress.setVisibility(View.INVISIBLE);
            }
        });
        webVista.setWebViewClient(new WebViewClient());
        webVista.getSettings().setJavaScriptEnabled(true);

        //GESTIONE BOTTONI
        fabDestra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeName('d');
                if(sala_precedente != numero_sala) {
                    sala_precedente = numero_sala;
                    turnWebOn();
                }
            }
        });


        fabSinistra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeName('s');
                if(sala_precedente != numero_sala) {
                    sala_precedente = numero_sala;
                    turnWebOn();
                }
            }
        });

        /*btnSala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sala_precedente != numero_sala) {
                    sala_precedente = numero_sala;
                    turnWebOn();
                }
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        turnWebOn();
        btnSala.setText(luoghi.get(numero_sala));
    }

    @Override
    protected void onPause() {
        turnWebOff();
        sala_precedente = 100;//era 10

        super.onPause();
    }

    @Override
    public void onBackPressed() {//decidere se lasciare la possibilità di navigare avanti/indietro oppure no
        if(webVista.canGoBack()) {
            webVista.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void changeName(char a){
        if(a == 'd') {
            if(numero_sala == SALA_MAX)
                numero_sala = 0;
            else
                numero_sala += 1;
        }
        else if(a == 's'){
            if(numero_sala == 0)
                numero_sala = SALA_MAX;
            else
                numero_sala -= 1;
        }

        //btnSala.setText(sale[numero_sala]);
        btnSala.setText(luoghi.get(numero_sala));
    }

    public void turnWebOn(){
        /*if(immagineSfondo.getVisibility() == View.VISIBLE) {
            immagineSfondo.setVisibility(View.INVISIBLE);
            istruzioni.setVisibility(View.INVISIBLE);
        }*/
        if(webVista.getVisibility() == View.INVISIBLE)
            webVista.setVisibility(View.VISIBLE);
        //webVista.loadUrl(INDIRIZZO_WEB  + beacon[numero_sala]);
        String nowUrl = INDIRIZZO_WEB  + id_beacon.get(numero_sala);
        if (!nowUrl.equals(lastUrl)){
            webVista.loadUrl(INDIRIZZO_WEB + id_beacon.get(numero_sala));
            lastUrl = nowUrl;
        }
    }
    public void turnWebOff(){
        webVista.setVisibility(View.INVISIBLE);
        //immagineSfondo.setVisibility(View.VISIBLE);
        istruzioni.setVisibility(View.INVISIBLE);
    }

}
