package com.vigilantebt.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;

    private boolean botActivo = false;
    private boolean yaAvisoApertura = false;
    private long tiempoInicioSesionMs = 0;
    private long ultimoAccionMs = 0;
    private int vueltaActual = 0;

    private enum EstadoBot {
        DETENIDO,
        CARGANDO_LOGIN,
        ENVIANDO_FASE1,
        ESPERANDO_FASE2,
        NAVEGANDO_MENU,
        VIGILANDO,
        INTENTO_1_ORDINARIA,
        LIMPIANDO_PANTALLA,
        INTENTO_2_ORDINARIA,
        COMPRANDO,
        CERRANDO_SESION,
        REINICIANDO_RED
    }

    private EstadoBot estadoActual = EstadoBot.DETENIDO;
    private int intentoLogout = 0;
    private long inicioEsperaEstadoMs = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    private void construirInterfazCompleta() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F1115"));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. BARRA SUPERIOR
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(10), dp(12), dp(6));

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
        panelControlContainer.setPadding(dp(12), dp(8), dp(12), dp(8));
        panelControlContainer.setBackground(fondoRedondeado("#18181B", 14));
        LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.15f);
        lpPanel.setMargins(dp(8), dp(2), dp(8), dp(4));

        // Botón Usuario + Ojo Privacidad
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

        // Frecuencia de Refresco
        LinearLayout filaRef = new LinearLayout(this);
        filaRef.setGravity(Gravity.CENTER_VERTICAL);
        TextView tRef = crearTexto("Frecuencia de Refresco:", "#F4F4F5", 11, true);
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
        filaRef.addView(tRef, new LinearLayout.LayoutParams(dp(135), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRef.addView(sldRef, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRef.addView(lblRefrescoVal, new LinearLayout.LayoutParams(dp(45), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRef);

        // Reinicio de Sesión
        LinearLayout filaRel = new LinearLayout(this);
        filaRel.setGravity(Gravity.CENTER_VERTICAL);
        TextView tRel = crearTexto("Reinicio de Sesión:", "#F4F4F5", 11, true);
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
        filaRel.addView(tRel, new LinearLayout.LayoutParams(dp(135), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRel.addView(sldRel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRel.addView(lblReloginVal, new LinearLayout.LayoutParams(dp(45), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRel);

        // Progreso Reinicio
        LinearLayout filaProg = new LinearLayout(this);
        filaProg.setGravity(Gravity.CENTER_VERTICAL);
        filaProg.setPadding(0, dp(4), 0, 0);
        TextView tProg = crearTexto("Progreso Reinicio:", "#A1A1AA", 10, true);
        progressRelogin = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressRelogin.setMax(100);
        progressRelogin.setProgress(0);
        lblTimerRelogin = crearTexto("--:--", "#38BDF8", 10, true);
        lblTimerRelogin.setGravity(Gravity.END);
        filaProg.addView(tProg, new LinearLayout.LayoutParams(dp(135), ViewGroup.LayoutParams.WRAP_CONTENT));
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

        // Cabecera Consola (Con botón ▼, Limpiar y Guardar)
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

        root.addView(panelControlContainer, lpPanel);

        // 3. NAVEGADOR INTEGRADO (WEBVIEW)
        webViewContainer = new LinearLayout(this);
        webViewContainer.setOrientation(LinearLayout.VERTICAL);
        webViewContainer.setBackground(fondoRedondeado("#18181B", 14));
        webViewContainer.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams lpWeb = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lpWeb.setMargins(dp(8), dp(2), dp(8), dp(8));

        webView = new WebView(this);
        webViewContainer.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(webViewContainer, lpWeb);
        setContentView(root);
    }

    private void toggleConsola() {
        consolaVisible = !consolaVisible;
        scrollConsola.setVisibility(consolaVisible ? View.VISIBLE : View.GONE);
        btnFoldConsola.setText(consolaVisible ? "▼" : "▲");
    }

    private void copiarLogPortapapeles() {
        String logTxt = txtConsola.getText().toString().trim();
        if (logTxt.isEmpty()) return;
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null) {
            cb.setPrimaryClip(ClipData.newPlainText("Log Vigilante BT", logTxt));
            Toast.makeText(this, "Log copiado al portapapeles", Toast.LENGTH_SHORT).show();
            log("[SISTEMA] Log copiado y guardado en portapapeles.");
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
        b.setOnClickListener(v -> accion.run());
        return b;
    }

    private void cambiarVista(int modo) {
        if (modo == 0) {
            panelControlContainer.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.VISIBLE);
            btnVistaDividida.setBackground(fondoRedondeado("#0284C7", 8));
            btnVistaPanel.setBackground(fondoRedondeado("#27272A", 8));
            btnVistaWeb.setBackground(fondoRedondeado("#27272A", 8));
        } else if (modo == 1) {
            panelControlContainer.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.GONE);
            btnVistaDividida.setBackground(fondoRedondeado("#27272A", 8));
            btnVistaPanel.setBackground(fondoRedondeado("#0284C7", 8));
            btnVistaWeb.setBackground(fondoRedondeado("#27272A", 8));
        } else {
            panelControlContainer.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);
            btnVistaDividida.setBackground(fondoRedondeado("#27272A", 8));
            btnVistaPanel.setBackground(fondoRedondeado("#27272A", 8));
            btnVistaWeb.setBackground(fondoRedondeado("#0284C7", 8));
        }
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

        webView.setWebChromeClient(new WebChromeClient());
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
        try {
            return new JSONObject(prefs.getString("cuentas_json", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
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
        while (keys.hasNext()) {
            listaNombres.add(keys.next());
        }

        final String[] cuentaSeleccionada = {
                (!usuario.isEmpty() && listaNombres.contains(usuario)) ? usuario : "(Agregar Nueva)"
        };
        final String[] tipoSeleccionado = {tipoDoc.isEmpty() ? "V" : tipoDoc};

        // Selector de Cuentas + Botón Eliminar
        LinearLayout filaSel = new LinearLayout(this);
        filaSel.setOrientation(LinearLayout.HORIZONTAL);

        final Button btnSelectCuenta = crearBoton(cuentaSeleccionada[0] + "  ▼", "#27272A", "#F4F4F5", 12, null);
        LinearLayout.LayoutParams lpSel = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpSel.setMargins(0, 0, dp(6), 0);
        filaSel.addView(btnSelectCuenta, lpSel);

        // Campos de Cédula, Usuario y Contraseña con botones de Ojo 👁
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

        // Fila Usuario con Ojo
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

        // Fila Contraseña con Ojo
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

        // Llenar datos si hay cuenta seleccionada
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
                            edtCedula.setText("");
                            edtUsuario.setText("");
                            edtClave.setText("");
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
                    cedula = "";
                    usuario = "";
                    contrasena = "";
                    actualizarTextoBotonUsuario();
                }
                log("[SISTEMA] Cuenta eliminada: " + sel);
                dialog.dismiss();
            }
        });
        filaSel.addView(btnEliminar, new LinearLayout.LayoutParams(dp(42), dp(38)));

        layout.addView(filaSel);
        layout.addView(filaCed);
        layout.addView(filaUsr);
        layout.addView(filaClv);

        Button btnGuardar = crearBoton("Guardar", "#0284C7", "#FFFFFF", 13, () -> {
            String u = edtUsuario.getText().toString().trim();
            String c = edtCedula.getText().toString().trim();
            String p = edtClave.getText().toString().trim();
            String t = tipoSeleccionado[0];
            if (u.isEmpty()) return;

            try {
                JSONObject nuevo = new JSONObject();
                nuevo.put("tipo_doc", t);
                nuevo.put("cedula", c);
                nuevo.put("usuario", u);
                nuevo.put("clave", p);
                cuentasObj.put(u, nuevo);
                prefs.edit()
                        .putString("cuentas_json", cuentasObj.toString())
                        .putString("ultima_cuenta", u)
                        .apply();

                tipoDoc = t;
                cedula = c;
                usuario = u;
                contrasena = p;
                actualizarTextoBotonUsuario();
                log("[SISTEMA] Cuenta vinculada: " + u);
                dialog.dismiss();
            } catch (Exception ignored) {}
        });

        layout.addView(btnGuardar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

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

    private void iniciarBot() {
        if (cedula.isEmpty() || usuario.isEmpty() || contrasena.isEmpty()) {
            log("⚠️ [ERROR] Agrega tus credenciales antes de iniciar.");
            return;
        }
        botActivo = true;
        yaAvisoApertura = false;
        vueltaActual = 0;
        estadoActual = EstadoBot.CARGANDO_LOGIN;
        ultimoAccionMs = 0;

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
        estadoActual = EstadoBot.DETENIDO;
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
        if (botActivo) {
            estadoActual = EstadoBot.CARGANDO_LOGIN;
            ultimoAccionMs = System.currentTimeMillis();
        }
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
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void actualizarBarraProgresoRelogin() {
        if (tiempoInicioSesionMs > 0) {
            long transcurridoSeg = (System.currentTimeMillis() - tiempoInicioSesionMs) / 1000;
            long totalSeg = minutosRelogin * 60L;
            int prog = (int) Math.min(100, (transcurridoSeg * 100) / totalSeg);
            progressRelogin.setProgress(prog);

            long restante = Math.max(0, totalSeg - transcurridoSeg);
            long mins = restante / 60;
            long secs = restante % 60;
            lblTimerRelogin.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));

            if (transcurridoSeg >= totalSeg && estadoActual == EstadoBot.VIGILANDO) {
                log("⏰ Tiempo de relogin alcanzado. Cerrando y reentrando...");
                iniciarCierreSesion();
            }
        }
    }

    private void iniciarCierreSesion() {
        estadoActual = EstadoBot.CERRANDO_SESION;
        intentoLogout = 0;
        inicioEsperaEstadoMs = 0;
    }

    private void ejecutarPasoMaquinaEstados() {
        long ahora = System.currentTimeMillis();

        String jsInspector =
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && btnX.offsetParent !== null) { btnX.click(); }" +
                "var btnOk = document.querySelector('.swal2-confirm');" +
                "if (btnOk && btnOk.innerText && btnOk.innerText.trim() === 'OK') { btnOk.click(); }" +
                "var doc = document.querySelector('#documento');" +
                "var pass = document.querySelector('#password');" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector('a.btn-tesoro-green');" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var inconv = bodyTxt.indexOf('presentando inconvenientes') !== -1;" +
                "var exito = bodyTxt.indexOf('registrada exitosamente') !== -1;" +
                "var sinCupo = bodyTxt.indexOf('no hay asignación disponible') !== -1;" +
                "return JSON.stringify({" +
                "  doc: !!(doc && doc.offsetParent !== null)," +
                "  pass: !!(pass && pass.offsetParent !== null)," +
                "  monto: !!(monto && monto.offsetParent !== null)," +
                "  ord: !!(ord && ord.offsetParent !== null)," +
                "  menuInt: !!(menuInt)," +
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
                procesarEstadoConDatos(
                        ahora,
                        st.optBoolean("doc"),
                        st.optBoolean("pass"),
                        st.optBoolean("monto"),
                        st.optBoolean("ord"),
                        st.optBoolean("menuInt"),
                        st.optBoolean("inconv"),
                        st.optBoolean("exito"),
                        st.optBoolean("sinCupo")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            long ahora, boolean visDoc, boolean visPass, boolean visMonto,
            boolean visOrd, boolean visMenuInt, boolean hayInconv, boolean hayExito, boolean haySinCupo
    ) {
        String cedSafe = cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (estadoActual) {

            case CARGANDO_LOGIN:
                if (visOrd || visMenuInt) {
                    tiempoInicioSesionMs = ahora;
                    estadoActual = EstadoBot.NAVEGANDO_MENU;
                    return;
                }
                if (visPass) {
                    estadoActual = EstadoBot.ESPERANDO_FASE2;
                    return;
                }
                if (visDoc) {
                    estadoActual = EstadoBot.ENVIANDO_FASE1;
                    ultimoAccionMs = 0;
                    return;
                }
                if (ahora - ultimoAccionMs > 12000) {
                    ultimoAccionMs = ahora;
                    log("⏳ Cargando entrada al banco...");
                    webView.loadUrl(URL_LOGIN);
                }
                break;

            case ENVIANDO_FASE1:
                if (visPass) {
                    estadoActual = EstadoBot.ESPERANDO_FASE2;
                    ultimoAccionMs = 0;
                    return;
                }
                if (ahora - ultimoAccionMs > 4000) {
                    ultimoAccionMs = ahora;
                    log("👉 [FASE 1] Rellenando Cédula y Usuario...");
                    String jsFase1 =
                            "var sel = document.querySelector('#tipodocben');" +
                            "if(sel) {" +
                            "  for(var i=0; i<sel.options.length; i++) {" +
                            "    if(sel.options[i].text.trim().toUpperCase() === '" + tipoDoc + "') { sel.selectedIndex = i; break; }" +
                            "  }" +
                            "  sel.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "}" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var d = document.querySelector('#documento');" +
                            "var u = document.querySelector('#username');" +
                            "if(d && u) {" +
                            "  setter.call(d, '" + cedSafe + "'); d.dispatchEvent(new Event('input', {bubbles:true})); d.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  setter.call(u, '" + usrSafe + "'); u.dispatchEvent(new Event('input', {bubbles:true})); u.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  var btn = document.querySelector(\"button[type='submit'], .btn-primary\");" +
                            "  if(btn) btn.click();" +
                            "}";
                    evaluarJS(jsFase1, null);
                }
                break;

            case ESPERANDO_FASE2:
                if (visMenuInt || visOrd) {
                    log("🎉 ✅ ¡Ingreso cargado correctamente!");
                    tiempoInicioSesionMs = ahora;
                    estadoActual = EstadoBot.NAVEGANDO_MENU;
                    ultimoAccionMs = 0;
                    return;
                }
                if (visDoc) {
                    estadoActual = EstadoBot.ENVIANDO_FASE1;
                    return;
                }
                if (visPass && ahora - ultimoAccionMs > 4000) {
                    ultimoAccionMs = ahora;
                    log("👉 [FASE 2] Escribiendo Contraseña...");
                    String jsFase2 =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  setter.call(p, '" + clvSafe + "');" +
                            "  p.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "  p.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  var btn = document.querySelector(\"button[type='submit'], .btn-primary\");" +
                            "  if(btn) btn.click();" +
                            "}";
                    evaluarJS(jsFase2, null);
                }
                break;

            case NAVEGANDO_MENU:
                if (visOrd) {
                    log("[OK] Conectado y vigilando...");
                    estadoActual = EstadoBot.VIGILANDO;
                    ultimoAccionMs = ahora;
                    return;
                }
                if (ahora - ultimoAccionMs > 3000) {
                    ultimoAccionMs = ahora;
                    log("📍 Clic en menú lateral: Operaciones Divisas -> Intervención...");
                    irAIntervencionJS();
                    estadoActual = EstadoBot.VIGILANDO;
                }
                break;

            case VIGILANDO:
                if (visDoc) {
                    log("⚠️ Sesión caída. Reintentando entrar...");
                    estadoActual = EstadoBot.ENVIANDO_FASE1;
                    return;
                }
                if (visOrd) {
                    log("🔔 !!! BOTÓN DETECTADO !!!");
                    if (!yaAvisoApertura) {
                        yaAvisoApertura = true;
                        sonarAlarma(false);
                    }
                    log("👉 Intento 1: Dando clic en Ordinaria...");
                    clicOrdinariaJS();
                    estadoActual = EstadoBot.INTENTO_1_ORDINARIA;
                    inicioEsperaEstadoMs = ahora;
                    return;
                }
                if (ahora - ultimoAccionMs >= intervaloRefresco * 1000L) {
                    ultimoAccionMs = ahora;
                    vueltaActual++;
                    log("[vuelta " + vueltaActual + "] Sin botón. Refrescando página...");
                    irAIntervencionJS();
                }
                break;

            case INTENTO_1_ORDINARIA:
                if (ahora - inicioEsperaEstadoMs < 2000) return;
                if (visMonto && !hayInconv) {
                    log("✅ ¡Formulario listo en Intento 1! Procesando compra...");
                    ejecutarCompraJS();
                    estadoActual = EstadoBot.COMPRANDO;
                    inicioEsperaEstadoMs = ahora;
                    return;
                }
                if (hayInconv) {
                    log("🚨 Error detectado en Intento 1: 'Presentando inconvenientes'.");
                } else {
                    log("⚠️ Pantalla en blanco en Intento 1.");
                }
                log("🔄 Aplicando limpieza de pantalla reentrando por menú lateral...");
                irAIntervencionJS();
                estadoActual = EstadoBot.LIMPIANDO_PANTALLA;
                inicioEsperaEstadoMs = ahora;
                break;

            case LIMPIANDO_PANTALLA:
                if (ahora - inicioEsperaEstadoMs < 2500) return;
                log("👉 Intento 2: Dando clic en Ordinaria tras limpiar pantalla...");
                clicOrdinariaJS();
                estadoActual = EstadoBot.INTENTO_2_ORDINARIA;
                inicioEsperaEstadoMs = ahora;
                break;

            case INTENTO_2_ORDINARIA:
                if (ahora - inicioEsperaEstadoMs < 2200) return;
                if (visMonto && !hayInconv) {
                    log("✅ ¡Formulario cargado en Intento 2! Montando compra...");
                    sonarAlarma(false);
                    ejecutarCompraJS();
                    estadoActual = EstadoBot.COMPRANDO;
                    inicioEsperaEstadoMs = ahora;
                    return;
                }
                log("❌ Ambos intentos quedaron en blanco o con errores. Cerrando sesión...");
                iniciarCierreSesion();
                break;

            case COMPRANDO:
                if (hayExito) {
                    log("🎉 ¡ORDEN MONTADA Y REGISTRADA CON ÉXITO!");
                    sonarAlarma(true);
                    detenerBot();
                    return;
                }
                if (haySinCupo) {
                    log("⚠️ Sin cupo disponible en este intento.");
                    estadoActual = EstadoBot.VIGILANDO;
                    return;
                }
                if (ahora - inicioEsperaEstadoMs > 8000) {
                    log("✅ Operación completada. Verifique estado en la web.");
                    sonarAlarma(true);
                    estadoActual = EstadoBot.VIGILANDO;
                }
                break;

            case CERRANDO_SESION:
                if (visDoc) {
                    log("✅ Sesión cerrada con éxito. Reingresando...");
                    estadoActual = EstadoBot.ENVIANDO_FASE1;
                    ultimoAccionMs = 0;
                    return;
                }
                if (inicioEsperaEstadoMs == 0) {
                    intentoLogout++;
                    inicioEsperaEstadoMs = ahora;
                    log("🔒 Intento #" + intentoLogout + " de salida por la interfaz...");
                    String jsLogout =
                            "if (typeof Logout === 'function') { Logout(); }" +
                            "else {" +
                            "  var b = document.querySelector(\"a[href='javascript:Logout()'], header ul li:nth-child(4) a\");" +
                            "  if(b) b.click();" +
                            "}" +
                            "setTimeout(function(){" +
                            "  var si = document.querySelector('button.swal2-confirm');" +
                            "  if(si) si.click();" +
                            "}, 500);";
                    evaluarJS(jsLogout, null);
                } else if (ahora - inicioEsperaEstadoMs > 8000) {
                    log("❌ El banco no respondió en 8s. Forzando reinicio de red interno...");
                    estadoActual = EstadoBot.REINICIANDO_RED;
                    inicioEsperaEstadoMs = ahora;
                    webView.getSettings().setBlockNetworkLoads(true);
                    webView.stopLoading();
                }
                break;

            case REINICIANDO_RED:
                if (ahora - inicioEsperaEstadoMs >= 3000) {
                    log("🌐 Reconectando red del navegador y cargando login...");
                    webView.getSettings().setBlockNetworkLoads(false);
                    CookieManager.getInstance().removeSessionCookies(null);
                    WebStorage.getInstance().deleteAllData();
                    webView.loadUrl(URL_LOGIN);
                    estadoActual = EstadoBot.CARGANDO_LOGIN;
                    ultimoAccionMs = ahora;
                }
                break;
        }
    }

    private void irAIntervencionJS() {
        String jsMenu =
                "var ops = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "if(ops) ops.click();" +
                "setTimeout(function(){" +
                "  var sub = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "  if(sub) sub.click();" +
                "}, 600);";
        evaluarJS(jsMenu, null);
    }

    private void clicOrdinariaJS() {
        evaluarJS("var b = document.querySelector('a.btn-tesoro-green'); if(b) b.click();", null);
    }

    private void ejecutarCompraJS() {
        log("📝 Rellenando campos de la compra (" + MONTO_COMPRA + ")...");
        String jsCompra =
                "var d = document.querySelector('#desde'); if(d && d.options.length > 1) { d.selectedIndex = 1; d.dispatchEvent(new Event('change', {bubbles:true})); }" +
                "var h = document.querySelector('#hasta'); if(h && h.options.length > 1) { h.selectedIndex = 1; h.dispatchEvent(new Event('change', {bubbles:true})); }" +
                "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                "var m = document.querySelector('#monto'); if(m) { setter.call(m, '" + MONTO_COMPRA + "'); m.dispatchEvent(new Event('input', {bubbles:true})); m.dispatchEvent(new Event('change', {bubbles:true})); }" +
                "var o = document.querySelector('#origen'); if(o) { for(var i=0;i<o.options.length;i++){ if(o.options[i].text.includes('" + ORIGEN_FONDOS + "')){ o.selectedIndex=i; break; } } o.dispatchEvent(new Event('change', {bubbles:true})); }" +
                "var dst = document.querySelector('#destino'); if(dst) { for(var j=0;j<dst.options.length;j++){ if(dst.options[j].text.includes('" + DESTINO_FONDOS + "')){ dst.selectedIndex=j; break; } } dst.dispatchEvent(new Event('change', {bubbles:true})); }" +
                "setTimeout(function(){" +
                "  var col = document.querySelector(\"a[href='javascript:mod.save()']\");" +
                "  if(col) col.click();" +
                "  setTimeout(function(){" +
                "    var conf = document.querySelector('.swal2-confirm');" +
                "    if(conf) conf.click();" +
                "  }, 800);" +
                "}, 400);";
        evaluarJS(jsCompra, null);
    }
}
