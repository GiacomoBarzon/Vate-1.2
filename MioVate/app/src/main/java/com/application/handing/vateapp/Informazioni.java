package com.application.handing.vateapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Informazioni extends AppCompatActivity {

    private Button btnLink;
    private TextView testoInfo;
    private Boolean isWebOpen;
    private WebView webVista;
    private ProgressBar webProgress;

    private final static String INDIRIZZO_WEB = "http://vate.eu/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informazioni);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btnLink = (Button) findViewById(R.id.btnSito);
        testoInfo = (TextView) findViewById(R.id.infoText);
        webVista = (WebView) findViewById(R.id.vistaWeb);
        webProgress = (ProgressBar) findViewById(R.id.progressWebView);

        isWebOpen = false;

        //GESTIONE WEBVIEW, PROGRESS BAR durante caricamento pagina web
        webVista.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (isWebOpen && progress < 75) {
                    if (webProgress.getVisibility() != View.VISIBLE)
                        webProgress.setVisibility(View.VISIBLE);
                    webProgress.setProgress(progress);
                } else
                    webProgress.setVisibility(View.INVISIBLE);
            }
        });
        webVista.setWebViewClient(new WebViewClient());
        webVista.getSettings().setJavaScriptEnabled(true);

        btnLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isWebOpen) {
                    isWebOpen = true;
                    turnWebOn();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        turnWebOff();//quando si esce dall'activity, ritorna allo stato iniziale e formatta gli array con ultimi majo/mino
        isWebOpen = false;

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

    //GESTIONE VISUALIZZAZIONE/SPEGNIMENTO LAYOUT WEB
    public void turnWebOn(){
        testoInfo.setVisibility(View.INVISIBLE);
        btnLink.setVisibility(View.GONE);
        webVista.setVisibility(View.VISIBLE);
        webProgress.setVisibility(View.VISIBLE);

        webVista.loadUrl(INDIRIZZO_WEB);
    }
    public void turnWebOff(){
        webVista.setVisibility(View.GONE);
        testoInfo.setVisibility(View.VISIBLE);
        btnLink.setVisibility(View.VISIBLE);
    }
    //END GESTIONE LAYOUT

}
