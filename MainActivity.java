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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/lg";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final int MONTO_MAX_USD = 300;
    private static final String CODIGO_ORIGEN_FONDOS = "1";  // 1 = Sueldos y Salarios
    private static final String CODIGO_DESTINO_FONDOS = "5"; // 5 = Ahorro
    private static final int MAX_CUENTAS_SIMULTANEAS = 4;
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};

    private LinearLayout panelControlContainer;
    private LinearLayout contenedorControlesDividido;
    private LinearLayout contenedorConsolasIndividuales;
    private LinearLayout webViewContainer;
    private LinearLayout barraPestanasMulti;
    private FrameLayout escenarioMultiWeb;
    private LinearLayout contenedorMosaico;
    private LinearLayout filaMosaicoSup, filaMosaicoInf;

    private EditText edtUrlBar;
    private ImageView btnChromeBack, btnChromeReload, btnChromeNet, iconLock;
    private TextView btnChromeZoom;
    private ProgressBar chromeProgressBar;
    private LinearLayout barraZoomDesplegable;
    private TextView lblZoomActualBarra;

    private TextView badgeEstado, lblRefrescoVal, lblReloginVal, txtConsola;
    private ScrollView scrollConsola;
    private FrameLayout consoleWrapper;
    private Button btnCuenta, btnOjoPrivacidad, btnIniciar, btnDetener, btnFoldConsola;
    private Button btnVistaDividida, btnVistaPanel, btnVistaWeb;

    private boolean usuarioOculto = false;
    private boolean consolaVisible = true;
    private int modoVistaActual = 0;

    // -1 = Vista Mosaico (Todas), 0..3 = Pestaña individual seleccionada
    private int slotEnfocado = -1;
    // Índice del panel cuya URL se muestra cuando está en modo Todas
    private int slotInspeccionadoUrl = 0;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;
    private int nivelZoom = 70;

    private boolean pulsoNeon = false;
    private boolean yaAvisoApertura = false;
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
        VIGILANDO_HTML_DIRECTO,
        COMPRANDO_DIRECTO_XHR,
        CERRANDO_SESION,
        REINICIANDO_RED
    }

    private class SesionBot {
        final int slotIndex;
        boolean activa = false;
        boolean corriendo = false;
        String tipoDoc = "V";
        String cedula = "";
        String usuario = "";
        String contrasena = "";

        LinearLayout tarjetaCompleta;
        LinearLayout barraCabeceraMini;
        TextView lblTituloMini;
        TextView lblEstadoMini;
        ProgressBar barraProgresoMini;
        WebView webView;
        Button btnTabSelector;

        // Consola individual exclusiva para el modo "Solo Panel"
        LinearLayout tarjetaConsolaIndividual;
        TextView lblTituloConsolaInd;
        TextView lblEstadoConsolaInd;
        ScrollView scrollConsolaIndividual;
        TextView txtConsolaIndividual;

        EstadoBot estadoActual = EstadoBot.DETENIDO;
        boolean cargandoWeb = false;
        int progresoWeb = 100;
        long tiempoInicioSesionMs = 0;
        long tiempoEstadoMs = 0;
        boolean accionEnCurso = false;
        boolean peticionRedEnCurso = false;
        boolean microCortePropio = false;
        int vueltaActual = 0;
        int intentoCreate = 0;
        int intentoLogout = 0;
        boolean compraCompletada = false;
        String ultimaUrlValida = URL_LOGIN;

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
    private Button btnTabMosaico;

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

    private Drawable crearDrawableBarraCarga(String bgHex, String fgHex) {
        ColorDrawable bgTrack = new ColorDrawable(Color.parseColor(bgHex));
        ClipDrawable fgProg = new ClipDrawable(new ColorDrawable(Color.parseColor(fgHex)), Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{bgTrack, fgProg});
        progressLayer.setId(0, android.R.id.background);
        progressLayer.setId(1, android.R.id.progress);
        return progressLayer;
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
            case "DIVIDIDO": {
                p.setStyle(Paint.Style.STROKE);
                RectF r = new RectF(s * 0.15f, s * 0.15f, s * 0.85f, s * 0.85f);
                c.drawRoundRect(r, s * 0.14f, s * 0.14f, p);
                c.drawLine(s * 0.15f, s * 0.5f, s * 0.85f, s * 0.5f, p);
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
            case "RELOAD": {
                p.setStyle(Paint.Style.STROKE);
                RectF oval = new RectF(s * 0.18f, s * 0.18f, s * 0.82f, s * 0.82f);
                c.drawArc(oval, -35, 290, false, p);
                Path arr = new Path();
                arr.moveTo(s * 0.82f, s * 0.14f);
                arr.lineTo(s * 0.82f, s * 0.38f);
                arr.lineTo(s * 0.58f, s * 0.38f);
                c.drawPath(arr, p);
                break;
            }
            case "STOP": {
                p.setStyle(Paint.Style.FILL);
                RectF r = new RectF(s * 0.22f, s * 0.22f, s * 0.78f, s * 0.78f);
                c.drawRoundRect(r, s * 0.12f, s * 0.12f, p);
                break;
            }
            case "BACK": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.80f, s * 0.50f, s * 0.20f, s * 0.50f, p);
                c.drawLine(s * 0.45f, s * 0.25f, s * 0.20f, s * 0.50f, p);
                c.drawLine(s * 0.45f, s * 0.75f, s * 0.20f, s * 0.50f, p);
                break;
            }
            case "LOCK": {
                p.setStyle(Paint.Style.STROKE);
                c.drawRoundRect(new RectF(s * 0.22f, s * 0.46f, s * 0.78f, s * 0.86f), s * 0.1f, s * 0.1f, p);
                c.drawArc(new RectF(s * 0.32f, s * 0.16f, s * 0.68f, s * 0.56f), 180, 180, false, p);
                break;
            }
            case "SIGNAL": {
                p.setStyle(Paint.Style.FILL);
                float wBar = s * 0.12f;
                c.drawRoundRect(new RectF(s * 0.14f, s * 0.65f, s * 0.14f + wBar, s * 0.85f), 2f, 2f, p);
                c.drawRoundRect(new RectF(s * 0.34f, s * 0.50f, s * 0.34f + wBar, s * 0.85f), 2f, 2f, p);
                c.drawRoundRect(new RectF(s * 0.54f, s * 0.34f, s * 0.54f + wBar, s * 0.85f), 2f, 2f, p);
                c.drawRoundRect(new RectF(s * 0.74f, s * 0.18f, s * 0.74f + wBar, s * 0.85f), 2f, 2f, p);
                break;
            }
            case "PLUS": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.50f, s * 0.20f, s * 0.50f, s * 0.80f, p);
                c.drawLine(s * 0.20f, s * 0.50f, s * 0.80f, s * 0.50f, p);
                break;
            }
            case "MINUS": {
                p.setStyle(Paint.Style.STROKE);
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
        ponerIconoYTexto(badgeEstado, crearCirculoNeon(false, false, 10), "APAGADO");
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

        btnVistaDividida = crearBotonConIcono("DIVIDIDO", "Dividido", "#0284C7", "#FFFFFF", 11, () -> cambiarVista(0));
        btnVistaPanel = crearBotonConIcono("PANEL", "Solo Panel", "#27272A", "#A1A1AA", 11, () -> cambiarVista(1));
        btnVistaWeb = crearBotonConIcono("GRID", "Solo Banco", "#27272A", "#A1A1AA", 11, () -> cambiarVista(2));

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

        // 2A. Contenedor para el modo "Dividido"
        contenedorControlesDividido = new LinearLayout(this);
        contenedorControlesDividido.setOrientation(LinearLayout.VERTICAL);
        contenedorControlesDividido.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout filaCuenta = new LinearLayout(this);
        filaCuenta.setOrientation(LinearLayout.HORIZONTAL);
        filaCuenta.setGravity(Gravity.CENTER_VERTICAL);

        btnCuenta = crearBotonConIcono("USERS", "0 Cuentas Registradas", "#27272A", "#F4F4F5", 12, this::mostrarPopupCuenta);
        LinearLayout.LayoutParams lpBtnCuenta = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnCuenta.setMargins(0, 0, dp(6), 0);
        filaCuenta.addView(btnCuenta, lpBtnCuenta);

        btnOjoPrivacidad = crearBotonConIcono("EYE", "", "#27272A", "#F4F4F5", 13, this::togglePrivacidadUsuario);
        filaCuenta.addView(btnOjoPrivacidad, new LinearLayout.LayoutParams(dp(42), dp(38)));
        contenedorControlesDividido.addView(filaCuenta);

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

        contenedorControlesDividido.addView(cardTiempos, lpCard);

        LinearLayout filaBotones = new LinearLayout(this);
        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
        btnIniciar = crearBotonConIcono("PLAY", "INICIAR", "#16A34A", "#FFFFFF", 12, this::accionBotonIniciar);
        btnDetener = crearBotonConIcono("STOP", "DETENER", "#7F1D1D", "#FCA5A5", 12, this::accionBotonDetener);
        btnDetener.setEnabled(false);

        LinearLayout.LayoutParams lpBtnMando = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnMando.setMargins(dp(3), 0, dp(3), 0);
        filaBotones.addView(btnIniciar, lpBtnMando);
        filaBotones.addView(btnDetener, lpBtnMando);
        contenedorControlesDividido.addView(filaBotones);

        LinearLayout headerConsola = new LinearLayout(this);
        headerConsola.setGravity(Gravity.CENTER_VERTICAL);
        headerConsola.setPadding(0, dp(4), 0, dp(2));

        TextView lblConsola = crearTexto("Consola Directa HTML/XHR", "#F4F4F5", 11.5f, true);
        lblConsola.setOnClickListener(v -> toggleConsola());
        headerConsola.addView(lblConsola);

        btnFoldConsola = crearBoton("▼", "#18181B", "#F4F4F5", 10, this::toggleConsola);
        LinearLayout.LayoutParams lpFold = new LinearLayout.LayoutParams(dp(26), dp(22));
        lpFold.setMargins(dp(4), 0, 0, 0);
        headerConsola.addView(btnFoldConsola, lpFold);

        contenedorControlesDividido.addView(headerConsola);

        consoleWrapper = new FrameLayout(this);
        scrollConsola = new ScrollView(this);
        scrollConsola.setBackground(fondoRedondeado("#1E2022", 8));
        scrollConsola.setPadding(dp(8), dp(6), dp(8), dp(6));
        txtConsola = new TextView(this);
        txtConsola.setTextColor(Color.parseColor("#4ADE80"));
        txtConsola.setTextSize(10.5f);
        txtConsola.setTypeface(Typeface.MONOSPACE);
        scrollConsola.addView(txtConsola);

        consoleWrapper.addView(scrollConsola, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView btnLimpiarCorner = new TextView(this);
        btnLimpiarCorner.setText("Limpiar");
        btnLimpiarCorner.setTextColor(Color.parseColor("#A1A1AA"));
        btnLimpiarCorner.setTextSize(9f);
        btnLimpiarCorner.setTypeface(null, Typeface.BOLD);
        btnLimpiarCorner.setGravity(Gravity.CENTER);
        btnLimpiarCorner.setPadding(dp(7), dp(2), dp(7), dp(2));
        btnLimpiarCorner.setBackground(fondoRedondeadoConBorde("#27272A", "#3F3F46", 5, 1));
        btnLimpiarCorner.setOnClickListener(v -> txtConsola.setText(""));

        FrameLayout.LayoutParams lpLimpiarCorner = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lpLimpiarCorner.setMargins(0, dp(4), dp(6), 0);
        consoleWrapper.addView(btnLimpiarCorner, lpLimpiarCorner);

        contenedorControlesDividido.addView(consoleWrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        panelControlContainer.addView(contenedorControlesDividido);

        // 2B. Contenedor exclusivo para el modo "Solo Panel" (Solo consolas separadas de cada cuenta)
        contenedorConsolasIndividuales = new LinearLayout(this);
        contenedorConsolasIndividuales.setOrientation(LinearLayout.VERTICAL);
        contenedorConsolasIndividuales.setVisibility(View.GONE);
        contenedorConsolasIndividuales.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        panelControlContainer.addView(contenedorConsolasIndividuales);

        root.addView(panelControlContainer);

        // 3. NAVEGADOR MULTI-CUENTA
        webViewContainer = new LinearLayout(this);
        webViewContainer.setOrientation(LinearLayout.VERTICAL);
        webViewContainer.setBackground(fondoRedondeadoConBorde("#202124", "#2E3035", 12, 1));
        webViewContainer.setPadding(dp(2), dp(2), dp(2), dp(2));

        // Barra de Pestañas Multi-Cuenta
        barraPestanasMulti = new LinearLayout(this);
        barraPestanasMulti.setOrientation(LinearLayout.HORIZONTAL);
        barraPestanasMulti.setGravity(Gravity.CENTER_VERTICAL);
        barraPestanasMulti.setPadding(dp(4), dp(3), dp(4), dp(1));
        barraPestanasMulti.setBackgroundColor(Color.parseColor("#18191C"));

        btnTabMosaico = crearBotonConIcono("GRID", "Todas", "#0284C7", "#FFFFFF", 10, () -> cambiarSlotEnfocado(-1));
        btnTabMosaico.setGravity(Gravity.CENTER);
        btnTabMosaico.setSingleLine(true);
        btnTabMosaico.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lpTabM = new LinearLayout.LayoutParams(0, dp(26), 1.15f);
        lpTabM.setMargins(dp(2), 0, dp(2), 0);
        barraPestanasMulti.addView(btnTabMosaico, lpTabM);

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
            barraPestanasMulti.addView(bTab, lpT);
        }
        webViewContainer.addView(barraPestanasMulti, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Cabecera Omnibox con URL independiente por cuenta
        LinearLayout chromeToolbar = new LinearLayout(this);
        chromeToolbar.setOrientation(LinearLayout.HORIZONTAL);
        chromeToolbar.setGravity(Gravity.CENTER_VERTICAL);
        chromeToolbar.setPadding(dp(4), dp(4), dp(4), dp(4));
        chromeToolbar.setBackground(fondoRedondeado("#202124", 10));

        btnChromeBack = crearBotonIconoNav("BACK", () -> {
            SesionBot obj = obtenerSesionControlUrl();
            if (obj != null && obj.webView != null) {
                obj.webView.stopLoading();
                if (obj.webView.canGoBack()) {
                    obj.webView.goBack();
                    logCuenta(obj, "[NAV] Retrocediendo a la pagina anterior...");
                }
            }
        });
        chromeToolbar.addView(btnChromeBack, new LinearLayout.LayoutParams(dp(32), dp(32)));

        btnChromeReload = crearBotonIconoNav("RELOAD", this::forzarRecargaCuentaSeleccionada);
        chromeToolbar.addView(btnChromeReload, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout omniboxPill = new LinearLayout(this);
        omniboxPill.setOrientation(LinearLayout.HORIZONTAL);
        omniboxPill.setGravity(Gravity.CENTER_VERTICAL);
        omniboxPill.setPadding(dp(10), dp(2), dp(6), dp(2));
        omniboxPill.setBackground(fondoRedondeado("#303134", 18));
        LinearLayout.LayoutParams lpOmnibox = new LinearLayout.LayoutParams(0, dp(32), 1f);
        lpOmnibox.setMargins(dp(4), 0, dp(4), 0);

        iconLock = new ImageView(this);
        iconLock.setImageDrawable(crearIconoVectorial("LOCK", "#9AA0A6", 13));
        LinearLayout.LayoutParams lpLock = new LinearLayout.LayoutParams(dp(14), dp(14));
        lpLock.setMargins(0, 0, dp(6), 0);
        omniboxPill.addView(iconLock, lpLock);

        edtUrlBar = new EditText(this);
        edtUrlBar.setSingleLine(true);
        edtUrlBar.setHorizontallyScrolling(true);
        edtUrlBar.setHint("https://tesoroenlinea.bt.com.ve/lg");
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

        btnChromeNet = crearBotonIconoNav("SIGNAL", () -> {
            SesionBot obj = obtenerSesionControlUrl();
            if (obj != null) {
                reiniciarConexionIndividual(obj, true);
            }
        });
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

        Button btnZMenos = crearBotonConIcono("MINUS", "Alejar", "#3C4043", "#E8EAED", 11, () -> cambiarZoomWeb(-10));
        lblZoomActualBarra = crearTexto("Zoom: " + nivelZoom + "%", "#8AB4F8", 11, true);
        lblZoomActualBarra.setGravity(Gravity.CENTER);
        Button btnZMas = crearBotonConIcono("PLUS", "Acercar", "#3C4043", "#E8EAED", 11, () -> cambiarZoomWeb(10));
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

        // Línea azul de carga superior (exclusiva para pestaña individual)
        chromeProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        chromeProgressBar.setMax(100);
        chromeProgressBar.setProgress(0);
        chromeProgressBar.setProgressDrawable(crearDrawableBarraCarga("#202124", "#8AB4F8"));
        chromeProgressBar.setVisibility(View.GONE);
        webViewContainer.addView(chromeProgressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        // Escenario que aloja las 4 vistas simultáneas
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

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void inicializarLosCuatroNavegadores() {
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            final SesionBot sesion = sesiones[i];

            // 1. Tarjeta de Consola Individual (para el modo "Solo Panel")
            LinearLayout cardConsolaInd = new LinearLayout(this);
            cardConsolaInd.setOrientation(LinearLayout.VERTICAL);
            cardConsolaInd.setBackground(fondoRedondeadoConBorde("#1E2022", "#2E3035", 8, 1));
            cardConsolaInd.setPadding(dp(1), dp(1), dp(1), dp(1));

            LinearLayout headerConsolaInd = new LinearLayout(this);
            headerConsolaInd.setOrientation(LinearLayout.HORIZONTAL);
            headerConsolaInd.setGravity(Gravity.CENTER_VERTICAL);
            headerConsolaInd.setBackgroundColor(Color.parseColor("#27272A"));
            headerConsolaInd.setPadding(dp(8), dp(4), dp(8), dp(4));

            TextView lblTitConInd = crearTexto("Sin cuenta", "#9CA3AF", 11f, true);
            TextView lblEstConInd = crearTexto("DETENIDO", "#9CA3AF", 9.5f, true);
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
            headerConsolaInd.addView(lblEstConInd, lpEstCon);
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
            sesion.lblEstadoConsolaInd = lblEstConInd;
            sesion.scrollConsolaIndividual = scrollInd;
            sesion.txtConsolaIndividual = txtInd;

            // 2. Tarjeta de Navegador Web con su propia barra de progreso individual
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(fondoRedondeadoConBorde("#18181B", "#2E3035", 6, 1));
            card.setPadding(dp(1), dp(1), dp(1), dp(1));

            LinearLayout headerMini = new LinearLayout(this);
            headerMini.setOrientation(LinearLayout.HORIZONTAL);
            headerMini.setGravity(Gravity.CENTER_VERTICAL);
            headerMini.setBackgroundColor(Color.parseColor("#27272A"));
            headerMini.setPadding(dp(6), dp(3), dp(6), dp(3));
            headerMini.setOnClickListener(v -> {
                slotInspeccionadoUrl = sesion.slotIndex;
                cambiarSlotEnfocado(sesion.slotIndex);
            });

            TextView lblTit = crearTexto("Sin cuenta", "#9CA3AF", 10.5f, true);
            TextView lblEst = crearTexto("DETENIDO", "#9CA3AF", 9, true);
            lblEst.setGravity(Gravity.END);

            headerMini.addView(lblTit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            headerMini.addView(lblEst, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            ProgressBar progMini = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progMini.setMax(100);
            progMini.setProgress(0);
            progMini.setProgressDrawable(crearDrawableBarraCarga("#18181B", "#38BDF8"));
            progMini.setVisibility(View.GONE);

            WebView wv = new WebView(this);
            wv.setBackgroundColor(Color.parseColor("#18181B"));

            wv.setOnTouchListener((View v, MotionEvent event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (slotInspeccionadoUrl != sesion.slotIndex) {
                        slotInspeccionadoUrl = sesion.slotIndex;
                        sincronizarBarraUrlConSesionActiva();
                        actualizarBarraCargaSuperior();
                        actualizarBotonesMandoSegunSeleccion();
                    }
                }
                return false;
            });

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
                    sesion.cargandoWeb = (newProgress < 100);
                    actualizarBarraCargaDeSesion(sesion);
                    if (newProgress >= 55) {
                        aplicarZoomASesion(sesion);
                    }
                }
            });

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    sesion.cargandoWeb = true;
                    sesion.progresoWeb = 15;
                    actualizarBarraCargaDeSesion(sesion);
                    actualizarUrlDeSesion(sesion, url);
                }

                @Override
                public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                    super.doUpdateVisitedHistory(view, url, isReload);
                    actualizarUrlDeSesion(sesion, url);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    sesion.cargandoWeb = false;
                    sesion.progresoWeb = 100;
                    actualizarBarraCargaDeSesion(sesion);
                    actualizarUrlDeSesion(sesion, url);
                    aplicarZoomASesion(sesion);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        sesion.cargandoWeb = false;
                        sesion.progresoWeb = 100;
                        actualizarBarraCargaDeSesion(sesion);
                        mantenerPaginaQuietaSinError(view);
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    sesion.cargandoWeb = false;
                    sesion.progresoWeb = 100;
                    actualizarBarraCargaDeSesion(sesion);
                    mantenerPaginaQuietaSinError(view);
                }
            });

            card.addView(headerMini, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.addView(progMini, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));
            card.addView(wv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            sesion.tarjetaCompleta = card;
            sesion.barraCabeceraMini = headerMini;
            sesion.lblTituloMini = lblTit;
            sesion.lblEstadoMini = lblEst;
            sesion.barraProgresoMini = progMini;
            sesion.webView = wv;

            wv.loadUrl(URL_LOGIN);
        }

        edtUrlBar.setText(URL_LOGIN);
        reorganizarVistasNavegadores();
        log("[SISTEMA] Motor Directo HTML/XHR listo (Compra instantanea por red a /int/store).");
    }

    private void actualizarBarraCargaDeSesion(SesionBot sesion) {
        if (sesion != null && sesion.barraProgresoMini != null) {
            if (sesion.cargandoWeb && sesion.progresoWeb < 100) {
                sesion.barraProgresoMini.setVisibility(View.VISIBLE);
                sesion.barraProgresoMini.setProgress(Math.max(12, sesion.progresoWeb));
            } else {
                sesion.barraProgresoMini.setVisibility(View.GONE);
                sesion.barraProgresoMini.setProgress(0);
            }
        }
        actualizarBarraCargaSuperior();
    }

    private void actualizarBarraCargaSuperior() {
        if (chromeProgressBar == null) return;
        if (slotEnfocado < 0) {
            chromeProgressBar.setVisibility(View.GONE);
            chromeProgressBar.setProgress(0);
            return;
        }
        SesionBot enfocada = sesiones[slotEnfocado];
        if (enfocada != null && enfocada.cargandoWeb && enfocada.progresoWeb < 100) {
            chromeProgressBar.setVisibility(View.VISIBLE);
            chromeProgressBar.setProgress(Math.max(12, enfocada.progresoWeb));
        } else {
            chromeProgressBar.setVisibility(View.GONE);
            chromeProgressBar.setProgress(0);
        }
    }

    private String limpiarUrlVisual(String url) {
        if (url == null) return URL_LOGIN;
        String clean = url.trim();
        if (clean.endsWith("#/")) {
            clean = clean.substring(0, clean.length() - 2);
        } else if (clean.endsWith("#")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.equals("https://tesoroenlinea.bt.com.ve") || clean.equals("https://tesoroenlinea.bt.com.ve/")) {
            return URL_LOGIN;
        }
        return clean;
    }

    private void actualizarUrlDeSesion(SesionBot sesion, String url) {
        if (url != null && url.startsWith("http")) {
            String urlLimpia = limpiarUrlVisual(url);
            sesion.ultimaUrlValida = urlLimpia;
            SesionBot obj = obtenerSesionControlUrl();
            if (obj != null && obj.slotIndex == sesion.slotIndex && !edtUrlBar.hasFocus()) {
                edtUrlBar.setText(urlLimpia);
            }
        }
    }

    private SesionBot obtenerSesionControlUrl() {
        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS) {
            return sesiones[slotEnfocado];
        }
        if (slotInspeccionadoUrl >= 0 && slotInspeccionadoUrl < MAX_CUENTAS_SIMULTANEAS && sesiones[slotInspeccionadoUrl].activa) {
            return sesiones[slotInspeccionadoUrl];
        }
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        for (SesionBot s : activas) {
            if (s.estaCorriendo()) return s;
        }
        if (!activas.isEmpty()) return activas.get(0);
        return sesiones[0];
    }

    private void sincronizarBarraUrlConSesionActiva() {
        final SesionBot obj = obtenerSesionControlUrl();
        if (obj == null) return;

        if (!edtUrlBar.hasFocus() && obj.ultimaUrlValida != null) {
            edtUrlBar.setText(obj.ultimaUrlValida);
        }
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

        ponerIconoYTexto(btnTabMosaico, crearIconoVectorial("GRID", slotEnfocado == -1 ? "#FFFFFF" : "#A1A1AA", 12), "Todas (" + cantActivas + ")");
        btnTabMosaico.setBackground(fondoRedondeado(slotEnfocado == -1 ? "#0284C7" : "#27272A", 6));
        btnTabMosaico.setTextColor(Color.parseColor(slotEnfocado == -1 ? "#FFFFFF" : "#A1A1AA"));

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            SesionBot s = sesiones[i];
            boolean corriendo = s.estaCorriendo();
            Drawable dotNeon = crearCirculoNeon(corriendo, pulsoNeon, 10);
            String colorTextoPanel = corriendo ? "#4ADE80" : "#9CA3AF";
            String nombre = s.nombreMostrado();

            ponerIconoYTexto(s.lblTituloMini, dotNeon, nombre);
            s.lblTituloMini.setTextColor(Color.parseColor(colorTextoPanel));

            if (s.lblTituloConsolaInd != null) {
                Drawable dotConsola = crearCirculoNeon(corriendo, pulsoNeon, 10);
                ponerIconoYTexto(s.lblTituloConsolaInd, dotConsola, nombre);
                s.lblTituloConsolaInd.setTextColor(Color.parseColor(colorTextoPanel));
            }

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

    private void reorganizarVistasNavegadores() {
        filaMosaicoSup.removeAllViews();
        filaMosaicoInf.removeAllViews();
        contenedorConsolasIndividuales.removeAllViews();

        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            ViewGroup parent = (ViewGroup) sesiones[i].tarjetaCompleta.getParent();
            if (parent != null) parent.removeView(sesiones[i].tarjetaCompleta);

            ViewGroup parentCon = (ViewGroup) sesiones[i].tarjetaConsolaIndividual.getParent();
            if (parentCon != null) parentCon.removeView(sesiones[i].tarjetaConsolaIndividual);
        }

        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (activas.isEmpty()) {
            activas.add(sesiones[0]);
        }

        for (SesionBot s : activas) {
            LinearLayout.LayoutParams lpConInd = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            lpConInd.setMargins(0, dp(3), 0, dp(3));
            contenedorConsolasIndividuales.addView(s.tarjetaConsolaIndividual, lpConInd);
        }

        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
        sincronizarBarraUrlConSesionActiva();
        actualizarBarraCargaSuperior();

        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS) {
            filaMosaicoSup.setVisibility(View.VISIBLE);
            filaMosaicoInf.setVisibility(View.GONE);
            for (SesionBot s : activas) {
                if (s.slotIndex == slotEnfocado) {
                    s.barraCabeceraMini.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams lpFull = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                    filaMosaicoSup.addView(s.tarjetaCompleta, lpFull);
                } else {
                    LinearLayout.LayoutParams lpBg = new LinearLayout.LayoutParams(1, 1);
                    filaMosaicoSup.addView(s.tarjetaCompleta, lpBg);
                }
            }
            return;
        }

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
        if (nuevoSlot >= 0) {
            slotInspeccionadoUrl = nuevoSlot;
        }
        reorganizarVistasNavegadores();
        inspeccionarEstadoYUrlReal(obtenerSesionControlUrl(), false);
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

    private ImageView crearBotonIconoNav(String tipoIcono, Runnable accion) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setImageDrawable(crearIconoVectorial(tipoIcono, "#E8EAED", 16));
        iv.setBackground(fondoRedondeado("#202124", 16));
        iv.setOnClickListener(v -> {
            iv.setBackground(fondoRedondeado("#3C4043", 16));
            handler.postDelayed(() -> iv.setBackground(fondoRedondeado("#202124", 16)), 180);
            if (accion != null) accion.run();
        });
        return iv;
    }

    private void toggleBarraZoomChrome() {
        if (barraZoomDesplegable.getVisibility() == View.VISIBLE) {
            barraZoomDesplegable.setVisibility(View.GONE);
        } else {
            barraZoomDesplegable.setVisibility(View.VISIBLE);
        }
    }

    private void forzarRecargaCuentaSeleccionada() {
        if (!hayInternetDisponible()) {
            log("[RED] Sin internet en el telefono. Las paginas se mantienen quietas.");
            return;
        }
        if (slotEnfocado >= 0) {
            recargarSesionIndividual(sesiones[slotEnfocado]);
            logCuenta(sesiones[slotEnfocado], "[NAV] Recargando pagina de la cuenta...");
        } else {
            for (SesionBot s : obtenerSesionesActivas()) {
                recargarSesionIndividual(s);
            }
            log("[NAV] Recargando todas las cuentas registradas...");
        }
    }

    private void recargarSesionIndividual(SesionBot s) {
        if (s != null && s.webView != null) {
            s.cargandoWeb = true;
            s.progresoWeb = 18;
            actualizarBarraCargaDeSesion(s);
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

    private void cambiarZoomWeb(int delta) {
        nivelZoom = Math.max(40, Math.min(150, nivelZoom + delta));
        prefs.edit().putInt("zoom_web", nivelZoom).apply();
        if (btnChromeZoom != null) btnChromeZoom.setText(nivelZoom + "%");
        if (lblZoomActualBarra != null) lblZoomActualBarra.setText("Zoom: " + nivelZoom + "%");
        for (int i = 0; i < MAX_CUENTAS_SIMULTANEAS; i++) {
            aplicarZoomASesion(sesiones[i]);
        }
        log("[ZOOM] Zoom ajustado a: " + nivelZoom + "%");
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
            log("[RED] No hay conexion a internet.");
            return;
        }

        SesionBot obj = obtenerSesionControlUrl();
        if (obj != null && obj.webView != null) {
            obj.cargandoWeb = true;
            obj.progresoWeb = 15;
            actualizarBarraCargaDeSesion(obj);
            obj.ultimaUrlValida = raw;
            obj.webView.stopLoading();
            obj.webView.loadUrl(raw);
            logCuenta(obj, "[NAV] Navegando a: " + raw);
        }
    }

    // =========================================================================================
    // CONTROL DE RED INDIVIDUAL POR NAVEGADOR
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
                            log("[RED] Internet desconectado. Cuentas en espera sin error.");
                        }
                    });
                }

                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        if (avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = false;
                            log("[RED] Internet restablecido. Retomando vigilancia...");
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

    private void reiniciarConexionIndividual(final SesionBot s, final boolean esManual) {
        if (s == null || s.microCortePropio) return;
        s.microCortePropio = true;
        s.peticionRedEnCurso = false;
        logCuenta(s, "[RED] Reiniciando conexion interna solo para esta cuenta...");

        final WebView wv = s.webView;
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
            if (esManual && s.estaCorriendo() && s.estadoActual == EstadoBot.CERRANDO_SESION) {
                s.accionEnCurso = false;
            }
            logCuenta(s, "[RED] Conexion interna restablecida.");
        }, 1400);
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
            contenedorControlesDividido.setVisibility(View.VISIBLE);
            contenedorConsolasIndividuales.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.VISIBLE);

            if (consolaVisible) {
                consoleWrapper.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.02f);
                lpPanel.setMargins(dp(8), dp(2), dp(8), dp(4));
                panelControlContainer.setLayoutParams(lpPanel);
            } else {
                consoleWrapper.setVisibility(View.GONE);
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
            contenedorControlesDividido.setVisibility(View.GONE);
            contenedorConsolasIndividuales.setVisibility(View.VISIBLE);
            webViewContainer.setVisibility(View.GONE);

            LinearLayout.LayoutParams lpPanel = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
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

        ponerIconoYTexto(btnVistaDividida, crearIconoVectorial("DIVIDIDO", modo == 0 ? "#FFFFFF" : "#A1A1AA", 13), "Dividido");
        btnVistaDividida.setTextColor(Color.parseColor(modo == 0 ? "#FFFFFF" : "#A1A1AA"));

        ponerIconoYTexto(btnVistaPanel, crearIconoVectorial("PANEL", modo == 1 ? "#FFFFFF" : "#A1A1AA", 13), "Solo Panel");
        btnVistaPanel.setTextColor(Color.parseColor(modo == 1 ? "#FFFFFF" : "#A1A1AA"));

        ponerIconoYTexto(btnVistaWeb, crearIconoVectorial("GRID", modo == 2 ? "#FFFFFF" : "#A1A1AA", 13), "Solo Banco");
        btnVistaWeb.setTextColor(Color.parseColor(modo == 2 ? "#FFFFFF" : "#A1A1AA"));

        actualizarDistribucionAlturas();
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
        reorganizarVistasNavegadores();
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
            txtConsola.append(hora + "  " + limpio + "\n");
            scrollConsola.post(() -> scrollConsola.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void logCuenta(final SesionBot s, String msg) {
        final String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String limpio = msg.replaceAll("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]", "").trim();
        runOnUiThread(() -> {
            txtConsola.append(hora + "  [" + s.nombreMostrado() + "] " + limpio + "\n");
            scrollConsola.post(() -> scrollConsola.fullScroll(View.FOCUS_DOWN));

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
        } else if (nuevoEstado == EstadoBot.VIGILANDO_HTML_DIRECTO || nuevoEstado == EstadoBot.COMPRANDO_DIRECTO_XHR) {
            colHex = "#4ADE80";
        } else {
            colHex = "#38BDF8";
        }

        if (s.lblEstadoMini != null) {
            s.lblEstadoMini.setText(txtEst);
            s.lblEstadoMini.setTextColor(Color.parseColor(colHex));
        }
        if (s.lblEstadoConsolaInd != null) {
            s.lblEstadoConsolaInd.setText(txtEst);
            s.lblEstadoConsolaInd.setTextColor(Color.parseColor(colHex));
        }

        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
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
            case ESPERANDO_CARGA_INICIO: return "ENTRANDO";
            case VIGILANDO_HTML_DIRECTO: return "VIGILANDO HTML";
            case COMPRANDO_DIRECTO_XHR: return "COMPRA XHR";
            case CERRANDO_SESION: return "SALIENDO";
            case REINICIANDO_RED: return "RECONECTANDO";
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
            slotInspeccionadoUrl = s.slotIndex;
            iniciarSesionIndividual(s);
            logCuenta(s, "[SISTEMA] Cuenta iniciada en modo Directo HTML/XHR.");
        } else {
            int iniciadas = 0;
            for (SesionBot s : activas) {
                if (!s.estaCorriendo()) {
                    iniciarSesionIndividual(s);
                    if (iniciadas == 0) slotInspeccionadoUrl = s.slotIndex;
                    iniciadas++;
                }
            }
            log("[SISTEMA] Vigilante Directo HTML/XHR iniciado en " + iniciadas + " cuenta(s).");
        }
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
        sincronizarBarraUrlConSesionActiva();
    }

    private void iniciarSesionIndividual(SesionBot s) {
        s.corriendo = true;
        s.vueltaActual = 0;
        s.intentoCreate = 0;
        s.intentoLogout = 0;
        s.tiempoInicioSesionMs = 0;
        s.compraCompletada = false;
        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
    }

    private void accionBotonDetener() {
        ArrayList<SesionBot> activas = obtenerSesionesActivas();
        if (slotEnfocado >= 0 && slotEnfocado < MAX_CUENTAS_SIMULTANEAS && sesiones[slotEnfocado].activa) {
            SesionBot s = sesiones[slotEnfocado];
            detenerSesionIndividual(s);
            logCuenta(s, "[SISTEMA] Cuenta detenida individualmente. Las demas siguen operando.");
        } else {
            for (SesionBot s : activas) {
                detenerSesionIndividual(s);
            }
            lblReloginVal.setText(minutosRelogin + " min (--:--)");
            log("[SISTEMA] Todas las cuentas han sido detenidas.");
        }
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
    }

    private void detenerSesionIndividual(SesionBot s) {
        s.corriendo = false;
        s.tiempoInicioSesionMs = 0;
        s.cargandoWeb = false;
        s.progresoWeb = 100;
        s.peticionRedEnCurso = false;
        if (s.webView != null) {
            s.webView.stopLoading();
        }
        actualizarBarraCargaDeSesion(s);
        cambiarEstado(s, EstadoBot.DETENIDO);
    }

    private void evaluarJS(SesionBot s, String script, ValueCallback<String> callback) {
        if (s == null || s.webView == null) return;
        s.webView.evaluateJavascript("(function() { try { " + script + " } catch(e) { return 'ERR:' + e.message; } })()", callback);
    }

    private void iniciarCicloReloj() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pulsoNeon = !pulsoNeon;
                actualizarEstilosIndicadoresNeon();

                ArrayList<SesionBot> activas = obtenerSesionesActivas();
                boolean internetOk = hayInternetDisponible();

                for (SesionBot s : activas) {
                    aplicarZoomASesion(s);
                    if (internetOk && !s.microCortePropio) {
                        inspeccionarEstadoYUrlReal(s, s.estaCorriendo());
                    } else if (!internetOk) {
                        s.tiempoEstadoMs = System.currentTimeMillis();
                    }
                }

                if (hayAlgunaCuentaCorriendo()) {
                    verificarTiemposDeSesionIndependientes(activas);
                } else {
                    lblReloginVal.setText(minutosRelogin + " min (--:--)");
                }
                handler.postDelayed(this, 600);
            }
        }, 600);
    }

    private void verificarTiemposDeSesionIndependientes(ArrayList<SesionBot> activas) {
        long ahora = System.currentTimeMillis();
        long totalSeg = minutosRelogin * 60L;
        long menorRestante = -1;

        for (SesionBot s : activas) {
            if (!s.estaCorriendo()) continue;

            if (s.tiempoInicioSesionMs > 0 && s.estadoActual == EstadoBot.VIGILANDO_HTML_DIRECTO) {
                long transcurridoSeg = (ahora - s.tiempoInicioSesionMs) / 1000;
                long restante = Math.max(0, totalSeg - transcurridoSeg);
                if (menorRestante == -1 || restante < menorRestante) {
                    menorRestante = restante;
                }
                if (transcurridoSeg >= totalSeg) {
                    logCuenta(s, "[SESION] Tiempo de sesion cumplido. Cerrando sesion de forma independiente...");
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

    private void inspeccionarEstadoYUrlReal(final SesionBot sesion, final boolean ejecutarBot) {
        if (sesion == null || sesion.webView == null) return;
        final long ahora = System.currentTimeMillis();

        String jsInspector =
                "if(document.body){ document.body.style.zoom = '" + nivelZoom + "%'; }" +
                "var ready = (document.readyState === 'complete');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var modalLogout = (bodyTxt.indexOf('deseas cerrar tu sesión') !== -1 || bodyTxt.indexOf('deseas cerrar tu sesion') !== -1);" +
                (ejecutarBot ?
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && btnX.offsetParent !== null && !modalLogout) { btnX.click(); }" +
                "var btnOk = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim() === 'OK' || el.innerText.trim() === 'Aceptar'));" +
                "if (btnOk && btnOk.offsetWidth > 0 && bodyTxt.indexOf('deseas realizar') === -1 && !modalLogout) { btnOk.click(); }"
                : "") +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector('a.btn-tesoro-green') || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var opsDiv = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "var visDoc = !!(doc && usr && doc.offsetWidth > 0 && usr.offsetWidth > 0 && !cargandoTxt);" +
                "var visPass = !!(pass && pass.offsetWidth > 0 && !cargandoTxt);" +
                "var visMonto = !!(monto && monto.offsetWidth > 0 && !cargandoTxt);" +
                "var visOrd = !!(ord && ord.offsetWidth > 0 && !cargandoTxt);" +
                "var tablaIntLista = !!(ready && !cargandoTxt && (bodyTxt.indexOf('operaciones de intervención') !== -1 || visOrd || visMonto));" +
                "var inicioListo = !!(ready && !cargandoTxt && (menuInt || opsDiv || btnLogout) && (bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || bodyTxt.indexOf('operaciones') !== -1 || bodyTxt.indexOf('últ. conexión') !== -1 || bodyTxt.indexOf('ult. conexión') !== -1));" +
                "var realUrl = window.location.origin + window.location.pathname;" +
                "if (tablaIntLista) {" +
                "  realUrl = 'https://tesoroenlinea.bt.com.ve/int';" +
                "  if (window.location.pathname !== '/int') { try { history.replaceState(null, '', '/int'); } catch(e){} }" +
                "} else if (inicioListo) {" +
                "  if (window.location.pathname.indexOf('/lg') !== -1 || window.location.pathname === '/') {" +
                "    realUrl = 'https://tesoroenlinea.bt.com.ve/ini';" +
                "    try { history.replaceState(null, '', '/ini'); } catch(e){}" +
                "  }" +
                "} else if (visPass) {" +
                "  realUrl = 'https://tesoroenlinea.bt.com.ve/lg/cl';" +
                "  if (window.location.pathname !== '/lg/cl') { try { history.replaceState(null, '', '/lg/cl'); } catch(e){} }" +
                "} else if (visDoc) {" +
                "  realUrl = 'https://tesoroenlinea.bt.com.ve/lg';" +
                "  if (window.location.pathname !== '/lg') { try { history.replaceState(null, '', '/lg'); } catch(e){} }" +
                "}" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1);" +
                "return JSON.stringify({" +
                "  url: realUrl," +
                "  ready: ready," +
                "  cargando: cargandoTxt," +
                "  doc: visDoc," +
                "  pass: visPass," +
                "  inicio: inicioListo," +
                "  tablaInt: tablaIntLista," +
                "  monto: visMonto," +
                "  ord: visOrd," +
                "  modalLogout: modalLogout," +
                "  sesionActivaErr: sesionActivaErr" +
                "});";

        evaluarJS(sesion, jsInspector, raw -> {
            if (raw == null || raw.equals("null")) return;
            try {
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\/", "/");
                }
                JSONObject st = new JSONObject(clean);
                String liveUrl = st.optString("url", "");
                if (!liveUrl.isEmpty() && sesion.estadoActual != EstadoBot.VIGILANDO_HTML_DIRECTO && sesion.estadoActual != EstadoBot.COMPRANDO_DIRECTO_XHR) {
                    actualizarUrlDeSesion(sesion, liveUrl);
                }

                if (!ejecutarBot || !sesion.estaCorriendo()) return;

                boolean nav100 = !sesion.cargandoWeb && (sesion.progresoWeb >= 100);
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
                        st.optBoolean("sesionActivaErr")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            SesionBot s, long ahora, boolean paginaCompleta, boolean visDoc, boolean visPass,
            boolean visInicioCompleto, boolean visTablaInt, boolean visMonto,
            boolean visOrd, boolean visModalLogout, boolean haySesionActivaErr
    ) {
        if (!s.estaCorriendo()) return;

        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (s.estadoActual) {

            case CARGANDO_LOGIN:
                if (paginaCompleta && (visInicioCompleto || visTablaInt || visMonto)) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "[SESION] Sesion activa detectada. Activando Motor Directo HTML/XHR...");
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                    ejecutarVigilanciaDirectaHtml(s);
                    return;
                }
                if (paginaCompleta && visPass) {
                    logCuenta(s, "[OK] Pantalla de contrasena cargada. Estabilizando 2s...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (paginaCompleta && visDoc) {
                    logCuenta(s, "[OK] Login cargado completo. Esperando 2s para estabilizar...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 25000) {
                    s.tiempoEstadoMs = ahora;
                    logCuenta(s, "[INFO] Esperando carga completa del login...");
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
                    logCuenta(s, "[FASE 1] Escribiendo Cedula y Usuario...");
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
                    logCuenta(s, "[OK] Fase 2 cargada. Esperando 2s para estabilizar...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 18000 && paginaCompleta && visDoc) {
                    logCuenta(s, "[INFO] No avanzo a contrasena. Reintentando Fase 1...");
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
                    logCuenta(s, "[FASE 2] Escribiendo Contrasena y enviando...");
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
                    logCuenta(s, "[INFO] Esperando sesion para activar Motor Directo HTML...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_INICIO);
                }
                break;

            case ESPERANDO_CARGA_INICIO:
                if (paginaCompleta && (visInicioCompleto || visTablaInt)) {
                    s.tiempoInicioSesionMs = ahora;
                    logCuenta(s, "[OK] Sesion iniciada. Pasando a Vigilancia Directa por HTML (/int)...");
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                    ejecutarVigilanciaDirectaHtml(s);
                    return;
                }
                if (haySesionActivaErr) {
                    logCuenta(s, "[ALERTA] El banco indica sesion activa. Reintentando...");
                    s.tiempoEstadoMs = ahora;
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 10000 && paginaCompleta && visDoc) {
                    logCuenta(s, "[INFO] Regreso al login. Reintentando...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            // =================================================================================
            // MODO DIRECTO POR RED HTML/XHR (SIN CLICS NI ESPERAS DE INTERFAZ)
            // =================================================================================
            case VIGILANDO_HTML_DIRECTO:
                if (paginaCompleta && visDoc) {
                    logCuenta(s, "[SESION] Sesion expirada. Reingresando al login...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (!s.peticionRedEnCurso && (ahora - s.tiempoEstadoMs >= intervaloRefresco * 1000L)) {
                    ejecutarVigilanciaDirectaHtml(s);
                }
                break;

            case COMPRANDO_DIRECTO_XHR:
                if (!s.peticionRedEnCurso && (ahora - s.tiempoEstadoMs >= 900)) {
                    ejecutarCompraDirectaCreateStore(s);
                }
                break;

            case CERRANDO_SESION:
                if (paginaCompleta && (visDoc || visPass)) {
                    logCuenta(s, "[SESION] Sesion cerrada correctamente. Reingresando...");
                    if (!visDoc) {
                        s.webView.loadUrl(URL_LOGIN);
                    }
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout++;
                    s.tiempoEstadoMs = ahora;
                    if (visModalLogout) {
                        logCuenta(s, "[SESION] Confirmando cierre de sesion (Intento #" + s.intentoLogout + ")...");
                    } else {
                        logCuenta(s, "[SESION] Cerrando sesion (Intento #" + s.intentoLogout + ")...");
                    }
                    ejecutarScriptLogout(s);
                } else if (ahora - s.tiempoEstadoMs > 5200) {
                    if (s.intentoLogout % 2 == 0) {
                        cambiarEstado(s, EstadoBot.REINICIANDO_RED);
                        reiniciarConexionIndividual(s, false);
                    } else {
                        s.accionEnCurso = false;
                    }
                }
                break;

            case REINICIANDO_RED:
                if (!s.microCortePropio && (ahora - s.tiempoEstadoMs >= 1600)) {
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;
        }
    }

    /**
     * Consulta directamente por red GET https://tesoroenlinea.bt.com.ve/int (3.1 kB HTML)
     * sin recargar la página ni esperar renderizado de tablas.
     */
    private void ejecutarVigilanciaDirectaHtml(final SesionBot s) {
        if (!s.estaCorriendo() || s.peticionRedEnCurso) return;
        s.peticionRedEnCurso = true;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.vueltaActual++;
        actualizarUrlDeSesion(s, "https://tesoroenlinea.bt.com.ve/int");

        String jsFetchInt =
                "window.__vbt_int_res = 'WAIT';" +
                "(async function(){" +
                "  try {" +
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
                "    var cont = document.querySelector('#container') || document.querySelector('#content');" +
                "    if (cont && txt.indexOf('Operaciones de Intervención') !== -1) { cont.innerHTML = txt; }" +
                "    var tieneOrd = (txt.indexOf('mod.add(1)') !== -1 || txt.indexOf('btn-tesoro-green') !== -1 || txt.indexOf('Ordinaria') !== -1);" +
                "    var esLogin = (txt.indexOf('id=\"documento\"') !== -1 && txt.indexOf('id=\"username\"') !== -1);" +
                "    window.__vbt_int_res = JSON.stringify({ status: r.status, ord: tieneOrd, login: esLogin, len: txt.length });" +
                "  } catch(e) {" +
                "    window.__vbt_int_res = JSON.stringify({ status: -1, err: e.message });" +
                "  }" +
                "})();";

        evaluarJS(s, jsFetchInt, null);
        sondearResultadoInt(s, 0);
    }

    private void sondearResultadoInt(final SesionBot s, final int intentoPoll) {
        if (!s.estaCorriendo() || s.estadoActual != EstadoBot.VIGILANDO_HTML_DIRECTO) {
            s.peticionRedEnCurso = false;
            return;
        }
        handler.postDelayed(() -> {
            evaluarJS(s, "return window.__vbt_int_res || 'WAIT';", raw -> {
                if (!s.estaCorriendo() || s.estadoActual != EstadoBot.VIGILANDO_HTML_DIRECTO) {
                    s.peticionRedEnCurso = false;
                    return;
                }
                if (raw == null || raw.contains("WAIT")) {
                    if (intentoPoll < 25) {
                        sondearResultadoInt(s, intentoPoll + 1);
                    } else {
                        s.peticionRedEnCurso = false;
                        logCuenta(s, "[RED] Tiempo agotado consultando /int. Reintentando...");
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
                        logCuenta(s, "[SESION] El servidor devolvio pantalla de login. Reingresando...");
                        s.webView.loadUrl(URL_LOGIN);
                        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                        return;
                    }
                    if (res.optBoolean("ord", false)) {
                        logCuenta(s, "[ALERTA] Codigo 'mod.add(1)' (Ordinaria) detectado en HTML (/int)!");
                        if (!yaAvisoApertura) { yaAvisoApertura = true; sonarAlarma(false); }
                        s.intentoCreate = 0;
                        cambiarEstado(s, EstadoBot.COMPRANDO_DIRECTO_XHR);
                        ejecutarCompraDirectaCreateStore(s);
                    } else {
                        logCuenta(s, "[Vuelta " + s.vueltaActual + "] HTML /int leido (" + res.optInt("len", 0) + " B). Sin boton Ordinaria.");
                    }
                } catch (Exception e) {
                    logCuenta(s, "[Vuelta " + s.vueltaActual + "] Respuesta /int verificada.");
                }
            });
        }, 160);
    }

    /**
     * Dispara directamente por red:
     * 1) POST https://tesoroenlinea.bt.com.ve/int/create
     * 2) Extrae en memoria _token, _form_sig, cuentas desde/hasta, tasa_usd y saldo_usd
     * 3) Dispara instantáneamente POST https://tesoroenlinea.bt.com.ve/int/store con los datos exactos.
     */
    private void ejecutarCompraDirectaCreateStore(final SesionBot s) {
        if (!s.estaCorriendo() || s.peticionRedEnCurso) return;
        s.peticionRedEnCurso = true;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.intentoCreate++;
        actualizarUrlDeSesion(s, "https://tesoroenlinea.bt.com.ve/int/create");

        logCuenta(s, "[DIRECTO] Solicitando HTML de formulario (POST /int/create - Intento #" + s.intentoCreate + ")...");

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
        if (!s.estaCorriendo() || s.estadoActual != EstadoBot.COMPRANDO_DIRECTO_XHR) {
            s.peticionRedEnCurso = false;
            return;
        }
        handler.postDelayed(() -> {
            evaluarJS(s, "return window.__vbt_buy_res || 'WAIT';", raw -> {
                if (!s.estaCorriendo() || s.estadoActual != EstadoBot.COMPRANDO_DIRECTO_XHR) {
                    s.peticionRedEnCurso = false;
                    return;
                }
                if (raw == null || raw.contains("WAIT")) {
                    if (intentoPoll < 35) {
                        sondearResultadoCompraDirecta(s, intentoPoll + 1);
                    } else {
                        s.peticionRedEnCurso = false;
                        logCuenta(s, "[AVISO] Tiempo de espera agotado en /int/create. Reintentando...");
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
                        logCuenta(s, "[COOKIE] Desafio de sesion resuelto en memoria. Reenviando /int/create...");
                        s.tiempoEstadoMs = System.currentTimeMillis() - 600;
                        return;
                    }

                    if (step.equals("CREATE_ERR") || step.equals("NO_SIG")) {
                        if (s.intentoCreate < 2) {
                            logCuenta(s, "[AVISO] Fallo en Intento #1 de /int/create. Disparo inmediato de Intento #2...");
                            s.tiempoEstadoMs = System.currentTimeMillis() - 500;
                        } else {
                            logCuenta(s, "[ERROR] El servidor rechazo ambos intentos de /int/create. Reiniciando sesion de esta cuenta...");
                            s.intentoLogout = 0;
                            cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                        }
                        return;
                    }

                    if (step.equals("NO_ACCOUNTS")) {
                        logCuenta(s, "[ERROR] El formulario no trajo las cuentas bancarias en #desde / #hasta. Reintentando...");
                        cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                        return;
                    }

                    if (step.equals("STORE_DONE")) {
                        String tasa = res.optString("tasa", "0");
                        double pool = res.optDouble("pool", 0);
                        String montoUsd = res.optString("montoUsd", "300.00");
                        String totalBs = res.optString("totalBs", "0,00");
                        String respRaw = res.optString("resp", "");

                        logCuenta(s, String.format(Locale.US,
                                "[DATOS HTML] Tasa BCV: %s | Pool Banco: %,.0f$ | Enviado: %s$ (%s Bs)",
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
                            logCuenta(s, "[EXITO DIRECTO] Respuesta de /int/store: {\"ok\":true} -> ORDEN REGISTRADA EXITOSAMENTE!");
                            sonarAlarma(true);
                            s.compraCompletada = true;
                            s.corriendo = false;
                            cambiarEstado(s, EstadoBot.DETENIDO);
                            verificarSiTodasCompraron();
                        } else {
                            logCuenta(s, "[RESPUESTA STORE] " + msgBanco + " -> Retomando vigilancia HTML...");
                            cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                        }
                    } else {
                        logCuenta(s, "[AVISO] Error de red en compra directa. Reintentando...");
                        cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                    }
                } catch (Exception e) {
                    cambiarEstado(s, EstadoBot.VIGILANDO_HTML_DIRECTO);
                }
            });
        }, 160);
    }

    private void verificarSiTodasCompraron() {
        for (SesionBot s : obtenerSesionesActivas()) {
            if (!s.compraCompletada) return;
        }
        log("[EXITO] TODAS LAS CUENTAS REGISTRADAS COMPRARON CON EXITO");
        actualizarEstilosIndicadoresNeon();
        actualizarBotonesMandoSegunSeleccion();
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
}
