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
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final String ORIGEN_FONDOS = "Sueldos y Salarios";
    private static final String DESTINO_FONDOS = "Ahorro";
    private static final int MAX_CUENTAS = 4;
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};

    // Vistas principales (Cuentas desplazable e Historial)
    private ScrollView vistaCuentasScroll;
    private LinearLayout contenedorTarjetasCuentas;
    private LinearLayout vistaHistorialContainer;
    private LinearLayout barraFiltrosHistorial;
    private ScrollView scrollHistorialConsola;
    private TextView txtHistorialConsola;

    // Contenedor de motores en tamaño real detrás del fondo oscuro
    private LinearLayout filaMotoresSup, filaMotoresInf;

    // Cabecera, chips y controles
    private TextView badgeEstadoGlobal;
    private TextView chipRegistradas, chipActivas, chipDetenidas;
    private TextView lblRefrescoVal, lblReloginVal;
    private Button btnIniciarTodas, btnDetenerTodas, btnNuevaCuenta;

    // Barra flotante inferior (Cuentas | Historial)
    private LinearLayout barraFlotante;
    private LinearLayout navBtnCuentas, navBtnHistorial;
    private ImageView navIconCuentas, navIconHistorial;
    private TextView navTxtCuentas, navTxtHistorial;

    // 0 = Cuentas (Principal), 1 = Historial
    private int pestanaInferiorActual = 0;
    // -1 = Todas las cuentas en el Historial, 0..3 = Filtrado por cuenta
    private int filtroHistorialSlot = -1;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;

    private boolean pulsoNeon = false;
    private boolean yaAvisoApertura = false;
    private boolean avisoSinInternetMostrado = false;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final ArrayList<EntradaLog> historialGlobalLogs = new ArrayList<>();

    private static class EntradaLog {
        final int slotIndex;
        final String usuario;
        final String hora;
        final String mensaje;

        EntradaLog(int slotIndex, String usuario, String hora, String mensaje) {
            this.slotIndex = slotIndex;
            this.usuario = usuario;
            this.hora = hora;
            this.mensaje = mensaje;
        }
    }

    private enum EstadoBot {
        DETENIDO,
        CARGANDO_LOGIN,
        ESPERANDO_ESTABILIZAR_LOGIN,
        ENVIANDO_FASE1,
        ESPERANDO_CARGA_PASS,
        ESPERANDO_ESTABILIZAR_PASS,
        ENVIANDO_FASE2,
        ESPERANDO_CARGA_INICIO,
        ESPERANDO_REINTENTO_SESION_ACTIVA,
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

    private class SesionBot {
        final int slotIndex;
        boolean registrada = false;
        boolean corriendo = false;
        boolean detenerTrasCerrarSesion = false;
        boolean sesionAbiertaEnServidor = false;
        boolean nombreOculto = false;
        boolean consolaDesplegada = false;

        String tipoDoc = "V";
        String cedula = "";
        String usuario = "";
        String contrasena = "";
        int montoCompra = 300;

        String saldoBs = "--";
        String saldoUsd = "--";
        long ultimaActualizacionSaldoMs = 0;
        String ultimoPasoTexto = "Sin actividad todavía";

        // Motor WebView en segundo plano
        WebView webViewOculto;

        // Elementos visuales de la Tarjeta de Cuenta
        LinearLayout cardView;
        TextView lblUsuarioCard;
        TextView badgeEstadoCard;
        TextView lblSaldosCard;
        TextView lblSubtituloPasoCard;
        ImageView btnPlayCard, btnStopCard, btnLogoutCard, btnEyeCard, btnEditCard;
        LinearLayout contenedorConsolaCard;
        ScrollView scrollConsolaCard;
        TextView txtConsolaCard;

        EstadoBot estadoActual = EstadoBot.DETENIDO;
        int progresoWeb = 100;
        boolean cargandoRed = false;
        long tiempoInicioSesionMs = 0;
        long tiempoEstadoMs = 0;
        boolean accionEnCurso = false;
        boolean microCortePropio = false;
        int vueltaActual = 0;
        int intentoLogout = 0;
        boolean compraCompletada = false;

        SesionBot(int idx) {
            this.slotIndex = idx;
        }

        String nombreMostrado() {
            if (usuario == null || usuario.isEmpty()) return "Sin cuenta";
            if (nombreOculto) return "********";
            return usuario;
        }

        String cedulaMostrada() {
            if (cedula == null || cedula.isEmpty()) return "";
            if (nombreOculto) return "********";
            return tipoDoc + "-" + cedula;
        }

        String textoSaldosMostrado() {
            if (nombreOculto) {
                return "Bs: ****   •   USD: ****   •   Orden: " + montoCompra + "$";
            }
            return "Bs: " + saldoBs + "   •   USD: " + saldoUsd + "   •   Orden: " + montoCompra + "$";
        }

        boolean estaCorriendo() {
            return registrada && corriendo && !compraCompletada && estadoActual != EstadoBot.DETENIDO;
        }
    }

    private final SesionBot[] sesiones = new SesionBot[MAX_CUENTAS];
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
        getWindow().setStatusBarColor(Color.parseColor("#0F1115"));
        getWindow().setNavigationBarColor(Color.parseColor("#0F1115"));
        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);

        for (int i = 0; i < MAX_CUENTAS; i++) {
            sesiones[i] = new SesionBot(i);
        }

        construirInterfazCompleta();
        inicializarMotoresOcultos();
        registrarMonitorPasivoRed();
        cargarCuentasGuardadas();
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
            for (int i = 0; i < MAX_CUENTAS; i++) {
                WebView wv = sesiones[i].webViewOculto;
                if (wv != null) {
                    wv.evaluateJavascript("if(typeof Logout==='function'){Logout();}", null);
                    wv.stopLoading();
                }
            }
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
            case "HISTORY": {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(s * 0.50f, s * 0.50f, s * 0.34f, p);
                c.drawLine(s * 0.50f, s * 0.30f, s * 0.50f, s * 0.50f, p);
                c.drawLine(s * 0.50f, s * 0.50f, s * 0.66f, s * 0.60f, p);
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
                play.moveTo(s * 0.28f, s * 0.20f);
                play.lineTo(s * 0.80f, s * 0.50f);
                play.lineTo(s * 0.28f, s * 0.80f);
                play.close();
                c.drawPath(play, p);
                break;
            }
            case "STOP": {
                p.setStyle(Paint.Style.FILL);
                RectF r = new RectF(s * 0.24f, s * 0.24f, s * 0.76f, s * 0.76f);
                c.drawRoundRect(r, s * 0.12f, s * 0.12f, p);
                break;
            }
            case "LOGOUT": {
                p.setStyle(Paint.Style.STROKE);
                Path door = new Path();
                door.moveTo(s * 0.48f, s * 0.18f);
                door.lineTo(s * 0.22f, s * 0.18f);
                door.lineTo(s * 0.22f, s * 0.82f);
                door.lineTo(s * 0.48f, s * 0.82f);
                c.drawPath(door, p);
                c.drawLine(s * 0.36f, s * 0.50f, s * 0.82f, s * 0.50f, p);
                c.drawLine(s * 0.64f, s * 0.32f, s * 0.82f, s * 0.50f, p);
                c.drawLine(s * 0.64f, s * 0.68f, s * 0.82f, s * 0.50f, p);
                break;
            }
            case "EDIT": {
                p.setStyle(Paint.Style.STROKE);
                Path pen = new Path();
                pen.moveTo(s * 0.20f, s * 0.80f);
                pen.lineTo(s * 0.36f, s * 0.76f);
                pen.lineTo(s * 0.78f, s * 0.34f);
                pen.lineTo(s * 0.66f, s * 0.22f);
                pen.lineTo(s * 0.24f, s * 0.64f);
                pen.close();
                c.drawPath(pen, p);
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

    // =========================================================================================
    // CONSTRUCCIÓN DE LA INTERFAZ COMPLETA
    // =========================================================================================
    @SuppressLint("SetJavaScriptEnabled")
    private void construirInterfazCompleta() {
        FrameLayout frameRoot = new FrameLayout(this);
        frameRoot.setBackgroundColor(Color.parseColor("#0F1115"));
        frameRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Cuadrícula 2x2 de tamaño real detrás del fondo oscuro para que Android nunca pause los WebViews
        LinearLayout contenedorMotores2x2 = new LinearLayout(this);
        contenedorMotores2x2.setOrientation(LinearLayout.VERTICAL);
        filaMotoresSup = new LinearLayout(this);
        filaMotoresSup.setOrientation(LinearLayout.HORIZONTAL);
        filaMotoresInf = new LinearLayout(this);
        filaMotoresInf.setOrientation(LinearLayout.HORIZONTAL);
        contenedorMotores2x2.addView(filaMotoresSup, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contenedorMotores2x2.addView(filaMotoresInf, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        frameRoot.addView(contenedorMotores2x2, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainVertical = new LinearLayout(this);
        mainVertical.setOrientation(LinearLayout.VERTICAL);
        mainVertical.setBackgroundColor(Color.parseColor("#0F1115"));
        mainVertical.setClickable(true);
        mainVertical.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. CABECERA SUPERIOR FIJA
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), dp(12), dp(16), dp(8));

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

        TextView lblTituloApp = new TextView(this);
        lblTituloApp.setText("Vigilante BT");
        lblTituloApp.setTextColor(Color.parseColor("#F4F4F5"));
        lblTituloApp.setTextSize(18);
        lblTituloApp.setTypeface(null, Typeface.BOLD);
        topBar.addView(lblTituloApp, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        badgeEstadoGlobal = new TextView(this);
        ponerIconoYTexto(badgeEstadoGlobal, crearCirculoNeon(false, false, 10), "APAGADO");
        badgeEstadoGlobal.setTextColor(Color.parseColor("#FCA5A5"));
        badgeEstadoGlobal.setTextSize(11);
        badgeEstadoGlobal.setTypeface(null, Typeface.BOLD);
        badgeEstadoGlobal.setGravity(Gravity.CENTER);
        badgeEstadoGlobal.setPadding(dp(12), dp(5), dp(12), dp(5));
        badgeEstadoGlobal.setBackground(fondoRedondeado("#450A0A", 12));
        topBar.addView(badgeEstadoGlobal);
        mainVertical.addView(topBar);

        // 2. VISTA PRINCIPAL DESPLAZABLE: "CUENTAS"
        vistaCuentasScroll = new ScrollView(this);
        vistaCuentasScroll.setFillViewport(true);
        vistaCuentasScroll.setVerticalScrollBarEnabled(false);

        LinearLayout contenidoScrollCuentas = new LinearLayout(this);
        contenidoScrollCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.setPadding(dp(14), dp(4), dp(14), dp(110));

        LinearLayout filaHeaderCuentas = new LinearLayout(this);
        filaHeaderCuentas.setOrientation(LinearLayout.HORIZONTAL);
        filaHeaderCuentas.setGravity(Gravity.CENTER_VERTICAL);
        filaHeaderCuentas.setPadding(0, dp(4), 0, dp(6));

        LinearLayout colTitCuentas = new LinearLayout(this);
        colTitCuentas.setOrientation(LinearLayout.VERTICAL);
        TextView lblCuentasBig = crearTexto("Cuentas", "#F4F4F5", 22f, true);
        TextView lblCuentasSub = crearTexto("Toca cualquier cuenta para abrir o cerrar su consola en vivo.", "#A1A1AA", 11.5f, false);
        colTitCuentas.addView(lblCuentasBig);
        colTitCuentas.addView(lblCuentasSub);
        filaHeaderCuentas.addView(colTitCuentas, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNuevaCuenta = crearBotonConIcono("PLUS", "Nueva", "#0284C7", "#FFFFFF", 12, () -> mostrarPopupEditorCuenta(-1));
        btnNuevaCuenta.setPadding(dp(12), 0, dp(12), 0);
        filaHeaderCuentas.addView(btnNuevaCuenta, new LinearLayout.LayoutParams(dp(92), dp(36)));
        contenidoScrollCuentas.addView(filaHeaderCuentas);

        // Chips de resumen
        LinearLayout filaChips = new LinearLayout(this);
        filaChips.setOrientation(LinearLayout.HORIZONTAL);
        filaChips.setPadding(0, dp(4), 0, dp(10));

        chipRegistradas = crearChipResumen("0 registradas", "#1E293B", "#38BDF8");
        chipActivas = crearChipResumen("0 activas", "#14532D", "#4ADE80");
        chipDetenidas = crearChipResumen("0 detenidas", "#27272A", "#A1A1AA");

        LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChip.setMargins(0, 0, dp(8), 0);
        filaChips.addView(chipRegistradas, lpChip);
        filaChips.addView(chipActivas, lpChip);
        filaChips.addView(chipDetenidas, lpChip);
        contenidoScrollCuentas.addView(filaChips);

        // Tarjeta de Tiempos
        LinearLayout cardTiempos = new LinearLayout(this);
        cardTiempos.setOrientation(LinearLayout.VERTICAL);
        cardTiempos.setPadding(dp(12), dp(10), dp(12), dp(10));
        cardTiempos.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 12, 1));
        LinearLayout.LayoutParams lpCardT = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCardT.setMargins(0, 0, 0, dp(10));

        LinearLayout filaRef = new LinearLayout(this);
        filaRef.setGravity(Gravity.CENTER_VERTICAL);
        TextView tRef = crearTexto("Frecuencia Refresco:", "#F4F4F5", 11.5f, true);
        SeekBar sldRef = new SeekBar(this);
        sldRef.setMax(29);
        sldRef.setProgress(2);
        lblRefrescoVal = crearTexto("3 seg", "#F4F4F5", 11.5f, true);
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
        filaRel.setPadding(0, dp(6), 0, 0);
        TextView tRel = crearTexto("Reinicio Sesión:", "#F4F4F5", 11.5f, true);
        SeekBar sldRel = new SeekBar(this);
        sldRel.setMax(OPCIONES_MINUTOS.length - 1);
        sldRel.setProgress(3);
        lblReloginVal = crearTexto("10 min (--:--)", "#F4F4F5", 11f, true);
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
        contenidoScrollCuentas.addView(cardTiempos, lpCardT);

        // Botones Globales: "Iniciar todas" y "Detener todas"
        LinearLayout filaBotonesGlobales = new LinearLayout(this);
        filaBotonesGlobales.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpFilaGlob = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpFilaGlob.setMargins(0, 0, 0, dp(12));

        btnIniciarTodas = crearBotonConIcono("PLAY", "Iniciar todas", "#16A34A", "#FFFFFF", 13, this::iniciarTodasLasCuentas);
        btnDetenerTodas = crearBotonConIcono("STOP", "Detener todas", "#7F1D1D", "#FCA5A5", 13, this::detenerTodasLasCuentas);
        btnDetenerTodas.setEnabled(false);

        LinearLayout.LayoutParams lpBtnG = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lpBtnG.setMargins(dp(3), 0, dp(3), 0);
        filaBotonesGlobales.addView(btnIniciarTodas, lpBtnG);
        filaBotonesGlobales.addView(btnDetenerTodas, lpBtnG);
        contenidoScrollCuentas.addView(filaBotonesGlobales, lpFilaGlob);

        // Lista vertical de Tarjetas de cada cuenta
        contenedorTarjetasCuentas = new LinearLayout(this);
        contenedorTarjetasCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.addView(contenedorTarjetasCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        vistaCuentasScroll.addView(contenidoScrollCuentas);
        mainVertical.addView(vistaCuentasScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3. VISTA SECUNDARIA: "HISTORIAL"
        vistaHistorialContainer = new LinearLayout(this);
        vistaHistorialContainer.setOrientation(LinearLayout.VERTICAL);
        vistaHistorialContainer.setPadding(dp(14), dp(6), dp(14), dp(105));
        vistaHistorialContainer.setVisibility(View.GONE);

        LinearLayout headerHist = new LinearLayout(this);
        headerHist.setOrientation(LinearLayout.HORIZONTAL);
        headerHist.setGravity(Gravity.CENTER_VERTICAL);
        headerHist.setPadding(0, 0, 0, dp(8));

        LinearLayout colTitHist = new LinearLayout(this);
        colTitHist.setOrientation(LinearLayout.VERTICAL);
        colTitHist.addView(crearTexto("Historial de Consola", "#F4F4F5", 20f, true));
        colTitHist.addView(crearTexto("Actividad en tiempo real de las cuentas registradas", "#A1A1AA", 11.5f, false));
        headerHist.addView(colTitHist, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button btnLimpiarHist = crearBoton("Limpiar", "#27272A", "#E4E4E7", 11, () -> {
            historialGlobalLogs.clear();
            for (int i = 0; i < MAX_CUENTAS; i++) {
                if (sesiones[i].txtConsolaCard != null) sesiones[i].txtConsolaCard.setText("");
            }
            refrescarVistaHistorial();
        });
        headerHist.addView(btnLimpiarHist, new LinearLayout.LayoutParams(dp(74), dp(32)));
        vistaHistorialContainer.addView(headerHist);

        HorizontalScrollView scrollFiltros = new HorizontalScrollView(this);
        scrollFiltros.setHorizontalScrollBarEnabled(false);
        barraFiltrosHistorial = new LinearLayout(this);
        barraFiltrosHistorial.setOrientation(LinearLayout.HORIZONTAL);
        barraFiltrosHistorial.setPadding(0, dp(2), 0, dp(8));
        scrollFiltros.addView(barraFiltrosHistorial);
        vistaHistorialContainer.addView(scrollFiltros, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollHistorialConsola = new ScrollView(this);
        scrollHistorialConsola.setBackground(fondoRedondeadoConBorde("#14161A", "#27272A", 12, 1));
        scrollHistorialConsola.setPadding(dp(12), dp(10), dp(12), dp(10));
        txtHistorialConsola = new TextView(this);
        txtHistorialConsola.setTextColor(Color.parseColor("#E4E4E7"));
        txtHistorialConsola.setTextSize(11f);
        txtHistorialConsola.setTypeface(Typeface.MONOSPACE);
        scrollHistorialConsola.addView(txtHistorialConsola);

        vistaHistorialContainer.addView(scrollHistorialConsola, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        mainVertical.addView(vistaHistorialContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        frameRoot.addView(mainVertical);

        // 4. BARRA FLOTANTE INFERIOR ("Cuentas" | "Historial")
        barraFlotante = new LinearLayout(this);
        barraFlotante.setOrientation(LinearLayout.HORIZONTAL);
        barraFlotante.setGravity(Gravity.CENTER);
        barraFlotante.setPadding(dp(6), dp(5), dp(6), dp(5));
        barraFlotante.setBackground(fondoRedondeadoConBorde("#18181B", "#2E323B", 30, 1));
        barraFlotante.setVisibility(View.GONE);

        navBtnCuentas = new LinearLayout(this);
        navBtnCuentas.setOrientation(LinearLayout.VERTICAL);
        navBtnCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.setPadding(dp(6), dp(6), dp(6), dp(6));
        navIconCuentas = new ImageView(this);
        navTxtCuentas = crearTexto("Cuentas", "#FFFFFF", 11.5f, true);
        navTxtCuentas.setSingleLine(true);
        navTxtCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.addView(navIconCuentas, new LinearLayout.LayoutParams(dp(22), dp(22)));
        navBtnCuentas.addView(navTxtCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnCuentas.setOnClickListener(v -> cambiarPestanaInferior(0));

        navBtnHistorial = new LinearLayout(this);
        navBtnHistorial.setOrientation(LinearLayout.VERTICAL);
        navBtnHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.setPadding(dp(6), dp(6), dp(6), dp(6));
        navIconHistorial = new ImageView(this);
        navTxtHistorial = crearTexto("Historial", "#A1A1AA", 11.5f, true);
        navTxtHistorial.setSingleLine(true);
        navTxtHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.addView(navIconHistorial, new LinearLayout.LayoutParams(dp(22), dp(22)));
        navBtnHistorial.addView(navTxtHistorial, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnHistorial.setOnClickListener(v -> cambiarPestanaInferior(1));

        barraFlotante.addView(navBtnCuentas, new LinearLayout.LayoutParams(dp(118), dp(54)));
        View separadorNav = new View(this);
        barraFlotante.addView(separadorNav, new LinearLayout.LayoutParams(dp(6), dp(1)));
        barraFlotante.addView(navBtnHistorial, new LinearLayout.LayoutParams(dp(118), dp(54)));

        FrameLayout.LayoutParams lpFloating = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpFloating.setMargins(0, 0, 0, dp(16));
        frameRoot.addView(barraFlotante, lpFloating);

        actualizarEstiloBarraFlotante();

        // 5. PANTALLA DE ENTRADA EN GRIS OSCURO (#18181B)
        LinearLayout splashOverlay = new LinearLayout(this);
        splashOverlay.setOrientation(LinearLayout.VERTICAL);
        splashOverlay.setGravity(Gravity.CENTER);
        splashOverlay.setBackgroundColor(Color.parseColor("#18181B"));
        splashOverlay.setClickable(true);
        splashOverlay.setElevation(dp(100));
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
                        barraFlotante.setVisibility(View.VISIBLE);
                    })
                    .start();
        }, 3000);
    }

    private TextView crearChipResumen(String texto, String bgHex, String fgHex) {
        TextView tv = crearTexto(texto, fgHex, 11f, true);
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        tv.setBackground(fondoRedondeado(bgHex, 14));
        return tv;
    }

    private void cambiarPestanaInferior(int nuevaPestana) {
        pestanaInferiorActual = nuevaPestana;
        if (nuevaPestana == 0) {
            vistaCuentasScroll.setVisibility(View.VISIBLE);
            vistaHistorialContainer.setVisibility(View.GONE);
        } else {
            vistaCuentasScroll.setVisibility(View.GONE);
            vistaHistorialContainer.setVisibility(View.VISIBLE);
            reconstruirFiltrosHistorial();
            refrescarVistaHistorial();
        }
        actualizarEstiloBarraFlotante();
    }

    private void actualizarEstiloBarraFlotante() {
        boolean enCuentas = (pestanaInferiorActual == 0);

        navBtnCuentas.setBackground(fondoRedondeado(enCuentas ? "#0284C7" : "#18181B", 24));
        navIconCuentas.setImageDrawable(crearIconoVectorial("USERS", enCuentas ? "#FFFFFF" : "#A1A1AA", 20));
        navTxtCuentas.setTextColor(Color.parseColor(enCuentas ? "#FFFFFF" : "#A1A1AA"));

        navBtnHistorial.setBackground(fondoRedondeado(!enCuentas ? "#0284C7" : "#18181B", 24));
        navIconHistorial.setImageDrawable(crearIconoVectorial("HISTORY", !enCuentas ? "#FFFFFF" : "#A1A1AA", 20));
        navTxtHistorial.setTextColor(Color.parseColor(!enCuentas ? "#FFFFFF" : "#A1A1AA"));
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void inicializarMotoresOcultos() {
        for (int i = 0; i < MAX_CUENTAS; i++) {
            final SesionBot sesion = sesiones[i];

            // 1. Construir Tarjeta de la Cuenta
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 14, 1));

            card.setOnClickListener(v -> {
                sesion.consolaDesplegada = !sesion.consolaDesplegada;
                sesion.contenedorConsolaCard.setVisibility(sesion.consolaDesplegada ? View.VISIBLE : View.GONE);
                if (sesion.consolaDesplegada && sesion.scrollConsolaCard != null) {
                    sesion.scrollConsolaCard.post(() -> sesion.scrollConsolaCard.fullScroll(View.FOCUS_DOWN));
                }
            });

            // Fila 1: Usuario + Estado
            LinearLayout filaTopCard = new LinearLayout(this);
            filaTopCard.setOrientation(LinearLayout.HORIZONTAL);
            filaTopCard.setGravity(Gravity.CENTER_VERTICAL);

            TextView lblUsr = crearTexto("Sin cuenta", "#9CA3AF", 15f, true);
            TextView badgeEst = crearTexto("DETENIDO", "#9CA3AF", 10f, true);
            badgeEst.setPadding(dp(10), dp(4), dp(10), dp(4));
            badgeEst.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 12, 1));

            filaTopCard.addView(lblUsr, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            filaTopCard.addView(badgeEst);
            card.addView(filaTopCard);

            // Fila 2: Saldos en vivo (Bs y USD) + Monto configurado
            TextView lblSaldos = crearTexto("Bs: --   •   USD: --   •   Orden: 300$", "#38BDF8", 11.5f, true);
            lblSaldos.setPadding(0, dp(5), 0, dp(2));
            card.addView(lblSaldos);

            // Fila 3: Paso actual en vivo
            TextView lblPaso = crearTexto("Sin actividad todavía", "#A1A1AA", 12f, false);
            lblPaso.setPadding(0, dp(2), 0, dp(10));
            card.addView(lblPaso);

            // Fila 4: Los 5 botones de acción (▶ Iniciar | ■ Detener | [→ Cerrar Sesión y Detener | 👁 Ocultar | ✎ Editar)
            LinearLayout filaAcciones = new LinearLayout(this);
            filaAcciones.setOrientation(LinearLayout.HORIZONTAL);
            filaAcciones.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            ImageView bPlay = crearBotonAccionTarjeta("PLAY", "#14532D", "#4ADE80", () -> {
                if (!sesion.estaCorriendo() || sesion.detenerTrasCerrarSesion) {
                    iniciarSesionIndividual(sesion, 0);
                }
            });

            ImageView bStop = crearBotonAccionTarjeta("STOP", "#450A0A", "#F87171", () -> {
                if (sesion.estaCorriendo()) {
                    // Si está adentro del banco y aún no está cerrando sesión, primero cierra sesión limpiamente antes de detenerse
                    if (sesion.sesionAbiertaEnServidor && sesion.estadoActual != EstadoBot.CERRANDO_SESION && sesion.estadoActual != EstadoBot.REINICIANDO_RED) {
                        cerrarSesionYDetenerCuenta(sesion);
                    } else {
                        detenerSesionInmediato(sesion, true);
                    }
                }
            });

            ImageView bLogoutStop = crearBotonAccionTarjeta("LOGOUT", "#431407", "#FB923C", () -> {
                cerrarSesionYDetenerCuenta(sesion);
            });

            ImageView bEye = crearBotonAccionTarjeta("EYE", "#1E293B", "#38BDF8", () -> {
                sesion.nombreOculto = !sesion.nombreOculto;
                actualizarVisualTarjeta(sesion);
                if (pestanaInferiorActual == 1) {
                    reconstruirFiltrosHistorial();
                    refrescarVistaHistorial();
                }
            });

            ImageView bEdit = crearBotonAccionTarjeta("EDIT", "#27272A", "#E4E4E7", () -> {
                mostrarPopupEditorCuenta(sesion.slotIndex);
            });

            LinearLayout.LayoutParams lpAct = new LinearLayout.LayoutParams(dp(40), dp(36));
            lpAct.setMargins(dp(5), 0, 0, 0);
            filaAcciones.addView(bPlay, lpAct);
            filaAcciones.addView(bStop, lpAct);
            filaAcciones.addView(bLogoutStop, lpAct);
            filaAcciones.addView(bEye, lpAct);
            filaAcciones.addView(bEdit, lpAct);
            card.addView(filaAcciones);

            // Fila 5: Consola desplegable interna
            LinearLayout boxConsola = new LinearLayout(this);
            boxConsola.setOrientation(LinearLayout.VERTICAL);
            boxConsola.setVisibility(View.GONE);
            boxConsola.setPadding(0, dp(10), 0, 0);

            FrameLayout frameConsolaInterna = new FrameLayout(this);
            ScrollView scrollCon = new ScrollView(this);
            scrollCon.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 10, 1));
            scrollCon.setPadding(dp(10), dp(8), dp(10), dp(8));

            scrollCon.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });

            TextView txtCon = new TextView(this);
            txtCon.setTextColor(Color.parseColor("#E4E4E7"));
            txtCon.setTextSize(10.5f);
            txtCon.setTypeface(Typeface.MONOSPACE);
            scrollCon.addView(txtCon);

            frameConsolaInterna.addView(scrollCon, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(155)));

            TextView btnLimpiarMini = new TextView(this);
            btnLimpiarMini.setText("Limpiar");
            btnLimpiarMini.setTextColor(Color.parseColor("#A1A1AA"));
            btnLimpiarMini.setTextSize(8.5f);
            btnLimpiarMini.setTypeface(null, Typeface.BOLD);
            btnLimpiarMini.setPadding(dp(7), dp(2), dp(7), dp(2));
            btnLimpiarMini.setBackground(fondoRedondeadoConBorde("#18181B", "#3F3F46", 5, 1));
            btnLimpiarMini.setOnClickListener(v -> txtCon.setText(""));

            FrameLayout.LayoutParams lpLimp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            lpLimp.setMargins(0, dp(6), dp(8), 0);
            frameConsolaInterna.addView(btnLimpiarMini, lpLimp);

            boxConsola.addView(frameConsolaInterna);
            card.addView(boxConsola);

            sesion.cardView = card;
            sesion.lblUsuarioCard = lblUsr;
            sesion.badgeEstadoCard = badgeEst;
            sesion.lblSaldosCard = lblSaldos;
            sesion.lblSubtituloPasoCard = lblPaso;
            sesion.btnPlayCard = bPlay;
            sesion.btnStopCard = bStop;
            sesion.btnLogoutCard = bLogoutStop;
            sesion.btnEyeCard = bEye;
            sesion.btnEditCard = bEdit;
            sesion.contenedorConsolaCard = boxConsola;
            sesion.scrollConsolaCard = scrollCon;
            sesion.txtConsolaCard = txtCon;

            // 2. Motor WebView en tamaño real detrás del fondo oscuro
            WebView wv = new WebView(this);
            wv.setBackgroundColor(Color.parseColor("#18181B"));
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
            ws.setLoadsImagesAutomatically(true);
            ws.setBlockNetworkImage(false);
            ws.setBlockNetworkLoads(false);
            ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Slot/" + (i + 1));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(wv, true);

            wv.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    sesion.progresoWeb = newProgress;
                    sesion.cargandoRed = (newProgress < 100);
                }
            });

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
                    sesion.progresoWeb = 100;
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        sesion.cargandoRed = false;
                        sesion.progresoWeb = 100;
                        view.stopLoading();
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    sesion.cargandoRed = false;
                    sesion.progresoWeb = 100;
                    view.stopLoading();
                }
            });

            sesion.webViewOculto = wv;
            LinearLayout.LayoutParams lpWv = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i < 2) {
                filaMotoresSup.addView(wv, lpWv);
            } else {
                filaMotoresInf.addView(wv, lpWv);
            }
            wv.loadUrl(URL_LOGIN);
        }
    }

    private ImageView crearBotonAccionTarjeta(String icono, String bgHex, String fgHex, Runnable accion) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setImageDrawable(crearIconoVectorial(icono, fgHex, 16));
        iv.setBackground(fondoRedondeadoConBorde(bgHex, fgHex, 10, 1));
        iv.setOnClickListener(v -> {
            if (accion != null) accion.run();
        });
        return iv;
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
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================================
    // ACTUALIZACIÓN VISUAL DE TARJETAS Y SALDOS EN VIVO (BS Y USD)
    // =========================================================================================
    private ArrayList<SesionBot> obtenerSesionesRegistradas() {
        ArrayList<SesionBot> lista = new ArrayList<>();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            if (sesiones[i].registrada && !sesiones[i].usuario.isEmpty()) {
                lista.add(sesiones[i]);
            }
        }
        return lista;
    }

    private int contarCuentasCorriendo() {
        int c = 0;
        for (SesionBot s : obtenerSesionesRegistradas()) {
            if (s.estaCorriendo()) c++;
        }
        return c;
    }

    private void reconstruirListaTarjetas() {
        contenedorTarjetasCuentas.removeAllViews();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            ViewGroup p = (ViewGroup) sesiones[i].cardView.getParent();
            if (p != null) p.removeView(sesiones[i].cardView);
        }

        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        for (SesionBot s : reg) {
            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpCard.setMargins(0, 0, 0, dp(10));
            contenedorTarjetasCuentas.addView(s.cardView, lpCard);
            actualizarVisualTarjeta(s);
        }

        actualizarResumenYBotonesGlobales();
    }

    private void actualizarVisualTarjeta(SesionBot s) {
        if (s == null || s.cardView == null) return;
        boolean corriendo = s.estaCorriendo();

        Drawable dot = crearCirculoNeon(corriendo || s.compraCompletada, corriendo ? pulsoNeon : true, 11);
        ponerIconoYTexto(s.lblUsuarioCard, dot, s.nombreMostrado());
        if (s.compraCompletada || corriendo) {
            s.lblUsuarioCard.setTextColor(Color.parseColor("#4ADE80"));
        } else {
            s.lblUsuarioCard.setTextColor(Color.parseColor("#9CA3AF"));
        }

        // Borde iluminado en Verde Neón brillante cuando completa la orden
        if (s.compraCompletada) {
            s.cardView.setBackground(fondoRedondeadoConBorde("#0D2818", "#00FF66", 14, 2));
        } else if (corriendo) {
            s.cardView.setBackground(fondoRedondeadoConBorde("#18181B", "#1E3A2F", 14, 1));
        } else {
            s.cardView.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 14, 1));
        }

        String txtBadge = nombreCortoEstado(s);
        s.badgeEstadoCard.setText(txtBadge);
        if (s.compraCompletada) {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#00FF66"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#064E3B", "#00FF66", 12, 1));
        } else if (corriendo) {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#4ADE80"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#14532D", "#22C55E", 12, 1));
        } else {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#9CA3AF"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 12, 1));
        }

        if (s.lblSaldosCard != null) {
            s.lblSaldosCard.setText(s.textoSaldosMostrado());
        }

        s.lblSubtituloPasoCard.setText(s.ultimoPasoTexto);
        s.lblSubtituloPasoCard.setTextColor(Color.parseColor(
                s.compraCompletada ? "#00FF66" : (corriendo ? "#E4E4E7" : "#A1A1AA")
        ));

        s.btnPlayCard.setAlpha(corriendo ? 0.35f : 1.0f);
        s.btnPlayCard.setEnabled(!corriendo);

        s.btnStopCard.setAlpha(corriendo ? 1.0f : 0.35f);
        s.btnStopCard.setEnabled(corriendo);

        s.btnEyeCard.setImageDrawable(crearIconoVectorial(s.nombreOculto ? "EYE_OFF" : "EYE", "#38BDF8", 16));
    }

    private void actualizarResumenYBotonesGlobales() {
        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        int total = reg.size();
        int activas = contarCuentasCorriendo();
        int detenidas = Math.max(0, total - activas);

        chipRegistradas.setText(total == 1 ? "1 registrada" : total + " registradas");
        chipActivas.setText(activas == 1 ? "1 activa" : activas + " activas");
        chipDetenidas.setText(detenidas == 1 ? "1 detenida" : detenidas + " detenidas");

        if (activas > 0) {
            ponerIconoYTexto(badgeEstadoGlobal, crearCirculoNeon(true, pulsoNeon, 10), "VIGILANDO (" + activas + ")");
            badgeEstadoGlobal.setTextColor(Color.parseColor("#4ADE80"));
            badgeEstadoGlobal.setBackground(fondoRedondeado("#14532D", 12));
        } else {
            ponerIconoYTexto(badgeEstadoGlobal, crearCirculoNeon(false, false, 10), "APAGADO");
            badgeEstadoGlobal.setTextColor(Color.parseColor("#FCA5A5"));
            badgeEstadoGlobal.setBackground(fondoRedondeado("#450A0A", 12));
        }

        boolean hayParaIniciar = (total > 0 && activas < total);
        boolean hayParaDetener = (activas > 0);

        btnIniciarTodas.setEnabled(hayParaIniciar || total == 0);
        btnIniciarTodas.setBackground(fondoRedondeado(hayParaIniciar || total == 0 ? "#16A34A" : "#14532D", 10));
        ponerIconoYTexto(btnIniciarTodas, crearIconoVectorial("PLAY", hayParaIniciar || total == 0 ? "#FFFFFF" : "#86EFAC", 14), "Iniciar todas");
        btnIniciarTodas.setTextColor(Color.parseColor(hayParaIniciar || total == 0 ? "#FFFFFF" : "#86EFAC"));

        btnDetenerTodas.setEnabled(hayParaDetener);
        btnDetenerTodas.setBackground(fondoRedondeado(hayParaDetener ? "#DC2626" : "#7F1D1D", 10));
        ponerIconoYTexto(btnDetenerTodas, crearIconoVectorial("STOP", hayParaDetener ? "#FFFFFF" : "#FCA5A5", 14), "Detener todas");
        btnDetenerTodas.setTextColor(Color.parseColor(hayParaDetener ? "#FFFFFF" : "#FCA5A5"));
    }

    /**
     * Extrae en segundo plano el Saldo Disponible en Bs y USD desde Posición Global (/pg),
     * desde el DOM actual y desde el endpoint de saldos de intervención.
     */
    private void actualizarSaldosEnSegundoPlano(final SesionBot s, boolean forzarAhora) {
        if (s == null || s.webViewOculto == null) return;
        long ahora = System.currentTimeMillis();
        if (!forzarAhora && (ahora - s.ultimaActualizacionSaldoMs < 3500)) return;
        s.ultimaActualizacionSaldoMs = ahora;

        String jsBalance =
                "(async function(){" +
                "  try {" +
                "    function fmtNum(v) {" +
                "      var n = parseFloat(v);" +
                "      if (isNaN(n)) return null;" +
                "      return n.toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                "    }" +
                "    function extraerDeTexto(txt) {" +
                "      var res = { bs: null, usd: null };" +
                "      if (!txt) return res;" +
                "      var m1 = txt.match(/([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s*(?:Bs|VES)/i);" +
                "      var m2 = txt.match(/(?:VES|Bs\\.?)[\\s:]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})/i);" +
                "      if (m1) res.bs = m1[1]; else if (m2) res.bs = m2[1];" +
                "      var u1 = txt.match(/([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s*(?:USD|\\$)/i);" +
                "      var u2 = txt.match(/(?:USD|TESORO DIGITAL\\$|\\$)[\\s:]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})/i);" +
                "      if (u1) res.usd = u1[1]; else if (u2) res.usd = u2[1];" +
                "      return res;" +
                "    }" +
                "    var optD = document.querySelector('#desde option[data-disponible]');" +
                "    var optH = document.querySelector('#hasta option[data-disponible]');" +
                "    if (optD) window.__vbt_bs = fmtNum(optD.getAttribute('data-disponible'));" +
                "    if (optH) window.__vbt_usd = fmtNum(optH.getAttribute('data-disponible'));" +
                "    var domRes = extraerDeTexto(document.body ? document.body.innerText : '');" +
                "    if (domRes.bs) window.__vbt_bs = domRes.bs;" +
                "    if (domRes.usd) window.__vbt_usd = domRes.usd;" +
                "    var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "    var tok = meta ? meta.getAttribute('content') : '';" +
                "    var rPg = await fetch('https://tesoroenlinea.bt.com.ve/pg', {" +
                "      method: 'GET'," +
                "      headers: {'X-Requested-With': 'XMLHttpRequest', 'X-CSRF-TOKEN': tok}," +
                "      credentials: 'same-origin'" +
                "    });" +
                "    if (rPg.status === 200) {" +
                "      var htmlPg = await rPg.text();" +
                "      var tmp = document.createElement('div');" +
                "      tmp.innerHTML = htmlPg;" +
                "      var pgRes = extraerDeTexto(tmp.innerText || htmlPg);" +
                "      if (pgRes.bs) window.__vbt_bs = pgRes.bs;" +
                "      if (pgRes.usd) window.__vbt_usd = pgRes.usd;" +
                "    }" +
                "  } catch(e){}" +
                "})();";

        evaluarJS(s, jsBalance, null);
    }

    // =========================================================================================
    // HISTORIAL DE LA CONSOLA DE LAS CUENTAS
    // =========================================================================================
    private void reconstruirFiltrosHistorial() {
        barraFiltrosHistorial.removeAllViews();

        Button btnAll = crearBoton("TODAS", filtroHistorialSlot == -1 ? "#0284C7" : "#27272A",
                filtroHistorialSlot == -1 ? "#FFFFFF" : "#A1A1AA", 10, () -> {
                    filtroHistorialSlot = -1;
                    reconstruirFiltrosHistorial();
                    refrescarVistaHistorial();
                });
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
        lpF.setMargins(0, 0, dp(6), 0);
        btnAll.setPadding(dp(12), 0, dp(12), 0);
        barraFiltrosHistorial.addView(btnAll, lpF);

        for (SesionBot s : obtenerSesionesRegistradas()) {
            boolean sel = (filtroHistorialSlot == s.slotIndex);
            Button bUsr = crearBoton(s.nombreMostrado(), sel ? "#0284C7" : "#27272A",
                    sel ? "#FFFFFF" : "#A1A1AA", 10, () -> {
                        filtroHistorialSlot = s.slotIndex;
                        reconstruirFiltrosHistorial();
                        refrescarVistaHistorial();
                    });
            bUsr.setPadding(dp(12), 0, dp(12), 0);
            barraFiltrosHistorial.addView(bUsr, lpF);
        }
    }

    private void refrescarVistaHistorial() {
        if (txtHistorialConsola == null) return;
        StringBuilder sb = new StringBuilder();
        for (EntradaLog e : historialGlobalLogs) {
            if (filtroHistorialSlot == -1 || filtroHistorialSlot == e.slotIndex) {
                String nombre = e.usuario;
                if (e.slotIndex >= 0 && e.slotIndex < MAX_CUENTAS) {
                    nombre = sesiones[e.slotIndex].nombreMostrado();
                }
                sb.append("[").append(e.hora).append("] [").append(nombre).append("] ").append(e.mensaje).append("\n");
            }
        }
        txtHistorialConsola.setText(sb.toString());
        scrollHistorialConsola.post(() -> scrollHistorialConsola.fullScroll(View.FOCUS_DOWN));
    }

    private void registrarPasoCuenta(final SesionBot s, final String mensajeClaro) {
        final String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        runOnUiThread(() -> {
            s.ultimoPasoTexto = mensajeClaro;
            actualizarVisualTarjeta(s);

            if (s.txtConsolaCard != null && s.scrollConsolaCard != null) {
                s.txtConsolaCard.append("[" + hora + "] " + mensajeClaro + "\n");
                if (s.consolaDesplegada) {
                    s.scrollConsolaCard.post(() -> s.scrollConsolaCard.fullScroll(View.FOCUS_DOWN));
                }
            }

            historialGlobalLogs.add(new EntradaLog(s.slotIndex, s.usuario, hora, mensajeClaro));
            if (historialGlobalLogs.size() > 500) {
                historialGlobalLogs.remove(0);
            }

            if (pestanaInferiorActual == 1 && (filtroHistorialSlot == -1 || filtroHistorialSlot == s.slotIndex)) {
                txtHistorialConsola.append("[" + hora + "] [" + s.nombreMostrado() + "] " + mensajeClaro + "\n");
                scrollHistorialConsola.post(() -> scrollHistorialConsola.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    // =========================================================================================
    // GESTIÓN DE CREDENCIALES Y MONTO PERSONALIZADO ("Escribe múltiplo de 10")
    // =========================================================================================
    private JSONArray obtenerArrayCuentasPrefs() {
        try {
            String raw = prefs.getString("lista_cuentas_v2", "");
            if (raw != null && !raw.isEmpty()) {
                return new JSONArray(raw);
            }
            JSONObject viejo = new JSONObject(prefs.getString("cuentas_json", "{}"));
            JSONArray migrado = new JSONArray();
            Iterator<String> it = viejo.keys();
            while (it.hasNext() && migrado.length() < MAX_CUENTAS) {
                String k = it.next();
                JSONObject o = viejo.optJSONObject(k);
                if (o != null) migrado.put(o);
            }
            if (migrado.length() > 0) {
                prefs.edit().putString("lista_cuentas_v2", migrado.toString()).apply();
            }
            return migrado;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void guardarArrayCuentasPrefs() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            SesionBot s = sesiones[i];
            if (s.registrada && !s.usuario.isEmpty()) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("tipo_doc", s.tipoDoc);
                    o.put("cedula", s.cedula);
                    o.put("usuario", s.usuario);
                    o.put("clave", s.contrasena);
                    o.put("monto", s.montoCompra);
                    arr.put(o);
                } catch (Exception ignored) {}
            }
        }
        prefs.edit().putString("lista_cuentas_v2", arr.toString()).apply();
    }

    private void cargarCuentasGuardadas() {
        JSONArray arr = obtenerArrayCuentasPrefs();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            sesiones[i].registrada = false;
            sesiones[i].tipoDoc = "V";
            sesiones[i].cedula = "";
            sesiones[i].usuario = "";
            sesiones[i].contrasena = "";
            sesiones[i].montoCompra = 300;
        }

        for (int i = 0; i < arr.length() && i < MAX_CUENTAS; i++) {
            JSONObject d = arr.optJSONObject(i);
            if (d != null) {
                sesiones[i].registrada = true;
                sesiones[i].tipoDoc = d.optString("tipo_doc", "V");
                sesiones[i].cedula = d.optString("cedula", "");
                sesiones[i].usuario = d.optString("usuario", "");
                sesiones[i].contrasena = d.optString("clave", "");
                int m = d.optInt("monto", 300);
                if (m < 10 || m > 300 || m % 10 != 0) m = 300;
                sesiones[i].montoCompra = m;
            }
        }
        reconstruirListaTarjetas();
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

    private void mostrarPopupEditorCuenta(final int slotEditar) {
        final boolean esEdicion = (slotEditar >= 0 && slotEditar < MAX_CUENTAS && sesiones[slotEditar].registrada);
        final SesionBot sesionEdit = esEdicion ? sesiones[slotEditar] : null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollPopup = new ScrollView(this);
        scrollPopup.setBackgroundColor(Color.parseColor("#18181B"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(16), dp(18), dp(16));
        layout.setBackgroundColor(Color.parseColor("#18181B"));
        scrollPopup.addView(layout);

        TextView titulo = crearTexto(esEdicion ? "Editar Credenciales" : "Agregar Nueva Cuenta", "#F4F4F5", 16f, true);
        titulo.setPadding(0, 0, 0, dp(10));
        layout.addView(titulo);

        final String[] tipoSeleccionado = {esEdicion ? sesionEdit.tipoDoc : "V"};
        final String[] tiposDocArray = {"V", "E", "J", "P", "G", "C", "R"};

        LinearLayout filaCed = new LinearLayout(this);
        filaCed.setOrientation(LinearLayout.HORIZONTAL);
        filaCed.setPadding(0, dp(4), 0, dp(6));

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
        if (esEdicion) edtCedula.setText(sesionEdit.cedula);

        final Button btnOjoCed = crearBotonConIcono("EYE", "", "#52525B", "#F4F4F5", 12, null);
        btnOjoCed.setOnClickListener(v -> toggleCampoOjo(edtCedula, btnOjoCed));

        LinearLayout.LayoutParams lpTipo = new LinearLayout.LayoutParams(dp(58), dp(38));
        lpTipo.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams lpEdt = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpEdt.setMargins(0, 0, dp(4), 0);
        filaCed.addView(btnTipoDoc, lpTipo);
        filaCed.addView(edtCedula, lpEdt);
        filaCed.addView(btnOjoCed, new LinearLayout.LayoutParams(dp(38), dp(38)));
        layout.addView(filaCed);

        LinearLayout filaUsr = new LinearLayout(this);
        filaUsr.setOrientation(LinearLayout.HORIZONTAL);
        filaUsr.setPadding(0, dp(4), 0, dp(6));

        final EditText edtUsuario = new EditText(this);
        edtUsuario.setHint("Usuario");
        edtUsuario.setHintTextColor(Color.parseColor("#94A3B8"));
        edtUsuario.setTextColor(Color.parseColor("#F4F4F5"));
        edtUsuario.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        edtUsuario.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        edtUsuario.setBackground(fondoRedondeado("#27272A", 6));
        edtUsuario.setPadding(dp(10), dp(6), dp(10), dp(6));
        if (esEdicion) edtUsuario.setText(sesionEdit.usuario);

        final Button btnOjoUsr = crearBotonConIcono("EYE", "", "#52525B", "#F4F4F5", 12, null);
        btnOjoUsr.setOnClickListener(v -> toggleCampoOjo(edtUsuario, btnOjoUsr));

        filaUsr.addView(edtUsuario, lpEdt);
        filaUsr.addView(btnOjoUsr, new LinearLayout.LayoutParams(dp(38), dp(38)));
        layout.addView(filaUsr);

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
        if (esEdicion) edtClave.setText(sesionEdit.contrasena);

        final Button btnOjoClv = crearBotonConIcono("EYE_OFF", "", "#27272A", "#F4F4F5", 12, null);
        btnOjoClv.setOnClickListener(v -> toggleCampoOjo(edtClave, btnOjoClv));

        filaClv.addView(edtClave, lpEdt);
        filaClv.addView(btnOjoClv, new LinearLayout.LayoutParams(dp(38), dp(38)));
        layout.addView(filaClv);

        // RECUADRO DE MONTO A COMPRAR ("300" por defecto -> Al tocarlo pasa a "Monto personalizado")
        TextView lblTitMonto = crearTexto("Monto a comprar ($):", "#38BDF8", 11.5f, true);
        lblTitMonto.setPadding(0, dp(2), 0, dp(4));
        layout.addView(lblTitMonto);

        int montoInicial = esEdicion ? sesionEdit.montoCompra : 300;
        final boolean[] modoPersonalizado = {montoInicial != 300};

        final Button btnSelectorMonto = crearBoton(
                modoPersonalizado[0] ? "Monto personalizado" : "300",
                "#27272A", modoPersonalizado[0] ? "#38BDF8" : "#F4F4F5", 13, null);
        LinearLayout.LayoutParams lpBtnMonto = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        lpBtnMonto.setMargins(0, 0, 0, dp(6));
        layout.addView(btnSelectorMonto, lpBtnMonto);

        final EditText edtMontoCustom = new EditText(this);
        edtMontoCustom.setHint("Escribe múltiplo de 10");
        edtMontoCustom.setHintTextColor(Color.parseColor("#71717A")); // Gris oscuro limpio y legible
        edtMontoCustom.setTextColor(Color.parseColor("#4ADE80"));
        edtMontoCustom.setTextSize(13.5f);
        edtMontoCustom.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtMontoCustom.setBackground(fondoRedondeadoConBorde("#121316", "#0284C7", 6, 1));
        edtMontoCustom.setPadding(dp(10), dp(6), dp(10), dp(6));
        if (modoPersonalizado[0]) {
            edtMontoCustom.setText(String.valueOf(montoInicial));
        }
        edtMontoCustom.setVisibility(modoPersonalizado[0] ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams lpEdtMonto = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        lpEdtMonto.setMargins(0, 0, 0, dp(12));
        layout.addView(edtMontoCustom, lpEdtMonto);

        btnSelectorMonto.setOnClickListener(v -> {
            modoPersonalizado[0] = !modoPersonalizado[0];
            if (modoPersonalizado[0]) {
                btnSelectorMonto.setText("Monto personalizado");
                btnSelectorMonto.setTextColor(Color.parseColor("#38BDF8"));
                edtMontoCustom.setVisibility(View.VISIBLE);
                edtMontoCustom.setText("");
                edtMontoCustom.requestFocus();
            } else {
                btnSelectorMonto.setText("300");
                btnSelectorMonto.setTextColor(Color.parseColor("#F4F4F5"));
                edtMontoCustom.setText("300");
                edtMontoCustom.setVisibility(View.GONE);
            }
        });

        builder.setView(scrollPopup);
        final AlertDialog dialog = builder.create();

        Button btnGuardar = crearBotonConIcono("CHECK", "Guardar Cuenta", "#0284C7", "#FFFFFF", 13, () -> {
            String u = edtUsuario.getText().toString().trim();
            String c = edtCedula.getText().toString().trim();
            String p = edtClave.getText().toString().trim();
            String t = tipoSeleccionado[0];
            if (u.isEmpty() || c.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Completa Cédula, Usuario y Contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            int montoFinal = 300;
            if (modoPersonalizado[0]) {
                String txtM = edtMontoCustom.getText().toString().trim();
                try {
                    montoFinal = Integer.parseInt(txtM);
                } catch (Exception e) {
                    montoFinal = -1;
                }
                if (montoFinal < 10 || montoFinal > 300 || (montoFinal % 10 != 0)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Monto no válido")
                            .setMessage("Tiene que ser múltiplo de 10 (mínimo 10 y máximo 300, por ejemplo: 10, 20, 30, 100, 120, 290, 300).")
                            .setPositiveButton("Aceptar", null)
                            .show();
                    return;
                }
            }

            if (esEdicion) {
                sesionEdit.tipoDoc = t;
                sesionEdit.cedula = c;
                sesionEdit.usuario = u;
                sesionEdit.contrasena = p;
                sesionEdit.montoCompra = montoFinal;
                guardarArrayCuentasPrefs();
                actualizarVisualTarjeta(sesionEdit);
                dialog.dismiss();
            } else {
                int slotLibre = -1;
                for (int i = 0; i < MAX_CUENTAS; i++) {
                    if (!sesiones[i].registrada) {
                        slotLibre = i;
                        break;
                    }
                }
                if (slotLibre == -1) {
                    Toast.makeText(this, "Máximo 4 cuentas simultáneas", Toast.LENGTH_SHORT).show();
                    return;
                }
                SesionBot nueva = sesiones[slotLibre];
                nueva.registrada = true;
                nueva.tipoDoc = t;
                nueva.cedula = c;
                nueva.usuario = u;
                nueva.contrasena = p;
                nueva.montoCompra = montoFinal;
                nueva.saldoBs = "--";
                nueva.saldoUsd = "--";
                nueva.compraCompletada = false;
                nueva.ultimoPasoTexto = "Sin actividad todavía";
                guardarArrayCuentasPrefs();
                reconstruirListaTarjetas();
                dialog.dismiss();
            }
        });
        layout.addView(btnGuardar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        if (esEdicion) {
            Button btnBorrar = crearBotonConIcono("CLOSE", "Eliminar esta cuenta", "#450A0A", "#FCA5A5", 12, () -> {
                detenerSesionInmediato(sesionEdit, false);
                sesionEdit.registrada = false;
                sesionEdit.usuario = "";
                sesionEdit.cedula = "";
                sesionEdit.contrasena = "";
                sesionEdit.txtConsolaCard.setText("");
                guardarArrayCuentasPrefs();
                reconstruirListaTarjetas();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams lpDel = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
            lpDel.setMargins(0, dp(8), 0, 0);
            layout.addView(btnBorrar, lpDel);
        }

        dialog.show();
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
        actualizarVisualTarjeta(s);
        actualizarResumenYBotonesGlobales();
    }

    private String nombreCortoEstado(SesionBot s) {
        if (s.compraCompletada) return "COMPLETADA";
        switch (s.estadoActual) {
            case DETENIDO: return "DETENIDO";
            case CARGANDO_LOGIN:
            case ESPERANDO_ESTABILIZAR_LOGIN:
            case ENVIANDO_FASE1: return "INICIANDO";
            case ESPERANDO_CARGA_PASS:
            case ESPERANDO_ESTABILIZAR_PASS:
            case ENVIANDO_FASE2: return "CONTRASEÑA";
            case ESPERANDO_REINTENTO_SESION_ACTIVA: return "SESIÓN ACTIVA";
            case ESPERANDO_CARGA_INICIO:
            case ESPERANDO_ESTABILIZAR_INICIO: return "POSICIÓN GLOBAL";
            case NAVEGANDO_MENU: return "INTERVENCIÓN";
            case VIGILANDO: return "VIGILANDO";
            case ESPERANDO_FORM_INTENTO1:
            case LIMPIANDO_PANTALLA:
            case ESPERANDO_FORM_INTENTO2:
            case ESPERANDO_ESTABILIZAR_FORM:
            case LLENANDO_COMPRA:
            case COMPRANDO: return "COMPRANDO";
            case CERRANDO_SESION:
            case REINICIANDO_RED: return "CERRANDO SESIÓN";
            default: return "ACTIVO";
        }
    }

    // =========================================================================================
    // ENCENDIDO, DETENCIÓN Y CIERRE DE SESIÓN CON DETENCIÓN
    // =========================================================================================
    private void iniciarTodasLasCuentas() {
        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        if (reg.isEmpty()) {
            mostrarPopupEditorCuenta(-1);
            return;
        }
        yaAvisoApertura = false;
        int delayEscalonado = 0;
        for (SesionBot s : reg) {
            if (!s.estaCorriendo() || s.detenerTrasCerrarSesion) {
                iniciarSesionIndividual(s, delayEscalonado);
                delayEscalonado += 350;
            }
        }
    }

    private void detenerTodasLasCuentas() {
        for (SesionBot s : obtenerSesionesRegistradas()) {
            if (s.estaCorriendo()) {
                if (s.sesionAbiertaEnServidor && s.estadoActual != EstadoBot.CERRANDO_SESION && s.estadoActual != EstadoBot.REINICIANDO_RED) {
                    cerrarSesionYDetenerCuenta(s);
                } else {
                    detenerSesionInmediato(s, true);
                }
            }
        }
        lblReloginVal.setText(minutosRelogin + " min (--:--)");
    }

    private void iniciarSesionIndividual(final SesionBot s, int delayMs) {
        s.corriendo = true;
        s.detenerTrasCerrarSesion = false;
        s.compraCompletada = false;
        s.vueltaActual = 0;
        s.intentoLogout = 0;
        s.tiempoInicioSesionMs = 0;
        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
        registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");

        handler.postDelayed(() -> {
            if (s.estaCorriendo() && s.webViewOculto != null) {
                s.webViewOculto.stopLoading();
                s.webViewOculto.loadUrl(URL_LOGIN);
            }
        }, delayMs);
    }

    /**
     * Cierra la sesión en el banco (cortando y reconectando la red interna las veces necesarias)
     * y detiene el bot de esa cuenta apenas queda fuera de la sesión.
     */
    private void cerrarSesionYDetenerCuenta(SesionBot s) {
        if (s == null || !s.registrada) return;
        s.corriendo = true;
        s.detenerTrasCerrarSesion = true;
        s.intentoLogout = 0;
        cambiarEstado(s, EstadoBot.CERRANDO_SESION);
        registrarPasoCuenta(s, "Cerrando sesión para detener...");
    }

    private void detenerSesionInmediato(SesionBot s, boolean registrarEnLog) {
        s.corriendo = false;
        s.detenerTrasCerrarSesion = false;
        s.tiempoInicioSesionMs = 0;
        s.cargandoRed = false;
        if (s.webViewOculto != null) {
            s.webViewOculto.stopLoading();
        }
        if (registrarEnLog) {
            registrarPasoCuenta(s, "Cuenta detenida");
        }
        cambiarEstado(s, EstadoBot.DETENIDO);
    }

    // =========================================================================================
    // CONTROL DE RED INTERNA (QUITAR Y PONER INTERNET AL NAVEGADOR DE LA CUENTA)
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
                        for (int i = 0; i < MAX_CUENTAS; i++) {
                            if (sesiones[i].webViewOculto != null) {
                                sesiones[i].webViewOculto.stopLoading();
                            }
                        }
                        avisoSinInternetMostrado = true;
                    });
                }

                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        if (avisoSinInternetMostrado) {
                            avisoSinInternetMostrado = false;
                            long ahora = System.currentTimeMillis();
                            for (int i = 0; i < MAX_CUENTAS; i++) {
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
     * Quita y vuelve a colocar el internet internamente en el navegador de la cuenta
     * para destrabar el cierre de sesión cuando el banco está colapsado.
     */
    private void reiniciarConexionIndividual(final SesionBot s) {
        if (s == null || s.microCortePropio) return;
        s.microCortePropio = true;

        final WebView wv = s.webViewOculto;
        if (wv != null) {
            try {
                wv.stopLoading();
                wv.getSettings().setBlockNetworkLoads(true);
                wv.setNetworkAvailable(false);
                evaluarJS(s, "if(window.stop){try{window.stop();}catch(e){}}", null);
            } catch (Exception ignored) {}
        }

        handler.postDelayed(() -> {
            if (wv != null) {
                try {
                    wv.getSettings().setBlockNetworkLoads(false);
                    wv.setNetworkAvailable(true);
                } catch (Exception ignored) {}
            }
            s.microCortePropio = false;
        }, 1300);
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
                ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
                for (SesionBot s : reg) {
                    actualizarVisualTarjeta(s);
                }
                actualizarResumenYBotonesGlobales();

                boolean internetOk = hayInternetDisponible();
                if (contarCuentasCorriendo() > 0) {
                    verificarTiemposDeSesionIndependientes(reg);
                    for (SesionBot s : reg) {
                        if (s.estaCorriendo() && internetOk && !s.microCortePropio) {
                            ejecutarPasoMaquinaEstados(s);
                        } else if (!internetOk) {
                            s.tiempoEstadoMs = System.currentTimeMillis();
                        }
                    }
                } else {
                    lblReloginVal.setText(minutosRelogin + " min (--:--)");
                }
                handler.postDelayed(this, 420);
            }
        }, 420);
    }

    private void verificarTiemposDeSesionIndependientes(ArrayList<SesionBot> reg) {
        long ahora = System.currentTimeMillis();
        long totalSeg = minutosRelogin * 60L;
        long menorRestante = -1;

        for (SesionBot s : reg) {
            if (!s.estaCorriendo()) continue;

            if (s.tiempoInicioSesionMs > 0 && s.estadoActual == EstadoBot.VIGILANDO) {
                long transcurridoSeg = (ahora - s.tiempoInicioSesionMs) / 1000;
                long restante = Math.max(0, totalSeg - transcurridoSeg);
                if (menorRestante == -1 || restante < menorRestante) {
                    menorRestante = restante;
                }
                if (transcurridoSeg >= totalSeg) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
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

    // =========================================================================================
    // MÁQUINA DE ESTADOS CON LECTURA DE SALDOS BS/USD, AJUSTE DE MONTO Y LOGOUT REAL
    // =========================================================================================
    private void ejecutarPasoMaquinaEstados(final SesionBot sesion) {
        if (sesion == null || sesion.webViewOculto == null) return;
        final long ahora = System.currentTimeMillis();

        String jsInspector =
                "var ready = (document.readyState === 'complete');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var rawTxt = document.body ? document.body.innerText : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var modalLogout = (bodyTxt.indexOf('deseas cerrar tu sesión') !== -1 || bodyTxt.indexOf('deseas cerrar tu sesion') !== -1);" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1 || bodyTxt.indexOf('posee una sesion') !== -1);" +
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close']\");" +
                "if (btnX && btnX.offsetParent !== null && !modalLogout) { btnX.click(); }" +
                "var btnOk = Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim() === 'OK' || el.innerText.trim() === 'Aceptar'));" +
                "if (btnOk && btnOk.offsetWidth > 0 && bodyTxt.indexOf('deseas realizar') === -1 && !modalLogout) { btnOk.click(); }" +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector(\"a.btn-tesoro-green, a[href*='mod.add(1)']\") || Array.from(document.querySelectorAll('.card-header a, .card-header button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var opsDiv = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "var inicioListo = !!(ready && !cargandoTxt && (menuInt || opsDiv || btnLogout) && (bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || bodyTxt.indexOf('operaciones') !== -1 || bodyTxt.indexOf('últ. conexión') !== -1 || bodyTxt.indexOf('ult. conexión') !== -1));" +
                "var tablaIntLista = !!(ready && !cargandoTxt && (bodyTxt.indexOf('operaciones de intervención') !== -1 || (ord && ord.offsetWidth > 0)));" +
                "var inconv = (bodyTxt.indexOf('presentando inconvenientes') !== -1 || bodyTxt.indexOf('no podemos procesar su solicitud') !== -1);" +
                "var exito = bodyTxt.indexOf('registrada exitosamente') !== -1;" +
                "var sinCupo = bodyTxt.indexOf('no hay asignación disponible') !== -1;" +
                "var optD = document.querySelector('#desde option[data-disponible]');" +
                "var optH = document.querySelector('#hasta option[data-disponible]');" +
                "if (optD) {" +
                "  var nBs = parseFloat(optD.getAttribute('data-disponible'));" +
                "  if (!isNaN(nBs)) window.__vbt_bs = nBs.toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                "}" +
                "if (optH) {" +
                "  var nUsd = parseFloat(optH.getAttribute('data-disponible'));" +
                "  if (!isNaN(nUsd)) window.__vbt_usd = nUsd.toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                "}" +
                "var mBsDom = rawTxt.match(/([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s*(?:Bs|VES)/i) || rawTxt.match(/(?:VES|Bs\\.?)[\\s:]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})/i);" +
                "var mUsdDom = rawTxt.match(/([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})\\s*(?:USD|\\$)/i) || rawTxt.match(/(?:USD|TESORO DIGITAL\\$)[\\s:]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})/i);" +
                "if (mBsDom) window.__vbt_bs = mBsDom[1];" +
                "if (mUsdDom) window.__vbt_usd = mUsdDom[1];" +
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
                "  inconv: inconv, exito: exito, sinCupo: sinCupo, sesionActivaErr: sesionActivaErr," +
                "  bs: window.__vbt_bs || ''," +
                "  usd: window.__vbt_usd || ''," +
                "  ajusteLog: window.__vbt_ajuste_log || ''" +
                "});";

        evaluarJS(sesion, jsInspector, raw -> {
            if (!sesion.estaCorriendo() || raw == null || raw.equals("null")) return;
            try {
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                }
                JSONObject st = new JSONObject(clean);
                String nuevoBs = st.optString("bs", "");
                String nuevoUsd = st.optString("usd", "");
                String ajusteLog = st.optString("ajusteLog", "");

                if (!ajusteLog.isEmpty()) {
                    evaluarJS(sesion, "window.__vbt_ajuste_log = '';", null);
                    registrarPasoCuenta(sesion, ajusteLog);
                }

                boolean cambioSaldo = false;
                if (!nuevoBs.isEmpty() && !nuevoBs.equals(sesion.saldoBs)) {
                    sesion.saldoBs = nuevoBs;
                    cambioSaldo = true;
                }
                if (!nuevoUsd.isEmpty() && !nuevoUsd.equals(sesion.saldoUsd)) {
                    sesion.saldoUsd = nuevoUsd;
                    cambioSaldo = true;
                }
                if (cambioSaldo) {
                    actualizarVisualTarjeta(sesion);
                }

                boolean nav100 = !sesion.cargandoRed && (sesion.progresoWeb >= 100);
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
        if (!s.estaCorriendo()) return;

        if (visInicioCompleto || visTablaInt || visMonto) {
            s.sesionAbiertaEnServidor = true;
            actualizarSaldosEnSegundoPlano(s, false);
        }

        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");

        switch (s.estadoActual) {

            case CARGANDO_LOGIN:
                if (paginaCompleta && visMonto) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    registrarPasoCuenta(s, "Sesión iniciada - Posición Global");
                    actualizarSaldosEnSegundoPlano(s, true);
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (paginaCompleta && visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (paginaCompleta && visDoc) {
                    s.sesionAbiertaEnServidor = false;
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 14000) {
                    s.tiempoEstadoMs = ahora;
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_LOGIN:
                if (!visDoc) {
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 250) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE1);
                }
                break;

            case ENVIANDO_FASE1:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
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
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 10);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(d, '" + cedSafe + "', 0, function() {" +
                            "    setTimeout(function() {" +
                            "      u.focus();" +
                            "      typeStr(u, '" + usrSafe + "', 0, function() {" +
                            "        setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 120);" +
                            "      });" +
                            "    }, 80);" +
                            "  });" +
                            "}";
                    evaluarJS(s, jsFase1, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                }
                break;

            case ESPERANDO_CARGA_PASS:
                if (haySesionActivaErr) {
                    registrarPasoCuenta(s, "Sesión activa detectada - esperando 6s para reintentar");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (paginaCompleta && visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 12000) {
                    registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_PASS:
                if (haySesionActivaErr) {
                    registrarPasoCuenta(s, "Sesión activa detectada - esperando 6s para reintentar");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (!visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 250) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                }
                break;

            case ENVIANDO_FASE2:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    registrarPasoCuenta(s, "Colocando contraseña");
                    String jsFase2 =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  p.focus(); setter.call(p, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, str.substring(0, i + 1)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 10);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(p, '" + clvSafe + "', 0, function() {" +
                            "    setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 120);" +
                            "  });" +
                            "}";
                    evaluarJS(s, jsFase2, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_INICIO);
                }
                break;

            case ESPERANDO_CARGA_INICIO:
                if (haySesionActivaErr) {
                    registrarPasoCuenta(s, "Sesión activa detectada - esperando 6s para reintentar");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    registrarPasoCuenta(s, "Sesión iniciada - Posición Global");
                    actualizarSaldosEnSegundoPlano(s, true);
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 10000 && paginaCompleta && visDoc) {
                    registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                }
                break;

            case ESPERANDO_REINTENTO_SESION_ACTIVA:
                if (ahora - s.tiempoEstadoMs >= 6000) {
                    registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_INICIO:
                if (ahora - s.tiempoEstadoMs >= 300) {
                    cambiarEstado(s, EstadoBot.NAVEGANDO_MENU);
                }
                break;

            case NAVEGANDO_MENU:
                if (paginaCompleta && visTablaInt) {
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    registrarPasoCuenta(s, "Entrando a intervención");
                    irAIntervencionJS(s);
                } else if (ahora - s.tiempoEstadoMs > 6000 && paginaCompleta) {
                    s.tiempoEstadoMs = ahora;
                    irAIntervencionJS(s);
                }
                break;

            case VIGILANDO:
                if (paginaCompleta && visDoc) {
                    s.sesionAbiertaEnServidor = false;
                    registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (paginaCompleta && visMonto && !hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visOrd) {
                    registrarPasoCuenta(s, "Clic en ordinaria");
                    if (!yaAvisoApertura) { yaAvisoApertura = true; sonarAlarma(false); }
                    clicarOrdinariaJS(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                if (paginaCompleta && (ahora - s.tiempoEstadoMs >= intervaloRefresco * 1000L)) {
                    s.tiempoEstadoMs = ahora;
                    s.vueltaActual++;
                    registrarPasoCuenta(s, "Sin botón intento " + s.vueltaActual);
                    irAIntervencionJS(s);
                }
                break;

            case ESPERANDO_FORM_INTENTO1:
                if (paginaCompleta && visMonto && !hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 3800)) {
                    irAIntervencionJS(s);
                    cambiarEstado(s, EstadoBot.LIMPIANDO_PANTALLA);
                }
                break;

            case LIMPIANDO_PANTALLA:
                if (paginaCompleta && visTablaInt && (ahora - s.tiempoEstadoMs >= 1600)) {
                    if (visOrd) {
                        registrarPasoCuenta(s, "Clic en ordinaria");
                        clicarOrdinariaJS(s);
                        cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO2);
                    } else {
                        cambiarEstado(s, EstadoBot.VIGILANDO);
                    }
                } else if (ahora - s.tiempoEstadoMs > 7000) {
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                }
                break;

            case ESPERANDO_FORM_INTENTO2:
                if (paginaCompleta && visMonto && !hayInconv) {
                    sonarAlarma(false);
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 4200)) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;

            case ESPERANDO_ESTABILIZAR_FORM:
                if (ahora - s.tiempoEstadoMs >= 250) {
                    cambiarEstado(s, EstadoBot.LLENANDO_COMPRA);
                }
                break;

            case LLENANDO_COMPRA:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    registrarPasoCuenta(s, "Rellenando formulario");
                    String jsCompraAutoMonto =
                            "var d = document.querySelector('#desde'); if(d && d.options.length > 1) { d.selectedIndex = 1; d.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var h = document.querySelector('#hasta'); if(h && h.options.length > 1) { h.selectedIndex = 1; h.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var o = document.querySelector('#origen'); if(o) { for(var i=0;i<o.options.length;i++){ if(o.options[i].text.includes('" + ORIGEN_FONDOS + "')){ o.selectedIndex=i; break; } } o.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var dst = document.querySelector('#destino'); if(dst) { for(var j=0;j<dst.options.length;j++){ if(dst.options[j].text.includes('" + DESTINO_FONDOS + "')){ dst.selectedIndex=j; break; } } dst.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var montoFinalInt = " + s.montoCompra + ";" +
                            "try {" +
                            "  var dispBs = d && d.options[d.selectedIndex] ? parseFloat(d.options[d.selectedIndex].getAttribute('data-disponible') || '0') : 0;" +
                            "  var tasaNum = (typeof tasa_usd !== 'undefined' && tasa_usd > 0) ? parseFloat(tasa_usd) : 0;" +
                            "  if (tasaNum <= 0) {" +
                            "    var elT = document.querySelector('#tasa');" +
                            "    if (elT && elT.value) tasaNum = parseFloat(elT.value.replace(/\\./g, '').replace(',', '.'));" +
                            "  }" +
                            "  var comPorc = (typeof com !== 'undefined') ? parseFloat(com) : 0.50;" +
                            "  if (dispBs > 0 && tasaNum > 0) {" +
                            "    var factor = 1 + (comPorc / 100);" +
                            "    var maxMultiplo10 = Math.floor((dispBs / (tasaNum * factor)) / 10) * 10;" +
                            "    if (maxMultiplo10 >= 10 && maxMultiplo10 < montoFinalInt) {" +
                            "      montoFinalInt = maxMultiplo10;" +
                            "      window.__vbt_ajuste_log = 'Saldo Bs cubre hasta ' + montoFinalInt + '$ - ajustando monto automáticamente';" +
                            "    }" +
                            "  }" +
                            "} catch(e){}" +
                            "var strEntero = String(montoFinalInt);" +
                            "var strCompleto = strEntero + ',00';" +
                            "var m = document.querySelector('#monto');" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "if(m) {" +
                            "  m.focus();" +
                            "  var pasos = [strEntero.substring(0, 1), strEntero, strCompleto];" +
                            "  function escribirPaso(idx) {" +
                            "    if(idx < pasos.length) {" +
                            "      setter.call(m, pasos[idx]);" +
                            "      m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "      setTimeout(function(){ escribirPaso(idx + 1); }, 35);" +
                            "    } else {" +
                            "      if(m.value.indexOf(strEntero) === -1) {" +
                            "        setter.call(m, strCompleto);" +
                            "        m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      }" +
                            "      m.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "      m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "      var elTot = document.querySelector('#total-03');" +
                            "      if (elTot && elTot.value) { window.__vbt_ultimo_bloqueo_bs = elTot.value; }" +
                            "      setTimeout(function(){" +
                            "        var col = document.querySelector(\"a[href='javascript:mod.save()']\") || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.toLowerCase().includes('colocar orden'));" +
                            "        if(col) col.click();" +
                            "        setTimeout(function(){" +
                            "          var btnSi = document.querySelector('.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                            "          if(btnSi) btnSi.click();" +
                            "        }, 350);" +
                            "      }, 220);" +
                            "    }" +
                            "  }" +
                            "  setTimeout(function(){ escribirPaso(0); }, 100);" +
                            "}";
                    evaluarJS(s, jsCompraAutoMonto, null);
                    cambiarEstado(s, EstadoBot.COMPRANDO);
                }
                break;

            case COMPRANDO:
                if (hayExito) {
                    s.compraCompletada = true;
                    s.corriendo = false;
                    // Descuenta en vivo los Bs bloqueados y refresca /pg para reflejar el nuevo saldo al instante
                    evaluarJS(s,
                            "try {" +
                            "  var optD = document.querySelector('#desde option[data-disponible]');" +
                            "  var tot = document.querySelector('#total-03');" +
                            "  if (optD && tot && tot.value) {" +
                            "    var dispActual = parseFloat(optD.getAttribute('data-disponible') || '0');" +
                            "    var bloq = parseFloat(tot.value.replace(/\\./g, '').replace(',', '.'));" +
                            "    if (!isNaN(dispActual) && !isNaN(bloq) && dispActual >= bloq) {" +
                            "      var rest = dispActual - bloq;" +
                            "      window.__vbt_bs = rest.toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                            "    }" +
                            "  }" +
                            "} catch(e){}", null);
                    actualizarSaldosEnSegundoPlano(s, true);
                    registrarPasoCuenta(s, "Orden completada");
                    sonarAlarma(true);
                    cambiarEstado(s, EstadoBot.DETENIDO);
                    return;
                }
                if (haySinCupo) {
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                    return;
                }
                if (hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 8500) {
                    actualizarSaldosEnSegundoPlano(s, true);
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                }
                break;

            // =================================================================================
            // CIERRE DE SESIÓN REAL (SIN BORRADO FORZADO: INTENTA -> QUITA/PONE RED -> REINTENTA)
            // =================================================================================
            case CERRANDO_SESION:
                if (paginaCompleta && (visDoc || visPass)) {
                    s.sesionAbiertaEnServidor = false;
                    if (!visDoc) {
                        s.webViewOculto.loadUrl(URL_LOGIN);
                    }
                    if (s.detenerTrasCerrarSesion) {
                        s.corriendo = false;
                        s.detenerTrasCerrarSesion = false;
                        s.tiempoInicioSesionMs = 0;
                        registrarPasoCuenta(s, "Sesión cerrada y cuenta detenida");
                        cambiarEstado(s, EstadoBot.DETENIDO);
                    } else {
                        registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    }
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout++;
                    s.tiempoEstadoMs = ahora;
                    registrarPasoCuenta(s, "Cerrando sesión intento " + s.intentoLogout);
                    ejecutarScriptLogout(s);
                } else if (ahora - s.tiempoEstadoMs > 3800) {
                    // Si la página está colapsada y no cerró en 3.8s, quita y pone el internet del navegador y vuelve a intentar
                    cambiarEstado(s, EstadoBot.REINICIANDO_RED);
                    reiniciarConexionIndividual(s);
                }
                break;

            case REINICIANDO_RED:
                if (!s.microCortePropio && (ahora - s.tiempoEstadoMs >= 1350)) {
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;
        }
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
                "  }, 450);" +
                "}";
        evaluarJS(s, jsLogout, null);
    }

    private void clicarOrdinariaJS(SesionBot s) {
        String jsOrd =
                "var b = document.querySelector(\"a.btn-tesoro-green, a[href*='mod.add(1)']\") || Array.from(document.querySelectorAll('.card-header a, .card-header button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "if(b) b.click();";
        evaluarJS(s, jsOrd, null);
    }

    private void irAIntervencionJS(SesionBot s) {
        String jsMenu =
                "var sub = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "if (sub && sub.offsetWidth > 0) {" +
                "  sub.click();" +
                "} else {" +
                "  var ops = Array.from(document.querySelectorAll('a')).find(el => el.textContent.includes('Operaciones Divisas'));" +
                "  if (ops) { ops.click(); }" +
                "  setTimeout(function(){" +
                "    var sub2 = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "    if (sub2) { sub2.click(); } else { window.location.hash = '#/int'; }" +
                "  }, 500);" +
                "}";
        evaluarJS(s, jsMenu, null);
    }
}
