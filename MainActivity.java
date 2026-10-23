package com.vigilantebt.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final String MONTO_COMPRA = "300,00";
    private static final String ORIGEN_FONDOS = "Sueldos y Salarios";
    private static final String DESTINO_FONDOS = "Ahorro";
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};

    private LinearLayout panelControlContainer;
    private LinearLayout webViewContainer;
    private WebView webView;
    private TextView badgeEstado, lblRefrescoVal, lblReloginVal, lblTimerRelogin, txtConsola;
    private ScrollView scrollConsola;
    private Button btnCuenta, btnOjoPrivacidad, btnIniciar, btnRecargar, btnDetener, btnFoldConsola;
    private Button btnVistaDividida, btnVistaPanel, btnVistaWeb;
    private ProgressBar progressRelogin;

    private String tipoDoc = "V";
    private String cedula = "";
    private String usuario = "";
    private String contrasena = "";
    private boolean usuarioOculto = false;
    private boolean consolaVisible = true;
    private int modoVistaActual = 0;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;

    private boolean botActivo = false;
    private boolean yaAvisoApertura = false;
    private int progresoWeb = 0;
    private long tiempoInicioSesionMs = 0;
    private long tiempoEstadoMs = 0;
    private boolean accionEnCurso = false;
    private int vueltaActual = 0;

    private enum EstadoBot {
        DETENIDO,
        CARGANDO_LOGIN,
        ESPERANDO_ESTABILIZAR_LOGIN,
        ENVIANDO_FASE1,
        ESPERANDO_CARGA_PASS,
        ESPERANDO_ESTABILIZAR_PASS,
        ENVIANDO_FASE2,
        ESPERANDO_CARGA_INICIO,
        ESPERANDO_ESTABILIZAR_INICIO,
        NAVEGANDO_MENU,
        VIGILANDO,
        ESPERANDO_FORM_INTENTO1,
        LIMPIANDO_PANTALLA,
        ESPERANDO_FORM_INTENTO2,
        ESPERANDO_ESTABILIZAR_FORM,
        LLENANDO_COMPRA,
        COMPRANDO,
        CERRANDO_SESION,
        REINICIANDO_RED
    }

    private EstadoBot estadoActual = EstadoBot.DETENIDO;
    private int intentoLogout = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#18181B"));
        getWindow().setNavigationBarColor(Color.parseColor("#18181B"));
        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);

        construirInterfazCompleta();
        configurarWebView();
        cargarUltimaCuentaUsada();
        iniciarCicloReloj();
    }

    private int dp(int val) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics()
        );
    }

    private GradientDrawable fondoRedondeado(String colorHex, int radioDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(dp(radioDp));
        return gd;
    }

    private Bitmap crearLogoTransparenteBT() {
        int s = 256;
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

        Path pathAzul = new Path();
        c.save();
        c.rotate(-4f, 105f, 110f);
        pathAzul.addRoundRect(new RectF(16f, 64f, 176f, 148f), 42f, 42f, Path.Direction.CW);
        c.restore();
        pathAzul.addRoundRect(new RectF(82f, 64f, 176f, 236f), 45f, 45f, Path.Direction.CW);

        Path pathAmarillo = new Path();
        c.save();
        Path tempAm = new Path();
        tempAm.addRoundRect(new RectF(98f, 38f, 244f, 128f), 44f, 44f, Path.Direction.CW);
        android.graphics.Matrix mRot = new android.graphics.Matrix();
        mRot.postRotate(-6f, 170f, 83f);
        tempAm.transform(mRot, pathAmarillo);
        c.restore();

        p.setColor(Color.parseColor("#F9B218"));
        c.drawPath(pathAmarillo, p);

        p.setColor(Color.parseColor("#008FD5"));
        c.drawPath(pathAzul, p);

        c.save();
        c.clipPath(pathAzul);
        p.setColor(Color.parseColor("#E41E25"));
        c.drawPath(pathAmarillo, p);
        c.restore();

        return bmp;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void construirInterfazCompleta() {
        FrameLayout frameRoot = new FrameLayout(this);
        frameRoot.setBackgroundColor(Color.parseColor("#18181B"));
        frameRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F1115"));
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. BARRA SUPERIOR (Con el icono PNG transparente al lado de "Vigilante BT")
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(10), dp(12), dp(6));

        ImageView iconTop = new ImageView(this);
        int resIdHeader = getResources().getIdentifier("ic_header_logo", "drawable", getPackageName());
        if (resIdHeader != 0) {
            iconTop.setImageResource(resIdHeader);
        } else {
            iconTop.setImageBitmap(crearLogoTransparenteBT());
        }
        iconTop.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(26), dp(26));
        lpIcon.setMargins(0, 0, dp(8), 0);
        topBar.addView(iconTop, lpIcon);

        TextView lblTitulo = new TextView(this);
        lblTitulo.setText("Vigilante BT");
        lblTitulo.setTextColor(Color.parseColor("#F4F4F5"));
        lblTitulo.setTextSize(18);
        lblTitulo.setTypeface(null, Typeface.BOLD);
        topBar.addView(lblTitulo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        badgeEstado = new TextView(this);
        badgeEstado.setText("🔴 APAGADO");
        badgeEstado.setTextColor(Color.parseColor("#FCA5A5"));
        badgeEstado.setTextSize(11);
        badgeEstado.setTypeface(null, Typeface.BOLD);
        badgeEstado.setGravity(Gravity.CENTER);
        badgeEstado.setPadding(dp(10), dp(5), dp(10), dp(5));
        badgeEstado.setBackground(fondoRedondeado("#450A0A", 8));
        topBar.addView(badgeEstado);
        root.addView(topBar);

        // Selector de Vista
        LinearLayout viewBar = new LinearLayout(this);
        viewBar.setOrientation(LinearLayout.HORIZONTAL);
        viewBar.setPadding(dp(12), dp(2), dp(12), dp(6));

        btnVistaDividida = crearBoton("📱 Dividido", "#0284C7", "#FFFFFF", 11, () -> cambiarVista(0));
        btnVistaPanel = crearBoton("🎛️ Solo Panel", "#27272A", "#A1A1AA", 11, () -> cambiarVista(1));
        btnVistaWeb = crearBoton("🌐 Solo Banco", "#27272A", "#A1A1AA", 11, () -> cambiarVista(2));

        LinearLayout.LayoutParams lpViewBtn = new LinearLayout.LayoutParams(0, dp(32), 1f);
        lpViewBtn.setMargins(dp(2), 0, dp(2), 0);
        viewBar.addView(btnVistaDividida, lpViewBtn);
        viewBar.addView(btnVistaPanel, lpViewBtn);
        viewBar.addView(btnVistaWeb, lpViewBtn);
        root.addView(viewBar);

        // 2. PANEL DE CONTROL
        panelControlContainer = new LinearLayout(this);
        panelControlContainer.setOrientation(LinearLayout.VERTICAL);
        panelControlContainer.setPadding(dp(12), dp(10), dp(12), dp(8));
        panelControlContainer.setBackground(fondoRedondeado("#18181B", 14));

        // Botón Usuario + Ojo
        LinearLayout filaCuenta = new LinearLayout(this);
        filaCuenta.setOrientation(LinearLayout.HORIZONTAL);
        filaCuenta.setGravity(Gravity.CENTER_VERTICAL);

        btnCuenta = crearBoton("Agregar Cuenta", "#27272A", "#F4F4F5", 13, this::mostrarPopupCuenta);
        LinearLayout.LayoutParams lpBtnCuenta = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnCuenta.setMargins(0, 0, dp(6), 0);
        filaCuenta.addView(btnCuenta, lpBtnCuenta);

        btnOjoPrivacidad = crearBoton("👁", "#27272A", "#F4F4F5", 13, this::togglePrivacidadUsuario);
        filaCuenta.addView(btnOjoPrivacidad, new LinearLayout.LayoutParams(dp(42), dp(38)));
        panelControlContainer.addView(filaCuenta);

        // Tarjeta de Tiempos
        LinearLayout cardTiempos = new LinearLayout(this);
        cardTiempos.setOrientation(LinearLayout.VERTICAL);
        cardTiempos.setPadding(dp(10), dp(8), dp(10), dp(8));
        cardTiempos.setBackground(fondoRedondeado("#27272A", 10));
        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, dp(6), 0, dp(6));

        LinearLayout filaRef = new LinearLayout(this);
        filaRef.setGravity(Gravity.CENTER_VERTICAL);
        TextView tRef = crearTexto("Frecuencia Refresco:", "#F4F4F5", 11, true);
        SeekBar sldRef = new SeekBar(this);
        sldRef.setMax(29);
        sldRef.setProgress(2);
        lblRefrescoVal = crearTexto("3 seg", "#F4F4F5", 11, true);
        lblRefrescoVal.setGravity(Gravity.END);
        sldRef.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intervaloRefresco = progress + 1;
                lblRefrescoVal.setText(intervaloRefresco + " seg");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        filaRef.addView(tRef, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRef.addView(sldRef, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRef.addView(lblRefrescoVal, new LinearLayout.LayoutParams(dp(45), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRef);

        LinearLayout filaRel = new LinearLayout(this);
        filaRel.setGravity(Gravity.CENTER_VERTICAL);
        TextView tRel = crearTexto("Reinicio Sesión:", "#F4F4F5", 11, true);
        SeekBar sldRel = new SeekBar(this);
        sldRel.setMax(OPCIONES_MINUTOS.length - 1);
        sldRel.setProgress(3);
        lblReloginVal = crearTexto("10 min", "#F4F4F5", 11, true);
        lblReloginVal.setGravity(Gravity.END);
        sldRel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minutosRelogin = OPCIONES_MINUTOS[progress];
                lblReloginVal.setText(minutosRelogin + " min");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        filaRel.addView(tRel, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRel.addView(sldRel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRel.addView(lblReloginVal, new LinearLayout.LayoutParams(dp(45), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRel);

        LinearLayout filaProg = new LinearLayout(this);
        filaProg.setGravity(Gravity.CENTER_VERTICAL);
        filaProg.setPadding(0, dp(4), 0, 0);
        TextView tProg = crearTexto("Progreso Reinicio:", "#A1A1AA", 10, true);
        progressRelogin = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressRelogin.setMax(100);
        progressRelogin.setProgress(0);
        lblTimerRelogin = crearTexto("--:--", "#38BDF8", 10, true);
        lblTimerRelogin.setGravity(Gravity.END);
        filaProg.addView(tProg, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaProg.addView(progressRelogin, new LinearLayout.LayoutParams(0, dp(10), 1f));
        filaProg.addView(lblTimerRelogin, new LinearLayout.LayoutParams(dp(45), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaProg);

        panelControlContainer.addView(cardTiempos, lpCard);

        // Botones INICIAR | RECARGAR | DETENER
        LinearLayout filaBotones = new LinearLayout(this);
        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
        btnIniciar = crearBoton("▶ INICIAR", "#16A34A", "#FFFFFF", 12, this::iniciarBot);
        btnRecargar = crearBoton("🔄 RECARGAR", "#0284C7", "#FFFFFF", 12, this::recargarManual);
        btnDetener = crearBoton("⬛ DETENER", "#7F1D1D", "#FCA5A5", 12, this::detenerBot);
        btnDetener.setEnabled(false);

        LinearLayout.LayoutParams lpBtnMando = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnMando.setMargins(dp(2), 0, dp(2), 0);
        filaBotones.addView(btnIniciar, lpBtnMando);
        filaBotones.addView(btnRecargar, lpBtnMando);
        filaBotones.addView(btnDetener, lpBtnMando);
        panelControlContainer.addView(filaBotones);

        // Cabecera Consola
        LinearLayout headerConsola = new LinearLayout(this);
        headerConsola.setGravity(Gravity.CENTER_VERTICAL);
        headerConsola.setPadding(0, dp(6), 0, dp(4));

        TextView lblConsola = crearTexto("Consola", "#F4F4F5", 12, true);
        headerConsola.addView(lblConsola);

        btnFoldConsola = crearBoton("▼", "#18181B", "#F4F4F5", 11, this::toggleConsola);
        LinearLayout.LayoutParams lpFold = new LinearLayout.LayoutParams(dp(30), dp(26));
        lpFold.setMargins(dp(4), 0, 0, 0);
        headerConsola.addView(btnFoldConsola, lpFold);

        View espaciadorConsola = new View(this);
        headerConsola.addView(espaciadorConsola, new LinearLayout.LayoutParams(0, dp(1), 1f));

        Button btnLimpiar = crearBoton("Limpiar", "#3F3F46", "#FFFFFF", 10, () -> txtConsola.setText(""));
        LinearLayout.LayoutParams lpLimpiar = new LinearLayout.LayoutParams(dp(65), dp(26));
        lpLimpiar.setMargins(0, 0, dp(4), 0);
        headerConsola.addView(btnLimpiar, lpLimpiar);

        Button btnGuardarLog = crearBoton("Guardar", "#3F3F46", "#FFFFFF", 10, this::copiarLogPortapapeles);
        headerConsola.addView(btnGuardarLog, new LinearLayout.LayoutParams(dp(65), dp(26)));
        panelControlContainer.addView(headerConsola);

        // Caja de Consola
        scrollConsola = new ScrollView(this);
        scrollConsola.setBackground(fondoRedondeado("#1E2022", 8));
        scrollConsola.setPadding(dp(8), dp(6), dp(8), dp(6));
        txtConsola = new TextView(this);
        txtConsola.setTextColor(Color.parseColor("#4ADE80"));
        txtConsola.setTextSize(11);
        txtConsola.setTypeface(Typeface.MONOSPACE);
        scrollConsola.addView(txtConsola);
        panelControlContainer.addView(scrollConsola, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(panelControlContainer);

        // 3. NAVEGADOR INTEGRADO (WEBVIEW)
        webViewContainer = new LinearLayout(this);
        webViewContainer.setOrientation(LinearLayout.VERTICAL);
        webViewContainer.setBackground(fondoRedondeado("#18181B", 14));
        webViewContainer.setPadding(dp(4), dp(4), dp(4), dp(4));

        webView = new WebView(this);
        webViewContainer.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(webViewContainer);
        actualizarDistribucionAlturas();

        frameRoot.addView(root);

        // 4. PANTALLA DE ENTRADA EN GRIS OSCURO (#18181B) CON EL GIF TRANSPARENTE (SIN FONDO BLANCO)
        LinearLayout splashOverlay = new LinearLayout(this);
        splashOverlay.setOrientation(LinearLayout.VERTICAL);
        splashOverlay.setGravity(Gravity.CENTER);
        splashOverlay.setBackgroundColor(Color.parseColor("#18181B"));
        splashOverlay.setClickable(true);
        splashOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebView splashWebView = new WebView(this);
        splashWebView.setBackgroundColor(Color.parseColor("#18181B"));
        splashWebView.setVerticalScrollBarEnabled(false);
        splashWebView.setHorizontalScrollBarEnabled(false);
        splashWebView.getSettings().setJavaScriptEnabled(true);
        splashWebView.getSettings().setLoadsImagesAutomatically(true);

        // El script de Canvas lee el GIF oficial en vivo y borra todos los pixeles blancos (r>230, g>230, b>230)
        // dejando únicamente el logo girando sobre el fondo gris oscuro (#18181B).
        String htmlSplash = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1.0'>" +
                "<style>" +
                "html,body{margin:0;padding:0;width:100%;height:100%;display:flex;justify-content:center;align-items:center;background:#18181B;overflow:hidden;}" +
                "#wrap{position:relative;width:170px;height:170px;display:flex;justify-content:center;align-items:center;}" +
                "canvas{width:170px;height:170px;display:none;}" +
                "svg{width:145px;height:145px;}" +
                "@keyframes draw{0%{stroke-dashoffset:360;}50%{stroke-dashoffset:0;}100%{stroke-dashoffset:-360;}}" +
                ".p1{fill:none;stroke:#0095DB;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out;}" +
                ".p2{fill:none;stroke:#F9B218;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out 0.2s;}" +
                ".p3{fill:none;stroke:#E41E25;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out 0.4s;}" +
                "</style></head><body>" +
                "<div id='wrap'>" +
                "<img id='rawGif' crossorigin='anonymous' src='" + URL_GIF_LOADER + "' style='position:absolute;opacity:0.001;pointer-events:none;width:1px;height:1px;' />" +
                "<canvas id='cv' width='220' height='220'></canvas>" +
                "<svg id='fb' viewBox='0 0 256 256'>" +
                "<rect class='p2' x='98' y='42' width='140' height='86' rx='43' transform='rotate(-6 168 85)'/>" +
                "<path class='p1' d='M58,68 L134,64 A42,42 0 0,1 176,106 L176,192 A44,44 0 0,1 88,192 L88,150 L58,152 A42,42 0 0,1 16,110 A42,42 0 0,1 58,68 Z'/>" +
                "<rect class='p3' x='98' y='64' width='78' height='64' rx='30'/>" +
                "</svg>" +
                "</div>" +
                "<script>" +
                "var img = document.getElementById('rawGif');" +
                "var cv = document.getElementById('cv');" +
                "var fb = document.getElementById('fb');" +
                "var ctx = cv.getContext('2d', {willReadFrequently: true});" +
                "var active = false;" +
                "function renderFrame(){" +
                "  if(!active) return;" +
                "  try {" +
                "    ctx.clearRect(0,0,220,220);" +
                "    ctx.drawImage(img,0,0,220,220);" +
                "    var frame = ctx.getImageData(0,0,220,220);" +
                "    var d = frame.data;" +
                "    for(var i=0; i<d.length; i+=4){" +
                "      var r=d[i], g=d[i+1], b=d[i+2];" +
                "      var maxC = Math.max(r,g,b), minC = Math.min(r,g,b);" +
                "      if(r > 215 && g > 215 && b > 215 && (maxC - minC) < 28){" +
                "        d[i+3] = 0;" +
                "      } else if(r > 185 && g > 185 && b > 185 && (maxC - minC) < 20){" +
                "        d[i+3] = Math.max(0, 255 - ((r+g+b)/3));" +
                "      }" +
                "    }" +
                "    ctx.putImageData(frame,0,0);" +
                "    requestAnimationFrame(renderFrame);" +
                "  } catch(e) {" +
                "    cv.style.display='none'; fb.style.display='block'; active=false;" +
                "  }" +
                "}" +
                "img.onload = function(){" +
                "  try {" +
                "    ctx.drawImage(img,0,0,220,220);" +
                "    ctx.getImageData(0,0,1,1);" +
                "    fb.style.display='none'; cv.style.display='block'; active=true;" +
                "    requestAnimationFrame(renderFrame);" +
                "  } catch(e) {}" +
                "};" +
                "</script></body></html>";

        splashWebView.loadDataWithBaseURL("https://tesoroenlinea.bt.com.ve/", htmlSplash, "text/html", "UTF-8", null);
        splashOverlay.addView(splashWebView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frameRoot.addView(splashOverlay);
        setContentView(frameRoot);

        handler.postDelayed(() -> {
            splashOverlay.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> {
                        splashOverlay.setVisibility(View.GONE);
                        splashWebView.destroy();
                    })
                    .start();
        }, 3000);
    }

    private void actualizarDistribucionAlturas() {
        if (modoVistaActual == 0) {
            panelControlContainer.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.VISIBLE);

            if (consolaVisible) {
                scrollConsola.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.12f);
                lpPanel.setMargins(dp(8), dp(2), dp(8), dp(4));
                panelControlContainer.setLayoutParams(lpPanel);
            } else {
                scrollConsola.setVisibility(View.GONE);
                LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
                lpPanel.setMargins(dp(8), dp(2), dp(8), dp(4));
                panelControlContainer.setLayoutParams(lpPanel);
            }

            LinearLayout.LayoutParams lpWeb = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            lpWeb.setMargins(dp(8), dp(2), dp(8), dp(8));
            webViewContainer.setLayoutParams(lpWeb);

        } else if (modoVistaActual == 1) {
            panelControlContainer.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.GONE);
            scrollConsola.setVisibility(consolaVisible ? View.VISIBLE : View.GONE);

            LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    consolaVisible ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT,
                    consolaVisible ? 1.0f : 0f);
            lpPanel.setMargins(dp(8), dp(2), dp(8), dp(8));
            panelControlContainer.setLayoutParams(lpPanel);

        } else {
            panelControlContainer.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams lpWeb = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            lpWeb.setMargins(dp(8), dp(2), dp(8), dp(8));
            webViewContainer.setLayoutParams(lpWeb);
        }
    }

    private void toggleConsola() {
        consolaVisible = !consolaVisible;
        btnFoldConsola.setText(consolaVisible ? "▼" : "▲");
        actualizarDistribucionAlturas();
    }

    private void cambiarVista(int modo) {
        modoVistaActual = modo;
        btnVistaDividida.setBackground(fondoRedondeado(modo == 0 ? "#0284C7" : "#27272A", 8));
        btnVistaPanel.setBackground(fondoRedondeado(modo == 1 ? "#0284C7" : "#27272A", 8));
        btnVistaWeb.setBackground(fondoRedondeado(modo == 2 ? "#0284C7" : "#27272A", 8));
        actualizarDistribucionAlturas();
    }

    private void copiarLogPortapapeles() {
        String logTxt = txtConsola.getText().toString().trim();
        if (logTxt.isEmpty()) return;
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null) {
            cb.setPrimaryClip(ClipData.newPlainText("Log Vigilante BT", logTxt));
            Toast.makeText(this, "Log copiado al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView crearTexto(String texto, String colorHex, int sizeSp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTextSize(sizeSp);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private Button crearBoton(String texto, String bgHex, String fgHex, int sizeSp, Runnable accion) {
        Button b = new Button(this);
        b.setText(texto);
        b.setAllCaps(false);
        b.setTextColor(Color.parseColor(fgHex));
        b.setTextSize(sizeSp);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setBackground(fondoRedondeado(bgHex, 8));
        b.setOnClickListener(v -> { if (accion != null) accion.run(); });
        return b;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView() {
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progresoWeb = newProgress;
            }
        });
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(URL_LOGIN);
        log("[SISTEMA] Navegador interno listo.");
    }

    private void togglePrivacidadUsuario() {
        if (usuario.isEmpty()) return;
        usuarioOculto = !usuarioOculto;
        actualizarTextoBotonUsuario();
    }

    private void actualizarTextoBotonUsuario() {
        if (usuario.isEmpty()) {
            btnCuenta.setText("Agregar Cuenta");
            btnOjoPrivacidad.setText("👁");
            return;
        }
        if (usuarioOculto) {
            btnCuenta.setText("👤 *********");
            btnOjoPrivacidad.setText("🔒");
        } else {
            btnCuenta.setText("👤 " + usuario);
            btnOjoPrivacidad.setText("👁");
        }
    }

    private JSONObject obtenerCuentasGuardadas() {
        try { return new JSONObject(prefs.getString("cuentas_json", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    private void cargarUltimaCuentaUsada() {
        String ultima = prefs.getString("ultima_cuenta", "");
        if (!ultima.isEmpty()) {
            JSONObject todas = obtenerCuentasGuardadas();
            if (todas.has(ultima)) {
                JSONObject d = todas.optJSONObject(ultima);
                if (d != null) {
                    tipoDoc = d.optString("tipo_doc", "V");
                    cedula = d.optString("cedula", "");
                    usuario = d.optString("usuario", "");
                    contrasena = d.optString("clave", "");
                    actualizarTextoBotonUsuario();
                }
            }
        }
    }

    private void toggleCampoOjo(EditText edt, Button btn) {
        if ((edt.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0) {
            edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btn.setBackground(fondoRedondeado("#52525B", 6));
        } else {
            edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btn.setBackground(fondoRedondeado("#27272A", 6));
        }
        edt.setSelection(edt.getText().length());
    }

    private void mostrarPopupCuenta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(14), dp(16), dp(14));
        layout.setBackgroundColor(Color.parseColor("#18181B"));

        TextView titulo = crearTexto("Gestión de Cuenta", "#F4F4F5", 16, true);
        titulo.setPadding(0, 0, 0, dp(10));
        layout.addView(titulo);

        final JSONObject cuentasObj = obtenerCuentasGuardadas();
        final ArrayList<String> listaNombres = new ArrayList<>();
        listaNombres.add("(Agregar Nueva)");
        Iterator<String> keys = cuentasObj.keys();
        while (keys.hasNext()) { listaNombres.add(keys.next()); }

        final String[] cuentaSeleccionada = { (!usuario.isEmpty() && listaNombres.contains(usuario)) ? usuario : "(Agregar Nueva)" };
        final String[] tipoSeleccionado = {tipoDoc.isEmpty() ? "V" : tipoDoc};

        LinearLayout filaSel = new LinearLayout(this);
        filaSel.setOrientation(LinearLayout.HORIZONTAL);

        final Button btnSelectCuenta = crearBoton(cuentaSeleccionada[0] + "  ▼", "#27272A", "#F4F4F5", 12, null);
        LinearLayout.LayoutParams lpSel = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpSel.setMargins(0, 0, dp(6), 0);
        filaSel.addView(btnSelectCuenta, lpSel);

        LinearLayout filaCed = new LinearLayout(this);
        filaCed.setOrientation(LinearLayout.HORIZONTAL);
        filaCed.setPadding(0, dp(8), 0, dp(4));

        final String[] tiposDocArray = {"V", "E", "J", "P", "G", "C", "R"};
        final Button btnTipoDoc = crearBoton(tipoSeleccionado[0] + " ▼", "#27272A", "#F4F4F5", 12, null);
        btnTipoDoc.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Tipo de Documento")
                    .setItems(tiposDocArray, (d, which) -> {
                        tipoSeleccionado[0] = tiposDocArray[which];
                        btnTipoDoc.setText(tipoSeleccionado[0] + " ▼");
                    }).show();
        });

        final EditText edtCedula = new EditText(this);
        edtCedula.setHint("Cédula");
        edtCedula.setHintTextColor(Color.parseColor("#94A3B8"));
        edtCedula.setTextColor(Color.parseColor("#F4F4F5"));
        edtCedula.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        edtCedula.setBackground(fondoRedondeado("#27272A", 6));
        edtCedula.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoCed = crearBoton("👁", "#52525B", "#F4F4F5", 12, null);
        btnOjoCed.setOnClickListener(v -> toggleCampoOjo(edtCedula, btnOjoCed));

        LinearLayout.LayoutParams lpTipo = new LinearLayout.LayoutParams(dp(58), dp(38));
        lpTipo.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams lpEdt = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpEdt.setMargins(0, 0, dp(4), 0);
        filaCed.addView(btnTipoDoc, lpTipo);
        filaCed.addView(edtCedula, lpEdt);
        filaCed.addView(btnOjoCed, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout filaUsr = new LinearLayout(this);
        filaUsr.setOrientation(LinearLayout.HORIZONTAL);
        filaUsr.setPadding(0, dp(4), 0, dp(4));

        final EditText edtUsuario = new EditText(this);
        edtUsuario.setHint("Usuario");
        edtUsuario.setHintTextColor(Color.parseColor("#94A3B8"));
        edtUsuario.setTextColor(Color.parseColor("#F4F4F5"));
        edtUsuario.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        edtUsuario.setBackground(fondoRedondeado("#27272A", 6));
        edtUsuario.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoUsr = crearBoton("👁", "#52525B", "#F4F4F5", 12, null);
        btnOjoUsr.setOnClickListener(v -> toggleCampoOjo(edtUsuario, btnOjoUsr));

        filaUsr.addView(edtUsuario, lpEdt);
        filaUsr.addView(btnOjoUsr, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout filaClv = new LinearLayout(this);
        filaClv.setOrientation(LinearLayout.HORIZONTAL);
        filaClv.setPadding(0, dp(4), 0, dp(8));

        final EditText edtClave = new EditText(this);
        edtClave.setHint("Contraseña");
        edtClave.setHintTextColor(Color.parseColor("#94A3B8"));
        edtClave.setTextColor(Color.parseColor("#F4F4F5"));
        edtClave.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtClave.setBackground(fondoRedondeado("#27272A", 6));
        edtClave.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoClv = crearBoton("👁", "#27272A", "#F4F4F5", 12, null);
        btnOjoClv.setOnClickListener(v -> toggleCampoOjo(edtClave, btnOjoClv));

        filaClv.addView(edtClave, lpEdt);
        filaClv.addView(btnOjoClv, new LinearLayout.LayoutParams(dp(38), dp(38)));

        if (!cuentaSeleccionada[0].equals("(Agregar Nueva)")) {
            edtCedula.setText(cedula);
            edtUsuario.setText(usuario);
            edtClave.setText(contrasena);
        }

        btnSelectCuenta.setOnClickListener(v -> {
            String[] items = listaNombres.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Seleccionar Cuenta")
                    .setItems(items, (d, which) -> {
                        String sel = items[which];
                        cuentaSeleccionada[0] = sel;
                        btnSelectCuenta.setText(sel + "  ▼");
                        if (sel.equals("(Agregar Nueva)")) {
                            edtCedula.setText(""); edtUsuario.setText(""); edtClave.setText("");
                        } else {
                            JSONObject dat = cuentasObj.optJSONObject(sel);
                            if (dat != null) {
                                tipoSeleccionado[0] = dat.optString("tipo_doc", "V");
                                btnTipoDoc.setText(tipoSeleccionado[0] + " ▼");
                                edtCedula.setText(dat.optString("cedula", ""));
                                edtUsuario.setText(dat.optString("usuario", ""));
                                edtClave.setText(dat.optString("clave", ""));
                            }
                        }
                    }).show();
        });

        builder.setView(layout);
        final AlertDialog dialog = builder.create();

        Button btnEliminar = crearBoton("🗑", "#7F1D1D", "#FCA5A5", 13, () -> {
            String sel = cuentaSeleccionada[0];
            if (!sel.equals("(Agregar Nueva)") && cuentasObj.has(sel)) {
                cuentasObj.remove(sel);
                prefs.edit().putString("cuentas_json", cuentasObj.toString()).apply();
                if (usuario.equals(sel)) {
                    cedula = ""; usuario = ""; contrasena = "";
                    actualizarTextoBotonUsuario();
                }
                log("[SISTEMA] Cuenta eliminada: " + sel);
                dialog.dismiss();
            }
        });
        filaSel.addView(btnEliminar, new LinearLayout.LayoutParams(dp(42), dp(38)));

        layout.addView(filaSel); layout.addView(filaCed); layout.addView(filaUsr); layout.addView(filaClv);

        Button btnGuardar = crearBoton("Guardar", "#0284C7", "#FFFFFF", 13, () -> {
            String u = edtUsuario.getText().toString().trim();
            String c = edtCedula.getText().toString().trim();
            String p = edtClave.getText().toString().trim();
            String t = tipoSeleccionado[0];
            if (u.isEmpty()) return;
            try {
                JSONObject nuevo = new JSONObject();
                nuevo.put("tipo_doc", t); nuevo.put("cedula", c);
                nuevo.put("usuario", u); nuevo.put("clave", p);
                cuentasObj.put(u, nuevo);
                prefs.edit().putString("cuentas_json", cuentasObj.toString()).putString("ultima_cuenta", u).apply();
                tipoDoc = t; cedula = c; usuario = u; contrasena = p;
                actualizarTextoBotonUsuario();
                log("[SISTEMA] Cuenta vinculada: " + u);
                dialog.dismiss();
            } catch (Exception ignored) {}
        });
        layout.addView(btnGuardar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        dialog.show();
    }

    private void log(String msg) {
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        runOnUiThread(() -> {
            txtConsola.append(hora + "  " + msg + "\n");
            scrollConsola.post(() -> scrollConsola.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void sonarAlarma(boolean exito) {
        new Thread(() -> {
            try {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                int repeticiones = exito ? 6 : 3;
                for (int i = 0; i < repeticiones; i++) {
                    tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220);
                    Thread.sleep(300);
                }
                tg.release();
            } catch (Exception ignored) {}
        }).start();
    }

    private void cambiarEstado(EstadoBot nuevoEstado) {
        estadoActual = nuevoEstado;
        tiempoEstadoMs = System.currentTimeMillis();
        accionEnCurso = false;
    }

    private void iniciarBot() {
        if (cedula.isEmpty() || usuario.isEmpty() || contrasena.isEmpty()) {
            log("⚠️ [ERROR] Agrega tus credenciales antes de iniciar.");
            return;
        }
        botActivo = true;
        yaAvisoApertura = false;
        vueltaActual = 0;
        cambiarEstado(EstadoBot.CARGANDO_LOGIN);

        badgeEstado.setText("⬛ VIGILANDO");
        badgeEstado.setTextColor(Color.parseColor("#86EFAC"));
        badgeEstado.setBackground(fondoRedondeado("#14532D", 8));
        btnIniciar.setEnabled(false);
        btnIniciar.setBackground(fondoRedondeado("#14532D", 8));
        btnDetener.setEnabled(true);
        btnDetener.setBackground(fondoRedondeado("#DC2626", 8));

        log("=== VIGILANTE INICIADO ===");
    }

    private void detenerBot() {
        botActivo = false;
        cambiarEstado(EstadoBot.DETENIDO);
        tiempoInicioSesionMs = 0;
        progressRelogin.setProgress(0);
        lblTimerRelogin.setText("--:--");

        badgeEstado.setText("🔴 APAGADO");
        badgeEstado.setTextColor(Color.parseColor("#FCA5A5"));
        badgeEstado.setBackground(fondoRedondeado("#450A0A", 8));
        btnIniciar.setEnabled(true);
        btnIniciar.setBackground(fondoRedondeado("#16A34A", 8));
        btnDetener.setEnabled(false);
        btnDetener.setBackground(fondoRedondeado("#7F1D1D", 8));

        log("=== VIGILANTE DETENIDO ===");
    }

    private void recargarManual() {
        log("🔄 [MANUAL] Recargando navegador interno...");
        webView.reload();
        if (botActivo) { cambiarEstado(EstadoBot.CARGANDO_LOGIN); }
    }

    private void evaluarJS(String script, ValueCallback<String> callback) {
        webView.evaluateJavascript("(function() { try { " + script + " } catch(e) { return 'ERR:' + e.message; } })()", callback);
    }

    private void iniciarCicloReloj() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (botActivo) {
                    actualizarBarraProgresoRelogin();
                    ejecutarPasoMaquinaEstados();
                }
                handler.postDelayed(this, 600);
            }
        }, 600);
    }

    private void actualizarBarraProgresoRelogin() {
        if (tiempoInicioSesionMs > 0 && estadoActual == EstadoBot.VIGILANDO) {
            long transcurridoSeg = (System.currentTimeMillis() - tiempoInicioSesionMs) / 1000;
            long totalSeg = minutosRelogin * 60L;
            int prog = (int) Math.min(100, (transcurridoSeg * 100) / totalSeg);
            progressRelogin.setProgress(prog);

            long restante = Math.max(0, totalSeg - transcurridoSeg);
            long mins = restante / 60;
            long secs = restante % 60;
            lblTimerRelogin.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));

            if (transcurridoSeg >= totalSeg) {
                log("⏰ Tiempo de relogin alcanzado. Cerrando y reentrando...");
                cambiarEstado(EstadoBot.CERRANDO_SESION);
                intentoLogout = 0;
            }
        }
    }

    private void ejecutarPasoMaquinaEstados() {
        long ahora = System.currentTimeMillis();

        String jsInspector =
                "var ready = (document.readyState === 'complete');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && btnX.offsetParent !== null) { btnX.click(); }" +
                "var btnOk = document.querySelector('.swal2-confirm');" +
                "if (btnOk && btnOk.innerText && btnOk.innerText.trim() === 'OK') { btnOk.click(); }" +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector('a.btn-tesoro-green');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var opsDiv = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "var inicioListo = !!(ready && !cargandoTxt && (menuInt || opsDiv || btnLogout) && (bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || bodyTxt.indexOf('operaciones') !== -1 || bodyTxt.indexOf('últ. conexión') !== -1 || bodyTxt.indexOf('ult. conexión') !== -1));" +
                "var tablaIntLista = !!(ready && !cargandoTxt && (bodyTxt.indexOf('operaciones de intervención') !== -1 || (ord && ord.offsetParent !== null)));" +
                "var inconv = bodyTxt.indexOf('presentando inconvenientes') !== -1;" +
                "var exito = bodyTxt.indexOf('registrada exitosamente') !== -1;" +
                "var sinCupo = bodyTxt.indexOf('no hay asignación disponible') !== -1;" +
                "return JSON.stringify({" +
                "  ready: ready," +
                "  cargando: cargandoTxt," +
                "  doc: !!(doc && usr && doc.offsetWidth > 0 && usr.offsetWidth > 0 && !cargandoTxt)," +
                "  pass: !!(pass && pass.offsetWidth > 0 && !cargandoTxt)," +
                "  inicio: inicioListo," +
                "  tablaInt: tablaIntLista," +
                "  monto: !!(monto && monto.offsetWidth > 0 && !cargandoTxt)," +
                "  ord: !!(ord && ord.offsetWidth > 0 && !cargandoTxt)," +
                "  inconv: inconv, exito: exito, sinCupo: sinCupo" +
                "});";

        evaluarJS(jsInspector, raw -> {
            if (!botActivo || raw == null || raw.equals("null")) return;
            try {
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                }
                JSONObject st = new JSONObject(clean);
                boolean nav100 = (progresoWeb >= 100);
                boolean ready = st.optBoolean("ready") && nav100;
                boolean cargando = st.optBoolean("cargando");

                procesarEstadoConDatos(
                        ahora,
                        ready && !cargando,
                        st.optBoolean("doc"),
                        st.optBoolean("pass"),
                        st.optBoolean("inicio"),
                        st.optBoolean("tablaInt"),
                        st.optBoolean("monto"),
                        st.optBoolean("ord"),
                        st.optBoolean("inconv"),
                        st.optBoolean("exito"),
                        st.optBoolean("sinCupo")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            long ahora, boolean paginaCompleta, boolean visDoc, boolean visPass,
            boolean visInicioCompleto, boolean visTablaInt, boolean visMonto,
            boolean visOrd, boolean hayInconv, boolean hayExito, boolean haySinCupo
    ) {
        String cedSafe = cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (estadoActual) {

            case CARGANDO_LOGIN:
                if (paginaCompleta && visInicioCompleto) {
                    tiempoInicioSesionMs = ahora;
                    log("✅ Página de inicio detectada y cargada. Estabilizando 2s...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (paginaCompleta && visPass) {
                    log("✅ Pantalla de contraseña cargada. Estabilizando 2s...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (paginaCompleta && visDoc) {
                    log("✅ Login cargado completo. Esperando 2s para estabilizar...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - tiempoEstadoMs > 25000) {
                    tiempoEstadoMs = ahora;
                    log("⏳ Esperando carga completa del login...");
                    webView.loadUrl(URL_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_LOGIN:
                if (!visDoc) {
                    cambiarEstado(EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (ahora - tiempoEstadoMs >= 2000) {
                    cambiarEstado(EstadoBot.ENVIANDO_FASE1);
                }
                break;

            case ENVIANDO_FASE1:
                if (!accionEnCurso) {
                    accionEnCurso = true;
                    log("👉 [FASE 1] Escribiendo Cédula y Usuario...");
                    String jsFase1 =
                            "var sel = document.querySelector('#tipodocben');" +
                            "if(sel) { for(var i=0; i<sel.options.length; i++) { if(sel.options[i].text.trim().toUpperCase() === '" + tipoDoc + "') { sel.selectedIndex = i; break; } } sel.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var d = document.querySelector('#documento'); var u = document.querySelector('#username');" +
                            "if(d && u) {" +
                            "  d.focus(); setter.call(d, ''); setter.call(u, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, el.value + str.charAt(i)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 30);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(d, '" + cedSafe + "', 0, function() {" +
                            "    setTimeout(function() {" +
                            "      u.focus();" +
                            "      typeStr(u, '" + usrSafe + "', 0, function() {" +
                            "        setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 350);" +
                            "      });" +
                            "    }, 250);" +
                            "  });" +
                            "}";
                    evaluarJS(jsFase1, null);
                    cambiarEstado(EstadoBot.ESPERANDO_CARGA_PASS);
                }
                break;

            case ESPERANDO_CARGA_PASS:
                if (paginaCompleta && visPass) {
                    log("✅ Fase 2 cargada completa. Esperando 2s para estabilizar...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (ahora - tiempoEstadoMs > 18000 && paginaCompleta && visDoc) {
                    log("⚠️ La página no avanzó a contraseña. Reintentando Fase 1...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_PASS:
                if (!visPass) {
                    cambiarEstado(EstadoBot.ESPERANDO_CARGA_PASS);
                    return;
                }
                if (ahora - tiempoEstadoMs >= 2000) {
                    cambiarEstado(EstadoBot.ENVIANDO_FASE2);
                }
                break;

            case ENVIANDO_FASE2:
                if (!accionEnCurso) {
                    accionEnCurso = true;
                    log("👉 [FASE 2] Escribiendo Contraseña y enviando...");
                    String jsFase2 =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  p.focus(); setter.call(p, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, el.value + str.charAt(i)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 30);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(p, '" + clvSafe + "', 0, function() {" +
                            "    setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 350);" +
                            "  });" +
                            "}";
                    evaluarJS(jsFase2, null);
                    log("⏳ Esperando a que cargue la página de Inicio completa...");
                    cambiarEstado(EstadoBot.ESPERANDO_CARGA_INICIO);
                }
                break;

            case ESPERANDO_CARGA_INICIO:
                if (paginaCompleta && visInicioCompleto) {
                    tiempoInicioSesionMs = ahora;
                    log("🎉 ¡Inicio cargado al 100%! Esperando 2s de estabilización...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (ahora - tiempoEstadoMs > 10000 && paginaCompleta && visDoc) {
                    log("⚠️ El banco regresó al login. Reintentando...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_INICIO:
                if (ahora - tiempoEstadoMs >= 2000) {
                    cambiarEstado(EstadoBot.NAVEGANDO_MENU);
                }
                break;

            case NAVEGANDO_MENU:
                if (paginaCompleta && visTablaInt) {
                    log("[OK] Sección de Intervención cargada completa. Vigilando...");
                    cambiarEstado(EstadoBot.VIGILANDO);
                    return;
                }
                if (!accionEnCurso) {
                    accionEnCurso = true;
                    log("📍 Abriendo Operaciones Divisas -> Intervención...");
                    irAIntervencionJS();
                } else if (ahora - tiempoEstadoMs > 8000 && paginaCompleta) {
                    tiempoEstadoMs = ahora;
                    irAIntervencionJS();
                }
                break;

            case VIGILANDO:
                if (paginaCompleta && visDoc) {
                    log("⚠️ Sesión cerrada detectada. Reingresando...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (paginaCompleta && visOrd) {
                    log("🔔 !!! BOTÓN ORDINARIA DETECTADO !!!");
                    if (!yaAvisoApertura) { yaAvisoApertura = true; sonarAlarma(false); }
                    log("👉 Intento 1: Dando clic en Ordinaria...");
                    evaluarJS("var b = document.querySelector('a.btn-tesoro-green'); if(b) b.click();", null);
                    cambiarEstado(EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                if (paginaCompleta && (ahora - tiempoEstadoMs >= intervaloRefresco * 1000L)) {
                    tiempoEstadoMs = ahora;
                    vueltaActual++;
                    log("[vuelta " + vueltaActual + "] Sin botón. Refrescando Intervención...");
                    irAIntervencionJS();
                }
                break;

            case ESPERANDO_FORM_INTENTO1:
                if (paginaCompleta && visMonto && !hayInconv) {
                    log("✅ ¡Formulario cargado en Intento 1! Estabilizando 2s...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - tiempoEstadoMs > 4500)) {
                    if (hayInconv) {
                        log("🚨 Error en Intento 1: 'Presentando inconvenientes'.");
                    } else {
                        log("⚠️ Pantalla en blanco en Intento 1.");
                    }
                    log("🔄 Limpiando pantalla por menú lateral...");
                    irAIntervencionJS();
                    cambiarEstado(EstadoBot.LIMPIANDO_PANTALLA);
                }
                break;

            case LIMPIANDO_PANTALLA:
                if (paginaCompleta && visTablaInt && (ahora - tiempoEstadoMs >= 2500)) {
                    log("👉 Intento 2: Dando clic en Ordinaria tras limpiar vista...");
                    evaluarJS("var b = document.querySelector('a.btn-tesoro-green'); if(b) b.click();", null);
                    cambiarEstado(EstadoBot.ESPERANDO_FORM_INTENTO2);
                } else if (ahora - tiempoEstadoMs > 10000) {
                    evaluarJS("var b = document.querySelector('a.btn-tesoro-green'); if(b) b.click();", null);
                    cambiarEstado(EstadoBot.ESPERANDO_FORM_INTENTO2);
                }
                break;

            case ESPERANDO_FORM_INTENTO2:
                if (paginaCompleta && visMonto && !hayInconv) {
                    log("✅ ¡Formulario cargado en Intento 2! Estabilizando 2s...");
                    sonarAlarma(false);
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - tiempoEstadoMs > 5000)) {
                    log("❌ Ambos intentos quedaron en blanco o con error. Cerrando sesión...");
                    cambiarEstado(EstadoBot.CERRANDO_SESION);
                    intentoLogout = 0;
                }
                break;

            case ESPERANDO_ESTABILIZAR_FORM:
                if (ahora - tiempoEstadoMs >= 2000) {
                    cambiarEstado(EstadoBot.LLENANDO_COMPRA);
                }
                break;

            case LLENANDO_COMPRA:
                if (!accionEnCurso) {
                    accionEnCurso = true;
                    log("📝 Rellenando formulario de compra...");
                    String jsCompra =
                            "var d = document.querySelector('#desde'); if(d && d.options.length > 1) { d.selectedIndex = 1; d.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var h = document.querySelector('#hasta'); if(h && h.options.length > 1) { h.selectedIndex = 1; h.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var o = document.querySelector('#origen'); if(o) { for(var i=0;i<o.options.length;i++){ if(o.options[i].text.includes('" + ORIGEN_FONDOS + "')){ o.selectedIndex=i; break; } } o.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var dst = document.querySelector('#destino'); if(dst) { for(var j=0;j<dst.options.length;j++){ if(dst.options[j].text.includes('" + DESTINO_FONDOS + "')){ dst.selectedIndex=j; break; } } dst.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var m = document.querySelector('#monto');" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "m.focus(); setter.call(m, '');" +
                            "function typeStr(el, str, i) {" +
                            "  if(i < str.length) {" +
                            "    setter.call(el, el.value + str.charAt(i)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "    setTimeout(function(){ typeStr(el, str, i+1); }, 35);" +
                            "  } else {" +
                            "    el.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "    setTimeout(function(){" +
                            "      var col = document.querySelector(\"a[href='javascript:mod.save()']\"); if(col) col.click();" +
                            "      setTimeout(function(){ var conf = document.querySelector('.swal2-confirm'); if(conf) conf.click(); }, 900);" +
                            "    }, 500);" +
                            "  }" +
                            "}" +
                            "setTimeout(function(){ typeStr(m, '" + MONTO_COMPRA + "', 0); }, 300);";
                    evaluarJS(jsCompra, null);
                    cambiarEstado(EstadoBot.COMPRANDO);
                }
                break;

            case COMPRANDO:
                if (hayExito) {
                    log("🎉 ¡ORDEN MONTADA Y REGISTRADA CON ÉXITO!");
                    sonarAlarma(true);
                    detenerBot();
                    return;
                }
                if (haySinCupo) {
                    log("⚠️ Sin asignación disponible en este intento.");
                    cambiarEstado(EstadoBot.VIGILANDO);
                    return;
                }
                if (ahora - tiempoEstadoMs > 10000) {
                    log("✅ Orden procesada. Retomando vigilancia...");
                    sonarAlarma(true);
                    cambiarEstado(EstadoBot.VIGILANDO);
                }
                break;

            case CERRANDO_SESION:
                if (paginaCompleta && visDoc) {
                    log("✅ Sesión cerrada y login cargado. Estabilizando 2s...");
                    cambiarEstado(EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (!accionEnCurso) {
                    accionEnCurso = true;
                    intentoLogout++;
                    tiempoEstadoMs = ahora;
                    log("🔒 Intento #" + intentoLogout + " de cerrar sesión...");
                    String jsLogout =
                            "if (typeof Logout === 'function') { Logout(); } else { var b = document.querySelector(\"a[href='javascript:Logout()'], header ul li:nth-child(4) a\"); if(b) b.click(); }" +
                            "setTimeout(function(){ var si = document.querySelector('button.swal2-confirm'); if(si) si.click(); }, 700);";
                    evaluarJS(jsLogout, null);
                } else if (ahora - tiempoEstadoMs > 8000) {
                    log("❌ El banco no cerró en 8s. Cortando red interna...");
                    cambiarEstado(EstadoBot.REINICIANDO_RED);
                    webView.getSettings().setBlockNetworkLoads(true);
                    webView.stopLoading();
                }
                break;

            case REINICIANDO_RED:
                if (ahora - tiempoEstadoMs >= 3000) {
                    log("🌐 Reconectando navegador interno y esperando login completo...");
                    webView.getSettings().setBlockNetworkLoads(false);
                    CookieManager.getInstance().removeSessionCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    webView.loadUrl(URL_LOGIN);
                    cambiarEstado(EstadoBot.CARGANDO_LOGIN);
                }
                break;
        }
    }

    private void irAIntervencionJS() {
        String jsMenu =
                "var sub = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var ops = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "if(ops) { ops.click(); }" +
                "setTimeout(function(){" +
                "  var sub2 = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "  if(sub2) { sub2.click(); } else { window.location.hash = '#/int'; }" +
                "}, 800);";
        evaluarJS(jsMenu, null);
    }
}
