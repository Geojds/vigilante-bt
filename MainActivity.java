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
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
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
    private static final int MAX_CUENTAS_SIMULTANEAS = 4;
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};

    private LinearLayout panelControlContainer;
    private LinearLayout webViewContainer;
    private LinearLayout barraPestanasMulti;
    private FrameLayout escenarioMultiWeb;
    private LinearLayout contenedorMosaico;
    private LinearLayout filaMosaicoSup, filaMosaicoInf;

    private EditText edtUrlBar;
    private TextView btnChromeBack, btnChromeReload, btnChromeZoom, btnChromeNet;
    private ProgressBar chromeProgressBar;
    private LinearLayout barraZoomDesplegable;
    private TextView lblZoomActualBarra;

    private TextView badgeEstado, lblRefrescoVal, lblReloginVal, lblTimerRelogin, txtConsola;
    private ScrollView scrollConsola;
    private Button btnCuenta, btnOjoPrivacidad, btnIniciar, btnRecargar, btnDetener, btnFoldConsola;
    private Button btnVistaDividida, btnVistaPanel, btnVistaWeb;
    private ProgressBar progressRelogin;

    private boolean usuarioOculto = false;
    private boolean consolaVisible = true;
    private int modoVistaActual = 0;

    // -1 = Vista Mosaico (todas las activas a la vez), 0..3 = Pestaña individual C1..C4
    private int slotEnfocado = -1;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;
    private int nivelZoom = 70;

    private boolean botActivo = false;
    private boolean yaAvisoApertura = false;
    private boolean microCorteEnCurso = false;
    private boolean avisoSinInternetMostrado = false;
    private ConnectivityManager.NetworkCallback networkCallback;

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
        ESPERANDO_SINCRONIZAR_CIERRE,
        CERRANDO_SESION,
        REINICIANDO_RED,
        ESPERANDO_SINCRONIZAR_LOGIN
    }

    // Clase que representa cada una de las 4 instancias simultáneas
    private class SesionBot {
        final int slotIndex;
        boolean activa = false;
        String tipoDoc = "V";
        String cedula = "";
        String usuario = "";
        String contrasena = "";

        LinearLayout tarjetaCompleta;
        LinearLayout barraCabeceraMini;
        TextView lblTituloMini;
        TextView lblEstadoMini;
        WebView webView;
        Button btnTabSelector;

        EstadoBot estadoActual = EstadoBot.DETENIDO;
        int progresoWeb = 0;
        long tiempoInicioSesionMs = 0;
        long tiempoEstadoMs = 0;
        boolean accionEnCurso = false;
        int vueltaActual = 0;
        int intentoLogout = 0;
        boolean compraCompletada = false;
        String ultimaUrlValida = URL_LOGIN;

        SesionBot(int idx) {
            this.slotIndex = idx;
        }

        String etiquetaCorta() {
            if (usuario.isEmpty()) return "C" + (slotIndex + 1);
            if (usuarioOculto) return "C" + (slotIndex + 1) + ":****";
            String u = usuario.length() > 8 ? usuario.substring(0, 8) : usuario;
            return "C" + (slotIndex + 1) + ":" + u;
        }
    }

    private final SesionBot[] sesiones = new SesionBot[MAX_CUENTAS_SIMULTANEAS];
    private Button btnTabMosaico;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#18181B"));
        getWindow().setNavigationBarColor(Color.parseColor("#18181B"));
        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);
        nivelZoom = prefs.getInt("zoom_web", 70);

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            sesiones[i] = new SesionBot(i);
        }

        construirInterfazCompleta();
        inicializarLosCuatroNavegadores();
        registrarMonitorPasivoRed();
        cargarCuentasActivasGuardadas();
        iniciarCicloReloj();
    }

    @Override
    protected void onDestroy() {
        try {
            ConnectivityManager cmNet = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cmNet != null && networkCallback != null) {
                cmNet.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}

        try {
            for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
                WebView wv = sesiones[i].webView;
                if (wv != null) {
                    wv.evaluateJavascript("if(typeof Logout==='function'){Logout();}", null);
                    wv.stopLoading();
                    wv.clearCache(true);
                    wv.clearHistory();
                    wv.clearFormData();
                }
                limpiarDatosPerfilAislado(i);
            }
            CookieManager cm = CookieManager.getInstance();
            cm.removeAllCookies(null);
            cm.flush();
            WebStorage.getInstance().deleteAllData();
        } catch (Exception ignored) {}
        super.onDestroy();
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

    private GradientDrawable fondoRedondeadoConBorde(String colorHex, String bordeHex, int radioDp, int grosorBordeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(dp(radioDp));
        gd.setStroke(dp(grosorBordeDp), Color.parseColor(bordeHex));
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

        // 1. BARRA SUPERIOR
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

        // Botón Cuentas (1 a 4) + Ojo
        LinearLayout filaCuenta = new LinearLayout(this);
        filaCuenta.setOrientation(LinearLayout.HORIZONTAL);
        filaCuenta.setGravity(Gravity.CENTER_VERTICAL);

        btnCuenta = crearBoton("👥 Cuentas Simultáneas (0/4)", "#27272A", "#F4F4F5", 12, this::mostrarPopupCuenta);
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
        TextView tProg = crearTexto("Cierre Sincronizado:", "#A1A1AA", 10, true);
        progressRelogin = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressRelogin.setMax(100);
        progressRelogin.setProgress(0);
        lblTimerRelogin = crearTexto("--:--", "#38BDF8", 10, true);
        lblTimerRelogin.setGravity(Gravity.END);
        filaProg.addView(tProg, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaProg.addView(progressRelogin, new LinearLayout.LayoutParams(0, dp(10), 1f));
        filaProg.addView(lblTimerRelogin, new LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));
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

        TextView lblConsola = crearTexto("Consola Multi-Cuenta", "#F4F4F5", 12, true);
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
        txtConsola.setTextSize(10.5f);
        txtConsola.setTypeface(Typeface.MONOSPACE);
        scrollConsola.addView(txtConsola);
        panelControlContainer.addView(scrollConsola, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(panelControlContainer);

        // 3. NAVEGADOR MULTI-CUENTA ESTILO GOOGLE CHROME
        webViewContainer = new LinearLayout(this);
        webViewContainer.setOrientation(LinearLayout.VERTICAL);
        webViewContainer.setBackground(fondoRedondeadoConBorde("#202124", "#2E3035", 12, 1));
        webViewContainer.setPadding(dp(2), dp(2), dp(2), dp(2));

        // Barra de Pestañas Multi-Cuenta (⊞ Todas | C1 | C2 | C3 | C4)
        barraPestanasMulti = new LinearLayout(this);
        barraPestanasMulti.setOrientation(LinearLayout.HORIZONTAL);
        barraPestanasMulti.setGravity(Gravity.CENTER_VERTICAL);
        barraPestanasMulti.setPadding(dp(4), dp(3), dp(4), dp(1));
        barraPestanasMulti.setBackgroundColor(Color.parseColor("#18191C"));

        btnTabMosaico = crearBoton("⊞ Todas", "#0284C7", "#FFFFFF", 10, () -> cambiarSlotEnfocado(-1));
        LinearLayout.LayoutParams lpTabM = new LinearLayout.LayoutParams(0, dp(26), 1.15f);
        lpTabM.setMargins(dp(2), 0, dp(2), 0);
        barraPestanasMulti.addView(btnTabMosaico, lpTabM);

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            final int idx = i;
            Button bTab = crearBoton("C" + (i + 1), "#27272A", "#A1A1AA", 10, () -> cambiarSlotEnfocado(idx));
            sesiones[i].btnTabSelector = bTab;
            LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, dp(26), 1f);
            lpT.setMargins(dp(2), 0, dp(2), 0);
            barraPestanasMulti.addView(bTab, lpT);
        }
        webViewContainer.addView(barraPestanasMulti, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Cabecera Omnibox estilo Chrome
        LinearLayout chromeToolbar = new LinearLayout(this);
        chromeToolbar.setOrientation(LinearLayout.HORIZONTAL);
        chromeToolbar.setGravity(Gravity.CENTER_VERTICAL);
        chromeToolbar.setPadding(dp(4), dp(4), dp(4), dp(4));
        chromeToolbar.setBackground(fondoRedondeado("#202124", 10));

        btnChromeBack = crearIconoChrome("←", () -> {
            for (SesionBot s : obtenerSesionesObjetivoBarra()) {
                if (s.webView != null) {
                    s.webView.stopLoading();
                    if (s.webView.canGoBack()) s.webView.goBack();
                }
            }
            log("◀ Regresando página anterior...");
        });
        chromeToolbar.addView(btnChromeBack, new LinearLayout.LayoutParams(dp(32), dp(32)));

        btnChromeReload = crearIconoChrome("↻", this::forzarRecargaEstiloChrome);
        chromeToolbar.addView(btnChromeReload, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout omniboxPill = new LinearLayout(this);
        omniboxPill.setOrientation(LinearLayout.HORIZONTAL);
        omniboxPill.setGravity(Gravity.CENTER_VERTICAL);
        omniboxPill.setPadding(dp(10), dp(2), dp(6), dp(2));
        omniboxPill.setBackground(fondoRedondeado("#303134", 18));
        LinearLayout.LayoutParams lpOmnibox = new LinearLayout.LayoutParams(0, dp(32), 1f);
        lpOmnibox.setMargins(dp(4), 0, dp(4), 0);

        TextView iconLock = new TextView(this);
        iconLock.setText("🔒");
        iconLock.setTextSize(10);
        iconLock.setPadding(0, 0, dp(6), 0);
        omniboxPill.addView(iconLock);

        edtUrlBar = new EditText(this);
        edtUrlBar.setSingleLine(true);
        edtUrlBar.setHorizontallyScrolling(true);
        edtUrlBar.setHint("https://tesoroenlinea.bt.com.ve/");
        edtUrlBar.setHintTextColor(Color.parseColor("#9AA0A6"));
        edtUrlBar.setTextColor(Color.parseColor("#E8EAED"));
        edtUrlBar.setTextSize(11.5f);
        edtUrlBar.setPadding(0, 0, dp(6), 0);
        edtUrlBar.setBackground(null);
        edtUrlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        edtUrlBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        edtUrlBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navegarDesdeBarraUrl();
                return true;
            }
            return false;
        });
        omniboxPill.addView(edtUrlBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        btnChromeZoom = new TextView(this);
        btnChromeZoom.setText(nivelZoom + "%");
        btnChromeZoom.setTextColor(Color.parseColor("#8AB4F8"));
        btnChromeZoom.setTextSize(10.5f);
        btnChromeZoom.setTypeface(null, Typeface.BOLD);
        btnChromeZoom.setGravity(Gravity.CENTER);
        btnChromeZoom.setPadding(dp(7), dp(3), dp(7), dp(3));
        btnChromeZoom.setBackground(fondoRedondeado("#202124", 12));
        btnChromeZoom.setOnClickListener(v -> toggleBarraZoomChrome());
        omniboxPill.addView(btnChromeZoom);

        chromeToolbar.addView(omniboxPill, lpOmnibox);

        btnChromeNet = crearIconoChrome("📶", () -> reiniciarConexionInternaWebView(true));
        chromeToolbar.addView(btnChromeNet, new LinearLayout.LayoutParams(dp(32), dp(32)));

        webViewContainer.addView(chromeToolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Sub-barra desplegable de Zoom
        barraZoomDesplegable = new LinearLayout(this);
        barraZoomDesplegable.setOrientation(LinearLayout.HORIZONTAL);
        barraZoomDesplegable.setGravity(Gravity.CENTER);
        barraZoomDesplegable.setPadding(dp(8), dp(4), dp(8), dp(4));
        barraZoomDesplegable.setBackgroundColor(Color.parseColor("#28292C"));
        barraZoomDesplegable.setVisibility(View.GONE);

        Button btnZMenos = crearBoton("➖ Alejar", "#3C4043", "#E8EAED", 11, () -> cambiarZoomWeb(-10));
        lblZoomActualBarra = crearTexto("Zoom: " + nivelZoom + "%", "#8AB4F8", 11, true);
        lblZoomActualBarra.setGravity(Gravity.CENTER);
        Button btnZMas = crearBoton("➕ Acercar", "#3C4043", "#E8EAED", 11, () -> cambiarZoomWeb(10));
        Button btnZReset = crearBoton("70%", "#0284C7", "#FFFFFF", 10, () -> {
            nivelZoom = 70;
            cambiarZoomWeb(0);
        });

        LinearLayout.LayoutParams lpZBtn = new LinearLayout.LayoutParams(dp(78), dp(26));
        lpZBtn.setMargins(dp(4), 0, dp(4), 0);
        barraZoomDesplegable.addView(btnZMenos, lpZBtn);
        barraZoomDesplegable.addView(lblZoomActualBarra, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        barraZoomDesplegable.addView(btnZMas, lpZBtn);
        barraZoomDesplegable.addView(btnZReset, new LinearLayout.LayoutParams(dp(46), dp(26)));

        webViewContainer.addView(barraZoomDesplegable, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Línea azul de carga estilo Chrome
        chromeProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        chromeProgressBar.setMax(100);
        chromeProgressBar.setProgress(0);
        ColorDrawable bgTrack = new ColorDrawable(Color.parseColor("#202124"));
        ClipDrawable fgProg = new ClipDrawable(new ColorDrawable(Color.parseColor("#8AB4F8")), Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{bgTrack, fgProg});
        progressLayer.setId(0, android.R.id.background);
        progressLayer.setId(1, android.R.id.progress);
        chromeProgressBar.setProgressDrawable(progressLayer);
        chromeProgressBar.setVisibility(View.GONE);
        webViewContainer.addView(chromeProgressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        // Escenario que aloja las 4 vistas simultáneas sin pausar ninguna
        escenarioMultiWeb = new FrameLayout(this);
        escenarioMultiWeb.setBackgroundColor(Color.parseColor("#18181B"));

        contenedorMosaico = new LinearLayout(this);
        contenedorMosaico.setOrientation(LinearLayout.VERTICAL);
        contenedorMosaico.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        filaMosaicoSup = new LinearLayout(this);
        filaMosaicoSup.setOrientation(LinearLayout.HORIZONTAL);
        filaMosaicoInf = new LinearLayout(this);
        filaMosaicoInf.setOrientation(LinearLayout.HORIZONTAL);

        contenedorMosaico.addView(filaMosaicoSup, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contenedorMosaico.addView(filaMosaicoInf, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        escenarioMultiWeb.addView(contenedorMosaico);
        webViewContainer.addView(escenarioMultiWeb, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(webViewContainer);
        actualizarDistribucionAlturas();

        frameRoot.addView(root);

        // 4. PANTALLA DE ENTRADA EN GRIS OSCURO (#18181B) CON EL GIF TRANSPARENTE
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

    /**
     * Inicializa los 4 navegadores independientes con perfil aislado (Multi-Profile)
     * para que puedan operar hasta 4 cuentas al mismo tiempo sin cruzar sesiones.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void inicializarLosCuatroNavegadores() {
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            final SesionBot sesion = sesiones[i];

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(fondoRedondeadoConBorde("#18181B", "#2E3035", 6, 1));
            card.setPadding(dp(1), dp(1), dp(1), dp(1));

            LinearLayout headerMini = new LinearLayout(this);
            headerMini.setOrientation(LinearLayout.HORIZONTAL);
            headerMini.setGravity(Gravity.CENTER_VERTICAL);
            headerMini.setBackgroundColor(Color.parseColor("#27272A"));
            headerMini.setPadding(dp(6), dp(2), dp(6), dp(2));
            headerMini.setOnClickListener(v -> cambiarSlotEnfocado(sesion.slotIndex));

            TextView lblTit = crearTexto("C" + (i + 1) + ": Sin cuenta", "#38BDF8", 10, true);
            TextView lblEst = crearTexto("DETENIDO", "#A1A1AA", 9, true);
            lblEst.setGravity(Gravity.END);

            headerMini.addView(lblTit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            headerMini.addView(lblEst, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            WebView wv = new WebView(this);
            wv.setBackgroundColor(Color.parseColor("#18181B"));

            // Asignar perfil aislado si AndroidX WebKit Multi-Profile está disponible
            configurarPerfilAisladoWebView(wv, i);

            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setSupportZoom(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setBlockNetworkLoads(false);
            ws.setTextZoom(nivelZoom);
            ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Slot/" + (i + 1));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(wv, true);

            wv.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    sesion.progresoWeb = newProgress;
                    if (slotEnfocado == sesion.slotIndex || (slotEnfocado == -1 && sesion.slotIndex == 0)) {
                        if (chromeProgressBar != null) {
                            if (newProgress < 100) {
                                chromeProgressBar.setVisibility(View.VISIBLE);
                                chromeProgressBar.setProgress(Math.max(12, newProgress));
                            } else {
                                chromeProgressBar.setProgress(100);
                                handler.postDelayed(() -> chromeProgressBar.setVisibility(View.GONE), 220);
                            }
                        }
                    }
                    if (newProgress >= 55) {
                        aplicarZoomASesion(sesion);
                    }
                }
            });

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if (url != null && url.startsWith("http")) {
                        sesion.ultimaUrlValida = url;
                        if ((slotEnfocado == sesion.slotIndex || slotEnfocado == -1) && !edtUrlBar.hasFocus()) {
                            edtUrlBar.setText(url);
                        }
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url != null && url.startsWith("http")) {
                        sesion.ultimaUrlValida = url;
                        if ((slotEnfocado == sesion.slotIndex || slotEnfocado == -1) && !edtUrlBar.hasFocus()) {
                            edtUrlBar.setText(url);
                        }
                    }
                    aplicarZoomASesion(sesion);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        mantenerPaginaQuietaSinError(view);
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    mantenerPaginaQuietaSinError(view);
                }
            });

            card.addView(headerMini, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(wv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            sesion.tarjetaCompleta = card;
            sesion.barraCabeceraMini = headerMini;
            sesion.lblTituloMini = lblTit;
            sesion.lblEstadoMini = lblEst;
            sesion.webView = wv;

            wv.loadUrl(URL_LOGIN);
        }

        edtUrlBar.setText(URL_LOGIN);
        reorganizarVistasNavegadores();
        log("[SISTEMA] Motor Multi-Cuenta (hasta 4 simultáneas) listo.");
    }

    /**
     * Aísla cada WebView en su propio perfil independiente (Cookies y Storage propios)
     * mediante AndroidX WebKit Multi-Profile cuando está presente en la compilación.
     */
    private void configurarPerfilAisladoWebView(WebView wv, int slotIdx) {
        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method isSupported = featureClass.getMethod("isFeatureSupported", String.class);
            Boolean ok = (Boolean) isSupported.invoke(null, "MULTI_PROFILE");
            if (ok != null && ok) {
                Class<?> compatClass = Class.forName("androidx.webkit.WebViewCompat");
                Method setProfile = compatClass.getMethod("setProfile", WebView.class, String.class);
                setProfile.invoke(null, wv, "VigilanteSlot_" + (slotIdx + 1));
                limpiarDatosPerfilAislado(slotIdx);
            }
        } catch (Throwable ignored) {}
    }

    private void limpiarDatosPerfilAislado(int slotIdx) {
        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method isSupported = featureClass.getMethod("isFeatureSupported", String.class);
            Boolean ok = (Boolean) isSupported.invoke(null, "MULTI_PROFILE");
            if (ok != null && ok) {
                Class<?> storeClass = Class.forName("androidx.webkit.ProfileStore");
                Method getInstance = storeClass.getMethod("getInstance");
                Object store = getInstance.invoke(null);
                Method getOrCreate = storeClass.getMethod("getOrCreateProfile", String.class);
                Object profile = getOrCreate.invoke(store, "VigilanteSlot_" + (slotIdx + 1));
                if (profile != null) {
                    Class<?> profileClass = Class.forName("androidx.webkit.Profile");
                    CookieManager cm = (CookieManager) profileClass.getMethod("getCookieManager").invoke(profile);
                    if (cm != null) {
                        cm.removeAllCookies(null);
                        cm.flush();
                    }
                    WebStorage ws = (WebStorage) profileClass.getMethod("getWebStorage").invoke(profile);
                    if (ws != null) {
                        ws.deleteAllData();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Reorganiza el escenario para ver todas las cuentas activas simultáneamente (Mosaico)
     * o enfocar una pestaña individual sin pausar las demás en segundo plano.
     */
    private void reorganizarVistasNavegadores() {
        filaMosaicoSup.removeAllViews();
        filaMosaicoInf.removeAllViews();
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            ViewGroup parent = (ViewGroup) sesiones[i].tarjetaCompleta.getParent();
            if (parent != null) parent.removeView(sesiones[i].tarjetaCompleta);
        }

        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            activas.add(sesiones[0]);
        }

        // Actualizar estilo de los botones de pestañas arriba
        btnTabMosaico.setText("⊞ Todas (" + activas.size() + ")");
        btnTabMosaico.setBackground(fondoRedondeado(slotEnfocado == -1 ? "#0284C7" : "#27272A", 6));
        btnTabMosaico.setTextColor(Color.parseColor(slotEnfocado == -1 ? "#FFFFFF" : "#A1A1AA"));

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            SesionBot s = sesiones[i];
            s.lblTituloMini.setText(s.etiquetaCorta());
            s.btnTabSelector.setText(s.etiquetaCorta());
            s.btnTabSelector.setVisibility(s.activa || i == 0 ? View.VISIBLE : View.GONE);
            boolean sel = (slotEnfocado == i);
            s.btnTabSelector.setBackground(fondoRedondeado(sel ? "#0284C7" : "#27272A", 6));
            s.btnTabSelector.setTextColor(Color.parseColor(sel ? "#FFFFFF" : "#E4E4E7"));
        }

        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS) {
            // Modo Pestaña Individual: las demás siguen visibles detrás (1x1 px) para que Android no las pause
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.GONE);
            for (SesionBot s : activas) {
                if (s.slotIndex == slotEnfocado) {
                    s.barraCabeceraMini.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams lpFull = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                    filaMosaicoSup.addView(s.tarjetaCompleta, lpFull);
                    if (s.ultimaUrlValida != null && !edtUrlBar.hasFocus()) {
                        edtUrlBar.setText(s.ultimaUrlValida);
                    }
                } else {
                    LinearLayout.LayoutParams lpBg = new LinearLayout.LayoutParams(1, 1);
                    filaMosaicoSup.addView(s.tarjetaCompleta, lpBg);
                }
            }
            return;
        }

        // Modo Mosaico Simultáneo (1, 2, 3 o 4 cuentas en pantalla al mismo tiempo)
        int n = activas.size();
        if (n == 1) {
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.GONE);
            SesionBot s0 = activas.get(0);
            s0.barraCabeceraMini.setVisibility(View.VISIBLE);
            filaMosaicoSup.addView(s0.tarjetaCompleta, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        } else if (n == 2) {
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.GONE);
            for (int i = 0; i < 2; i++) {
                SesionBot s = activas.get(i);
                s.barraCabeceraMini.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                filaMosaicoSup.addView(s.tarjetaCompleta, lp);
            }
        } else if (n == 3) {
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.VISIBLE);
            for (int i = 0; i < 2; i++) {
                SesionBot s = activas.get(i);
                s.barraCabeceraMini.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                filaMosaicoSup.addView(s.tarjetaCompleta, lp);
            }
            SesionBot s2 = activas.get(2);
            s2.barraCabeceraMini.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lpInf = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lpInf.setMargins(dp(1), dp(1), dp(1), dp(1));
            filaMosaicoInf.addView(s2.tarjetaCompleta, lpInf);
        } else {
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.VISIBLE);
            for (int i = 0; i < 2; i++) {
                SesionBot s = activas.get(i);
                s.barraCabeceraMini.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                filaMosaicoSup.addView(s.tarjetaCompleta, lp);
            }
            for (int i = 2; i < 4; i++) {
                SesionBot s = activas.get(i);
                s.barraCabeceraMini.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                filaMosaicoInf.addView(s.tarjetaCompleta, lp);
            }
        }
    }

    private void cambiarSlotEnfocado(int nuevoSlot) {
        slotEnfocado = nuevoSlot;
        reorganizarVistasNavegadores();
    }

    private ArrayList<SesionBot> obtenerSesionesActivas() {
        ArrayList<SesionBot> lista = new ArrayList<>();
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            if (sesiones[i].activa && !sesiones[i].usuario.isEmpty()) {
                lista.add(sesiones[i]);
            }
        }
        return lista;
    }

    private ArrayList<SesionBot> obtenerSesionesObjetivoBarra() {
        ArrayList<SesionBot> lista = new ArrayList<>();
        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS) {
            lista.add(sesiones[slotEnfocado]);
        } else {
            lista.addAll(obtenerSesionesActivas());
            if (lista.isEmpty()) lista.add(sesiones[0]);
        }
        return lista;
    }

    private TextView crearIconoChrome(String simbolo, Runnable accion) {
        TextView tv = new TextView(this);
        tv.setText(simbolo);
        tv.setTextColor(Color.parseColor("#E8EAED"));
        tv.setTextSize(17);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(fondoRedondeado("#202124", 16));
        tv.setOnClickListener(v -> {
            tv.setBackground(fondoRedondeado("#3C4043", 16));
            handler.postDelayed(() -> tv.setBackground(fondoRedondeado("#202124", 16)), 180);
            if (accion != null) accion.run();
        });
        return tv;
    }

    private void toggleBarraZoomChrome() {
        if (barraZoomDesplegable.getVisibility() == View.VISIBLE) {
            barraZoomDesplegable.setVisibility(View.GONE);
        } else {
            barraZoomDesplegable.setVisibility(View.VISIBLE);
        }
    }

    private void forzarRecargaEstiloChrome() {
        if (!hayInternetDisponible()) {
            log("⏸️ Sin internet en el teléfono. Las páginas se mantienen quietas sin recargar.");
            return;
        }
        chromeProgressBar.setVisibility(View.VISIBLE);
        chromeProgressBar.setProgress(18);
        for (SesionBot s : obtenerSesionesObjetivoBarra()) {
            if (s.webView != null) {
                s.webView.stopLoading();
                String urlActual = s.webView.getUrl();
                if (urlActual == null || urlActual.isEmpty() || urlActual.startsWith("about:") || urlActual.startsWith("chrome")) {
                    urlActual = (s.ultimaUrlValida != null && !s.ultimaUrlValida.isEmpty()) ? s.ultimaUrlValida : URL_LOGIN;
                    s.webView.loadUrl(urlActual);
                } else {
                    s.webView.reload();
                }
            }
        }
        log("🔄 Recargando vista activa...");
    }

    private void cambiarZoomWeb(int delta) {
        nivelZoom = Math.max(40, Math.min(150, nivelZoom + delta));
        prefs.edit().putInt("zoom_web", nivelZoom).apply();
        if (btnChromeZoom != null) btnChromeZoom.setText(nivelZoom + "%");
        if (lblZoomActualBarra != null) lblZoomActualBarra.setText("Zoom: " + nivelZoom + "%");
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            aplicarZoomASesion(sesiones[i]);
        }
        log("🔍 Zoom ajustado en todas las cuentas a: " + nivelZoom + "%");
    }

    private void aplicarZoomASesion(SesionBot s) {
        if (s == null || s.webView == null) return;
        try {
            s.webView.getSettings().setTextZoom(nivelZoom);
            String jsZoom = "if(document.body){ document.body.style.zoom = '" + nivelZoom + "%'; }";
            evaluarJS(s, jsZoom, null);
        } catch (Exception ignored) {}
    }

    private void navegarDesdeBarraUrl() {
        String raw = edtUrlBar.getText().toString().trim();
        if (raw.isEmpty()) return;
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            if (raw.contains(".") && !raw.contains(" ")) {
                raw = "https://" + raw;
            } else {
                raw = "https://www.google.com/search?q=" + raw.replace(" ", "+");
            }
        }
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(edtUrlBar.getWindowToken(), 0);
        } catch (Exception ignored) {}
        edtUrlBar.clearFocus();
        if (!hayInternetDisponible()) {
            log("⏸️ No hay conexión a internet.");
            return;
        }
        chromeProgressBar.setVisibility(View.VISIBLE);
        chromeProgressBar.setProgress(15);
        for (SesionBot s : obtenerSesionesObjetivoBarra()) {
            s.ultimaUrlValida = raw;
            if (s.webView != null) {
                s.webView.stopLoading();
                s.webView.loadUrl(raw);
            }
        }
        log("🌐 Navegando a: " + raw);
    }

    // =========================================================================================
    // CONTROL INTERNO DE RED DEL NAVEGADOR (CERO PERMISOS DE WI-FI / PÁGINA SIEMPRE QUIETA)
    // =========================================================================================
    @SuppressWarnings("deprecation")
    private boolean hayInternetDisponible() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network net = cm.getActiveNetwork();
                if (net == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                return ni != null && ni.isConnected();
            }
        } catch (Exception ignored) {
            return true;
        }
    }

    private void registrarMonitorPasivoRed() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(Network network) {
                    runOnUiThread(() -> {
                        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
                            if (sesiones[i].webView != null) {
                                sesiones[i].webView.stopLoading();
                            }
                        }
                        if (!avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = true;
                            log("⏸️ [RED] Internet desconectado. Todas las cuentas congeladas en su sitio sin error.");
                        }
                    });
                }

                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        if (avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = false;
                            log("📶 [RED] Internet detectado nuevamente. Retomando todas las cuentas...");
                            long ahora = System.currentTimeMillis();
                            for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
                                sesiones[i].tiempoEstadoMs = ahora;
                                sesiones[i].accionEnCurso = false;
                            }
                        }
                    });
                }
            };
            cm.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception ignored) {}
    }

    /**
     * Corta y reconecta internamente el flujo de red de los navegadores activos sin pedir permisos
     * de Wi-Fi y manteniendo todas las páginas quietas en pantalla.
     */
    private void reiniciarConexionInternaWebView(boolean esManual) {
        if (microCorteEnCurso) return;
        microCorteEnCurso = true;
        log("📶 Cortando y reconectando red interna en los navegadores activos (sin permisos)...");

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            WebView wv = sesiones[i].webView;
            if (wv != null) {
                try {
                    wv.stopLoading();
                    wv.getSettings().setBlockNetworkLoads(true);
                    wv.setNetworkAvailable(false);
                    evaluarJS(sesiones[i], "if(window.stop){try{window.stop();}catch(e){}}", null);
                } catch (Exception ignored) {}
            }
        }

        handler.postDelayed(() -> {
            for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
                WebView wv = sesiones[i].webView;
                if (wv != null) {
                    try {
                        wv.getSettings().setBlockNetworkLoads(false);
                        wv.setNetworkAvailable(true);
                    } catch (Exception ignored) {}
                }
                if (esManual && botActivo && sesiones[i].estadoActual == EstadoBot.CERRANDO_SESION) {
                    sesiones[i].accionEnCurso = false;
                }
            }
            microCorteEnCurso = false;
            log("✅ Red interna reconectada en todas las cuentas.");
        }, 1500);
    }

    private void mantenerPaginaQuietaSinError(WebView view) {
        try {
            view.stopLoading();
            if (view.canGoBack()) {
                view.goBack();
            }
        } catch (Exception ignored) {}
    }

    private void actualizarDistribucionAlturas() {
        if (modoVistaActual == 0) {
            panelControlContainer.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.VISIBLE);

            if (consolaVisible) {
                scrollConsola.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.02f);
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
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.08f);
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
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackground(fondoRedondeado(bgHex, 8));
        b.setOnClickListener(v -> { if (accion != null) accion.run(); });
        return b;
    }

    // =========================================================================================
    // GESTIÓN DE HASTA 4 CUENTAS SIMULTÁNEAS
    // =========================================================================================
    private void togglePrivacidadUsuario() {
        usuarioOculto = !usuarioOculto;
        actualizarTextoBotonUsuario();
        reorganizarVistasNavegadores();
    }

    private void actualizarTextoBotonUsuario() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            btnCuenta.setText("👥 Agregar / Elegir Cuentas (0/4)");
            btnOjoPrivacidad.setText("👁");
            return;
        }
        if (usuarioOculto) {
            btnCuenta.setText("👥 " + activas.size() + " Cuentas Activas (*********)");
            btnOjoPrivacidad.setText("🔒");
        } else if (activas.size() == 1) {
            btnCuenta.setText("👤 1 Activa: " + activas.get(0).usuario);
            btnOjoPrivacidad.setText("👁");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("👥 ").append(activas.size()).append(" Activas: ");
            for (int i = 0; i < activas.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(activas.get(i).usuario);
            }
            btnCuenta.setText(sb.toString());
            btnOjoPrivacidad.setText("👁");
        }
    }

    private JSONObject obtenerCuentasGuardadas() {
        try { return new JSONObject(prefs.getString("cuentas_json", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    private ArrayList<String> obtenerNombresCuentasActivasPrefs() {
        ArrayList<String> res = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("cuentas_activas_arr", "[]"));
            for (int i = 0; i < arr.length() && res.size() < MAX_CUENTAS_SIMULTANEAS; i++) {
                String u = arr.optString(i, "");
                if (!u.isEmpty() && !res.contains(u)) res.add(u);
            }
        } catch (Exception ignored) {}
        if (res.isEmpty()) {
            String ultima = prefs.getString("ultima_cuenta", "");
            if (!ultima.isEmpty()) res.add(ultima);
        }
        return res;
    }

    private void guardarNombresCuentasActivasPrefs(ArrayList<String> activas) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < activas.size() && i < MAX_CUENTAS_SIMULTANEAS; i++) {
            arr.put(activas.get(i));
        }
        prefs.edit().putString("cuentas_activas_arr", arr.toString()).apply();
    }

    private void cargarCuentasActivasGuardadas() {
        JSONObject todas = obtenerCuentasGuardadas();
        ArrayList<String> nombresActivos = obtenerNombresCuentasActivasPrefs();

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            sesiones[i].activa = false;
            sesiones[i].tipoDoc = "V";
            sesiones[i].cedula = "";
            sesiones[i].usuario = "";
            sesiones[i].contrasena = "";
        }

        int slot = 0;
        for (String nombre : nombresActivos) {
            if (slot >= MAX_CUENTAS_SIMULTANEAS) break;
            if (todas.has(nombre)) {
                JSONObject d = todas.optJSONObject(nombre);
                if (d != null) {
                    sesiones[slot].activa = true;
                    sesiones[slot].tipoDoc = d.optString("tipo_doc", "V");
                    sesiones[slot].cedula = d.optString("cedula", "");
                    sesiones[slot].usuario = d.optString("usuario", "");
                    sesiones[slot].contrasena = d.optString("clave", "");
                    slot++;
                }
            }
        }

        actualizarTextoBotonUsuario();
        reorganizarVistasNavegadores();
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
        ScrollView scrollPopup = new ScrollView(this);
        scrollPopup.setBackgroundColor(Color.parseColor("#18181B"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(14), dp(16), dp(14));
        layout.setBackgroundColor(Color.parseColor("#18181B"));
        scrollPopup.addView(layout);

        TextView titulo = crearTexto("Gestión de Cuentas (Hasta 4 Simultáneas)", "#F4F4F5", 15, true);
        titulo.setPadding(0, 0, 0, dp(8));
        layout.addView(titulo);

        final JSONObject cuentasObj = obtenerCuentasGuardadas();
        final ArrayList<String> activasSeleccionadas = obtenerNombresCuentasActivasPrefs();

        // SECCIÓN 1: Casillas para marcar de 1 a 4 cuentas simultáneas
        TextView subActivas = crearTexto("1. Marca las cuentas que comprarán al mismo tiempo (Máx. 4):", "#38BDF8", 11, true);
        subActivas.setPadding(0, 0, 0, dp(4));
        layout.addView(subActivas);

        final LinearLayout contenedorChecks = new LinearLayout(this);
        contenedorChecks.setOrientation(LinearLayout.VERTICAL);
        contenedorChecks.setPadding(dp(8), dp(6), dp(8), dp(6));
        contenedorChecks.setBackground(fondoRedondeado("#27272A", 8));
        layout.addView(contenedorChecks);

        final Runnable[] refrescarListaCheckboxes = new Runnable[1];
        refrescarListaCheckboxes[0] = () -> {
            contenedorChecks.removeAllViews();
            Iterator<String> kIt = cuentasObj.keys();
            boolean hayAlguna = false;
            while (kIt.hasNext()) {
                hayAlguna = true;
                final String usrKey = kIt.next();
                JSONObject objC = cuentasObj.optJSONObject(usrKey);
                String cedInfo = objC != null ? (objC.optString("tipo_doc", "V") + "-" + objC.optString("cedula", "")) : "";

                CheckBox cb = new CheckBox(this);
                cb.setText(usrKey + "  (" + cedInfo + ")");
                cb.setTextColor(Color.parseColor("#F4F4F5"));
                cb.setTextSize(12);
                cb.setChecked(activasSeleccionadas.contains(usrKey));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (activasSeleccionadas.size() >= MAX_CUENTAS_SIMULTANEAS && !activasSeleccionadas.contains(usrKey)) {
                            buttonView.setChecked(false);
                            Toast.makeText(this, "Máximo 4 cuentas simultáneas permitidas", Toast.LENGTH_SHORT).show();
                        } else if (!activasSeleccionadas.contains(usrKey)) {
                            activasSeleccionadas.add(usrKey);
                        }
                    } else {
                        activasSeleccionadas.remove(usrKey);
                    }
                });
                contenedorChecks.addView(cb);
            }
            if (!hayAlguna) {
                TextView tvVacio = crearTexto("No hay cuentas guardadas todavía. Agrega una abajo.", "#A1A1AA", 11, false);
                contenedorChecks.addView(tvVacio);
            }
        };
        refrescarListaCheckboxes[0].run();

        // SECCIÓN 2: Crear o Editar Cuenta
        TextView subEditor = crearTexto("2. Agregar nueva cuenta o editar existente:", "#38BDF8", 11, true);
        subEditor.setPadding(0, dp(10), 0, dp(4));
        layout.addView(subEditor);

        final ArrayList<String> listaNombres = new ArrayList<>();
        final Runnable actualizarSelectorNombres = () -> {
            listaNombres.clear();
            listaNombres.add("(Agregar Nueva)");
            Iterator<String> keys = cuentasObj.keys();
            while (keys.hasNext()) { listaNombres.add(keys.next()); }
        };
        actualizarSelectorNombres.run();

        final String[] cuentaSeleccionada = {"(Agregar Nueva)"};
        final String[] tipoSeleccionado = {"V"};

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

        btnSelectCuenta.setOnClickListener(v -> {
            actualizarSelectorNombres.run();
            String[] items = listaNombres.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Seleccionar Cuenta para Editar")
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

        builder.setView(scrollPopup);
        final AlertDialog dialog = builder.create();

        Button btnEliminar = crearBoton("🗑", "#7F1D1D", "#FCA5A5", 13, () -> {
            String sel = cuentaSeleccionada[0];
            if (!sel.equals("(Agregar Nueva)") && cuentasObj.has(sel)) {
                cuentasObj.remove(sel);
                activasSeleccionadas.remove(sel);
                prefs.edit().putString("cuentas_json", cuentasObj.toString()).apply();
                guardarNombresCuentasActivasPrefs(activasSeleccionadas);
                cuentaSeleccionada[0] = "(Agregar Nueva)";
                btnSelectCuenta.setText("(Agregar Nueva)  ▼");
                edtCedula.setText(""); edtUsuario.setText(""); edtClave.setText("");
                refrescarListaCheckboxes[0].run();
                cargarCuentasActivasGuardadas();
                log("[SISTEMA] Cuenta eliminada: " + sel);
            }
        });
        filaSel.addView(btnEliminar, new LinearLayout.LayoutParams(dp(42), dp(38)));

        layout.addView(filaSel);
        layout.addView(filaCed);
        layout.addView(filaUsr);
        layout.addView(filaClv);

        Button btnGuardarCuenta = crearBoton("➕ Guardar Cuenta en la Lista", "#3F3F46", "#FFFFFF", 12, () -> {
            String u = edtUsuario.getText().toString().trim();
            String c = edtCedula.getText().toString().trim();
            String p = edtClave.getText().toString().trim();
            String t = tipoSeleccionado[0];
            if (u.isEmpty() || c.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Completa Cédula, Usuario y Contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject nuevo = new JSONObject();
                nuevo.put("tipo_doc", t);
                nuevo.put("cedula", c);
                nuevo.put("usuario", u);
                nuevo.put("clave", p);
                cuentasObj.put(u, nuevo);
                if (!activasSeleccionadas.contains(u) && activasSeleccionadas.size() < MAX_CUENTAS_SIMULTANEAS) {
                    activasSeleccionadas.add(u);
                }
                prefs.edit().putString("cuentas_json", cuentasObj.toString()).putString("ultima_cuenta", u).apply();
                guardarNombresCuentasActivasPrefs(activasSeleccionadas);
                edtCedula.setText(""); edtUsuario.setText(""); edtClave.setText("");
                cuentaSeleccionada[0] = "(Agregar Nueva)";
                btnSelectCuenta.setText("(Agregar Nueva)  ▼");
                refrescarListaCheckboxes[0].run();
                Toast.makeText(this, "Cuenta guardada: " + u, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        });
        LinearLayout.LayoutParams lpBtnAdd = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        lpBtnAdd.setMargins(0, 0, 0, dp(8));
        layout.addView(btnGuardarCuenta, lpBtnAdd);

        Button btnAplicar = crearBoton("✅ Aplicar Cuentas Seleccionadas", "#0284C7", "#FFFFFF", 13, () -> {
            // Si el usuario escribió una cuenta en los campos y dio directo a Aplicar, la guarda también
            String uPend = edtUsuario.getText().toString().trim();
            String cPend = edtCedula.getText().toString().trim();
            String pPend = edtClave.getText().toString().trim();
            if (!uPend.isEmpty() && !cPend.isEmpty() && !pPend.isEmpty()) {
                try {
                    JSONObject nuevo = new JSONObject();
                    nuevo.put("tipo_doc", tipoSeleccionado[0]);
                    nuevo.put("cedula", cPend);
                    nuevo.put("usuario", uPend);
                    nuevo.put("clave", pPend);
                    cuentasObj.put(uPend, nuevo);
                    if (!activasSeleccionadas.contains(uPend) && activasSeleccionadas.size() < MAX_CUENTAS_SIMULTANEAS) {
                        activasSeleccionadas.add(uPend);
                    }
                    prefs.edit().putString("cuentas_json", cuentasObj.toString()).putString("ultima_cuenta", uPend).apply();
                } catch (Exception ignored) {}
            }
            guardarNombresCuentasActivasPrefs(activasSeleccionadas);
            cargarCuentasActivasGuardadas();
            log("[SISTEMA] Cuentas activas configuradas: " + activasSeleccionadas.size() + " simultánea(s).");
            dialog.dismiss();
        });
        layout.addView(btnAplicar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        dialog.show();
    }

    private void log(String msg) {
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        runOnUiThread(() -> {
            txtConsola.append(hora + "  " + msg + "\n");
            scrollConsola.post(() -> scrollConsola.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void logCuenta(SesionBot s, String msg) {
        log("[" + s.etiquetaCorta() + "] " + msg);
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

    private void cambiarEstado(SesionBot s, EstadoBot nuevoEstado) {
        s.estadoActual = nuevoEstado;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.accionEnCurso = false;
        if (s.lblEstadoMini != null) {
            s.lblEstadoMini.setText(nombreCortoEstado(nuevoEstado));
            if (nuevoEstado == EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE || nuevoEstado == EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN) {
                s.lblEstadoMini.setTextColor(Color.parseColor("#FACC15"));
            } else if (nuevoEstado == EstadoBot.VIGILANDO || nuevoEstado == EstadoBot.COMPRANDO) {
                s.lblEstadoMini.setTextColor(Color.parseColor("#4ADE80"));
            } else {
                s.lblEstadoMini.setTextColor(Color.parseColor("#38BDF8"));
            }
        }
    }

    private String nombreCortoEstado(EstadoBot st) {
        switch (st) {
            case DETENIDO: return "DETENIDO";
            case CARGANDO_LOGIN:
            case ESPERANDO_ESTABILIZAR_LOGIN: return "LOGIN";
            case ENVIANDO_FASE1: return "USUARIO";
            case ESPERANDO_CARGA_PASS:
            case ESPERANDO_ESTABILIZAR_PASS:
            case ENVIANDO_FASE2: return "CLAVE";
            case ESPERANDO_CARGA_INICIO:
            case ESPERANDO_ESTABILIZAR_INICIO: return "ENTRANDO";
            case NAVEGANDO_MENU: return "MENÚ";
            case VIGILANDO: return "VIGILANDO";
            case ESPERANDO_FORM_INTENTO1: return "INTENTO 1";
            case LIMPIANDO_PANTALLA: return "LIMPIANDO";
            case ESPERANDO_FORM_INTENTO2: return "INTENTO 2";
            case ESPERANDO_ESTABILIZAR_FORM:
            case LLENANDO_COMPRA:
            case COMPRANDO: return "COMPRANDO";
            case ESPERANDO_SINCRONIZAR_CIERRE: return "⏳ ESPERANDO OTRAS";
            case CERRANDO_SESION: return "SALIENDO";
            case REINICIANDO_RED: return "RED INTERNA";
            case ESPERANDO_SINCRONIZAR_LOGIN: return "⏳ LISTA LOGIN";
            default: return "";
        }
    }

    private void iniciarBot() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            log("⚠️ [ERROR] Selecciona al menos 1 cuenta activa antes de iniciar.");
            mostrarPopupCuenta();
            return;
        }
        botActivo = true;
        yaAvisoApertura = false;

        for (SesionBot s : activas) {
            s.vueltaActual = 0;
            s.intentoLogout = 0;
            s.tiempoInicioSesionMs = 0;
            s.compraCompletada = false;
            cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
        }

        badgeEstado.setText("⬛ VIGILANDO (" + activas.size() + ")");
        badgeEstado.setTextColor(Color.parseColor("#86EFAC"));
        badgeEstado.setBackground(fondoRedondeado("#14532D", 8));
        btnIniciar.setEnabled(false);
        btnIniciar.setBackground(fondoRedondeado("#14532D", 8));
        btnDetener.setEnabled(true);
        btnDetener.setBackground(fondoRedondeado("#DC2626", 8));

        reorganizarVistasNavegadores();
        log("=== VIGILANTE INICIADO CON " + activas.size() + " CUENTA(S) SIMULTÁNEA(S) ===");
    }

    private void detenerBot() {
        botActivo = false;
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            cambiarEstado(sesiones[i], EstadoBot.DETENIDO);
            sesiones[i].tiempoInicioSesionMs = 0;
        }
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
        log("🔄 [MANUAL] Refrescando páginas activas...");
        forzarRecargaEstiloChrome();
    }

    private void evaluarJS(SesionBot s, String script, ValueCallback<String> callback) {
        if (s == null || s.webView == null) return;
        s.webView.evaluateJavascript("(function() { try { " + script + " } catch(e) { return 'ERR:' + e.message; } })()", callback);
    }

    private void iniciarCicloReloj() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<SesionBot> activas = obtenerSesionesActivas();
                for (SesionBot s : activas) {
                    aplicarZoomASesion(s);
                }

                if (botActivo) {
                    actualizarBarraProgresoRelogin(activas);
                    if (!microCorteEnCurso && hayInternetDisponible()) {
                        for (SesionBot s : activas) {
                            if (!s.compraCompletada && s.estadoActual != EstadoBot.DETENIDO) {
                                ejecutarPasoMaquinaEstados(s);
                            }
                        }
                        verificarBarrerasSincronizadas(activas);
                    } else if (!hayInternetDisponible()) {
                        long ahora = System.currentTimeMillis();
                        for (SesionBot s : activas) {
                            s.tiempoEstadoMs = ahora;
                        }
                    }
                }
                handler.postDelayed(this, 600);
            }
        }, 600);
    }

    /**
     * Calcula el tiempo de sesión de cada cuenta activa. Cuando a una cuenta se le cumple el tiempo,
     * no cierra sesión sola: se pone en ESPERANDO_SINCRONIZAR_CIERRE hasta que las demás también lleguen.
     */
    private void actualizarBarraProgresoRelogin(ArrayList<SesionBot> activas) {
        long ahora = System.currentTimeMillis();
        long totalSeg = minutosRelogin * 60L;
        long maxTranscurridoSeg = 0;
        int cuentasEnEsperaCierre = 0;
        int cuentasOperando = 0;

        for (SesionBot s : activas) {
            if (s.compraCompletada || s.estadoActual == EstadoBot.DETENIDO) continue;
            cuentasOperando++;

            if (s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE) {
                cuentasEnEsperaCierre++;
            }

            if (s.tiempoInicioSesionMs > 0 && s.estadoActual == EstadoBot.VIGILANDO) {
                long transcurridoSeg = (ahora - s.tiempoInicioSesionMs) / 1000;
                if (transcurridoSeg > maxTranscurridoSeg) {
                    maxTranscurridoSeg = transcurridoSeg;
                }
                if (transcurridoSeg >= totalSeg) {
                    logCuenta(s, "⏰ Tiempo cumplido. Esperando a las demás cuentas para cerrar sesión todas juntas...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE);
                    cuentasEnEsperaCierre++;
                }
            }
        }

        if (cuentasEnEsperaCierre > 0 && cuentasEnEsperaCierre < cuentasOperando) {
            progressRelogin.setProgress(100);
            lblTimerRelogin.setText("SYNC " + cuentasEnEsperaCierre + "/" + cuentasOperando);
        } else if (maxTranscurridoSeg > 0) {
            int prog = (int) Math.min(100, (maxTranscurridoSeg * 100) / totalSeg);
            progressRelogin.setProgress(prog);
            long restante = Math.max(0, totalSeg - maxTranscurridoSeg);
            long mins = restante / 60;
            long secs = restante % 60;
            lblTimerRelogin.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
        }
    }

    /**
     * BARRERA DE SINCRONIZACIÓN:
     * 1) Cuando una cuenta llega a ESPERANDO_SINCRONIZAR_CIERRE, espera sin tocar el botón de salir
     *    hasta que TODAS las demás cuentas con sesión abierta también lleguen al cierre.
     *    En ese instante, todas disparan CERRANDO_SESION exactamente al mismo tiempo.
     * 2) Al terminar de cerrar sesión, esperan en ESPERANDO_SINCRONIZAR_LOGIN a que todas hayan salido
     *    para iniciar el nuevo ciclo juntas.
     */
    private void verificarBarrerasSincronizadas(ArrayList<SesionBot> activas) {
        ArrayList<SesionBot> operativas = new ArrayList<>();
        for (SesionBot s : activas) {
            if (!s.compraCompletada && s.estadoActual != EstadoBot.DETENIDO) {
                operativas.add(s);
            }
        }
        if (operativas.isEmpty()) return;

        // 1. Verificar barrera de Cierre de Sesión simultáneo
        boolean hayAlgunaEsperandoCierre = false;
        boolean todasListasParaCerrar = true;

        for (SesionBot s : operativas) {
            if (s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE) {
                hayAlgunaEsperandoCierre = true;
            } else if (s.estadoActual == EstadoBot.CARGANDO_LOGIN
                    || s.estadoActual == EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN
                    || s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN) {
                // Esta cuenta ya está afuera en el login, no impide el cierre de las que están adentro
            } else {
                todasListasParaCerrar = false;
            }
        }

        if (hayAlgunaEsperandoCierre && todasListasParaCerrar) {
            log("🔒 [SINCRONIZADO] ¡Todas las cuentas listas! Dando clic en Cerrar Sesión simultáneamente...");
            for (SesionBot s : operativas) {
                if (s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE) {
                    s.intentoLogout = 0;
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
            }
        }

        // 2. Verificar si alguna cuenta en CERRANDO_SESION necesita el micro-corte interno de red
        long ahora = System.currentTimeMillis();
        boolean algunaNecesitaCorteRed = false;
        for (SesionBot s : operativas) {
            if (s.estadoActual == EstadoBot.CERRANDO_SESION
                    && s.accionEnCurso
                    && (ahora - s.tiempoEstadoMs > 5500)
                    && (s.intentoLogout % 2 == 0)) {
                algunaNecesitaCorteRed = true;
                break;
            }
        }
        if (algunaNecesitaCorteRed) {
            log("⚠️ Cuenta(s) colgada(s) cerrando sesión. Ejecutando corte/reconexión interna sincronizada...");
            for (SesionBot s : operativas) {
                if (s.estadoActual == EstadoBot.CERRANDO_SESION) {
                    cambiarEstado(s, EstadoBot.REINICIANDO_RED);
                }
            }
            reiniciarConexionInternaWebView(false);
        }

        // 3. Verificar barrera de reingreso en Login después de cerrar sesión
        boolean hayAlgunaEsperandoLogin = false;
        boolean hayAlgunaTodaviaSaliendo = false;
        for (SesionBot s : operativas) {
            if (s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN) {
                hayAlgunaEsperandoLogin = true;
            }
            if (s.estadoActual == EstadoBot.CERRANDO_SESION || s.estadoActual == EstadoBot.REINICIANDO_RED) {
                hayAlgunaTodaviaSaliendo = true;
            }
        }

        if (hayAlgunaEsperandoLogin && !hayAlgunaTodaviaSaliendo) {
            log("🚀 [SINCRONIZADO] Todas las cuentas cerraron sesión. Iniciando login simultáneo...");
            for (SesionBot s : operativas) {
                if (s.estadoActual == EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
            }
        }
    }

    private void ejecutarPasoMaquinaEstados(final SesionBot sesion) {
        final long ahora = System.currentTimeMillis();

        String jsInspector =
                "if(document.body){ document.body.style.zoom = '" + nivelZoom + "%'; }" +
                "var ready = (document.readyState === 'complete');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var modalLogout = (bodyTxt.indexOf('deseas cerrar tu sesión') !== -1 || bodyTxt.indexOf('deseas cerrar tu sesion') !== -1);" +
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && btnX.offsetParent !== null && !modalLogout) { btnX.click(); }" +
                "var btnOk = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim() === 'OK' || el.innerText.trim() === 'Aceptar'));" +
                "if (btnOk && btnOk.offsetWidth > 0 && bodyTxt.indexOf('deseas realizar') === -1 && !modalLogout) { btnOk.click(); }" +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector('a.btn-tesoro-green') || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var opsDiv = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "var inicioListo = !!(ready && !cargandoTxt && (menuInt || opsDiv || btnLogout) && (bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || bodyTxt.indexOf('operaciones') !== -1 || bodyTxt.indexOf('últ. conexión') !== -1 || bodyTxt.indexOf('ult. conexión') !== -1));" +
                "var tablaIntLista = !!(ready && !cargandoTxt && (bodyTxt.indexOf('operaciones de intervención') !== -1 || (ord && ord.offsetWidth > 0)));" +
                "var inconv = (bodyTxt.indexOf('presentando inconvenientes') !== -1 || bodyTxt.indexOf('no podemos procesar su solicitud') !== -1);" +
                "var exito = bodyTxt.indexOf('registrada exitosamente') !== -1;" +
                "var sinCupo = bodyTxt.indexOf('no hay asignación disponible') !== -1;" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1);" +
                "return JSON.stringify({" +
                "  ready: ready," +
                "  cargando: cargandoTxt," +
                "  doc: !!(doc && usr && doc.offsetWidth > 0 && usr.offsetWidth > 0 && !cargandoTxt)," +
                "  pass: !!(pass && pass.offsetWidth > 0 && !cargandoTxt)," +
                "  inicio: inicioListo," +
                "  tablaInt: tablaIntLista," +
                "  monto: !!(monto && monto.offsetWidth > 0 && !cargandoTxt)," +
                "  ord: !!(ord && ord.offsetWidth > 0 && !cargandoTxt)," +
                "  modalLogout: modalLogout," +
                "  inconv: inconv, exito: exito, sinCupo: sinCupo, sesionActivaErr: sesionActivaErr" +
                "});";

        evaluarJS(sesion, jsInspector, raw -> {
            if (!botActivo || raw == null || raw.equals("null")) return;
            try {
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                }
                JSONObject st = new JSONObject(clean);
                boolean nav100 = (sesion.progresoWeb >= 100);
                boolean ready = st.optBoolean("ready") && nav100;
                boolean cargando = st.optBoolean("cargando");

                procesarEstadoConDatos(
                        sesion,
                        ahora,
                        ready && !cargando,
                        st.optBoolean("doc"),
                        st.optBoolean("pass"),
                        st.optBoolean("inicio"),
                        st.optBoolean("tablaInt"),
                        st.optBoolean("monto"),
                        st.optBoolean("ord"),
                        st.optBoolean("modalLogout"),
                        st.optBoolean("inconv"),
                        st.optBoolean("exito"),
                        st.optBoolean("sinCupo"),
                        st.optBoolean("sesionActivaErr")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            SesionBot s, long ahora, boolean paginaCompleta, boolean visDoc, boolean visPass,
            boolean visInicioCompleto, boolean visTablaInt, boolean visMonto,
            boolean visOrd, boolean visModalLogout, boolean hayInconv, boolean hayExito,
            boolean haySinCupo, boolean haySesionActivaErr
    ) {
        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (s.estadoActual) {

            case CARGANDO_LOGIN:
                if (paginaCompleta && visMonto) {
                    logCuenta(s, "✅ Formulario detectado en pantalla. Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "✅ Página de inicio detectada. Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (paginaCompleta && visPass) {
                    logCuenta(s, "✅ Pantalla de contraseña cargada. Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (paginaCompleta && visDoc) {
                    logCuenta(s, "✅ Login cargado completo. Esperando 2s para estabilizar...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 25000) {
                    s.tiempoEstadoMs = ahora;
                    logCuenta(s, "⏳ Esperando carga completa del login...");
                    s.webView.stopLoading();
                    s.webView.loadUrl(URL_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_LOGIN:
                if (!visDoc) {
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 2000) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE1);
                }
                break;

            case ENVIANDO_FASE1:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    logCuenta(s, "👉 [FASE 1] Escribiendo Cédula y Usuario...");
                    String jsFase1 =
                            "var sel = document.querySelector('#tipodocben');" +
                            "if(sel) { for(var i=0; i<sel.options.length; i++) { if(sel.options[i].text.trim().toUpperCase() === '" + s.tipoDoc + "') { sel.selectedIndex = i; break; } } sel.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var d = document.querySelector('#documento'); var u = document.querySelector('#username');" +
                            "if(d && u) {" +
                            "  d.focus(); setter.call(d, ''); setter.call(u, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, str.substring(0, i + 1)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 35);" +
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
                    evaluarJS(s, jsFase1, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                }
                break;

            case ESPERANDO_CARGA_PASS:
                if (paginaCompleta && visPass) {
                    logCuenta(s, "✅ Fase 2 cargada completa. Esperando 2s para estabilizar...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 18000 && paginaCompleta && visDoc) {
                    logCuenta(s, "⚠️ No avanzó a contraseña. Reintentando Fase 1...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_PASS:
                if (!visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 2000) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                }
                break;

            case ENVIANDO_FASE2:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    logCuenta(s, "👉 [FASE 2] Escribiendo Contraseña y enviando...");
                    String jsFase2 =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  p.focus(); setter.call(p, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, str.substring(0, i + 1)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 35);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(p, '" + clvSafe + "', 0, function() {" +
                            "    setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 350);" +
                            "  });" +
                            "}";
                    evaluarJS(s, jsFase2, null);
                    logCuenta(s, "⏳ Esperando carga de Inicio completa...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_INICIO);
                }
                break;

            case ESPERANDO_CARGA_INICIO:
                if (paginaCompleta && visInicioCompleto) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "🎉 ¡Inicio cargado al 100%! Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (haySesionActivaErr) {
                    logCuenta(s, "⚠️ El banco indica sesión activa. Esperando 5s y reintentando...");
                    s.tiempoEstadoMs = ahora;
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 10000 && paginaCompleta && visDoc) {
                    logCuenta(s, "⚠️ Regresó al login. Reintentando...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_INICIO:
                if (ahora - s.tiempoEstadoMs >= 2000) {
                    cambiarEstado(s, EstadoBot.NAVEGANDO_MENU);
                }
                break;

            case NAVEGANDO_MENU:
                if (paginaCompleta && visTablaInt) {
                    logCuenta(s, "[OK] Sección de Intervención cargada. Vigilando...");
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    logCuenta(s, "📍 Abriendo Operaciones Divisas -> Intervención...");
                    irAIntervencionJS(s);
                } else if (ahora - s.tiempoEstadoMs > 8000 && paginaCompleta) {
                    s.tiempoEstadoMs = ahora;
                    irAIntervencionJS(s);
                }
                break;

            case VIGILANDO:
                if (paginaCompleta && visDoc) {
                    logCuenta(s, "⚠️ Sesión cerrada detectada. Reingresando...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (paginaCompleta && visMonto) {
                    logCuenta(s, "✅ Formulario detectado abierto. Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visOrd) {
                    logCuenta(s, "🔔 !!! BOTÓN ORDINARIA DETECTADO !!!");
                    if (!yaAvisoApertura) { yaAvisoApertura = true; sonarAlarma(false); }
                    logCuenta(s, "👉 Intento 1: Dando clic en Ordinaria...");
                    clicarOrdinariaJS(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                if (paginaCompleta && (ahora - s.tiempoEstadoMs >= intervaloRefresco * 1000L)) {
                    s.tiempoEstadoMs = ahora;
                    s.vueltaActual++;
                    logCuenta(s, "[vuelta " + s.vueltaActual + "] Sin botón. Refrescando Intervención...");
                    irAIntervencionJS(s);
                }
                break;

            case ESPERANDO_FORM_INTENTO1:
                if (paginaCompleta && visMonto && !hayInconv) {
                    logCuenta(s, "✅ ¡Formulario cargado en Intento 1! Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 4500)) {
                    logCuenta(s, "⚠️ Error o pantalla en blanco en Intento 1. Limpiando por menú...");
                    irAIntervencionJS(s);
                    cambiarEstado(s, EstadoBot.LIMPIANDO_PANTALLA);
                }
                break;

            case LIMPIANDO_PANTALLA:
                if (paginaCompleta && visTablaInt && (ahora - s.tiempoEstadoMs >= 2500)) {
                    logCuenta(s, "👉 Intento 2: Dando clic en Ordinaria tras limpiar vista...");
                    clicarOrdinariaJS(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO2);
                } else if (ahora - s.tiempoEstadoMs > 10000) {
                    clicarOrdinariaJS(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO2);
                }
                break;

            case ESPERANDO_FORM_INTENTO2:
                if (paginaCompleta && visMonto && !hayInconv) {
                    logCuenta(s, "✅ ¡Formulario cargado en Intento 2! Estabilizando 2s...");
                    sonarAlarma(false);
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 5000)) {
                    logCuenta(s, "❌ Ambos intentos con error. Esperando a las demás cuentas para cerrar sesión juntas...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_SINCRONIZAR_CIERRE);
                }
                break;

            case ESPERANDO_ESTABILIZAR_FORM:
                if (ahora - s.tiempoEstadoMs >= 2000) {
                    cambiarEstado(s, EstadoBot.LLENANDO_COMPRA);
                }
                break;

            case LLENANDO_COMPRA:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    logCuenta(s, "📝 Rellenando formulario y fijando monto exacto de 300,00$...");
                    String jsCompra =
                            "var d = document.querySelector('#desde'); if(d && d.options.length > 1) { d.selectedIndex = 1; d.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var h = document.querySelector('#hasta'); if(h && h.options.length > 1) { h.selectedIndex = 1; h.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var o = document.querySelector('#origen'); if(o) { for(var i=0;i<o.options.length;i++){ if(o.options[i].text.includes('" + ORIGEN_FONDOS + "')){ o.selectedIndex=i; break; } } o.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var dst = document.querySelector('#destino'); if(dst) { for(var j=0;j<dst.options.length;j++){ if(dst.options[j].text.includes('" + DESTINO_FONDOS + "')){ dst.selectedIndex=j; break; } } dst.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var m = document.querySelector('#monto');" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "if(m) {" +
                            "  m.focus();" +
                            "  var pasos = ['3', '30', '300', '" + MONTO_COMPRA + "'];" +
                            "  function escribirPaso(idx) {" +
                            "    if(idx < pasos.length) {" +
                            "      setter.call(m, pasos[idx]);" +
                            "      m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "      setTimeout(function(){ escribirPaso(idx + 1); }, 80);" +
                            "    } else {" +
                            "      if(m.value.indexOf('300') === -1) {" +
                            "        setter.call(m, '" + MONTO_COMPRA + "');" +
                            "        m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      }" +
                            "      m.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "      m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "      setTimeout(function(){" +
                            "        var col = document.querySelector(\"a[href='javascript:mod.save()']\") || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.toLowerCase().includes('colocar orden'));" +
                            "        if(col) col.click();" +
                            "        setTimeout(function(){" +
                            "          var btnSi = document.querySelector('.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                            "          if(btnSi) btnSi.click();" +
                            "        }, 900);" +
                            "      }, 600);" +
                            "    }" +
                            "  }" +
                            "  setTimeout(function(){ escribirPaso(0); }, 250);" +
                            "}";
                    evaluarJS(s, jsCompra, null);
                    cambiarEstado(s, EstadoBot.COMPRANDO);
                }
                break;

            case COMPRANDO:
                if (hayExito) {
                    logCuenta(s, "🎉 ¡ORDEN MONTADA Y REGISTRADA CON ÉXITO!");
                    sonarAlarma(true);
                    s.compraCompletada = true;
                    cambiarEstado(s, EstadoBot.DETENIDO);
                    verificarSiTodasCompraron();
                    return;
                }
                if (haySinCupo) {
                    logCuenta(s, "⚠️ Sin asignación disponible en este intento.");
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                    return;
                }
                if (hayInconv) {
                    logCuenta(s, "⚠️ El banco indicó 'No podemos procesar su solicitud'. Reintentando...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 10000) {
                    logCuenta(s, "✅ Orden procesada. Retomando vigilancia...");
                    sonarAlarma(true);
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                }
                break;

            case ESPERANDO_SINCRONIZAR_CIERRE:
                // Si el banco cerró la sesión por su cuenta mientras esperaba, queda lista en login
                if (paginaCompleta && (visDoc || visPass)) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN);
                    return;
                }
                // Si mientras espera se abre el formulario milagrosamente, lo aprovecha
                if (paginaCompleta && visMonto && !hayInconv) {
                    logCuenta(s, "✅ Formulario abrió mientras esperaba. Comprando...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                }
                break;

            case CERRANDO_SESION:
                if (paginaCompleta && (visDoc || visPass)) {
                    logCuenta(s, "✅ Sesión cerrada correctamente. Esperando al resto en el Login...");
                    if (!visDoc) {
                        s.webView.loadUrl(URL_LOGIN);
                    }
                    cambiarEstado(s, EstadoBot.ESPERANDO_SINCRONIZAR_LOGIN);
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout++;
                    s.tiempoEstadoMs = ahora;
                    if (visModalLogout) {
                        logCuenta(s, "🔒 Confirmando botón 'Sí' en ¿Deseas cerrar tu sesión? (Intento #" + s.intentoLogout + ")...");
                    } else {
                        logCuenta(s, "🔒 Intento #" + s.intentoLogout + " de cerrar sesión...");
                    }
                    ejecutarScriptLogout(s);
                } else if (ahora - s.tiempoEstadoMs > 4800 && s.intentoLogout % 2 == 1) {
                    s.accionEnCurso = false;
                }
                break;

            case REINICIANDO_RED:
                if (!microCorteEnCurso && (ahora - s.tiempoEstadoMs >= 2000)) {
                    logCuenta(s, "🔄 Red interna restablecida. Dando clic en Cerrar Sesión / Sí nuevamente...");
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;

            case ESPERANDO_SINCRONIZAR_LOGIN:
                if (!visDoc && paginaCompleta) {
                    s.webView.loadUrl(URL_LOGIN);
                }
                break;
        }
    }

    private void verificarSiTodasCompraron() {
        for (SesionBot s : obtenerSesionesActivas()) {
            if (!s.compraCompletada) return;
        }
        log("🏆 ¡TODAS LAS CUENTAS ACTIVAS COMPRARON CON ÉXITO!");
        detenerBot();
    }

    private void ejecutarScriptLogout(SesionBot s) {
        String jsLogout =
                "var btnSiExistente = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                "if (btnSiExistente && btnSiExistente.offsetWidth > 0) {" +
                "  btnSiExistente.click();" +
                "} else {" +
                "  var btnSalir = document.querySelector(\"a[href*='Logout' i], a[onclick*='Logout' i], header ul li:nth-child(4) a, .btn-danger\");" +
                "  if (btnSalir) { btnSalir.click(); }" +
                "  if (typeof Logout === 'function') { try { Logout(); } catch(e){} }" +
                "  setTimeout(function(){" +
                "    var si = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí' || el.innerText.trim().toLowerCase() === 'aceptar'));" +
                "    if(si) si.click();" +
                "  }, 650);" +
                "}";
        evaluarJS(s, jsLogout, null);
    }

    private void clicarOrdinariaJS(SesionBot s) {
        String jsOrd =
                "var b = document.querySelector('a.btn-tesoro-green') || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "if(b) b.click();";
        evaluarJS(s, jsOrd, null);
    }

    private void irAIntervencionJS(SesionBot s) {
        String jsMenu =
                "var sub = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var ops = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "if(ops && (!sub || sub.offsetWidth === 0)) { ops.click(); }" +
                "setTimeout(function(){" +
                "  var sub2 = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "  if(sub2) { sub2.click(); } else { window.location.hash = '#/int'; }" +
                "}, 800);";
        evaluarJS(s, jsMenu, null);
    }
}
