package com.vigilantebt.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
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

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/lg";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final int MONTO_MAX_USD = 300;
    private static final String CODIGO_ORIGEN_FONDOS = "1";  // 1 = Sueldos y Salarios
    private static final String CODIGO_DESTINO_FONDOS = "5"; // 5 = Ahorro
    private static final int MAX_CUENTAS_SIMULTANEAS = 4;
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};

    private LinearLayout contenedorControlesSuperiores;
    private LinearLayout barraPestanasCuentas;
    private FrameLayout contenedorConsolaUnificada;
    private LinearLayout contenedorConsolasIndividuales;
    private FrameLayout contenedorMotoresOcultos;

    private TextView badgeEstado, lblRefrescoVal, lblReloginVal, txtConsolaGeneral;
    private ScrollView scrollConsolaGeneral;
    private Button btnCuenta, btnOjoPrivacidad, btnIniciar, btnDetener;
    private Button btnModoUnificado, btnModoSeparado;
    private Button btnTabTodas;

    private boolean usuarioOculto = false;
    // 0 = Vista con Panel + Consola Unificada, 1 = Vista Solo Consolas Separadas por Cuenta
    private int modoVistaActual = 0;

    // -1 = Todas las cuentas, 0..3 = Cuenta individual seleccionada para Iniciar/Detener/Ver
    private int slotEnfocado = -1;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;

    private boolean pulsoNeon = false;
    private boolean yaAvisoApertura = false;
    private boolean avisoSinInternetMostrado = false;
    private ConnectivityManager.NetworkCallback networkCallback;

    private enum EstadoBot {
        DETENIDO,
        CONECTANDO_SERVIDOR,
        ENVIANDO_USUARIO_CHK,
        ESPERANDO_CLAVE_CHK,
        ENVIANDO_CLAVE_EXE,
        VERIFICANDO_SESION,
        VIGILANDO_HTML_INT,
        COMPRANDO_XHR_STORE,
        CERRANDO_SESION,
        RECONECTANDO_SOCKET
    }

    private class SesionBot {
        final int slotIndex;
        boolean activa = false;
        boolean corriendo = false;
        String tipoDoc = "V";
        String cedula = "";
        String usuario = "";
        String contrasena = "";

        // Motor oculto en segundo plano (sin interfaz visual)
        WebView webViewOculto;
        Button btnTabSelector;

        // Tarjeta de consola individual
        LinearLayout tarjetaConsolaIndividual;
        TextView lblTituloConsolaInd;
        TextView lblPasoActualInd;
        ScrollView scrollConsolaIndividual;
        TextView txtConsolaIndividual;

        EstadoBot estadoActual = EstadoBot.DETENIDO;
        boolean cargandoRed = false;
        long tiempoInicioSesionMs = 0;
        long tiempoEstadoMs = 0;
        boolean accionEnCurso = false;
        boolean peticionRedEnCurso = false;
        boolean microCortePropio = false;
        int vueltaActual = 0;
        int intentoCreate = 0;
        int intentoLogout = 0;
        boolean compraCompletada = false;

        SesionBot(int idx) {
            this.slotIndex = idx;
        }

        String nombreMostrado() {
            if (usuario == null || usuario.isEmpty()) return "Sin cuenta";
            if (usuarioOculto) return "********";
            return usuario;
        }

        boolean estaCorriendo() {
            return activa && corriendo && !compraCompletada && estadoActual != EstadoBot.DETENIDO;
        }
    }

    private final SesionBot[] sesiones = new SesionBot[MAX_CUENTAS_SIMULTANEAS];
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private static class CenteredImageSpan extends ImageSpan {
        CenteredImageSpan(Drawable d) {
            super(d);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            Drawable b = getDrawable();
            canvas.save();
            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            int transY = y + (fm.descent + fm.ascent - b.getBounds().bottom) / 2;
            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#18181B"));
        getWindow().setNavigationBarColor(Color.parseColor("#18181B"));
        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            sesiones[i] = new SesionBot(i);
        }

        construirInterfazModoConsola();
        inicializarMotoresOcultosHeadless();
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
                WebView wv = sesiones[i].webViewOculto;
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

    // =========================================================================================
    // MOTOR DE ICONOS VECTORIALES Y LUCES LED NEÓN
    // =========================================================================================
    private Drawable crearCirculoNeon(boolean encendido, boolean faseBrillante, int sizeDp) {
        int s = dp(sizeDp);
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = s / 2f;
        float cy = s / 2f;
        float rMax = s / 2f;

        if (!encendido) {
            RadialGradient glow = new RadialGradient(
                    cx, cy, rMax,
                    new int[]{Color.parseColor("#FF1744"), Color.parseColor("#88FF1744"), Color.TRANSPARENT},
                    new float[]{0.25f, 0.65f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            p.setShader(glow);
            c.drawCircle(cx, cy, rMax, p);

            p.setShader(null);
            p.setColor(Color.parseColor("#FF1744"));
            c.drawCircle(cx, cy, rMax * 0.52f, p);

            p.setColor(Color.parseColor("#FF8A80"));
            c.drawCircle(cx - rMax * 0.12f, cy - rMax * 0.12f, rMax * 0.16f, p);
        } else {
            if (faseBrillante) {
                RadialGradient glow = new RadialGradient(
                        cx, cy, rMax,
                        new int[]{Color.parseColor("#00FF66"), Color.parseColor("#9900FF66"), Color.TRANSPARENT},
                        new float[]{0.30f, 0.72f, 1.0f},
                        Shader.TileMode.CLAMP
                );
                p.setShader(glow);
                c.drawCircle(cx, cy, rMax, p);

                p.setShader(null);
                p.setColor(Color.parseColor("#00FF66"));
                c.drawCircle(cx, cy, rMax * 0.55f, p);

                p.setColor(Color.parseColor("#CCFF90"));
                c.drawCircle(cx - rMax * 0.12f, cy - rMax * 0.12f, rMax * 0.18f, p);
            } else {
                RadialGradient glowDim = new RadialGradient(
                        cx, cy, rMax,
                        new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#3300E676"), Color.TRANSPARENT},
                        new float[]{0.2f, 0.55f, 1.0f},
                        Shader.TileMode.CLAMP
                );
                p.setShader(glowDim);
                c.drawCircle(cx, cy, rMax * 0.85f, p);

                p.setShader(null);
                p.setColor(Color.parseColor("#00C853"));
                p.setAlpha(115);
                c.drawCircle(cx, cy, rMax * 0.44f, p);
            }
        }

        BitmapDrawable bd = new BitmapDrawable(getResources(), bmp);
        bd.setBounds(0, 0, s, s);
        return bd;
    }

    private Drawable crearIconoVectorial(String tipo, String colorHex, int sizeDp) {
        int s = dp(sizeDp);
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int col = Color.parseColor(colorHex);
        p.setColor(col);
        float st = Math.max(2f, s * 0.11f);
        p.setStrokeWidth(st);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        switch (tipo) {
            case "TERMINAL": {
                p.setStyle(Paint.Style.STROKE);
                RectF r = new RectF(s * 0.12f, s * 0.16f, s * 0.88f, s * 0.84f);
                c.drawRoundRect(r, s * 0.12f, s * 0.12f, p);
                c.drawLine(s * 0.26f, s * 0.38f, s * 0.42f, s * 0.50f, p);
                c.drawLine(s * 0.42f, s * 0.50f, s * 0.26f, s * 0.62f, p);
                c.drawLine(s * 0.48f, s * 0.62f, s * 0.68f, s * 0.62f, p);
                break;
            }
            case "PANEL": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.15f, s * 0.32f, s * 0.85f, s * 0.32f, p);
                c.drawLine(s * 0.15f, s * 0.68f, s * 0.85f, s * 0.68f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(s * 0.38f, s * 0.32f, s * 0.13f, p);
                c.drawCircle(s * 0.65f, s * 0.68f, s * 0.13f, p);
                break;
            }
            case "GRID": {
                p.setStyle(Paint.Style.STROKE);
                float g1 = s * 0.15f, g2 = s * 0.44f, g3 = s * 0.56f, g4 = s * 0.85f;
                c.drawRoundRect(new RectF(g1, g1, g2, g2), s * 0.06f, s * 0.06f, p);
                c.drawRoundRect(new RectF(g3, g1, g4, g2), s * 0.06f, s * 0.06f, p);
                c.drawRoundRect(new RectF(g1, g3, g2, g4), s * 0.06f, s * 0.06f, p);
                c.drawRoundRect(new RectF(g3, g3, g4, g4), s * 0.06f, s * 0.06f, p);
                break;
            }
            case "USERS": {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(s * 0.38f, s * 0.34f, s * 0.15f, p);
                Path arc1 = new Path();
                arc1.addArc(new RectF(s * 0.14f, s * 0.55f, s * 0.62f, s * 0.95f), 180, 180);
                c.drawPath(arc1, p);
                c.drawCircle(s * 0.68f, s * 0.36f, s * 0.12f, p);
                Path arc2 = new Path();
                arc2.addArc(new RectF(s * 0.50f, s * 0.58f, s * 0.88f, s * 0.95f), 200, 160);
                c.drawPath(arc2, p);
                break;
            }
            case "EYE": {
                p.setStyle(Paint.Style.STROKE);
                Path eye = new Path();
                eye.moveTo(s * 0.10f, s * 0.50f);
                eye.quadTo(s * 0.50f, s * 0.15f, s * 0.90f, s * 0.50f);
                eye.quadTo(s * 0.50f, s * 0.85f, s * 0.10f, s * 0.50f);
                c.drawPath(eye, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(s * 0.50f, s * 0.50f, s * 0.14f, p);
                break;
            }
            case "EYE_OFF": {
                p.setStyle(Paint.Style.STROKE);
                Path eye = new Path();
                eye.moveTo(s * 0.10f, s * 0.50f);
                eye.quadTo(s * 0.50f, s * 0.15f, s * 0.90f, s * 0.50f);
                eye.quadTo(s * 0.50f, s * 0.85f, s * 0.10f, s * 0.50f);
                c.drawPath(eye, p);
                c.drawLine(s * 0.18f, s * 0.18f, s * 0.82f, s * 0.82f, p);
                break;
            }
            case "PLAY": {
                p.setStyle(Paint.Style.FILL);
                Path play = new Path();
                play.moveTo(s * 0.25f, s * 0.18f);
                play.lineTo(s * 0.82f, s * 0.50f);
                play.lineTo(s * 0.25f, s * 0.82f);
                play.close();
                c.drawPath(play, p);
                break;
            }
            case "STOP": {
                p.setStyle(Paint.Style.FILL);
                RectF r = new RectF(s * 0.22f, s * 0.22f, s * 0.78f, s * 0.78f);
                c.drawRoundRect(r, s * 0.12f, s * 0.12f, p);
                break;
            }
            case "PLUS": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.50f, s * 0.20f, s * 0.50f, s * 0.80f, p);
                c.drawLine(s * 0.20f, s * 0.50f, s * 0.80f, s * 0.50f, p);
                break;
            }
            case "CLOSE": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.25f, s * 0.25f, s * 0.75f, s * 0.75f, p);
                c.drawLine(s * 0.75f, s * 0.25f, s * 0.25f, s * 0.75f, p);
                break;
            }
            case "CHECK": {
                p.setStyle(Paint.Style.STROKE);
                Path chk = new Path();
                chk.moveTo(s * 0.18f, s * 0.52f);
                chk.lineTo(s * 0.42f, s * 0.76f);
                chk.lineTo(s * 0.82f, s * 0.26f);
                c.drawPath(chk, p);
                break;
            }
        }

        BitmapDrawable bd = new BitmapDrawable(getResources(), bmp);
        bd.setBounds(0, 0, s, s);
        return bd;
    }

    private void ponerIconoYTexto(TextView tv, Drawable icono, String texto) {
        if (texto == null || texto.isEmpty()) {
            SpannableString ss = new SpannableString(" ");
            ss.setSpan(new CenteredImageSpan(icono), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(ss);
            return;
        }
        SpannableString ss = new SpannableString("  " + texto);
        ss.setSpan(new CenteredImageSpan(icono), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(ss);
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
    private void construirInterfazModoConsola() {
        FrameLayout frameRoot = new FrameLayout(this);
        frameRoot.setBackgroundColor(Color.parseColor("#18181B"));
        frameRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Contenedor invisible de 1x1 píxel donde viven los 4 motores HTML sin verse jamás en pantalla
        contenedorMotoresOcultos = new FrameLayout(this);
        contenedorMotoresOcultos.setAlpha(0f);
        frameRoot.addView(contenedorMotoresOcultos, new FrameLayout.LayoutParams(1, 1));

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
        ponerIconoYTexto(badgeEstado, crearCirculoNeon(false, false, 10), "APAGADO");
        badgeEstado.setTextColor(Color.parseColor("#FCA5A5"));
        badgeEstado.setTextSize(11);
        badgeEstado.setTypeface(null, Typeface.BOLD);
        badgeEstado.setGravity(Gravity.CENTER);
        badgeEstado.setPadding(dp(10), dp(5), dp(10), dp(5));
        badgeEstado.setBackground(fondoRedondeado("#450A0A", 8));
        topBar.addView(badgeEstado);
        root.addView(topBar);

        // Selector de Modo: Panel + Consola General | Solo Consolas Separadas
        LinearLayout viewBar = new LinearLayout(this);
        viewBar.setOrientation(LinearLayout.HORIZONTAL);
        viewBar.setPadding(dp(12), dp(2), dp(12), dp(6));

        btnModoUnificado = crearBotonConIcono("TERMINAL", "Panel Principal", "#0284C7", "#FFFFFF", 11, () -> cambiarVista(0));
        btnModoSeparado = crearBotonConIcono("PANEL", "Solo Panel (Consolas)", "#27272A", "#A1A1AA", 11, () -> cambiarVista(1));

        LinearLayout.LayoutParams lpViewBtn = new LinearLayout.LayoutParams(0, dp(32), 1f);
        lpViewBtn.setMargins(dp(2), 0, dp(2), 0);
        viewBar.addView(btnModoUnificado, lpViewBtn);
        viewBar.addView(btnModoSeparado, lpViewBtn);
        root.addView(viewBar);

        // 2. TARJETA PRINCIPAL DE CONTROLES Y CONSOLAS (Ocupa toda la pantalla, sin navegador)
        LinearLayout mainCard = new LinearLayout(this);
        mainCard.setOrientation(LinearLayout.VERTICAL);
        mainCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        mainCard.setBackground(fondoRedondeado("#18181B", 14));
        LinearLayout.LayoutParams lpMain = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lpMain.setMargins(dp(8), dp(2), dp(8), dp(8));

        // Controles superiores (Se ocultan en "Solo Panel" para dejar únicamente las consolas separadas)
        contenedorControlesSuperiores = new LinearLayout(this);
        contenedorControlesSuperiores.setOrientation(LinearLayout.VERTICAL);

        LinearLayout filaCuenta = new LinearLayout(this);
        filaCuenta.setOrientation(LinearLayout.HORIZONTAL);
        filaCuenta.setGravity(Gravity.CENTER_VERTICAL);

        btnCuenta = crearBotonConIcono("USERS", "0 Cuentas Registradas", "#27272A", "#F4F4F5", 12, this::mostrarPopupCuenta);
        LinearLayout.LayoutParams lpBtnCuenta = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnCuenta.setMargins(0, 0, dp(6), 0);
        filaCuenta.addView(btnCuenta, lpBtnCuenta);

        btnOjoPrivacidad = crearBotonConIcono("EYE", "", "#27272A", "#F4F4F5", 13, this::togglePrivacidadUsuario);
        filaCuenta.addView(btnOjoPrivacidad, new LinearLayout.LayoutParams(dp(42), dp(38)));
        contenedorControlesSuperiores.addView(filaCuenta);

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
        filaRef.addView(lblRefrescoVal, new LinearLayout.LayoutParams(dp(75), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRef);

        LinearLayout filaRel = new LinearLayout(this);
        filaRel.setGravity(Gravity.CENTER_VERTICAL);
        filaRel.setPadding(0, dp(4), 0, 0);
        TextView tRel = crearTexto("Reinicio Sesión:", "#F4F4F5", 11, true);
        SeekBar sldRel = new SeekBar(this);
        sldRel.setMax(OPCIONES_MINUTOS.length - 1);
        sldRel.setProgress(3);
        lblReloginVal = crearTexto("10 min (--:--)", "#F4F4F5", 10.5f, true);
        lblReloginVal.setGravity(Gravity.END);
        sldRel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minutosRelogin = OPCIONES_MINUTOS[progress];
                lblReloginVal.setText(minutosRelogin + " min (--:--)");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        filaRel.addView(tRel, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRel.addView(sldRel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRel.addView(lblReloginVal, new LinearLayout.LayoutParams(dp(75), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRel);

        contenedorControlesSuperiores.addView(cardTiempos, lpCard);

        // Botones INICIAR y DETENER
        LinearLayout filaBotones = new LinearLayout(this);
        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
        btnIniciar = crearBotonConIcono("PLAY", "INICIAR", "#16A34A", "#FFFFFF", 12, this::accionBotonIniciar);
        btnDetener = crearBotonConIcono("STOP", "DETENER", "#7F1D1D", "#FCA5A5", 12, this::accionBotonDetener);
        btnDetener.setEnabled(false);

        LinearLayout.LayoutParams lpBtnMando = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnMando.setMargins(dp(3), 0, dp(3), 0);
        filaBotones.addView(btnIniciar, lpBtnMando);
        filaBotones.addView(btnDetener, lpBtnMando);
        contenedorControlesSuperiores.addView(filaBotones);

        // Barra de Pestañas de Cuentas (Para elegir si encender/detener Todas o una cuenta específica)
        barraPestanasCuentas = new LinearLayout(this);
        barraPestanasCuentas.setOrientation(LinearLayout.HORIZONTAL);
        barraPestanasCuentas.setGravity(Gravity.CENTER_VERTICAL);
        barraPestanasCuentas.setPadding(dp(2), dp(8), dp(2), dp(4));

        btnTabTodas = crearBotonConIcono("GRID", "Todas", "#0284C7", "#FFFFFF", 10, () -> cambiarSlotEnfocado(-1));
        btnTabTodas.setGravity(Gravity.CENTER);
        btnTabTodas.setSingleLine(true);
        btnTabTodas.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lpTabM = new LinearLayout.LayoutParams(0, dp(26), 1.15f);
        lpTabM.setMargins(dp(2), 0, dp(2), 0);
        barraPestanasCuentas.addView(btnTabTodas, lpTabM);

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            final int idx = i;
            Button bTab = crearBoton("", "#27272A", "#A1A1AA", 10, () -> cambiarSlotEnfocado(idx));
            bTab.setGravity(Gravity.CENTER);
            bTab.setSingleLine(true);
            bTab.setEllipsize(TextUtils.TruncateAt.END);
            bTab.setIncludeFontPadding(false);
            bTab.setPadding(dp(3), 0, dp(3), 0);
            sesiones[i].btnTabSelector = bTab;
            LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(0, dp(26), 1f);
            lpT.setMargins(dp(2), 0, dp(2), 0);
            barraPestanasCuentas.addView(bTab, lpT);
        }
        contenedorControlesSuperiores.addView(barraPestanasCuentas);

        mainCard.addView(contenedorControlesSuperiores);

        // 3. ÁREA DE CONSOLAS (Sin ningún navegador visible)
        // 3A. Consola Unificada + Tarjetas de Estado en Tiempo Real
        contenedorConsolaUnificada = new FrameLayout(this);
        scrollConsolaGeneral = new ScrollView(this);
        scrollConsolaGeneral.setBackground(fondoRedondeadoConBorde("#121316", "#27272A", 8, 1));
        scrollConsolaGeneral.setPadding(dp(10), dp(8), dp(10), dp(8));
        txtConsolaGeneral = new TextView(this);
        txtConsolaGeneral.setTextColor(Color.parseColor("#4ADE80"));
        txtConsolaGeneral.setTextSize(11f);
        txtConsolaGeneral.setTypeface(Typeface.MONOSPACE);
        scrollConsolaGeneral.addView(txtConsolaGeneral);

        contenedorConsolaUnificada.addView(scrollConsolaGeneral, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView btnLimpiarCorner = new TextView(this);
        btnLimpiarCorner.setText("Limpiar");
        btnLimpiarCorner.setTextColor(Color.parseColor("#A1A1AA"));
        btnLimpiarCorner.setTextSize(9f);
        btnLimpiarCorner.setTypeface(null, Typeface.BOLD);
        btnLimpiarCorner.setGravity(Gravity.CENTER);
        btnLimpiarCorner.setPadding(dp(7), dp(2), dp(7), dp(2));
        btnLimpiarCorner.setBackground(fondoRedondeadoConBorde("#27272A", "#3F3F46", 5, 1));
        btnLimpiarCorner.setOnClickListener(v -> txtConsolaGeneral.setText(""));

        FrameLayout.LayoutParams lpLimpiarCorner = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lpLimpiarCorner.setMargins(0, dp(6), dp(8), 0);
        contenedorConsolaUnificada.addView(btnLimpiarCorner, lpLimpiarCorner);

        mainCard.addView(contenedorConsolaUnificada, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3B. Consolas Separadas por cada Cuenta (Cada una con su barra de usuario, paso actual y log propio)
        contenedorConsolasIndividuales = new LinearLayout(this);
        contenedorConsolasIndividuales.setOrientation(LinearLayout.VERTICAL);
        contenedorConsolasIndividuales.setVisibility(View.GONE);
        mainCard.addView(contenedorConsolasIndividuales, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(mainCard, lpMain);
        frameRoot.addView(root);

        // 4. PANTALLA DE ENTRADA EN GRIS OSCURO (#18181B)
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
     * Inicializa los 4 motores invisibles en memoria con bloqueo total de imágenes
     * y construye las 4 consolas individuales.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void inicializarMotoresOcultosHeadless() {
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            final SesionBot sesion = sesiones[i];

            // 1. Consola individual de la cuenta
            LinearLayout cardConsolaInd = new LinearLayout(this);
            cardConsolaInd.setOrientation(LinearLayout.VERTICAL);
            cardConsolaInd.setBackground(fondoRedondeadoConBorde("#121316", "#27272A", 8, 1));
            cardConsolaInd.setPadding(dp(1), dp(1), dp(1), dp(1));

            LinearLayout headerConsolaInd = new LinearLayout(this);
            headerConsolaInd.setOrientation(LinearLayout.HORIZONTAL);
            headerConsolaInd.setGravity(Gravity.CENTER_VERTICAL);
            headerConsolaInd.setBackgroundColor(Color.parseColor("#202227"));
            headerConsolaInd.setPadding(dp(8), dp(4), dp(8), dp(4));

            TextView lblTitConInd = crearTexto("Sin cuenta", "#9CA3AF", 11f, true);
            TextView lblPasoInd = crearTexto("DETENIDO", "#9CA3AF", 9.5f, true);
            LinearLayout.LayoutParams lpEstCon = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpEstCon.setMargins(dp(6), 0, dp(8), 0);

            TextView btnLimpiarInd = new TextView(this);
            btnLimpiarInd.setText("Limpiar");
            btnLimpiarInd.setTextColor(Color.parseColor("#A1A1AA"));
            btnLimpiarInd.setTextSize(8.5f);
            btnLimpiarInd.setTypeface(null, Typeface.BOLD);
            btnLimpiarInd.setPadding(dp(6), dp(2), dp(6), dp(2));
            btnLimpiarInd.setBackground(fondoRedondeadoConBorde("#18181B", "#3F3F46", 4, 1));
            btnLimpiarInd.setOnClickListener(v -> {
                if (sesion.txtConsolaIndividual != null) sesion.txtConsolaIndividual.setText("");
            });

            headerConsolaInd.addView(lblTitConInd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            headerConsolaInd.addView(lblPasoInd, lpEstCon);
            headerConsolaInd.addView(btnLimpiarInd);

            ScrollView scrollInd = new ScrollView(this);
            scrollInd.setPadding(dp(8), dp(5), dp(8), dp(5));
            TextView txtInd = new TextView(this);
            txtInd.setTextColor(Color.parseColor("#4ADE80"));
            txtInd.setTextSize(10f);
            txtInd.setTypeface(Typeface.MONOSPACE);
            scrollInd.addView(txtInd);

            cardConsolaInd.addView(headerConsolaInd, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cardConsolaInd.addView(scrollInd, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            sesion.tarjetaConsolaIndividual = cardConsolaInd;
            sesion.lblTituloConsolaInd = lblTitConInd;
            sesion.lblPasoActualInd = lblPasoInd;
            sesion.scrollConsolaIndividual = scrollInd;
            sesion.txtConsolaIndividual = txtInd;

            // 2. Motor HTML Oculto (Sin renderizado de imágenes ni elementos pesados)
            WebView wv = new WebView(this);
            configurarPerfilAisladoWebView(wv, i);

            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            // Bloquea al 100% la descarga de imágenes del banco para máxima velocidad
            ws.setLoadsImagesAutomatically(false);
            ws.setBlockNetworkImage(true);
            ws.setBlockNetworkLoads(false);
            ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 HeadlessSlot/" + (i + 1));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(wv, true);

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    sesion.cargandoRed = true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    sesion.cargandoRed = false;
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        sesion.cargandoRed = false;
                        view.stopLoading();
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    sesion.cargandoRed = false;
                    view.stopLoading();
                }
            });

            sesion.webViewOculto = wv;
            contenedorMotoresOcultos.addView(wv, new FrameLayout.LayoutParams(1, 1));
            wv.loadUrl(URL_LOGIN);
        }

        reorganizarConsolasYPestanas();
        log("[SISTEMA] Motor Headless HTML/XHR activado (Navegador oculto / 0% carga grafica).");
    }

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

    private boolean hayAlgunaCuentaCorriendo() {
        for (SesionBot s : obtenerSesionesActivas()) {
            if (s.estaCorriendo()) return true;
        }
        return false;
    }

    private int contarCuentasCorriendo() {
        int c = 0;
        for (SesionBot s : obtenerSesionesActivas()) {
            if (s.estaCorriendo()) c++;
        }
        return c;
    }

    private void actualizarEstilosIndicadoresNeon() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        int cantActivas = activas.isEmpty() ? 1 : activas.size();
        int cantCorriendo = contarCuentasCorriendo();

        if (cantCorriendo > 0) {
            ponerIconoYTexto(badgeEstado, crearCirculoNeon(true, pulsoNeon, 10), "VIGILANDO (" + cantCorriendo + ")");
            badgeEstado.setTextColor(Color.parseColor("#4ADE80"));
            badgeEstado.setBackground(fondoRedondeado("#14532D", 8));
        } else {
            ponerIconoYTexto(badgeEstado, crearCirculoNeon(false, false, 10), "APAGADO");
            badgeEstado.setTextColor(Color.parseColor("#FCA5A5"));
            badgeEstado.setBackground(fondoRedondeado("#450A0A", 8));
        }

        ponerIconoYTexto(btnTabTodas, crearIconoVectorial("GRID", slotEnfocado == -1 ? "#FFFFFF" : "#A1A1AA", 12), "Todas (" + cantActivas + ")");
        btnTabTodas.setBackground(fondoRedondeado(slotEnfocado == -1 ? "#0284C7" : "#27272A", 6));
        btnTabTodas.setTextColor(Color.parseColor(slotEnfocado == -1 ? "#FFFFFF" : "#A1A1AA"));

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            SesionBot s = sesiones[i];
            boolean corriendo = s.estaCorriendo();
            String colorTextoPanel = corriendo ? "#4ADE80" : "#9CA3AF";
            String nombre = s.nombreMostrado();

            // Cabecera de cada consola individual (Con círculo neón rojo/verde titilando)
            if (s.lblTituloConsolaInd != null) {
                Drawable dotConsola = crearCirculoNeon(corriendo, pulsoNeon, 10);
                ponerIconoYTexto(s.lblTituloConsolaInd, dotConsola, nombre);
                s.lblTituloConsolaInd.setTextColor(Color.parseColor(colorTextoPanel));
            }

            // Pestañas de selección: solo el nombre en gris centrado (#A1A1AA)
            s.btnTabSelector.setText(nombre);
            s.btnTabSelector.setTextColor(Color.parseColor("#A1A1AA"));
            s.btnTabSelector.setGravity(Gravity.CENTER);
            s.btnTabSelector.setVisibility(s.activa || (activas.isEmpty() && i == 0) ? View.VISIBLE : View.GONE);

            boolean tabSeleccionada = (slotEnfocado == i);
            if (tabSeleccionada) {
                s.btnTabSelector.setBackground(fondoRedondeadoConBorde("#1E293B", "#0284C7", 6, 1));
            } else {
                s.btnTabSelector.setBackground(fondoRedondeado("#27272A", 6));
            }
        }
    }

    private void actualizarBotonesMandoSegunSeleccion() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            btnIniciar.setEnabled(true);
            btnIniciar.setBackground(fondoRedondeado("#16A34A", 8));
            ponerIconoYTexto(btnIniciar, crearIconoVectorial("PLAY", "#FFFFFF", 14), "INICIAR");

            btnDetener.setEnabled(false);
            btnDetener.setBackground(fondoRedondeado("#7F1D1D", 8));
            ponerIconoYTexto(btnDetener, crearIconoVectorial("STOP", "#FCA5A5", 14), "DETENER");
            return;
        }

        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS && sesiones[slotEnfocado].activa) {
            SesionBot sel = sesiones[slotEnfocado];
            boolean corriendo = sel.estaCorriendo();

            btnIniciar.setEnabled(!corriendo);
            btnIniciar.setBackground(fondoRedondeado(corriendo ? "#14532D" : "#16A34A", 8));
            ponerIconoYTexto(btnIniciar, crearIconoVectorial("PLAY", corriendo ? "#86EFAC" : "#FFFFFF", 14), "INICIAR");
            btnIniciar.setTextColor(Color.parseColor(corriendo ? "#86EFAC" : "#FFFFFF"));

            btnDetener.setEnabled(corriendo);
            btnDetener.setBackground(fondoRedondeado(corriendo ? "#DC2626" : "#7F1D1D", 8));
            ponerIconoYTexto(btnDetener, crearIconoVectorial("STOP", corriendo ? "#FFFFFF" : "#FCA5A5", 14), "DETENER");
            btnDetener.setTextColor(Color.parseColor(corriendo ? "#FFFFFF" : "#FCA5A5"));
        } else {
            boolean algunaDetenida = false;
            boolean algunaCorriendo = false;
            for (SesionBot s : activas) {
                if (s.estaCorriendo()) algunaCorriendo = true;
                else algunaDetenida = true;
            }

            btnIniciar.setEnabled(algunaDetenida);
            btnIniciar.setBackground(fondoRedondeado(algunaDetenida ? "#16A34A" : "#14532D", 8));
            ponerIconoYTexto(btnIniciar, crearIconoVectorial("PLAY", algunaDetenida ? "#FFFFFF" : "#86EFAC", 14), "INICIAR");
            btnIniciar.setTextColor(Color.parseColor(algunaDetenida ? "#FFFFFF" : "#86EFAC"));

            btnDetener.setEnabled(algunaCorriendo);
            btnDetener.setBackground(fondoRedondeado(algunaCorriendo ? "#DC2626" : "#7F1D1D", 8));
            ponerIconoYTexto(btnDetener, crearIconoVectorial("STOP", algunaCorriendo ? "#FFFFFF" : "#FCA5A5", 14), "DETENER");
            btnDetener.setTextColor(Color.parseColor(algunaCorriendo ? "#FFFFFF" : "#FCA5A5"));
        }
    }

    private void reorganizarConsolasYPestanas() {
        contenedorConsolasIndividuales.removeAllViews();
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            ViewGroup parentCon = (ViewGroup) sesiones[i].tarjetaConsolaIndividual.getParent();
            if (parentCon != null) parentCon.removeView(sesiones[i].tarjetaConsolaIndividual);
        }

        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            activas.add(sesiones[0]);
        }

        for (SesionBot s : activas) {
            if (slotEnfocado == -1 || slotEnfocado == s.slotIndex || modoVistaActual == 1) {
                LinearLayout.LayoutParams lpConInd = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                lpConInd.setMargins(0, dp(3), 0, dp(3));
                contenedorConsolasIndividuales.addView(s.tarjetaConsolaIndividual, lpConInd);
            }
        }

        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
        actualizarModoVisualConsolas();
    }

    private void cambiarSlotEnfocado(int nuevoSlot) {
        slotEnfocado = nuevoSlot;
        reorganizarConsolasYPestanas();
    }

    private void cambiarVista(int modo) {
        modoVistaActual = modo;
        btnModoUnificado.setBackground(fondoRedondeado(modo == 0 ? "#0284C7" : "#27272A", 8));
        btnModoSeparado.setBackground(fondoRedondeado(modo == 1 ? "#0284C7" : "#27272A", 8));

        ponerIconoYTexto(btnModoUnificado, crearIconoVectorial("TERMINAL", modo == 0 ? "#FFFFFF" : "#A1A1AA", 13), "Panel Principal");
        btnModoUnificado.setTextColor(Color.parseColor(modo == 0 ? "#FFFFFF" : "#A1A1AA"));

        ponerIconoYTexto(btnModoSeparado, crearIconoVectorial("PANEL", modo == 1 ? "#FFFFFF" : "#A1A1AA", 13), "Solo Panel (Consolas)");
        btnModoSeparado.setTextColor(Color.parseColor(modo == 1 ? "#FFFFFF" : "#A1A1AA"));

        reorganizarConsolasYPestanas();
    }

    private void actualizarModoVisualConsolas() {
        if (modoVistaActual == 0) {
            // Panel Principal: Muestra controles arriba; si está en "Todas" muestra la consola general, y si toca un usuario muestra la consola de ese usuario
            contenedorControlesSuperiores.setVisibility(View.VISIBLE);
            if (slotEnfocado == -1) {
                contenedorConsolaUnificada.setVisibility(View.VISIBLE);
                contenedorConsolasIndividuales.setVisibility(View.GONE);
            } else {
                contenedorConsolaUnificada.setVisibility(View.GONE);
                contenedorConsolasIndividuales.setVisibility(View.VISIBLE);
            }
        } else {
            // Solo Panel: Oculta botones de agregar cuentas e iniciar/detener, y muestra únicamente las consolas separadas de cada cuenta
            contenedorControlesSuperiores.setVisibility(View.GONE);
            contenedorConsolaUnificada.setVisibility(View.GONE);
            contenedorConsolasIndividuales.setVisibility(View.VISIBLE);
        }
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

    // =========================================================================================
    // CONTROL DE RED INDIVIDUAL EN SEGUNDO PLANO
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
                            if (sesiones[i].webViewOculto != null) {
                                sesiones[i].webViewOculto.stopLoading();
                            }
                        }
                        if (!avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = true;
                            log("[RED] Internet desconectado. Motores en espera...");
                        }
                    });
                }

                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        if (avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = false;
                            log("[RED] Internet restablecido. Retomando operaciones...");
                            long ahora = System.currentTimeMillis();
                            for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
                                sesiones[i].tiempoEstadoMs = ahora;
                                sesiones[i].accionEnCurso = false;
                                sesiones[i].peticionRedEnCurso = false;
                            }
                        }
                    });
                }
            };
            cm.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception ignored) {}
    }

    private void reiniciarConexionIndividual(final SesionBot s) {
        if (s == null || s.microCortePropio) return;
        s.microCortePropio = true;
        s.peticionRedEnCurso = false;
        logCuenta(s, "[SOCKET] Reiniciando canal de red interno para esta cuenta...");

        final WebView wv = s.webViewOculto;
        if (wv != null) {
            try {
                wv.stopLoading();
                wv.getSettings().setBlockNetworkLoads(true);
                evaluarJS(s, "if(window.stop){try{window.stop();}catch(e){}}", null);
            } catch (Exception ignored) {}
        }

        handler.postDelayed(() -> {
            if (wv != null) {
                try {
                    wv.getSettings().setBlockNetworkLoads(false);
                } catch (Exception ignored) {}
            }
            s.microCortePropio = false;
            logCuenta(s, "[SOCKET] Canal de red restablecido.");
        }, 1200);
    }

    private TextView crearTexto(String texto, String colorHex, float sizeSp, boolean bold) {
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

    private Button crearBotonConIcono(String tipoIcono, String texto, String bgHex, String fgHex, int sizeSp, Runnable accion) {
        Button b = crearBoton("", bgHex, fgHex, sizeSp, accion);
        Drawable d = crearIconoVectorial(tipoIcono, fgHex, 14);
        ponerIconoYTexto(b, d, texto);
        return b;
    }

    // =========================================================================================
    // GESTIÓN DE CUENTAS ("X Cuentas Registradas")
    // =========================================================================================
    private void togglePrivacidadUsuario() {
        usuarioOculto = !usuarioOculto;
        actualizarTextoBotonUsuario();
        actualizarEstilosIndicadoresNeon();
    }

    private void actualizarTextoBotonUsuario() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        int n = activas.size();
        String textoCont;
        if (n == 0) {
            textoCont = "0 Cuentas Registradas";
        } else if (n == 1) {
            textoCont = "1 Cuenta Registrada";
        } else {
            textoCont = n + " Cuentas Registradas";
        }
        ponerIconoYTexto(btnCuenta, crearIconoVectorial("USERS", "#38BDF8", 15), textoCont);
        ponerIconoYTexto(btnOjoPrivacidad, crearIconoVectorial(usuarioOculto ? "EYE_OFF" : "EYE", "#F4F4F5", 15), "");
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
        reorganizarConsolasYPestanas();
    }

    private void toggleCampoOjo(EditText edt, Button btn) {
        boolean estaOculto = (edt.getTransformationMethod() instanceof PasswordTransformationMethod);
        if (estaOculto) {
            edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            edt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btn.setBackground(fondoRedondeado("#52525B", 6));
            ponerIconoYTexto(btn, crearIconoVectorial("EYE", "#F4F4F5", 14), "");
        } else {
            edt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            edt.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btn.setBackground(fondoRedondeado("#27272A", 6));
            ponerIconoYTexto(btn, crearIconoVectorial("EYE_OFF", "#F4F4F5", 14), "");
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

        TextView subActivas = crearTexto("1. Marca las cuentas registradas a usar (Máx. 4):", "#38BDF8", 11, true);
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

        TextView subEditor = crearTexto("2. Agregar Cuenta o Editar Existente:", "#38BDF8", 11, true);
        subEditor.setPadding(0, dp(10), 0, dp(4));
        layout.addView(subEditor);

        final ArrayList<String> listaNombres = new ArrayList<>();
        final Runnable actualizarSelectorNombres = () -> {
            listaNombres.clear();
            listaNombres.add("Agregar Cuenta");
            Iterator<String> keys = cuentasObj.keys();
            while (keys.hasNext()) { listaNombres.add(keys.next()); }
        };
        actualizarSelectorNombres.run();

        final String[] cuentaSeleccionada = {"Agregar Cuenta"};
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
        edtCedula.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        edtCedula.setBackground(fondoRedondeado("#27272A", 6));
        edtCedula.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoCed = crearBotonConIcono("EYE", "", "#52525B", "#F4F4F5", 12, null);
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
        edtUsuario.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        edtUsuario.setBackground(fondoRedondeado("#27272A", 6));
        edtUsuario.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoUsr = crearBotonConIcono("EYE", "", "#52525B", "#F4F4F5", 12, null);
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
        edtClave.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edtClave.setBackground(fondoRedondeado("#27272A", 6));
        edtClave.setPadding(dp(10), dp(6), dp(10), dp(6));

        final Button btnOjoClv = crearBotonConIcono("EYE_OFF", "", "#27272A", "#F4F4F5", 12, null);
        btnOjoClv.setOnClickListener(v -> toggleCampoOjo(edtClave, btnOjoClv));

        filaClv.addView(edtClave, lpEdt);
        filaClv.addView(btnOjoClv, new LinearLayout.LayoutParams(dp(38), dp(38)));

        btnSelectCuenta.setOnClickListener(v -> {
            actualizarSelectorNombres.run();
            String[] items = listaNombres.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Seleccionar Cuenta")
                    .setItems(items, (d, which) -> {
                        String sel = items[which];
                        cuentaSeleccionada[0] = sel;
                        btnSelectCuenta.setText(sel + "  ▼");
                        if (sel.equals("Agregar Cuenta")) {
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

        Button btnEliminar = crearBotonConIcono("CLOSE", "", "#27272A", "#A1A1AA", 13, () -> {
            String sel = cuentaSeleccionada[0];
            if (!sel.equals("Agregar Cuenta") && cuentasObj.has(sel)) {
                cuentasObj.remove(sel);
                activasSeleccionadas.remove(sel);
                prefs.edit().putString("cuentas_json", cuentasObj.toString()).apply();
                guardarNombresCuentasActivasPrefs(activasSeleccionadas);
                cuentaSeleccionada[0] = "Agregar Cuenta";
                btnSelectCuenta.setText("Agregar Cuenta  ▼");
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

        Button btnGuardarCuenta = crearBotonConIcono("PLUS", "Guardar Cuenta en la Lista", "#3F3F46", "#FFFFFF", 12, () -> {
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
                cuentaSeleccionada[0] = "Agregar Cuenta";
                btnSelectCuenta.setText("Agregar Cuenta  ▼");
                refrescarListaCheckboxes[0].run();
                Toast.makeText(this, "Cuenta guardada: " + u, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        });
        LinearLayout.LayoutParams lpBtnAdd = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        lpBtnAdd.setMargins(0, 0, 0, dp(8));
        layout.addView(btnGuardarCuenta, lpBtnAdd);

        Button btnAplicar = crearBotonConIcono("CHECK", "Aplicar Cuentas Seleccionadas", "#0284C7", "#FFFFFF", 13, () -> {
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
            log("[SISTEMA] Cuentas registradas listas: " + activasSeleccionadas.size());
            dialog.dismiss();
        });
        layout.addView(btnAplicar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        dialog.show();
    }

    private void log(String msg) {
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String limpio = msg.replaceAll("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]", "").trim();
        runOnUiThread(() -> {
            txtConsolaGeneral.append(hora + "  " + limpio + "\n");
            scrollConsolaGeneral.post(() -> scrollConsolaGeneral.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void logCuenta(final SesionBot s, String msg) {
        final String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String limpio = msg.replaceAll("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]", "").trim();
        runOnUiThread(() -> {
            txtConsolaGeneral.append(hora + "  [" + s.nombreMostrado() + "] " + limpio + "\n");
            scrollConsolaGeneral.post(() -> scrollConsolaGeneral.fullScroll(View.FOCUS_DOWN));

            if (s.txtConsolaIndividual != null && s.scrollConsolaIndividual != null) {
                s.txtConsolaIndividual.append(hora + "  " + limpio + "\n");
                s.scrollConsolaIndividual.post(() -> s.scrollConsolaIndividual.fullScroll(View.FOCUS_DOWN));
            }
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

    private void cambiarEstado(SesionBot s, EstadoBot nuevoEstado) {
        s.estadoActual = nuevoEstado;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.accionEnCurso = false;
        s.peticionRedEnCurso = false;

        String txtEst = nombreCortoEstado(nuevoEstado);
        String colHex;
        if (nuevoEstado == EstadoBot.DETENIDO) {
            colHex = "#9CA3AF";
        } else if (nuevoEstado == EstadoBot.VIGILANDO_HTML_INT || nuevoEstado == EstadoBot.COMPRANDO_XHR_STORE) {
            colHex = "#4ADE80";
        } else {
            colHex = "#38BDF8";
        }

        if (s.lblPasoActualInd != null) {
            s.lblPasoActualInd.setText(txtEst);
            s.lblPasoActualInd.setTextColor(Color.parseColor(colHex));
        }

        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
    }

    private String nombreCortoEstado(EstadoBot st) {
        switch (st) {
            case DETENIDO: return "DETENIDO";
            case CONECTANDO_SERVIDOR: return "PASO 1/4: CONECTANDO /lg";
            case ENVIANDO_USUARIO_CHK: return "PASO 2/4: USUARIO /lg/chk";
            case ESPERANDO_CLAVE_CHK:
            case ENVIANDO_CLAVE_EXE: return "PASO 3/4: CLAVE /lg/exe";
            case VERIFICANDO_SESION: return "PASO 4/4: ABRIENDO SESION";
            case VIGILANDO_HTML_INT: return "VIGILANDO HTML (/int)";
            case COMPRANDO_XHR_STORE: return "COMPRANDO (/int/store)";
            case CERRANDO_SESION: return "CERRANDO SESION";
            case RECONECTANDO_SOCKET: return "RECONECTANDO RED";
            default: return "";
        }
    }

    // =========================================================================================
    // ENCENDIDO Y DETENCIÓN INDIVIDUAL O GLOBAL
    // =========================================================================================
    private void accionBotonIniciar() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            log("[ERROR] Registra y selecciona al menos 1 cuenta antes de iniciar.");
            mostrarPopupCuenta();
            return;
        }

        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS && sesiones[slotEnfocado].activa) {
            SesionBot s = sesiones[slotEnfocado];
            iniciarSesionIndividual(s);
            logCuenta(s, "[INICIO] Motor Headless iniciado para esta cuenta.");
        } else {
            int iniciadas = 0;
            for (SesionBot s : activas) {
                if (!s.estaCorriendo()) {
                    iniciarSesionIndividual(s);
                    iniciadas++;
                }
            }
            log("[INICIO] Motor Headless iniciado en " + iniciadas + " cuenta(s) simultanea(s).");
        }
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
    }

    private void iniciarSesionIndividual(SesionBot s) {
        s.corriendo = true;
        s.vueltaActual = 0;
        s.intentoCreate = 0;
        s.intentoLogout = 0;
        s.tiempoInicioSesionMs = 0;
        s.compraCompletada = false;
        cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
        logCuenta(s, "[PASO 1/4] Conectando con servidor Banco del Tesoro (/lg)...");
    }

    private void accionBotonDetener() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS && sesiones[slotEnfocado].activa) {
            SesionBot s = sesiones[slotEnfocado];
            detenerSesionIndividual(s);
            logCuenta(s, "[STOP] Cuenta detenida individualmente.");
        } else {
            for (SesionBot s : activas) {
                detenerSesionIndividual(s);
            }
            lblReloginVal.setText(minutosRelogin + " min (--:--)");
            log("[STOP] Todas las cuentas han sido detenidas.");
        }
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
    }

    private void detenerSesionIndividual(SesionBot s) {
        s.corriendo = false;
        s.tiempoInicioSesionMs = 0;
        s.cargandoRed = false;
        s.peticionRedEnCurso = false;
        if (s.webViewOculto != null) {
            s.webViewOculto.stopLoading();
        }
        cambiarEstado(s, EstadoBot.DETENIDO);
    }

    private void evaluarJS(SesionBot s, String script, ValueCallback<String> callback) {
        if (s == null || s.webViewOculto == null) return;
        s.webViewOculto.evaluateJavascript("(function() { try { " + script + " } catch(e) { return 'ERR:' + e.message; } })()", callback);
    }

    private void iniciarCicloReloj() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pulsoNeon = !pulsoNeon;
                actualizarEstilosIndicadoresNeon();

                ArrayList<SesionBot> activas = obtenerSesionesActivas();
                boolean internetOk = hayInternetDisponible();

                if (hayAlgunaCuentaCorriendo()) {
                    verificarTiemposDeSesionIndependientes(activas);
                    for (SesionBot s : activas) {
                        if (s.estaCorriendo() && internetOk && !s.microCortePropio) {
                            ejecutarPasoMotorOculto(s);
                        } else if (!internetOk) {
                            s.tiempoEstadoMs = System.currentTimeMillis();
                        }
                    }
                } else {
                    lblReloginVal.setText(minutosRelogin + " min (--:--)");
                }
                handler.postDelayed(this, 550);
            }
        }, 550);
    }

    private void verificarTiemposDeSesionIndependientes(ArrayList<SesionBot> activas) {
        long ahora = System.currentTimeMillis();
        long totalSeg = minutosRelogin * 60L;
        long menorRestante = -1;

        for (SesionBot s : activas) {
            if (!s.estaCorriendo()) continue;

            if (s.tiempoInicioSesionMs > 0 && s.estadoActual == EstadoBot.VIGILANDO_HTML_INT) {
                long transcurridoSeg = (ahora - s.tiempoInicioSesionMs) / 1000;
                long restante = Math.max(0, totalSeg - transcurridoSeg);
                if (menorRestante == -1 || restante < menorRestante) {
                    menorRestante = restante;
                }
                if (transcurridoSeg >= totalSeg) {
                    logCuenta(s, "[SESION] Tiempo de sesion cumplido. Ejecutando Logout en segundo plano...");
                    s.intentoLogout = 0;
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
            }
        }

        if (menorRestante >= 0) {
            long mins = menorRestante / 60;
            long secs = menorRestante % 60;
            lblReloginVal.setText(String.format(Locale.getDefault(), "%d min (%02d:%02d)", minutosRelogin, mins, secs));
        } else {
            lblReloginVal.setText(minutosRelogin + " min (--:--)");
        }
    }

    private void ejecutarPasoMotorOculto(final SesionBot sesion) {
        if (sesion == null || sesion.webViewOculto == null) return;
        final long ahora = System.currentTimeMillis();

        String jsInspectorHeadless =
                "var ready = (document.readyState === 'complete');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var modalLogout = (bodyTxt.indexOf('deseas cerrar tu sesión') !== -1 || bodyTxt.indexOf('deseas cerrar tu sesion') !== -1);" +
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && !modalLogout) { btnX.click(); }" +
                "var btnOk = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim() === 'OK' || el.innerText.trim() === 'Aceptar'));" +
                "if (btnOk && bodyTxt.indexOf('deseas realizar') === -1 && !modalLogout) { btnOk.click(); }" +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var opsDiv = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "var hasDoc = !!(doc && usr && !cargandoTxt);" +
                "var hasPass = !!(pass && !cargandoTxt);" +
                "var sesionAdentro = !!(ready && !cargandoTxt && (menuInt || opsDiv || btnLogout || bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || bodyTxt.indexOf('operaciones de intervención') !== -1));" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1);" +
                "return JSON.stringify({" +
                "  ready: ready && !cargandoTxt," +
                "  doc: hasDoc," +
                "  pass: hasPass," +
                "  adentro: sesionAdentro," +
                "  sesionActivaErr: sesionActivaErr" +
                "});";

        evaluarJS(sesion, jsInspectorHeadless, raw -> {
            if (!sesion.estaCorriendo() || raw == null || raw.equals("null")) return;
            try {
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                }
                JSONObject st = new JSONObject(clean);
                boolean paginaCompleta = st.optBoolean("ready") && !sesion.cargandoRed;

                procesarEstadoHeadless(
                        sesion,
                        ahora,
                        paginaCompleta,
                        st.optBoolean("doc"),
                        st.optBoolean("pass"),
                        st.optBoolean("adentro"),
                        st.optBoolean("sesionActivaErr")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoHeadless(
            SesionBot s, long ahora, boolean paginaCompleta, boolean hasDoc, boolean hasPass,
            boolean sesionAdentro, boolean haySesionActivaErr
    ) {
        if (!s.estaCorriendo()) return;

        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (s.estadoActual) {

            case CONECTANDO_SERVIDOR:
                if (paginaCompleta && sesionAdentro) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "[SESION OK] Sesion activa en memoria. Iniciando lectura directa de HTML (/int)...");
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                    ejecutarVigilanciaDirectaHtml(s);
                    return;
                }
                if (paginaCompleta && hasPass) {
                    logCuenta(s, "[PASO 3/4] Servidor solicito clave (/lg/chk OK). Preparando envio a /lg/exe...");
                    cambiarEstado(s, EstadoBot.ENVIANDO_CLAVE_EXE);
                    return;
                }
                if (paginaCompleta && hasDoc) {
                    if (ahora - s.tiempoEstadoMs >= 1200) {
                        cambiarEstado(s, EstadoBot.ENVIANDO_USUARIO_CHK);
                    }
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 20000) {
                    s.tiempoEstadoMs = ahora;
                    logCuenta(s, "[REINTENTO] Reconectando con /lg en segundo plano...");
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                }
                break;

            case ENVIANDO_USUARIO_CHK:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    logCuenta(s, "[PASO 2/4] Cifrando y enviando Cedula + Usuario a /lg/chk...");
                    String jsChk =
                            "var sel = document.querySelector('#tipodocben');" +
                            "if(sel) { for(var i=0; i<sel.options.length; i++) { if(sel.options[i].text.trim().toUpperCase() === '" + s.tipoDoc + "') { sel.selectedIndex = i; break; } } sel.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var d = document.querySelector('#documento'); var u = document.querySelector('#username');" +
                            "if(d && u) {" +
                            "  setter.call(d, '" + cedSafe + "'); d.dispatchEvent(new Event('input', {bubbles:true})); d.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  setter.call(u, '" + usrSafe + "'); u.dispatchEvent(new Event('input', {bubbles:true})); u.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 200);" +
                            "}";
                    evaluarJS(s, jsChk, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CLAVE_CHK);
                }
                break;

            case ESPERANDO_CLAVE_CHK:
                if (paginaCompleta && hasPass) {
                    logCuenta(s, "[PASO 3/4] Respuesta 200 de /lg/chk recibida. Enviando contrasena a /lg/exe...");
                    cambiarEstado(s, EstadoBot.ENVIANDO_CLAVE_EXE);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 14000 && paginaCompleta && hasDoc) {
                    logCuenta(s, "[REINTENTO] /lg/chk no avanzo. Reenviando credenciales...");
                    cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                }
                break;

            case ENVIANDO_CLAVE_EXE:
                if (ahora - s.tiempoEstadoMs >= 900 && !s.accionEnCurso) {
                    s.accionEnCurso = true;
                    String jsExe =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  setter.call(p, '" + clvSafe + "'); p.dispatchEvent(new Event('input', {bubbles:true})); p.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 200);" +
                            "}";
                    evaluarJS(s, jsExe, null);
                    cambiarEstado(s, EstadoBot.VERIFICANDO_SESION);
                }
                break;

            case VERIFICANDO_SESION:
                if (paginaCompleta && sesionAdentro) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "[PASO 4/4] Sesion autenticada con exito (JWT + XSRF activos). Vigilando /int por HTML...");
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                    ejecutarVigilanciaDirectaHtml(s);
                    return;
                }
                if (haySesionActivaErr) {
                    logCuenta(s, "[AVISO] El servidor reporta sesion previa activa. Reintentando en 4s...");
                    s.tiempoEstadoMs = ahora;
                    cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 10000 && paginaCompleta && hasDoc) {
                    logCuenta(s, "[REINTENTO] El servidor devolvio a /lg. Reintentando login...");
                    cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                }
                break;

            case VIGILANDO_HTML_INT:
                if (paginaCompleta && hasDoc) {
                    logCuenta(s, "[SESION] Sesion expirada en servidor. Reconectando automaticamente...");
                    cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                    return;
                }
                if (!s.peticionRedEnCurso && (ahora - s.tiempoEstadoMs >= intervaloRefresco * 1000L)) {
                    ejecutarVigilanciaDirectaHtml(s);
                }
                break;

            case COMPRANDO_XHR_STORE:
                if (!s.peticionRedEnCurso && (ahora - s.tiempoEstadoMs >= 800)) {
                    ejecutarCompraDirectaCreateStore(s);
                }
                break;

            case CERRANDO_SESION:
                if (paginaCompleta && (hasDoc || hasPass)) {
                    logCuenta(s, "[SESION] Sesion cerrada limpiamente. Iniciando nuevo ciclo...");
                    if (!hasDoc) {
                        s.webViewOculto.loadUrl(URL_LOGIN);
                    }
                    cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout++;
                    s.tiempoEstadoMs = ahora;
                    logCuenta(s, "[LOGOUT] Ejecutando cierre de sesion en segundo plano (Intento #" + s.intentoLogout + ")...");
                    ejecutarScriptLogout(s);
                } else if (ahora - s.tiempoEstadoMs > 4800) {
                    if (s.intentoLogout % 2 == 0) {
                        cambiarEstado(s, EstadoBot.RECONECTANDO_SOCKET);
                        reiniciarConexionIndividual(s);
                    } else {
                        s.accionEnCurso = false;
                    }
                }
                break;

            case RECONECTANDO_SOCKET:
                if (!s.microCortePropio && (ahora - s.tiempoEstadoMs >= 1400)) {
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;
        }
    }

    /**
     * Lee directamente por red el HTML de https://tesoroenlinea.bt.com.ve/int (3.1 kB)
     * buscando el enlace "javascript:mod.add(1)" de Ordinaria.
     */
    private void ejecutarVigilanciaDirectaHtml(final SesionBot s) {
        if (!s.estaCorriendo() || s.peticionRedEnCurso) return;
        s.peticionRedEnCurso = true;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.vueltaActual++;

        String jsFetchInt =
                "window.__vbt_int_res = 'WAIT';" +
                "(async function(){" +
                "  try {" +
                "    var t0 = Date.now();" +
                "    var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "    var tok = meta ? meta.getAttribute('content') : '';" +
                "    var r = await fetch('https://tesoroenlinea.bt.com.ve/int', {" +
                "      method: 'GET'," +
                "      headers: {" +
                "        'Accept': 'text/html, */*; q=0.01'," +
                "        'X-Requested-With': 'XMLHttpRequest'," +
                "        'X-CSRF-TOKEN': tok" +
                "      }," +
                "      credentials: 'same-origin'" +
                "    });" +
                "    var txt = await r.text();" +
                "    var ms = Date.now() - t0;" +
                "    var tieneOrd = (txt.indexOf('mod.add(1)') !== -1 || txt.indexOf('btn-tesoro-green') !== -1 || txt.indexOf('Ordinaria') !== -1);" +
                "    var esLogin = (txt.indexOf('id=\"documento\"') !== -1 && txt.indexOf('id=\"username\"') !== -1);" +
                "    window.__vbt_int_res = JSON.stringify({ status: r.status, ord: tieneOrd, login: esLogin, len: txt.length, ms: ms });" +
                "  } catch(e) {" +
                "    window.__vbt_int_res = JSON.stringify({ status: -1, err: e.message });" +
                "  }" +
                "})();";

        evaluarJS(s, jsFetchInt, null);
        sondearResultadoInt(s, 0);
    }

    private void sondearResultadoInt(final SesionBot s, final int intentoPoll) {
        if (!s.estaCorriendo() || s.estadoActual != EstadoBot.VIGILANDO_HTML_INT) {
            s.peticionRedEnCurso = false;
            return;
        }
        handler.postDelayed(() -> {
            evaluarJS(s, "return window.__vbt_int_res || 'WAIT';", raw -> {
                if (!s.estaCorriendo() || s.estadoActual != EstadoBot.VIGILANDO_HTML_INT) {
                    s.peticionRedEnCurso = false;
                    return;
                }
                if (raw == null || raw.contains("WAIT")) {
                    if (intentoPoll < 25) {
                        sondearResultadoInt(s, intentoPoll + 1);
                    } else {
                        s.peticionRedEnCurso = false;
                        logCuenta(s, "[TIMEOUT] /int tardo mas de 4s. Reintentando en el proximo ciclo...");
                    }
                    return;
                }
                s.peticionRedEnCurso = false;
                try {
                    String clean = raw;
                    if (clean.startsWith("\"") && clean.endsWith("\"")) {
                        clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                    }
                    JSONObject res = new JSONObject(clean);
                    if (res.optBoolean("login", false)) {
                        logCuenta(s, "[SESION] El servidor cerro la sesion. Reautenticando en /lg...");
                        s.webViewOculto.loadUrl(URL_LOGIN);
                        cambiarEstado(s, EstadoBot.CONECTANDO_SERVIDOR);
                        return;
                    }
                    if (res.optBoolean("ord", false)) {
                        logCuenta(s, "[ALERTA] 'mod.add(1)' (Ordinaria) detectado en /int (" + res.optInt("ms", 0) + " ms)! Disparando compra XHR...");
                        if (!yaAvisoApertura) { yaAvisoApertura = true; sonarAlarma(false); }
                        s.intentoCreate = 0;
                        cambiarEstado(s, EstadoBot.COMPRANDO_XHR_STORE);
                        ejecutarCompraDirectaCreateStore(s);
                    } else {
                        logCuenta(s, "[Vuelta " + s.vueltaActual + "] GET /int [200 OK - " + res.optInt("ms", 0) + " ms] -> Sin boton Ordinaria.");
                    }
                } catch (Exception e) {
                    logCuenta(s, "[Vuelta " + s.vueltaActual + "] GET /int verificado.");
                }
            });
        }, 150);
    }

    /**
     * Pide el HTML de /int/create, extrae _token, _form_sig, tasa_usd, saldo_usd y las cuentas,
     * y envía inmediatamente el POST a /int/store sin renderizar ningún formulario.
     */
    private void ejecutarCompraDirectaCreateStore(final SesionBot s) {
        if (!s.estaCorriendo() || s.peticionRedEnCurso) return;
        s.peticionRedEnCurso = true;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.intentoCreate++;

        logCuenta(s, "[XHR 1/2] Solicitando firma _form_sig en POST /int/create (Intento #" + s.intentoCreate + ")...");

        String jsCreateAndStore =
                "window.__vbt_buy_res = 'WAIT';" +
                "(async function(){" +
                "  try {" +
                "    function fmtBT(val, dec) {" +
                "      var num = parseFloat(val);" +
                "      return Number(num).toFixed(dec).replace(/\\./g, ',');" +
                "    }" +
                "    var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "    var tok = meta ? meta.getAttribute('content') : '';" +
                "    var rCreate = await fetch('https://tesoroenlinea.bt.com.ve/int/create', {" +
                "      method: 'POST'," +
                "      headers: {" +
                "        'Accept': '*/*'," +
                "        'X-Requested-With': 'XMLHttpRequest'," +
                "        'X-CSRF-TOKEN': tok" +
                "      }," +
                "      credentials: 'same-origin'" +
                "    });" +
                "    var html = await rCreate.text();" +
                "    if (rCreate.status !== 200 || html.indexOf('presentando inconvenientes') !== -1 || html.indexOf('no podemos procesar') !== -1) {" +
                "      window.__vbt_buy_res = JSON.stringify({ step: 'CREATE_ERR', status: rCreate.status });" +
                "      return;" +
                "    }" +
                "    if (html.indexOf('eval(function(p,a,c,k,e,d)') !== -1 && html.indexOf('_form_sig') === -1) {" +
                "      var mEval = html.match(/eval\\(function\\(p,a,c,k,e,d\\)[\\s\\S]*?<\\/script>/i);" +
                "      if (mEval) {" +
                "        var rawJs = mEval[0].replace(/<\\/?script[^>]*>/gi, '');" +
                "        try { (0, eval)(rawJs); } catch(e){}" +
                "      }" +
                "      window.__vbt_buy_res = JSON.stringify({ step: 'CHALLENGE_JS' });" +
                "      return;" +
                "    }" +
                "    var parser = new DOMParser();" +
                "    var doc = parser.parseFromString(html, 'text/html');" +
                "    var inToken = doc.querySelector('input[name=\"_token\"]');" +
                "    var inSig = doc.querySelector('input[name=\"_form_sig\"]');" +
                "    var inOper = doc.querySelector('input[name=\"oper\"]');" +
                "    if (!inSig || !inSig.value) {" +
                "      window.__vbt_buy_res = JSON.stringify({ step: 'NO_SIG', len: html.length });" +
                "      return;" +
                "    }" +
                "    var formToken = inToken ? inToken.value : tok;" +
                "    var formSig = inSig.value;" +
                "    var operVal = inOper ? inOper.value : 'VE';" +
                "    var optDesde = Array.from(doc.querySelectorAll('#desde option')).find(o => o.value && o.value.trim() !== '');" +
                "    var optHasta = Array.from(doc.querySelectorAll('#hasta option')).find(o => o.value && o.value.trim() !== '');" +
                "    if (!optDesde || !optHasta) {" +
                "      window.__vbt_buy_res = JSON.stringify({ step: 'NO_ACCOUNTS' });" +
                "      return;" +
                "    }" +
                "    var valDesde = optDesde.value.trim();" +
                "    var dispBs = parseFloat(optDesde.getAttribute('data-disponible') || '0');" +
                "    var valHasta = optHasta.value.trim();" +
                "    var monedaX = optHasta.getAttribute('data-moneda') || 'USD';" +
                "    var mTasaUsd = html.match(/tasa_usd\\s*=\\s*parseFloat\\(['\"]([^'\"]+)['\"]\\)/);" +
                "    var mTasaEur = html.match(/tasa_eur\\s*=\\s*parseFloat\\(['\"]([^'\"]+)['\"]\\)/);" +
                "    var mSaldoUsd = html.match(/saldo_usd\\s*=\\s*parseFloat\\(['\"]([^'\"]+)['\"]\\)/);" +
                "    var mCom = html.match(/com\\s*=\\s*parseFloat\\(['\"]([^'\"]+)['\"]\\)/);" +
                "    var mCom2 = html.match(/com2\\s*=\\s*parseFloat\\(['\"]([^'\"]+)['\"]\\)/);" +
                "    var tasaNum = (monedaX === 'EUR' && mTasaEur) ? parseFloat(mTasaEur[1]) : (mTasaUsd ? parseFloat(mTasaUsd[1]) : 0);" +
                "    var saldoPool = mSaldoUsd ? parseFloat(mSaldoUsd[1]) : 999999;" +
                "    var comPorc = mCom ? parseFloat(mCom[1]) : 0.50;" +
                "    var com2Porc = mCom2 ? parseFloat(mCom2[1]) : 0;" +
                "    var montoUsd = " + MONTO_MAX_USD + ";" +
                "    if (dispBs > 0 && tasaNum > 0) {" +
                "      var factorCom = 1 + ((comPorc + com2Porc) / 100);" +
                "      var maxUsdPorSaldo = Math.floor((dispBs / (tasaNum * factorCom)) / 10) * 10;" +
                "      if (maxUsdPorSaldo >= 10 && maxUsdPorSaldo < montoUsd) {" +
                "        montoUsd = maxUsdPorSaldo;" +
                "      }" +
                "    }" +
                "    var subTotalBs = montoUsd * tasaNum;" +
                "    var com1Bs = subTotalBs * (comPorc / 100);" +
                "    var com2Bs = subTotalBs * (com2Porc / 100);" +
                "    var totalBloquearStr = fmtBT(subTotalBs + com1Bs + com2Bs, 2);" +
                "    var tasaStr = fmtBT(tasaNum, 8);" +
                "    var montoStr = Number(montoUsd).toFixed(2);" +
                "    var params = new URLSearchParams();" +
                "    params.append('_token', formToken);" +
                "    params.append('_form_sig', formSig);" +
                "    params.append('oper', operVal);" +
                "    params.append('tasa', tasaStr);" +
                "    params.append('monedaX', monedaX);" +
                "    params.append('desde', valDesde);" +
                "    params.append('hasta', valHasta);" +
                "    params.append('monto', montoStr);" +
                "    params.append('origen', '" + CODIGO_ORIGEN_FONDOS + "');" +
                "    params.append('destino', '" + CODIGO_DESTINO_FONDOS + "');" +
                "    params.append('total-03', totalBloquearStr);" +
                "    var rStore = await fetch('https://tesoroenlinea.bt.com.ve/int/store', {" +
                "      method: 'POST'," +
                "      headers: {" +
                "        'Accept': '*/*'," +
                "        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'," +
                "        'X-Requested-With': 'XMLHttpRequest'," +
                "        'X-CSRF-TOKEN': formToken" +
                "      }," +
                "      body: params.toString()," +
                "      credentials: 'same-origin'" +
                "    });" +
                "    var storeTxt = await rStore.text();" +
                "    window.__vbt_buy_res = JSON.stringify({" +
                "      step: 'STORE_DONE'," +
                "      status: rStore.status," +
                "      tasa: tasaStr," +
                "      pool: saldoPool," +
                "      dispBs: dispBs," +
                "      montoUsd: montoStr," +
                "      totalBs: totalBloquearStr," +
                "      resp: storeTxt" +
                "    });" +
                "  } catch(e) {" +
                "    window.__vbt_buy_res = JSON.stringify({ step: 'EX', err: e.message });" +
                "  }" +
                "})();";

        evaluarJS(s, jsCreateAndStore, null);
        sondearResultadoCompraDirecta(s, 0);
    }

    private void sondearResultadoCompraDirecta(final SesionBot s, final int intentoPoll) {
        if (!s.estaCorriendo() || s.estadoActual != EstadoBot.COMPRANDO_XHR_STORE) {
            s.peticionRedEnCurso = false;
            return;
        }
        handler.postDelayed(() -> {
            evaluarJS(s, "return window.__vbt_buy_res || 'WAIT';", raw -> {
                if (!s.estaCorriendo() || s.estadoActual != EstadoBot.COMPRANDO_XHR_STORE) {
                    s.peticionRedEnCurso = false;
                    return;
                }
                if (raw == null || raw.contains("WAIT")) {
                    if (intentoPoll < 35) {
                        sondearResultadoCompraDirecta(s, intentoPoll + 1);
                    } else {
                        s.peticionRedEnCurso = false;
                        logCuenta(s, "[TIMEOUT] /int/create sin respuesta. Reintentando...");
                    }
                    return;
                }
                s.peticionRedEnCurso = false;
                try {
                    String clean = raw;
                    if (clean.startsWith("\"") && clean.endsWith("\"")) {
                        clean = clean.substring(1, clean.length() - 1)
                                .replace("\\\"", "\"")
                                .replace("\\/", "/");
                    }
                    JSONObject res = new JSONObject(clean);
                    String step = res.optString("step", "");

                    if (step.equals("CHALLENGE_JS")) {
                        logCuenta(s, "[COOKIE] Desafio JS de /int/create resuelto en memoria. Reenviando...");
                        s.tiempoEstadoMs = System.currentTimeMillis() - 600;
                        return;
                    }

                    if (step.equals("CREATE_ERR") || step.equals("NO_SIG")) {
                        if (s.intentoCreate < 2) {
                            logCuenta(s, "[REINTENTO] /int/create devolvio error en Intento #1. Disparando Intento #2...");
                            s.tiempoEstadoMs = System.currentTimeMillis() - 500;
                        } else {
                            logCuenta(s, "[ERROR] Fallo en ambos intentos de /int/create. Reiniciando sesion de la cuenta...");
                            s.intentoLogout = 0;
                            cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                        }
                        return;
                    }

                    if (step.equals("NO_ACCOUNTS")) {
                        logCuenta(s, "[AVISO] El HTML de /int/create no incluyo cuentas (#desde / #hasta). Volviendo a /int...");
                        cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                        return;
                    }

                    if (step.equals("STORE_DONE")) {
                        String tasa = res.optString("tasa", "0");
                        double pool = res.optDouble("pool", 0);
                        String montoUsd = res.optString("montoUsd", "300.00");
                        String totalBs = res.optString("totalBs", "0,00");
                        String respRaw = res.optString("resp", "");

                        logCuenta(s, String.format(Locale.US,
                                "[XHR 2/2] POST /int/store -> Tasa: %s | Pool: %,.0f$ | Orden: %s$ (%s Bs)",
                                tasa, pool, montoUsd, totalBs));

                        boolean compraOk = false;
                        String msgBanco = respRaw;
                        try {
                            JSONObject jStore = new JSONObject(respRaw);
                            compraOk = jStore.optBoolean("ok", false);
                            if (jStore.has("msg")) {
                                msgBanco = jStore.optString("msg", respRaw);
                            }
                        } catch (Exception ignored) {
                            if (respRaw.contains("\"ok\":true") || respRaw.toLowerCase().contains("exitosamente")) {
                                compraOk = true;
                            }
                        }

                        if (compraOk) {
                            logCuenta(s, "[COMPRA EXITOSA] Respuesta servidor: {\"ok\":true} -> ORDEN REGISTRADA!");
                            sonarAlarma(true);
                            s.compraCompletada = true;
                            s.corriendo = false;
                            cambiarEstado(s, EstadoBot.DETENIDO);
                            verificarSiTodasCompraron();
                        } else {
                            logCuenta(s, "[SERVIDOR] " + msgBanco + " -> Continuando vigilancia en /int...");
                            cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                        }
                    } else {
                        cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                    }
                } catch (Exception e) {
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_INT);
                }
            });
        }, 150);
    }

    private void verificarSiTodasCompraron() {
        for (SesionBot s : obtenerSesionesActivas()) {
            if (!s.compraCompletada) return;
        }
        log("[EXITO TOTAL] TODAS LAS CUENTAS REGISTRADAS COMPRARON CON EXITO!");
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
    }

    private void ejecutarScriptLogout(SesionBot s) {
        String jsLogout =
                "var btnSiExistente = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                "if (btnSiExistente) {" +
                "  btnSiExistente.click();" +
                "} else {" +
                "  var btnSalir = document.querySelector(\"a[href*='Logout' i], a[onclick*='Logout' i], header ul li:nth-child(4) a, .btn-danger\");" +
                "  if (btnSalir) { btnSalir.click(); }" +
                "  if (typeof Logout === 'function') { try { Logout(); } catch(e){} }" +
                "  setTimeout(function(){" +
                "    var si = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí' || el.innerText.trim().toLowerCase() === 'aceptar'));" +
                "    if(si) si.click();" +
                "  }, 550);" +
                "}";
        evaluarJS(s, jsLogout, null);
    }
}
