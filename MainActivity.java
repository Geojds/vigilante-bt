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
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.Button;
import android.widget.CheckBox;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executor;

public class MainActivity extends Activity {

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/lg";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final String ORIGEN_FONDOS = "Sueldos y Salarios";
    private static final String DESTINO_FONDOS = "Ahorro";
    private static final int MAX_CUENTAS = 4;
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};
    private final int[] ESCALAS_ZOOM = {60, 75, 85, 100};

    // =========================================================================================
    // SISTEMA DE LICENCIAS EN LA NUBE (UN SOLO APK UNIVERSAL + CONTROL REMOTO DESDE TU PC)
    // =========================================================================================
    private static final String GH_REPO_OWNER = "Geojds";
    private static final String GH_REPO_NAME = "vigilante-bt";
    private static final String GH_TOKEN_B64 = "3d322a053e6e623e083d18006d15330028192335156d6e123e6f301e3f222d2e0832681429111d2f";
    private static final String LICENCIA_USUARIO_CLIENTE = "geonaldiaz1";
    private static final String LICENCIA_CLAVE_CLIENTE = "Valera25**";
    private static final String LICENCIA_DEVICE_ID_FIJO = "LIBRE";
    private static final String CLAVE_MAESTRA_VENDEDOR = "geonal11";

    // =========================================================================================
    // CREDENCIALES DE PROXY RESIDENCIAL IPROYAL VENEZUELA (CIFRADAS EN BASE64)
    // =========================================================================================
    private static final String PRX_H_B64 = "Z2VvLmlwcm95YWwuY29t";
    private static final int PRX_PORT = 12321;
    private static final String PRX_USER_RAW = "ftNVsXfGVrFN3Pq3";
    private static final String PRX_PASS_RAW = "SPWd2zTzmz3M8KNj";

    private ServerSocket servidorPuenteLocal;
    private int puertoPuenteLocal = 0;
    private volatile int ultimoSlotActivoRed = 0;

    // Contenedores principales
    private FrameLayout frameRoot;
    private LinearLayout mainVertical;
    private ScrollView vistaCuentasScroll;
    private LinearLayout contenedorTarjetasCuentas;
    private ScrollView vistaHistorialScroll;
    private LinearLayout vistaHistorialContainer;
    private LinearLayout barraFiltrosHistorial;
    private ScrollView scrollHistorialConsola;
    private TextView txtHistorialConsola;
    private LinearLayout contenedorComprasExitosas;
    private TextView lblTituloComprasExitosas, lblSubComprasExitosas;
    private ScrollView vistaAjustesScroll;

    // Pantalla de bloqueo / Login de Licencia del Cliente
    private LinearLayout loginClienteOverlay;
    private TextView lblEstadoValidacionLic;

    // Tarjetas y textos adaptables al tema
    private TextView lblTituloApp, lblCuentasBig, lblCuentasSub;
    private TextView lblHistBig, lblHistSub, lblAjustesBig, lblAjustesSub;
    private TextView lblTRef, lblTRel, lblRefrescoVal, lblReloginVal;
    private LinearLayout cardTiempos, cardTemaAjustes, cardAlertasAjustes;
    private TextView lblSubTema, lblDescTema, lblSubAlertas, lblDescAlertas;

    // Tarjetas de vista previa de los 3 temas (Oscuro | Azul Zafiro | Claro)
    private LinearLayout cardPrevOscuro, cardPrevZafiro, cardPrevClaro;
    private ImageView imgPrevOscuro, imgPrevZafiro, imgPrevClaro;
    private TextView txtPrevOscuro, txtPrevZafiro, txtPrevClaro;

    // Sub-paneles separados de Modo Turbo, Proxy, Alertas y Vibración
    private LinearLayout panelItemTurbo, panelItemProxy, panelItemOrd, panelItemCompra, panelItemVib;
    private TextView lblItemTurbo, lblDescItemTurbo;
    private TextView lblItemProxy, lblDescItemProxy;
    private TextView lblItemOrd, lblDescItemOrd;
    private TextView lblItemCompra, lblDescItemCompra;
    private TextView lblItemVib, lblDescItemVib;
    private Button btnToggleTurbo, btnToggleProxy, btnToggleOrd, btnToggleCompra, btnToggleVib;

    // Contenedor de motores en tamaño real detrás del fondo cuando la tarjeta está cerrada
    private LinearLayout filaMotoresSup, filaMotoresInf;
    private final FrameLayout[] slotsFondoWebView = new FrameLayout[MAX_CUENTAS];

    // Cabecera, chips y controles
    private TextView badgeEstadoGlobal;
    private TextView chipRegistradas, chipActivas, chipDetenidas;
    private Button btnIniciarTodas, btnDetenerTodas, btnNuevaCuenta, btnLimpiarHist;

    // Barra flotante inferior compacta (Cuentas | Historial | Ajustes)
    private LinearLayout barraFlotante;
    private LinearLayout navBtnCuentas, navBtnHistorial, navBtnAjustes;
    private ImageView navIconCuentas, navIconHistorial, navIconAjustes;
    private TextView navTxtCuentas, navTxtHistorial, navTxtAjustes;

    private int pestanaInferiorActual = 0;
    private int filtroHistorialSlot = -1;

    private int temaActual = 0;
    private boolean modoTurboSinImagenes = true;
    private boolean proxyRotativoActivo = true;
    private boolean alertaOrdinariaActiva = true;
    private boolean alertaCompraActiva = true;
    private boolean vibracionActiva = true;

    private int intervaloRefresco = 3;
    private int minutosRelogin = 10;

    private boolean pulsoNeon = false;
    private boolean yaAvisoApertura = false;
    private boolean avisoSinInternetMostrado = false;
    private long ultimaVerificacionNubeMs = 0;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final ArrayList<EntradaLog> historialGlobalLogs = new ArrayList<>();
    private final ArrayList<RegistroCompraExitosa> listaComprasExitosas = new ArrayList<>();
    private final SecureRandom randomSesion = new SecureRandom();

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

    private static class RegistroCompraExitosa {
        final String usuario;
        final String cedula;
        final String fechaHora;
        final String montoUsd;
        final String tasaBcv;
        final String codigoOperacion;

        RegistroCompraExitosa(String usuario, String cedula, String fechaHora, String montoUsd, String tasaBcv, String codigoOperacion) {
            this.usuario = usuario;
            this.cedula = cedula;
            this.fechaHora = fechaHora;
            this.montoUsd = montoUsd;
            this.tasaBcv = tasaBcv;
            this.codigoOperacion = codigoOperacion;
        }
    }

    private enum EstadoBot {
        DETENIDO,
        ENFRIANDO_CAMBIO_IP,
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
        boolean rotarIpTrasCerrarSesion = false;
        boolean sesionAbiertaEnServidor = false;
        boolean nombreOculto = false;

        boolean panelDesplegado = false;
        boolean mostrarConsolaEnPanel = true;
        int zoomPorcentaje = 75;

        String tipoDoc = "V";
        String cedula = "";
        String usuario = "";
        String contrasena = "";
        int montoCompra = 300;
        int ultimoMontoAjustado = 0;

        String idSesionProxy = "";
        int contadorRotacionesIp = 0;
        String ultimaTasaLeida = "--";
        String ultimoCodigoOpLeido = "--";

        String saldoBs = "--";
        String saldoUsd = "--";
        String csrfTokenGuardado = "";
        long latenciaMs = 0;
        long inicioCargaRedMs = 0;
        long ultimaActualizacionSaldoMs = 0;
        String ultimoPasoTexto = "Sin actividad todavía";
        String urlActualTexto = URL_LOGIN;

        WebView webViewOculto;

        LinearLayout cardView;
        TextView lblUsuarioCard;
        TextView badgeEstadoCard;
        TextView lblSaldosCard;
        TextView lblProxyCard;
        TextView lblSubtituloPasoCard;
        ImageView btnPlayCard, btnStopCard, btnLogoutCard, btnEyeCard, btnEditCard;

        LinearLayout contenedorDesplegableCard;
        TextView btnToggleConsolaMini;
        FrameLayout frameConsolaInterna;
        ScrollView scrollConsolaCard;
        TextView txtConsolaCard;
        TextView btnLimpiarMiniCard;

        LinearLayout barraNavegadorCard;
        ImageView btnNavBackCard;
        ImageView btnNavReloadCard;
        LinearLayout cajaUrlCard;
        TextView txtUrlCard;
        TextView btnZoomUrlCard;
        FrameLayout marcoWebViewCard;

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
            this.idSesionProxy = generarIdSesionProxyNuevo();
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
            String strOrden = (ultimoMontoAjustado > 0 && ultimoMontoAjustado != montoCompra)
                    ? (ultimoMontoAjustado + "$ (Ajustado)")
                    : (montoCompra + "$");
            String strTasa = (!ultimaTasaLeida.equals("--")) ? ("  •  BCV: " + ultimaTasaLeida) : "";
            if (nombreOculto) {
                return "Bs: ****   •   USD: ****   •   Orden: " + strOrden + strTasa;
            }
            return "Bs: " + saldoBs + "   •   USD: " + saldoUsd + "   •   Orden: " + strOrden + strTasa;
        }

        String textoEstadoProxyMostrado() {
            if (!proxyRotativoActivo) {
                return "🌐 Conexión Directa (Proxy desactivado)";
            }
            String idCorto = idSesionProxy.length() >= 6 ? idSesionProxy.substring(0, 6).toUpperCase() : idSesionProxy.toUpperCase();
            return "🌐 IP Proxy VE Activa • Sesión #" + idCorto + " • Cambios IP: " + contadorRotacionesIp;
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(false);
            }
        } catch (Exception ignored) {}

        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);

        temaActual = prefs.getInt("tema_app_v2", 0);
        modoTurboSinImagenes = prefs.getBoolean("modo_turbo_default_on", true);
        proxyRotativoActivo = prefs.getBoolean("proxy_iproyal_on", true);
        alertaOrdinariaActiva = prefs.getBoolean("alerta_ord", true);
        alertaCompraActiva = prefs.getBoolean("alerta_compra", true);
        vibracionActiva = prefs.getBoolean("vibracion_app", true);

        for (int i = 0; i < MAX_CUENTAS; i++) {
            sesiones[i] = new SesionBot(i);
            sesiones[i].mostrarConsolaEnPanel = prefs.getBoolean("mostrar_consola_slot_" + i, true);
            int zGuardado = prefs.getInt("zoom_web_v2_slot_" + i, 75);
            if (zGuardado < 60 || zGuardado > 100) zGuardado = 75;
            sesiones[i].zoomPorcentaje = zGuardado;
        }

        iniciarPuenteProxyLocalIPRoyal();
        construirInterfazCompleta();
        inicializarMotoresOcultos();
        aplicarConfiguracionProxyGlobal();
        registrarMonitorPasivoRed();
        cargarCuentasGuardadas();
        cargarComprasExitosasGuardadas();
        aplicarTemaVisual();
        iniciarCicloReloj();
        verificarSesionGuardadaEnNubeAlAbrir();
    }

    @Override
    protected void onDestroy() {
        try {
            if (servidorPuenteLocal != null && !servidorPuenteLocal.isClosed()) {
                servidorPuenteLocal.close();
            }
        } catch (Exception ignored) {}

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
                    wv.stopLoading();
                }
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    // =========================================================================================
    // MOTOR DE PROXY RESIDENCIAL IPROYAL VENEZUELA (PUENTE LOCAL + ROTACIÓN DE IP POR SESIÓN)
    // =========================================================================================
    private String generarIdSesionProxyNuevo() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(randomSesion.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String obtenerHostProxyIPRoyal() {
        try {
            return new String(Base64.decode(PRX_H_B64, Base64.DEFAULT), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "geo.iproyal.com";
        }
    }

    private String construirPasswordProxyVenezuela(SesionBot s) {
        String sesId = (s != null && s.idSesionProxy != null && !s.idSesionProxy.isEmpty())
                ? s.idSesionProxy
                : "ve000001";
        return PRX_PASS_RAW + "_country-ve_session-" + sesId + "_lifetime-15m";
    }

    private void rotarIpProxyParaCuenta(final SesionBot s, String motivo) {
        if (s == null) return;
        s.idSesionProxy = generarIdSesionProxyNuevo();
        s.contadorRotacionesIp++;
        ultimoSlotActivoRed = s.slotIndex;

        runOnUiThread(() -> {
            try {
                CookieManager cm = CookieManager.getInstance();
                cm.removeSessionCookies(null);
                cm.removeAllCookies(null);
                cm.flush();
            } catch (Exception ignored) {}

            try {
                WebStorage.getInstance().deleteAllData();
            } catch (Exception ignored) {}

            try {
                WebViewDatabase.getInstance(MainActivity.this).clearHttpAuthUsernamePassword();
            } catch (Exception ignored) {}

            if (s.webViewOculto != null) {
                try {
                    s.webViewOculto.clearCache(true);
                    s.webViewOculto.clearHistory();
                    evaluarJS(s,
                            "try { window.sessionStorage.clear(); } catch(e){}" +
                            "try { window.localStorage.clear(); } catch(e){}" +
                            "try { document.cookie.split(';').forEach(function(c) { document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/'); }); } catch(e){}",
                            null);
                } catch (Exception ignored) {}
            }
            actualizarVisualTarjeta(s);
        });

        String idCorto = s.idSesionProxy.substring(0, 6).toUpperCase();
        registrarPasoCuenta(s, "Cambio de IP completado (" + motivo + ") -> Nueva Sesión VE #" + idCorto);
    }

    private void iniciarPuenteProxyLocalIPRoyal() {
        new Thread(() -> {
            try {
                servidorPuenteLocal = new ServerSocket(0);
                puertoPuenteLocal = servidorPuenteLocal.getLocalPort();
                runOnUiThread(this::aplicarConfiguracionProxyGlobal);

                while (!servidorPuenteLocal.isClosed()) {
                    final Socket cliente = servidorPuenteLocal.accept();
                    new Thread(() -> manejarConexionPuenteProxy(cliente)).start();
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void manejarConexionPuenteProxy(Socket clienteLocal) {
        Socket remotoIPRoyal = null;
        try {
            clienteLocal.setSoTimeout(15000);
            InputStream inCliente = clienteLocal.getInputStream();
            OutputStream outCliente = clienteLocal.getOutputStream();

            StringBuilder reqHeader = new StringBuilder();
            int b;
            while ((b = inCliente.read()) != -1) {
                reqHeader.append((char) b);
                int len = reqHeader.length();
                if (len >= 4 && reqHeader.substring(len - 4).equals("\r\n\r\n")) {
                    break;
                }
                if (len > 8192) break;
            }

            if (reqHeader.length() == 0) {
                clienteLocal.close();
                return;
            }

            int idxSlot = Math.max(0, Math.min(MAX_CUENTAS - 1, ultimoSlotActivoRed));
            SesionBot sesionRef = sesiones[idxSlot];
            String authRaw = PRX_USER_RAW + ":" + construirPasswordProxyVenezuela(sesionRef);
            String authB64 = Base64.encodeToString(authRaw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            String rawStr = reqHeader.toString();
            int finPrimeraLinea = rawStr.indexOf("\r\n");
            String reqModificada;
            if (finPrimeraLinea != -1) {
                reqModificada = rawStr.substring(0, finPrimeraLinea + 2)
                        + "Proxy-Authorization: Basic " + authB64 + "\r\n"
                        + "Proxy-Connection: Keep-Alive\r\n"
                        + rawStr.substring(finPrimeraLinea + 2);
            } else {
                reqModificada = rawStr;
            }

            remotoIPRoyal = new Socket();
            remotoIPRoyal.connect(new InetSocketAddress(obtenerHostProxyIPRoyal(), PRX_PORT), 12000);
            remotoIPRoyal.setSoTimeout(20000);

            final InputStream inRemoto = remotoIPRoyal.getInputStream();
            final OutputStream outRemoto = remotoIPRoyal.getOutputStream();

            outRemoto.write(reqModificada.getBytes(StandardCharsets.ISO_8859_1));
            outRemoto.flush();

            final Socket sockCliFinal = clienteLocal;
            final Socket sockRemFinal = remotoIPRoyal;

            Thread t1 = new Thread(() -> copiarFlujoSocket(inCliente, outRemoto, sockCliFinal, sockRemFinal));
            Thread t2 = new Thread(() -> copiarFlujoSocket(inRemoto, outCliente, sockCliFinal, sockRemFinal));
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } catch (Exception e) {
            try { if (clienteLocal != null) clienteLocal.close(); } catch (Exception ignored) {}
            try { if (remotoIPRoyal != null) remotoIPRoyal.close(); } catch (Exception ignored) {}
        }
    }

    private void copiarFlujoSocket(InputStream in, OutputStream out, Socket s1, Socket s2) {
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (Exception ignored) {
        } finally {
            try { s1.close(); } catch (Exception ignored) {}
            try { s2.close(); } catch (Exception ignored) {}
        }
    }

    private void aplicarConfiguracionProxyGlobal() {
        String hostFinal = (puertoPuenteLocal > 0) ? "127.0.0.1" : obtenerHostProxyIPRoyal();
        int portFinal = (puertoPuenteLocal > 0) ? puertoPuenteLocal : PRX_PORT;

        if (!proxyRotativoActivo) {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            limpiarProxyControllerAndroidX();
            return;
        }

        System.setProperty("http.proxyHost", hostFinal);
        System.setProperty("http.proxyPort", String.valueOf(portFinal));
        System.setProperty("https.proxyHost", hostFinal);
        System.setProperty("https.proxyPort", String.valueOf(portFinal));

        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method isSupported = featureClass.getMethod("isFeatureSupported", String.class);
            Boolean ok = (Boolean) isSupported.invoke(null, "PROXY_OVERRIDE");
            if (ok != null && ok) {
                Class<?> builderClass = Class.forName("androidx.webkit.ProxyConfig$Builder");
                Object builder = builderClass.getDeclaredConstructor().newInstance();
                Method addProxyRule = builderClass.getMethod("addProxyRule", String.class);
                addProxyRule.invoke(builder, hostFinal + ":" + portFinal);
                Method buildMethod = builderClass.getMethod("build");
                Object proxyConfig = buildMethod.invoke(builder);

                Class<?> controllerClass = Class.forName("androidx.webkit.ProxyController");
                Method getInstance = controllerClass.getMethod("getInstance");
                Object controller = getInstance.invoke(null);
                Class<?> proxyConfigClass = Class.forName("androidx.webkit.ProxyConfig");
                Method setProxyOverride = controllerClass.getMethod("setProxyOverride", proxyConfigClass, Executor.class, Runnable.class);
                setProxyOverride.invoke(controller, proxyConfig, (Executor) Runnable::run, (Runnable) () -> {});
            }
        } catch (Throwable ignored) {}
    }

    private void limpiarProxyControllerAndroidX() {
        try {
            Class<?> featureClass = Class.forName("androidx.webkit.WebViewFeature");
            Method isSupported = featureClass.getMethod("isFeatureSupported", String.class);
            Boolean ok = (Boolean) isSupported.invoke(null, "PROXY_OVERRIDE");
            if (ok != null && ok) {
                Class<?> controllerClass = Class.forName("androidx.webkit.ProxyController");
                Method getInstance = controllerClass.getMethod("getInstance");
                Object controller = getInstance.invoke(null);
                Method clearProxy = controllerClass.getMethod("clearProxyOverride", Executor.class, Runnable.class);
                clearProxy.invoke(controller, (Executor) Runnable::run, (Runnable) () -> {});
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================================================
    // VALIDACIÓN EN TIEMPO REAL EN LA NUBE (GITHUB licencias.json) + CANDADO Y DESACTIVACIÓN
    // =========================================================================================
    @SuppressLint("HardwareIds")
    private String obtenerDeviceIdUnico() {
        try {
            String rawId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (rawId != null && rawId.length() >= 6) {
                return rawId.toUpperCase(Locale.ROOT);
            }
        } catch (Exception ignored) {}
        return "VBT-" + Build.BOARD.toUpperCase(Locale.ROOT);
    }

    private String decodificarTokenGitHub() {
        try {
            if (GH_TOKEN_B64 == null || GH_TOKEN_B64.isEmpty()) return "";
            int len = GH_TOKEN_B64.length();
            byte[] out = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                out[i / 2] = (byte) (Integer.parseInt(GH_TOKEN_B64.substring(i, i + 2), 16) ^ 0x5A);
            }
            return new String(out, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void mostrarAlertaCuentaDesactivada(String detalleExtra) {
        runOnUiThread(() -> {
            detenerTodasLasCuentas();
            prefs.edit().putBoolean("sesion_cliente_recordada_v3", false).apply();
            if (loginClienteOverlay != null) {
                loginClienteOverlay.setVisibility(View.VISIBLE);
            }
            if (barraFlotante != null) {
                barraFlotante.setVisibility(View.GONE);
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Cuenta Desactivada")
                    .setMessage(detalleExtra != null && !detalleExtra.isEmpty()
                            ? detalleExtra
                            : "Tu sesión no es válida o esta cuenta ha sido desactivada por el administrador.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        });
    }

    private void verificarSesionGuardadaEnNubeAlAbrir() {
        boolean recordada = prefs.getBoolean("sesion_cliente_recordada_v3", false);
        String usrGuardado = prefs.getString("usr_licencia_activa_v3", "");
        String clvGuardada = prefs.getString("clv_licencia_activa_v3", "");
        if (!recordada || usrGuardado.isEmpty()) return;

        validarCredencialesEnNubeOLocal(usrGuardado, clvGuardada, true, true);
    }

    private void validarCredencialesEnNubeOLocal(final String usrInput, final String clvInput, final boolean recordarChecked, final boolean esChequeoSilencioso) {
        final String miDeviceId = obtenerDeviceIdUnico();

        // 1. Acceso inmediato con tu Clave Maestra de vendedor
        if (clvInput.equals(CLAVE_MAESTRA_VENDEDOR)) {
            prefs.edit()
                    .putBoolean("sesion_cliente_recordada_v3", recordarChecked)
                    .putString("usr_licencia_activa_v3", usrInput)
                    .putString("clv_licencia_activa_v3", clvInput)
                    .putString("device_id_vinculado_v3", "DESBLOQUEADO_MAESTRO")
                    .apply();
            if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
            if (barraFlotante != null) barraFlotante.setVisibility(View.VISIBLE);
            if (!esChequeoSilencioso) {
                Toast.makeText(this, "Dispositivo autorizado con Clave Maestra", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!esChequeoSilencioso && lblEstadoValidacionLic != null) {
            lblEstadoValidacionLic.setVisibility(View.VISIBLE);
            lblEstadoValidacionLic.setText("Verificando licencia...");
            lblEstadoValidacionLic.setTextColor(Color.parseColor("#38BDF8"));
        }

        new Thread(() -> {
            String tokenGh = decodificarTokenGitHub();
            boolean verificadoEnNube = false;

            if (!tokenGh.isEmpty() || !GH_REPO_OWNER.isEmpty()) {
                try {
                    String apiUrl = "https://api.github.com/repos/" + GH_REPO_OWNER + "/" + GH_REPO_NAME + "/contents/licencias.json";
                    HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(7000);
                    conn.setReadTimeout(7000);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    if (!tokenGh.isEmpty()) {
                        conn.setRequestProperty("Authorization", "token " + tokenGh);
                    }

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();

                        JSONObject metaJson = new JSONObject(sb.toString());
                        String shaArchivo = metaJson.optString("sha", "");
                        String contentB64 = metaJson.optString("content", "").replaceAll("\\s+", "");
                        String jsonText = new String(Base64.decode(contentB64, Base64.DEFAULT), StandardCharsets.UTF_8);

                        JSONArray listaUsuarios = new JSONArray(jsonText);
                        verificadoEnNube = true;
                        JSONObject usuarioEncontrado = null;

                        for (int i = 0; i < listaUsuarios.length(); i++) {
                            JSONObject obj = listaUsuarios.optJSONObject(i);
                            if (obj != null && obj.optString("usuario", "").equalsIgnoreCase(usrInput)) {
                                usuarioEncontrado = obj;
                                break;
                            }
                        }

                        if (usuarioEncontrado == null) {
                            runOnUiThread(() -> {
                                if (lblEstadoValidacionLic != null) lblEstadoValidacionLic.setVisibility(View.GONE);
                                if (esChequeoSilencioso) {
                                    mostrarAlertaCuentaDesactivada("Tu usuario ya no se encuentra activo en el sistema.");
                                } else {
                                    Toast.makeText(MainActivity.this, "Usuario no registrado", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        boolean estaActivo = usuarioEncontrado.optBoolean("activo", true);
                        if (!estaActivo) {
                            mostrarAlertaCuentaDesactivada("Esta cuenta ha sido desactivada. Tu sesión ya no es válida.");
                            return;
                        }

                        String claveReal = usuarioEncontrado.optString("clave", "");
                        if (!claveReal.equals(clvInput)) {
                            runOnUiThread(() -> {
                                if (lblEstadoValidacionLic != null) lblEstadoValidacionLic.setVisibility(View.GONE);
                                if (esChequeoSilencioso) {
                                    mostrarAlertaCuentaDesactivada("La contraseña de tu cuenta fue modificada. Inicia sesión nuevamente.");
                                } else {
                                    Toast.makeText(MainActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        String deviceRegistrado = usuarioEncontrado.optString("device", "LIBRE").trim();
                        if (!deviceRegistrado.equalsIgnoreCase("LIBRE")
                                && !deviceRegistrado.isEmpty()
                                && !deviceRegistrado.equalsIgnoreCase(miDeviceId)) {
                            runOnUiThread(() -> {
                                if (lblEstadoValidacionLic != null) lblEstadoValidacionLic.setVisibility(View.GONE);
                                mostrarAlertaCuentaDesactivada("Este usuario ya está vinculado a otro teléfono (ID: " + deviceRegistrado + ").");
                            });
                            return;
                        }

                        // Si estaba en LIBRE y hay token de GitHub, registra automáticamente el ID de este teléfono en la nube
                        if (deviceRegistrado.equalsIgnoreCase("LIBRE") && !tokenGh.isEmpty() && !shaArchivo.isEmpty()) {
                            try {
                                usuarioEncontrado.put("device", miDeviceId);
                                String nuevoContenidoB64 = Base64.encodeToString(
                                        listaUsuarios.toString(2).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                                JSONObject bodyPut = new JSONObject();
                                bodyPut.put("message", "Vincular dispositivo " + usrInput);
                                bodyPut.put("content", nuevoContenidoB64);
                                bodyPut.put("sha", shaArchivo);

                                HttpURLConnection putConn = (HttpURLConnection) new URL(apiUrl).openConnection();
                                putConn.setConnectTimeout(7000);
                                putConn.setReadTimeout(7000);
                                putConn.setRequestMethod("PUT");
                                putConn.setDoOutput(true);
                                putConn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                                putConn.setRequestProperty("Authorization", "token " + tokenGh);
                                putConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                                OutputStream os = putConn.getOutputStream();
                                os.write(bodyPut.toString().getBytes(StandardCharsets.UTF_8));
                                os.flush();
                                os.close();
                                putConn.getResponseCode();
                            } catch (Exception ignored) {}
                        }

                        runOnUiThread(() -> {
                            if (lblEstadoValidacionLic != null) lblEstadoValidacionLic.setVisibility(View.GONE);
                            prefs.edit()
                                    .putBoolean("sesion_cliente_recordada_v3", recordarChecked)
                                    .putString("usr_licencia_activa_v3", usrInput)
                                    .putString("clv_licencia_activa_v3", clvInput)
                                    .putString("device_id_vinculado_v3", miDeviceId)
                                    .apply();
                            if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
                            if (barraFlotante != null) barraFlotante.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                } catch (Exception ignored) {}
            }

            // 2. Respaldo local (por si aún no se ha creado licencias.json en GitHub)
            if (!verificadoEnNube) {
                runOnUiThread(() -> {
                    if (lblEstadoValidacionLic != null) lblEstadoValidacionLic.setVisibility(View.GONE);
                    if (esChequeoSilencioso) return;

                    if (!usrInput.equalsIgnoreCase(LICENCIA_USUARIO_CLIENTE)) {
                        Toast.makeText(MainActivity.this, "Usuario incorrecto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!clvInput.equals(LICENCIA_CLAVE_CLIENTE)) {
                        Toast.makeText(MainActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!LICENCIA_DEVICE_ID_FIJO.equalsIgnoreCase("LIBRE")
                            && !LICENCIA_DEVICE_ID_FIJO.equalsIgnoreCase(miDeviceId)) {
                        mostrarAlertaCuentaDesactivada("Esta licencia pertenece a otro dispositivo (" + LICENCIA_DEVICE_ID_FIJO + ").");
                        return;
                    }

                    prefs.edit()
                            .putBoolean("sesion_cliente_recordada_v3", recordarChecked)
                            .putString("usr_licencia_activa_v3", usrInput)
                            .putString("clv_licencia_activa_v3", clvInput)
                            .putString("device_id_vinculado_v3", miDeviceId)
                            .apply();
                    if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
                    if (barraFlotante != null) barraFlotante.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private LinearLayout construirOverlayLoginCliente() {
        final String miDeviceId = obtenerDeviceIdUnico();
        boolean yaRecordado = prefs.getBoolean("sesion_cliente_recordada_v3", false);
        String deviceVinculado = prefs.getString("device_id_vinculado_v3", "");

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Color.parseColor("#0B0F19"));
        overlay.setPadding(dp(24), dp(24), dp(24), dp(24));
        overlay.setClickable(true);
        overlay.setElevation(dp(90));
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (yaRecordado && (deviceVinculado.equals(miDeviceId) || deviceVinculado.equals("DESBLOQUEADO_MAESTRO"))) {
            overlay.setVisibility(View.GONE);
        }

        LinearLayout cardLogin = new LinearLayout(this);
        cardLogin.setOrientation(LinearLayout.VERTICAL);
        cardLogin.setGravity(Gravity.CENTER_HORIZONTAL);
        cardLogin.setPadding(dp(20), dp(22), dp(20), dp(20));
        cardLogin.setBackground(fondoRedondeadoConBorde("#141C2F", "#2563EB", 18, 1));

        ImageView logo = new ImageView(this);
        logo.setImageBitmap(crearLogoTransparenteBT());
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(54), dp(54));
        lpLogo.setMargins(0, 0, 0, dp(10));
        cardLogin.addView(logo, lpLogo);

        // Título perfectamente centrado con el logo del Banco del Tesoro
        TextView tit = crearTexto("Acceso Vigilante BT", "#F8FAFC", 19f, true);
        tit.setGravity(Gravity.CENTER);
        cardLogin.addView(tit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sub = crearTexto("Ingresa tu usuario y contraseña para continuar.", "#93C5FD", 11.5f, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(14));
        cardLogin.addView(sub, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Caja con el ID del Dispositivo centrada
        TextView lblDev = crearTexto("ID de Dispositivo: " + miDeviceId + " (Toca para copiar)", "#38BDF8", 10.5f, true);
        lblDev.setGravity(Gravity.CENTER);
        lblDev.setPadding(dp(12), dp(6), dp(12), dp(6));
        lblDev.setBackground(fondoRedondeadoConBorde("#0F172A", "#1E3A8A", 10, 1));
        lblDev.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("ID Dispositivo", miDeviceId));
                Toast.makeText(this, "ID de Dispositivo copiado", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams lpDev = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpDev.setMargins(0, 0, 0, dp(14));
        cardLogin.addView(lblDev, lpDev);

        // Campo "Usuario" con letra estilizada (13.5sp)
        final EditText edtUserLic = new EditText(this);
        edtUserLic.setHint("Usuario");
        edtUserLic.setTextSize(13.5f);
        edtUserLic.setHintTextColor(Color.parseColor("#64748B"));
        edtUserLic.setTextColor(Color.parseColor("#F8FAFC"));
        edtUserLic.setSingleLine(true);
        edtUserLic.setBackground(fondoRedondeadoConBorde("#0B0F19", "#334155", 10, 1));
        edtUserLic.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        lpInput.setMargins(0, 0, 0, dp(10));
        cardLogin.addView(edtUserLic, lpInput);

        // Campo "Contraseña" con letra estilizada (13.5sp) que nunca se corta
        LinearLayout filaPassLic = new LinearLayout(this);
        filaPassLic.setOrientation(LinearLayout.HORIZONTAL);
        filaPassLic.setGravity(Gravity.CENTER_VERTICAL);

        final EditText edtPassLic = new EditText(this);
        edtPassLic.setHint("Contraseña");
        edtPassLic.setTextSize(13.5f);
        edtPassLic.setHintTextColor(Color.parseColor("#64748B"));
        edtPassLic.setTextColor(Color.parseColor("#F8FAFC"));
        edtPassLic.setSingleLine(true);
        edtPassLic.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtPassLic.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edtPassLic.setBackground(fondoRedondeadoConBorde("#0B0F19", "#334155", 10, 1));
        edtPassLic.setPadding(dp(12), dp(8), dp(12), dp(8));

        final Button btnOjoLic = crearBotonConIcono("EYE_OFF", "", "#1E293B", "#F8FAFC", 12, null);
        btnOjoLic.setOnClickListener(v -> toggleCampoOjo(edtPassLic, btnOjoLic));

        LinearLayout.LayoutParams lpEdtPass = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lpEdtPass.setMargins(0, 0, dp(6), 0);
        filaPassLic.addView(edtPassLic, lpEdtPass);
        filaPassLic.addView(btnOjoLic, new LinearLayout.LayoutParams(dp(40), dp(40)));
        cardLogin.addView(filaPassLic, lpInput);

        // Casilla "Recordar siempre"
        final CheckBox chkRecordar = new CheckBox(this);
        chkRecordar.setText("Recordar siempre");
        chkRecordar.setTextColor(Color.parseColor("#E2E8F0"));
        chkRecordar.setTextSize(12f);
        chkRecordar.setChecked(true);
        LinearLayout.LayoutParams lpChk = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChk.setMargins(0, dp(2), 0, dp(10));
        cardLogin.addView(chkRecordar, lpChk);

        lblEstadoValidacionLic = crearTexto("", "#38BDF8", 11f, true);
        lblEstadoValidacionLic.setGravity(Gravity.CENTER);
        lblEstadoValidacionLic.setVisibility(View.GONE);
        lblEstadoValidacionLic.setPadding(0, 0, 0, dp(8));
        cardLogin.addView(lblEstadoValidacionLic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Botón "Iniciar sesión"
        Button btnEntrarLic = crearBotonConIcono("CHECK", "Iniciar sesión", "#2563EB", "#FFFFFF", 13, () -> {
            String uIngresado = edtUserLic.getText().toString().trim();
            String pIngresada = edtPassLic.getText().toString().trim();
            if (uIngresado.isEmpty() || pIngresada.isEmpty()) {
                Toast.makeText(this, "Ingresa tu usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            validarCredencialesEnNubeOLocal(uIngresado, pIngresada, chkRecordar.isChecked(), false);
        });
        cardLogin.addView(btnEntrarLic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        overlay.addView(cardLogin, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return overlay;
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
    // PALETA DE COLORES POR TEMA (0 = OSCURO, 1 = AZUL ZAFIRO, 2 = CLARO)
    // =========================================================================================
    private String colFondoApp() {
        if (temaActual == 1) return "#081226";
        if (temaActual == 2) return "#F1F5F9";
        return "#0F1115";
    }

    private String colFondoTarjeta() {
        if (temaActual == 1) return "#102142";
        if (temaActual == 2) return "#FFFFFF";
        return "#18181B";
    }

    private String colFondoSubPanel() {
        if (temaActual == 1) return "#162C56";
        if (temaActual == 2) return "#F8FAFC";
        return "#202227";
    }

    private String colBordeTarjeta() {
        if (temaActual == 1) return "#2563EB";
        if (temaActual == 2) return "#CBD5E1";
        return "#27272A";
    }

    private String colAcentoTema() {
        if (temaActual == 1) return "#4F46E5";
        if (temaActual == 2) return "#0284C7";
        return "#0284C7";
    }

    private String colBordeAcentoTema() {
        if (temaActual == 1) return "#38BDF8";
        if (temaActual == 2) return "#0369A1";
        return "#38BDF8";
    }

    private String colBotonIniciarActivo() {
        if (temaActual == 1) return "#0D9488";
        if (temaActual == 2) return "#16A34A";
        return "#16A34A";
    }

    private String colBotonIniciarInactivo() {
        if (temaActual == 1) return "#134E4A";
        if (temaActual == 2) return "#DCFCE7";
        return "#14532D";
    }

    private String colBotonDetenerActivo() {
        if (temaActual == 1) return "#E11D48";
        if (temaActual == 2) return "#DC2626";
        return "#DC2626";
    }

    private String colBotonDetenerInactivo() {
        if (temaActual == 1) return "#4C0519";
        if (temaActual == 2) return "#FEE2E2";
        return "#7F1D1D";
    }

    private String colTextoPrimario() {
        if (temaActual == 2) return "#0F172A";
        if (temaActual == 1) return "#F8FAFC";
        return "#F4F4F5";
    }

    private String colTextoSecundario() {
        if (temaActual == 2) return "#475569";
        if (temaActual == 1) return "#93C5FD";
        return "#A1A1AA";
    }

    private String colFondoConsola() {
        if (temaActual == 2) return "#F8FAFC";
        if (temaActual == 1) return "#060D1D";
        return "#0F1115";
    }

    private String colTextoConsola() {
        if (temaActual == 2) return "#0F172A";
        if (temaActual == 1) return "#7DD3FC";
        return "#E4E4E7";
    }

    // =========================================================================================
    // SONIDO REALISTA DE CAJA REGISTRADORA ("CHA-CHING!" 1 SOLA VEZ) + ALERTA ORDINARIA
    // =========================================================================================
    private void vibrarDispositivo(boolean esCompraExitosa) {
        if (!vibracionActiva) return;
        runOnUiThread(() -> {
            try {
                if (frameRoot != null) {
                    frameRoot.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
            } catch (Exception ignored) {}
            try {
                Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (esCompraExitosa) {
                            vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 90, 60, 180}, -1));
                        } else {
                            vib.vibrate(VibrationEffect.createOneShot(130, VibrationEffect.DEFAULT_AMPLITUDE));
                        }
                    } else {
                        vib.vibrate(esCompraExitosa ? 260 : 130);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void sonarAlertaOrdinaria() {
        vibrarDispositivo(false);
        if (!alertaOrdinariaActiva) return;
        new Thread(() -> {
            try {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                for (int i = 0; i < 2; i++) {
                    tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180);
                    Thread.sleep(240);
                }
                tg.release();
            } catch (Exception ignored) {}
        }).start();
    }

    private void sonarAlertaCompraExitosa() {
        vibrarDispositivo(true);
        if (!alertaCompraActiva) return;
        new Thread(() -> {
            try {
                int sampleRate = 44100;
                int durMs = 680;
                int numSamples = (sampleRate * durMs) / 1000;
                short[] buffer = new short[numSamples];

                java.util.Random rng = new java.util.Random(777);
                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / sampleRate;
                    double val = 0;

                    if (t < 0.085) {
                        double envCha = Math.exp(-t * 42.0);
                        double noise = (rng.nextDouble() * 2.0 - 1.0) * 0.45;
                        double lever = 0.40 * Math.sin(2.0 * Math.PI * 1480.0 * t)
                                + 0.35 * Math.sin(2.0 * Math.PI * 2960.0 * t);
                        val = (noise + lever) * envCha;
                    } else if (t >= 0.085 && t < 0.115) {
                        double tc = t - 0.085;
                        val = 0.65 * Math.sin(2.0 * Math.PI * 3520.0 * tc) * Math.exp(-tc * 55.0);
                    } else {
                        double tb = t - 0.115;
                        double envBell = Math.exp(-tb * 5.8);
                        double envShimmer = Math.exp(-tb * 14.0);
                        val = 0.52 * Math.sin(2.0 * Math.PI * 2637.0 * tb) * envBell
                                + 0.28 * Math.sin(2.0 * Math.PI * 5274.0 * tb) * envBell
                                + 0.14 * Math.sin(2.0 * Math.PI * 6380.0 * tb) * envShimmer
                                + 0.10 * Math.sin(2.0 * Math.PI * 7911.0 * tb) * envShimmer;
                    }

                    int pcm = (int) (val * 29500);
                    if (pcm > 32767) pcm = 32767;
                    if (pcm < -32768) pcm = -32768;
                    buffer[i] = (short) pcm;
                }

                AudioTrack track;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    track = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                            .setBufferSizeInBytes(buffer.length * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build();
                } else {
                    track = new AudioTrack(AudioManager.STREAM_ALARM, sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                            buffer.length * 2, AudioTrack.MODE_STATIC);
                }
                track.write(buffer, 0, buffer.length);
                track.play();
                Thread.sleep(durMs + 60);
                track.release();
            } catch (Exception ignored) {}
        }).start();
    }

    // =========================================================================================
    // GENERADOR DE IMÁGENES DE VISTA PREVIA DE TEMAS (OSCURO | AZUL ZAFIRO | CLARO)
    // =========================================================================================
    private Bitmap crearImagenPreviewTema(int tipoTema, boolean seleccionado) {
        int w = dp(92);
        int h = dp(86);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        String bgHex = (tipoTema == 0) ? "#121318" : (tipoTema == 1 ? "#0A1936" : "#F1F5F9");
        String cardHex = (tipoTema == 0) ? "#1E2028" : (tipoTema == 1 ? "#152C5B" : "#FFFFFF");
        String bar1Hex = (tipoTema == 0) ? "#E4E4E7" : (tipoTema == 1 ? "#E2E8F0" : "#334155");
        String pillGreen = (tipoTema == 0) ? "#34D399" : (tipoTema == 1 ? "#38BDF8" : "#0284C7");
        String pillAccent = (tipoTema == 0) ? "#818CF8" : (tipoTema == 1 ? "#6366F1" : "#94A3B8");

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor(bgHex));
        c.drawRoundRect(new RectF(0, 0, w, h), dp(12), dp(12), p);

        if (tipoTema == 1) {
            Paint pGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            RadialGradient rg = new RadialGradient(w * 0.75f, h * 0.25f, w * 0.6f,
                    new int[]{Color.parseColor("#4438BDF8"), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
            pGlow.setShader(rg);
            c.drawRoundRect(new RectF(0, 0, w, h), dp(12), dp(12), pGlow);

            p.setColor(Color.parseColor("#93C5FD"));
            c.drawCircle(w * 0.80f, h * 0.20f, dp(1), p);
            c.drawCircle(w * 0.65f, h * 0.14f, dp(1), p);
            c.drawCircle(w * 0.85f, h * 0.48f, dp(1), p);
            c.drawCircle(w * 0.72f, h * 0.65f, dp(1), p);
        }

        p.setColor(Color.parseColor(cardHex));
        c.drawRoundRect(new RectF(dp(8), dp(10), w - dp(8), h - dp(18)), dp(8), dp(8), p);

        p.setColor(Color.parseColor(pillAccent));
        c.drawCircle(dp(15), dp(19), dp(3), p);
        p.setColor(Color.parseColor(bar1Hex));
        c.drawRoundRect(new RectF(dp(22), dp(16), w * 0.62f, dp(22)), dp(3), dp(3), p);

        p.setColor(Color.parseColor(pillGreen));
        c.drawRoundRect(new RectF(dp(13), dp(29), w * 0.56f, dp(36)), dp(3), dp(3), p);

        p.setColor(Color.parseColor(bar1Hex));
        p.setAlpha(180);
        c.drawRoundRect(new RectF(dp(13), dp(42), w * 0.50f, dp(47)), dp(2), dp(2), p);
        p.setAlpha(255);

        p.setColor(Color.parseColor(pillAccent));
        c.drawCircle(dp(15), dp(56), dp(2), p);
        c.drawRoundRect(new RectF(dp(20), dp(53), w * 0.54f, dp(59)), dp(3), dp(3), p);

        for (int i = 0; i < 4; i++) {
            p.setColor(i == 0 ? Color.parseColor(pillGreen) : Color.parseColor(bar1Hex));
            p.setAlpha(i == 0 ? 255 : 100);
            float cx = dp(20) + i * dp(16);
            c.drawCircle(cx, h - dp(9), dp(2), p);
        }
        p.setAlpha(255);

        if (seleccionado) {
            float rChk = dp(8);
            float cx = w - dp(12);
            float cy = dp(12);
            p.setColor(Color.parseColor(tipoTema == 1 ? "#38BDF8" : "#6366F1"));
            c.drawCircle(cx, cy, rChk, p);

            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(dp(2));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            Path chk = new Path();
            chk.moveTo(cx - dp(3), cy);
            chk.lineTo(cx - dp(1), cy + dp(3));
            chk.lineTo(cx + dp(4), cy - dp(3));
            c.drawPath(chk, p);
        }

        return bmp;
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
        float st = Math.max(1.8f, s * 0.11f);
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
            case "SETTINGS": {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(s * 0.50f, s * 0.50f, s * 0.16f, p);
                c.drawCircle(s * 0.50f, s * 0.50f, s * 0.32f, p);
                for (int a = 0; a < 360; a += 45) {
                    double rad = Math.toRadians(a);
                    float x1 = s * 0.50f + (float) Math.cos(rad) * (s * 0.28f);
                    float y1 = s * 0.50f + (float) Math.sin(rad) * (s * 0.28f);
                    float x2 = s * 0.50f + (float) Math.cos(rad) * (s * 0.40f);
                    float y2 = s * 0.50f + (float) Math.sin(rad) * (s * 0.40f);
                    c.drawLine(x1, y1, x2, y2, p);
                }
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
            case "BACK": {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(s * 0.78f, s * 0.50f, s * 0.22f, s * 0.50f, p);
                c.drawLine(s * 0.46f, s * 0.26f, s * 0.22f, s * 0.50f, p);
                c.drawLine(s * 0.46f, s * 0.74f, s * 0.22f, s * 0.50f, p);
                break;
            }
            case "RELOAD": {
                p.setStyle(Paint.Style.STROKE);
                RectF arcR = new RectF(s * 0.20f, s * 0.20f, s * 0.80f, s * 0.80f);
                c.drawArc(arcR, 35f, 290f, false, p);
                c.drawLine(s * 0.78f, s * 0.18f, s * 0.78f, s * 0.42f, p);
                c.drawLine(s * 0.56f, s * 0.38f, s * 0.78f, s * 0.42f, p);
                break;
            }
            case "LOCK": {
                p.setStyle(Paint.Style.STROKE);
                RectF body = new RectF(s * 0.24f, s * 0.46f, s * 0.76f, s * 0.84f);
                c.drawRoundRect(body, s * 0.08f, s * 0.08f, p);
                RectF shackle = new RectF(s * 0.33f, s * 0.20f, s * 0.67f, s * 0.54f);
                c.drawArc(shackle, 180f, 180f, false, p);
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
    // CONSTRUCCIÓN DE LA INTERFAZ COMPLETA (CUENTAS | HISTORIAL + COMPRAS | AJUSTES)
    // =========================================================================================
    @SuppressLint("SetJavaScriptEnabled")
    private void construirInterfazCompleta() {
        frameRoot = new FrameLayout(this);
        frameRoot.setBackgroundColor(Color.parseColor("#0F1115"));
        frameRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        mainVertical = new LinearLayout(this);
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

        lblTituloApp = new TextView(this);
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

        // 2. VISTA 1: "CUENTAS" (DESPLAZABLE)
        vistaCuentasScroll = new ScrollView(this);
        vistaCuentasScroll.setFillViewport(true);
        vistaCuentasScroll.setVerticalScrollBarEnabled(false);

        LinearLayout contenidoScrollCuentas = new LinearLayout(this);
        contenidoScrollCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.setPadding(dp(14), dp(4), dp(14), dp(95));

        LinearLayout filaHeaderCuentas = new LinearLayout(this);
        filaHeaderCuentas.setOrientation(LinearLayout.HORIZONTAL);
        filaHeaderCuentas.setGravity(Gravity.CENTER_VERTICAL);
        filaHeaderCuentas.setPadding(0, dp(4), 0, dp(6));

        LinearLayout colTitCuentas = new LinearLayout(this);
        colTitCuentas.setOrientation(LinearLayout.VERTICAL);
        lblCuentasBig = crearTexto("Cuentas", "#F4F4F5", 21f, true);
        lblCuentasSub = crearTexto("Toca cualquier cuenta para abrir su navegador y consola en vivo.", "#A1A1AA", 11f, false);
        colTitCuentas.addView(lblCuentasBig);
        colTitCuentas.addView(lblCuentasSub);
        filaHeaderCuentas.addView(colTitCuentas, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNuevaCuenta = crearBotonConIcono("PLUS", "Nueva", "#0284C7", "#FFFFFF", 12, () -> mostrarPopupEditorCuenta(-1));
        btnNuevaCuenta.setPadding(dp(10), 0, dp(10), 0);
        filaHeaderCuentas.addView(btnNuevaCuenta, new LinearLayout.LayoutParams(dp(88), dp(34)));
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
        cardTiempos = new LinearLayout(this);
        cardTiempos.setOrientation(LinearLayout.VERTICAL);
        cardTiempos.setPadding(dp(12), dp(10), dp(12), dp(10));
        cardTiempos.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 12, 1));
        LinearLayout.LayoutParams lpCardT = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCardT.setMargins(0, 0, 0, dp(10));

        LinearLayout filaRef = new LinearLayout(this);
        filaRef.setGravity(Gravity.CENTER_VERTICAL);
        lblTRef = crearTexto("Frecuencia Refresco:", "#F4F4F5", 11.5f, true);
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
        filaRef.addView(lblTRef, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRef.addView(sldRef, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRef.addView(lblRefrescoVal, new LinearLayout.LayoutParams(dp(75), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRef);

        LinearLayout filaRel = new LinearLayout(this);
        filaRel.setGravity(Gravity.CENTER_VERTICAL);
        filaRel.setPadding(0, dp(6), 0, 0);
        lblTRel = crearTexto("Reinicio Sesión:", "#F4F4F5", 11.5f, true);
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
        filaRel.addView(lblTRel, new LinearLayout.LayoutParams(dp(125), ViewGroup.LayoutParams.WRAP_CONTENT));
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

        btnIniciarTodas = crearBotonConIcono("PLAY", "Iniciar todas", "#16A34A", "#FFFFFF", 12, this::iniciarTodasLasCuentas);
        btnDetenerTodas = crearBotonConIcono("STOP", "Detener todas", "#7F1D1D", "#FCA5A5", 12, this::detenerTodasLasCuentas);
        btnDetenerTodas.setEnabled(false);

        LinearLayout.LayoutParams lpBtnG = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lpBtnG.setMargins(dp(3), 0, dp(3), 0);
        filaBotonesGlobales.addView(btnIniciarTodas, lpBtnG);
        filaBotonesGlobales.addView(btnDetenerTodas, lpBtnG);
        contenidoScrollCuentas.addView(filaBotonesGlobales, lpFilaGlob);

        contenedorTarjetasCuentas = new LinearLayout(this);
        contenedorTarjetasCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.addView(contenedorTarjetasCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        vistaCuentasScroll.addView(contenidoScrollCuentas);
        mainVertical.addView(vistaCuentasScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3. VISTA 2: "HISTORIAL" + SECCIÓN DE "COMPRAS EXITOSAS CON TASA BCV Y CÓDIGO" ABAJO
        vistaHistorialScroll = new ScrollView(this);
        vistaHistorialScroll.setFillViewport(true);
        vistaHistorialScroll.setVerticalScrollBarEnabled(false);
        vistaHistorialScroll.setVisibility(View.GONE);

        vistaHistorialContainer = new LinearLayout(this);
        vistaHistorialContainer.setOrientation(LinearLayout.VERTICAL);
        vistaHistorialContainer.setPadding(dp(14), dp(6), dp(14), dp(95));

        LinearLayout headerHist = new LinearLayout(this);
        headerHist.setOrientation(LinearLayout.HORIZONTAL);
        headerHist.setGravity(Gravity.CENTER_VERTICAL);
        headerHist.setPadding(0, 0, 0, dp(8));

        LinearLayout colTitHist = new LinearLayout(this);
        colTitHist.setOrientation(LinearLayout.VERTICAL);
        lblHistBig = crearTexto("Historial de Consola", "#F4F4F5", 20f, true);
        lblHistSub = crearTexto("Actividad en tiempo real y registro de compras aprobadas", "#A1A1AA", 11f, false);
        colTitHist.addView(lblHistBig);
        colTitHist.addView(lblHistSub);
        headerHist.addView(colTitHist, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnLimpiarHist = crearBoton("Limpiar", "#27272A", "#E4E4E7", 11, () -> {
            if (filtroHistorialSlot == -1) {
                historialGlobalLogs.clear();
                for (int i = 0; i < MAX_CUENTAS; i++) {
                    if (sesiones[i].txtConsolaCard != null) {
                        sesiones[i].txtConsolaCard.setText("");
                    }
                }
            } else {
                for (int i = historialGlobalLogs.size() - 1; i >= 0; i--) {
                    if (historialGlobalLogs.get(i).slotIndex == filtroHistorialSlot) {
                        historialGlobalLogs.remove(i);
                    }
                }
                if (filtroHistorialSlot >= 0 && filtroHistorialSlot < MAX_CUENTAS) {
                    if (sesiones[filtroHistorialSlot].txtConsolaCard != null) {
                        sesiones[filtroHistorialSlot].txtConsolaCard.setText("");
                    }
                }
            }
            refrescarVistaHistorial();
        });
        headerHist.addView(btnLimpiarHist, new LinearLayout.LayoutParams(dp(72), dp(30)));
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
        scrollHistorialConsola.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        txtHistorialConsola = new TextView(this);
        txtHistorialConsola.setTextColor(Color.parseColor("#E4E4E7"));
        txtHistorialConsola.setTextSize(11f);
        txtHistorialConsola.setTypeface(Typeface.MONOSPACE);
        scrollHistorialConsola.addView(txtHistorialConsola);

        LinearLayout.LayoutParams lpScrollConsolaHist = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(290));
        lpScrollConsolaHist.setMargins(0, 0, 0, dp(16));
        vistaHistorialContainer.addView(scrollHistorialConsola, lpScrollConsolaHist);

        lblTituloComprasExitosas = crearTexto("Compras Exitosas y Comprobantes", "#4ADE80", 16.5f, true);
        lblSubComprasExitosas = crearTexto("Registro automático con Tasa BCV y Código de Operación de la tabla.", "#A1A1AA", 11f, false);
        lblSubComprasExitosas.setPadding(0, dp(2), 0, dp(10));
        vistaHistorialContainer.addView(lblTituloComprasExitosas);
        vistaHistorialContainer.addView(lblSubComprasExitosas);

        contenedorComprasExitosas = new LinearLayout(this);
        contenedorComprasExitosas.setOrientation(LinearLayout.VERTICAL);
        vistaHistorialContainer.addView(contenedorComprasExitosas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        vistaHistorialScroll.addView(vistaHistorialContainer);
        mainVertical.addView(vistaHistorialScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 4. VISTA 3: "AJUSTES"
        vistaAjustesScroll = new ScrollView(this);
        vistaAjustesScroll.setFillViewport(true);
        vistaAjustesScroll.setVerticalScrollBarEnabled(false);
        vistaAjustesScroll.setVisibility(View.GONE);

        LinearLayout contenidoAjustes = new LinearLayout(this);
        contenidoAjustes.setOrientation(LinearLayout.VERTICAL);
        contenidoAjustes.setPadding(dp(14), dp(6), dp(14), dp(95));

        lblAjustesBig = crearTexto("Ajustes", "#F4F4F5", 21f, true);
        lblAjustesSub = crearTexto("Personaliza la apariencia, modo turbo, proxy, alertas y vibración.", "#A1A1AA", 11f, false);
        lblAjustesSub.setPadding(0, dp(2), 0, dp(12));
        contenidoAjustes.addView(lblAjustesBig);
        contenidoAjustes.addView(lblAjustesSub);

        cardTemaAjustes = new LinearLayout(this);
        cardTemaAjustes.setOrientation(LinearLayout.VERTICAL);
        cardTemaAjustes.setPadding(dp(14), dp(14), dp(14), dp(14));
        cardTemaAjustes.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 16, 1));
        LinearLayout.LayoutParams lpCardAj = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCardAj.setMargins(0, 0, 0, dp(14));

        lblSubTema = crearTexto("Apariencia", "#F4F4F5", 15f, true);
        lblDescTema = crearTexto("Oscuro es el predeterminado; Azul Zafiro resalta la identidad del bot.", "#A1A1AA", 11f, false);
        lblDescTema.setPadding(0, dp(2), 0, dp(12));
        cardTemaAjustes.addView(lblSubTema);
        cardTemaAjustes.addView(lblDescTema);

        LinearLayout filaTarjetasTema = new LinearLayout(this);
        filaTarjetasTema.setOrientation(LinearLayout.HORIZONTAL);

        cardPrevOscuro = new LinearLayout(this);
        cardPrevOscuro.setOrientation(LinearLayout.VERTICAL);
        cardPrevOscuro.setGravity(Gravity.CENTER_HORIZONTAL);
        cardPrevOscuro.setPadding(dp(6), dp(8), dp(6), dp(8));
        imgPrevOscuro = new ImageView(this);
        imgPrevOscuro.setScaleType(ImageView.ScaleType.FIT_CENTER);
        txtPrevOscuro = crearTexto("Oscuro", "#F4F4F5", 11.5f, true);
        txtPrevOscuro.setGravity(Gravity.CENTER);
        txtPrevOscuro.setPadding(0, dp(6), 0, 0);
        cardPrevOscuro.addView(imgPrevOscuro, new LinearLayout.LayoutParams(dp(86), dp(80)));
        cardPrevOscuro.addView(txtPrevOscuro);
        cardPrevOscuro.setOnClickListener(v -> cambiarTema(0));

        cardPrevZafiro = new LinearLayout(this);
        cardPrevZafiro.setOrientation(LinearLayout.VERTICAL);
        cardPrevZafiro.setGravity(Gravity.CENTER_HORIZONTAL);
        cardPrevZafiro.setPadding(dp(6), dp(8), dp(6), dp(8));
        imgPrevZafiro = new ImageView(this);
        imgPrevZafiro.setScaleType(ImageView.ScaleType.FIT_CENTER);
        txtPrevZafiro = crearTexto("Azul Zafiro", "#F4F4F5", 11.5f, true);
        txtPrevZafiro.setGravity(Gravity.CENTER);
        txtPrevZafiro.setPadding(0, dp(6), 0, 0);
        cardPrevZafiro.addView(imgPrevZafiro, new LinearLayout.LayoutParams(dp(86), dp(80)));
        cardPrevZafiro.addView(txtPrevZafiro);
        cardPrevZafiro.setOnClickListener(v -> cambiarTema(1));

        cardPrevClaro = new LinearLayout(this);
        cardPrevClaro.setOrientation(LinearLayout.VERTICAL);
        cardPrevClaro.setGravity(Gravity.CENTER_HORIZONTAL);
        cardPrevClaro.setPadding(dp(6), dp(8), dp(6), dp(8));
        imgPrevClaro = new ImageView(this);
        imgPrevClaro.setScaleType(ImageView.ScaleType.FIT_CENTER);
        txtPrevClaro = crearTexto("Claro", "#F4F4F5", 11.5f, true);
        txtPrevClaro.setGravity(Gravity.CENTER);
        txtPrevClaro.setPadding(0, dp(6), 0, 0);
        cardPrevClaro.addView(imgPrevClaro, new LinearLayout.LayoutParams(dp(86), dp(80)));
        cardPrevClaro.addView(txtPrevClaro);
        cardPrevClaro.setOnClickListener(v -> cambiarTema(2));

        LinearLayout.LayoutParams lpPrevCard = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpPrevCard.setMargins(dp(4), 0, dp(4), 0);
        filaTarjetasTema.addView(cardPrevOscuro, lpPrevCard);
        filaTarjetasTema.addView(cardPrevZafiro, lpPrevCard);
        filaTarjetasTema.addView(cardPrevClaro, lpPrevCard);

        cardTemaAjustes.addView(filaTarjetasTema);
        contenidoAjustes.addView(cardTemaAjustes, lpCardAj);

        cardAlertasAjustes = new LinearLayout(this);
        cardAlertasAjustes.setOrientation(LinearLayout.VERTICAL);
        cardAlertasAjustes.setPadding(dp(14), dp(14), dp(14), dp(14));
        cardAlertasAjustes.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 16, 1));

        lblSubAlertas = crearTexto("Rendimiento, Proxy, Alertas y Vibración", "#F4F4F5", 15f, true);
        lblDescAlertas = crearTexto("Opciones de aceleración, cambio automático de IP y avisos sonoros.", "#A1A1AA", 11f, false);
        lblDescAlertas.setPadding(0, dp(2), 0, dp(12));
        cardAlertasAjustes.addView(lblSubAlertas);
        cardAlertasAjustes.addView(lblDescAlertas);

        LinearLayout.LayoutParams lpSubPanel = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSubPanel.setMargins(0, 0, 0, dp(10));

        // Panel Separado 0: Modo Turbo sin Imágenes (Activado por defecto y con confirmación antes de desactivar)
        panelItemTurbo = new LinearLayout(this);
        panelItemTurbo.setOrientation(LinearLayout.HORIZONTAL);
        panelItemTurbo.setGravity(Gravity.CENTER_VERTICAL);
        panelItemTurbo.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtTurbo = new LinearLayout(this);
        colTxtTurbo.setOrientation(LinearLayout.VERTICAL);
        lblItemTurbo = crearTexto("Modo Turbo sin Imágenes", "#F4F4F5", 13.5f, true);
        lblDescItemTurbo = crearTexto("Oculta banners e imágenes pesadas al vigilar y comprar para máxima velocidad y ahorro de datos.", "#A1A1AA", 10.5f, false);
        lblDescItemTurbo.setPadding(0, dp(2), dp(8), 0);
        colTxtTurbo.addView(lblItemTurbo);
        colTxtTurbo.addView(lblDescItemTurbo);
        btnToggleTurbo = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            if (modoTurboSinImagenes) {
                new AlertDialog.Builder(this)
                        .setTitle("¿Desactivar Modo Turbo?")
                        .setMessage("¿Estás seguro de desactivar el Modo Turbo?\n\nSi lo desactivas, la página del banco descargará todas las imágenes pesadas y cargará más lento durante la intervención.")
                        .setPositiveButton("Desactivar", (d, w) -> {
                            modoTurboSinImagenes = false;
                            prefs.edit().putBoolean("modo_turbo_default_on", false).apply();
                            aplicarModoTurboEnWebViews();
                            actualizarBotonesAjustes();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            } else {
                modoTurboSinImagenes = true;
                prefs.edit().putBoolean("modo_turbo_default_on", true).apply();
                aplicarModoTurboEnWebViews();
                actualizarBotonesAjustes();
            }
        });
        panelItemTurbo.addView(colTxtTurbo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemTurbo.addView(btnToggleTurbo, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemTurbo, lpSubPanel);

        // Panel Separado 0.5: Rotación Automática de IP (Proxy Venezuela Integrado)
        panelItemProxy = new LinearLayout(this);
        panelItemProxy.setOrientation(LinearLayout.HORIZONTAL);
        panelItemProxy.setGravity(Gravity.CENTER_VERTICAL);
        panelItemProxy.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtProxy = new LinearLayout(this);
        colTxtProxy.setOrientation(LinearLayout.VERTICAL);
        lblItemProxy = crearTexto("Rotación Automática de IP (Proxy VE)", "#F4F4F5", 13.5f, true);
        lblDescItemProxy = crearTexto("Asigna IPs residenciales de Venezuela y cambia de IP en /lg si el banco restringe la sesión.", "#A1A1AA", 10.5f, false);
        lblDescItemProxy.setPadding(0, dp(2), dp(8), 0);
        colTxtProxy.addView(lblItemProxy);
        colTxtProxy.addView(lblDescItemProxy);
        btnToggleProxy = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            proxyRotativoActivo = !proxyRotativoActivo;
            prefs.edit().putBoolean("proxy_iproyal_on", proxyRotativoActivo).apply();
            aplicarConfiguracionProxyGlobal();
            actualizarBotonesAjustes();
            for (SesionBot s : obtenerSesionesRegistradas()) {
                actualizarVisualTarjeta(s);
            }
        });
        panelItemProxy.addView(colTxtProxy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemProxy.addView(btnToggleProxy, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemProxy, lpSubPanel);

        // Panel Separado 1: Alerta de Ordinaria
        panelItemOrd = new LinearLayout(this);
        panelItemOrd.setOrientation(LinearLayout.HORIZONTAL);
        panelItemOrd.setGravity(Gravity.CENTER_VERTICAL);
        panelItemOrd.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtOrd = new LinearLayout(this);
        colTxtOrd.setOrientation(LinearLayout.VERTICAL);
        lblItemOrd = crearTexto("Alerta de Ordinaria", "#F4F4F5", 13.5f, true);
        lblDescItemOrd = crearTexto("Sonido de aviso cuando aparece el botón de Ordinaria.", "#A1A1AA", 10.5f, false);
        lblDescItemOrd.setPadding(0, dp(2), dp(8), 0);
        colTxtOrd.addView(lblItemOrd);
        colTxtOrd.addView(lblDescItemOrd);
        btnToggleOrd = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            alertaOrdinariaActiva = !alertaOrdinariaActiva;
            prefs.edit().putBoolean("alerta_ord", alertaOrdinariaActiva).apply();
            actualizarBotonesAjustes();
            if (alertaOrdinariaActiva) sonarAlertaOrdinaria();
        });
        panelItemOrd.addView(colTxtOrd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemOrd.addView(btnToggleOrd, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemOrd, lpSubPanel);

        // Panel Separado 2: Alerta Compra Exitosa
        panelItemCompra = new LinearLayout(this);
        panelItemCompra.setOrientation(LinearLayout.HORIZONTAL);
        panelItemCompra.setGravity(Gravity.CENTER_VERTICAL);
        panelItemCompra.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtCompra = new LinearLayout(this);
        colTxtCompra.setOrientation(LinearLayout.VERTICAL);
        lblItemCompra = crearTexto("Alerta Compra Exitosa", "#F4F4F5", 13.5f, true);
        lblDescItemCompra = crearTexto("Sonido de caja registradora al completarse una orden.", "#A1A1AA", 10.5f, false);
        lblDescItemCompra.setPadding(0, dp(2), dp(8), 0);
        colTxtCompra.addView(lblItemCompra);
        colTxtCompra.addView(lblDescItemCompra);
        btnToggleCompra = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            alertaCompraActiva = !alertaCompraActiva;
            prefs.edit().putBoolean("alerta_compra", alertaCompraActiva).apply();
            actualizarBotonesAjustes();
            if (alertaCompraActiva) sonarAlertaCompraExitosa();
        });
        panelItemCompra.addView(colTxtCompra, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemCompra.addView(btnToggleCompra, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemCompra, lpSubPanel);

        // Panel Separado 3: Vibración del Dispositivo
        panelItemVib = new LinearLayout(this);
        panelItemVib.setOrientation(LinearLayout.HORIZONTAL);
        panelItemVib.setGravity(Gravity.CENTER_VERTICAL);
        panelItemVib.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtVib = new LinearLayout(this);
        colTxtVib.setOrientation(LinearLayout.VERTICAL);
        lblItemVib = crearTexto("Vibración del Dispositivo", "#F4F4F5", 13.5f, true);
        lblDescItemVib = crearTexto("Vibración breve junto a los avisos de Ordinaria y compra.", "#A1A1AA", 10.5f, false);
        lblDescItemVib.setPadding(0, dp(2), dp(8), 0);
        colTxtVib.addView(lblItemVib);
        colTxtVib.addView(lblDescItemVib);
        btnToggleVib = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            vibracionActiva = !vibracionActiva;
            prefs.edit().putBoolean("vibracion_app", vibracionActiva).apply();
            actualizarBotonesAjustes();
            if (vibracionActiva) vibrarDispositivo(false);
        });
        panelItemVib.addView(colTxtVib, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemVib.addView(btnToggleVib, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemVib);

        contenidoAjustes.addView(cardAlertasAjustes, lpCardAj);
        vistaAjustesScroll.addView(contenidoAjustes);
        mainVertical.addView(vistaAjustesScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        frameRoot.addView(mainVertical);

        // 5. BARRA FLOTANTE INFERIOR COMPACTA ("Cuentas" | "Historial" | "Ajustes")
        barraFlotante = new LinearLayout(this);
        barraFlotante.setOrientation(LinearLayout.HORIZONTAL);
        barraFlotante.setGravity(Gravity.CENTER);
        barraFlotante.setPadding(dp(5), dp(4), dp(5), dp(4));
        barraFlotante.setBackground(fondoRedondeadoConBorde("#18181B", "#2E323B", 24, 1));
        barraFlotante.setVisibility(View.GONE);

        navBtnCuentas = new LinearLayout(this);
        navBtnCuentas.setOrientation(LinearLayout.VERTICAL);
        navBtnCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.setPadding(dp(4), dp(3), dp(4), dp(3));
        navIconCuentas = new ImageView(this);
        navTxtCuentas = crearTexto("Cuentas", "#FFFFFF", 10f, true);
        navTxtCuentas.setSingleLine(true);
        navTxtCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.addView(navIconCuentas, new LinearLayout.LayoutParams(dp(16), dp(16)));
        navBtnCuentas.addView(navTxtCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnCuentas.setOnClickListener(v -> cambiarPestanaInferior(0));

        navBtnHistorial = new LinearLayout(this);
        navBtnHistorial.setOrientation(LinearLayout.VERTICAL);
        navBtnHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.setPadding(dp(4), dp(3), dp(4), dp(3));
        navIconHistorial = new ImageView(this);
        navTxtHistorial = crearTexto("Historial", "#A1A1AA", 10f, true);
        navTxtHistorial.setSingleLine(true);
        navTxtHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.addView(navIconHistorial, new LinearLayout.LayoutParams(dp(16), dp(16)));
        navBtnHistorial.addView(navTxtHistorial, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnHistorial.setOnClickListener(v -> cambiarPestanaInferior(1));

        navBtnAjustes = new LinearLayout(this);
        navBtnAjustes.setOrientation(LinearLayout.VERTICAL);
        navBtnAjustes.setGravity(Gravity.CENTER);
        navBtnAjustes.setPadding(dp(4), dp(3), dp(4), dp(3));
        navIconAjustes = new ImageView(this);
        navTxtAjustes = crearTexto("Ajustes", "#A1A1AA", 10f, true);
        navTxtAjustes.setSingleLine(true);
        navTxtAjustes.setGravity(Gravity.CENTER);
        navBtnAjustes.addView(navIconAjustes, new LinearLayout.LayoutParams(dp(16), dp(16)));
        navBtnAjustes.addView(navTxtAjustes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnAjustes.setOnClickListener(v -> cambiarPestanaInferior(2));

        barraFlotante.addView(navBtnCuentas, new LinearLayout.LayoutParams(dp(84), dp(40)));
        View sep1 = new View(this);
        barraFlotante.addView(sep1, new LinearLayout.LayoutParams(dp(4), dp(1)));
        barraFlotante.addView(navBtnHistorial, new LinearLayout.LayoutParams(dp(84), dp(40)));
        View sep2 = new View(this);
        barraFlotante.addView(sep2, new LinearLayout.LayoutParams(dp(4), dp(1)));
        barraFlotante.addView(navBtnAjustes, new LinearLayout.LayoutParams(dp(84), dp(40)));

        FrameLayout.LayoutParams lpFloating = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpFloating.setMargins(0, 0, 0, dp(12));
        frameRoot.addView(barraFlotante, lpFloating);

        // 5.5 VENTANA DE LOGIN DE LICENCIA DEL CLIENTE (CENTRADA Y ESTILIZADA)
        loginClienteOverlay = construirOverlayLoginCliente();
        frameRoot.addView(loginClienteOverlay);

        // 6. PANTALLA DE ENTRADA EN GRIS OSCURO (#18181B)
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
                        if (loginClienteOverlay.getVisibility() != View.VISIBLE) {
                            barraFlotante.setVisibility(View.VISIBLE);
                        }
                    })
                    .start();
        }, 3000);
    }

    private void cambiarTema(int nuevoTema) {
        temaActual = nuevoTema;
        prefs.edit().putInt("tema_app_v2", nuevoTema).apply();
        aplicarTemaVisual();
    }

    private void aplicarModoTurboEnWebViews() {
        for (int i = 0; i < MAX_CUENTAS; i++) {
            SesionBot s = sesiones[i];
            if (s != null && s.webViewOculto != null) {
                try {
                    WebSettings ws = s.webViewOculto.getSettings();
                    ws.setBlockNetworkImage(modoTurboSinImagenes);
                    ws.setLoadsImagesAutomatically(!modoTurboSinImagenes);
                    if (modoTurboSinImagenes) {
                        evaluarJS(s,
                                "var st = document.getElementById('__vbt_turbo_css');" +
                                "if(!st){ st = document.createElement('style'); st.id='__vbt_turbo_css'; if(document.head) document.head.appendChild(st); }" +
                                "if(st) st.innerHTML = 'img, svg:not(#fb), .carousel, .banner { opacity:0 !important; pointer-events:none !important; }';", null);
                    } else {
                        evaluarJS(s,
                                "var st = document.getElementById('__vbt_turbo_css'); if(st) st.remove();", null);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void aplicarTemaVisual() {
        String bgApp = colFondoApp();
        String bgCard = colFondoTarjeta();
        String bgSub = colFondoSubPanel();
        String borderCard = colBordeTarjeta();
        String txtPri = colTextoPrimario();
        String txtSec = colTextoSecundario();
        String acento = colAcentoTema();

        getWindow().setStatusBarColor(Color.parseColor(bgApp));
        getWindow().setNavigationBarColor(Color.parseColor(bgApp));
        if (frameRoot != null) frameRoot.setBackgroundColor(Color.parseColor(bgApp));
        if (mainVertical != null) mainVertical.setBackgroundColor(Color.parseColor(bgApp));

        if (lblTituloApp != null) lblTituloApp.setTextColor(Color.parseColor(txtPri));
        if (lblCuentasBig != null) lblCuentasBig.setTextColor(Color.parseColor(txtPri));
        if (lblCuentasSub != null) lblCuentasSub.setTextColor(Color.parseColor(txtSec));
        if (lblHistBig != null) lblHistBig.setTextColor(Color.parseColor(txtPri));
        if (lblHistSub != null) lblHistSub.setTextColor(Color.parseColor(txtSec));
        if (lblAjustesBig != null) lblAjustesBig.setTextColor(Color.parseColor(txtPri));
        if (lblAjustesSub != null) lblAjustesSub.setTextColor(Color.parseColor(txtSec));

        if (lblTituloComprasExitosas != null) {
            lblTituloComprasExitosas.setTextColor(Color.parseColor(temaActual == 2 ? "#15803D" : "#4ADE80"));
        }
        if (lblSubComprasExitosas != null) {
            lblSubComprasExitosas.setTextColor(Color.parseColor(txtSec));
        }

        if (lblTRef != null) lblTRef.setTextColor(Color.parseColor(txtPri));
        if (lblTRel != null) lblTRel.setTextColor(Color.parseColor(txtPri));
        if (lblRefrescoVal != null) lblRefrescoVal.setTextColor(Color.parseColor(txtPri));
        if (lblReloginVal != null) lblReloginVal.setTextColor(Color.parseColor(txtPri));

        if (lblSubTema != null) lblSubTema.setTextColor(Color.parseColor(txtPri));
        if (lblDescTema != null) lblDescTema.setTextColor(Color.parseColor(txtSec));
        if (lblSubAlertas != null) lblSubAlertas.setTextColor(Color.parseColor(txtPri));
        if (lblDescAlertas != null) lblDescAlertas.setTextColor(Color.parseColor(txtSec));

        if (lblItemTurbo != null) lblItemTurbo.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemTurbo != null) lblDescItemTurbo.setTextColor(Color.parseColor(txtSec));
        if (lblItemProxy != null) lblItemProxy.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemProxy != null) lblDescItemProxy.setTextColor(Color.parseColor(txtSec));
        if (lblItemOrd != null) lblItemOrd.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemOrd != null) lblDescItemOrd.setTextColor(Color.parseColor(txtSec));
        if (lblItemCompra != null) lblItemCompra.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemCompra != null) lblDescItemCompra.setTextColor(Color.parseColor(txtSec));
        if (lblItemVib != null) lblItemVib.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemVib != null) lblDescItemVib.setTextColor(Color.parseColor(txtSec));

        if (btnNuevaCuenta != null) {
            btnNuevaCuenta.setBackground(fondoRedondeado(acento, 8));
            ponerIconoYTexto(btnNuevaCuenta, crearIconoVectorial("PLUS", "#FFFFFF", 14), "Nueva");
            btnNuevaCuenta.setTextColor(Color.WHITE);
        }

        if (btnLimpiarHist != null) {
            btnLimpiarHist.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 8, 1));
            btnLimpiarHist.setTextColor(Color.parseColor(txtPri));
        }

        if (cardTiempos != null) cardTiempos.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 12, 1));
        if (cardTemaAjustes != null) cardTemaAjustes.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 16, 1));
        if (cardAlertasAjustes != null) cardAlertasAjustes.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 16, 1));

        if (panelItemTurbo != null) panelItemTurbo.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemProxy != null) panelItemProxy.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemOrd != null) panelItemOrd.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemCompra != null) panelItemCompra.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemVib != null) panelItemVib.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));

        if (scrollHistorialConsola != null) {
            scrollHistorialConsola.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 12, 1));
        }
        if (txtHistorialConsola != null) {
            txtHistorialConsola.setTextColor(Color.parseColor(colTextoConsola()));
        }

        if (barraFlotante != null) {
            barraFlotante.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 24, 1));
        }

        actualizarTarjetasPreviewTema();
        actualizarBotonesAjustes();
        actualizarEstiloBarraFlotante();
        actualizarResumenYBotonesGlobales();
        reconstruirListaComprasExitosas();
        if (pestanaInferiorActual == 1) {
            reconstruirFiltrosHistorial();
        }
        for (SesionBot s : obtenerSesionesRegistradas()) {
            actualizarVisualTarjeta(s);
        }
    }

    private void actualizarTarjetasPreviewTema() {
        if (imgPrevOscuro == null) return;
        imgPrevOscuro.setImageBitmap(crearImagenPreviewTema(0, temaActual == 0));
        imgPrevZafiro.setImageBitmap(crearImagenPreviewTema(1, temaActual == 1));
        imgPrevClaro.setImageBitmap(crearImagenPreviewTema(2, temaActual == 2));

        String bgSub = colFondoSubPanel();
        String borderInact = colBordeTarjeta();
        String txtPri = colTextoPrimario();
        String txtSec = colTextoSecundario();

        if (temaActual == 0) {
            cardPrevOscuro.setBackground(fondoRedondeadoConBorde("#1E1B4B", "#6366F1", 14, 2));
            txtPrevOscuro.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            cardPrevOscuro.setBackground(fondoRedondeadoConBorde(bgSub, borderInact, 14, 1));
            txtPrevOscuro.setTextColor(Color.parseColor(txtSec));
        }

        if (temaActual == 1) {
            cardPrevZafiro.setBackground(fondoRedondeadoConBorde("#172554", "#38BDF8", 14, 2));
            txtPrevZafiro.setTextColor(Color.parseColor("#38BDF8"));
        } else {
            cardPrevZafiro.setBackground(fondoRedondeadoConBorde(bgSub, borderInact, 14, 1));
            txtPrevZafiro.setTextColor(Color.parseColor(txtSec));
        }

        if (temaActual == 2) {
            cardPrevClaro.setBackground(fondoRedondeadoConBorde("#E0F2FE", "#0284C7", 14, 2));
            txtPrevClaro.setTextColor(Color.parseColor(txtPri));
        } else {
            cardPrevClaro.setBackground(fondoRedondeadoConBorde(bgSub, borderInact, 14, 1));
            txtPrevClaro.setTextColor(Color.parseColor(txtSec));
        }
    }

    private void actualizarBotonesAjustes() {
        configurarBotonInterruptor(btnToggleTurbo, modoTurboSinImagenes);
        configurarBotonInterruptor(btnToggleProxy, proxyRotativoActivo);
        configurarBotonInterruptor(btnToggleOrd, alertaOrdinariaActiva);
        configurarBotonInterruptor(btnToggleCompra, alertaCompraActiva);
        configurarBotonInterruptor(btnToggleVib, vibracionActiva);
    }

    private void configurarBotonInterruptor(Button b, boolean activo) {
        if (b == null) return;
        if (activo) {
            b.setText("ACTIVADO");
            if (temaActual == 1) {
                b.setTextColor(Color.parseColor("#38BDF8"));
                b.setBackground(fondoRedondeadoConBorde("#1E3A8A", "#38BDF8", 10, 1));
            } else if (temaActual == 2) {
                b.setTextColor(Color.parseColor("#FFFFFF"));
                b.setBackground(fondoRedondeadoConBorde("#0284C7", "#0369A1", 10, 1));
            } else {
                b.setTextColor(Color.parseColor("#4ADE80"));
                b.setBackground(fondoRedondeadoConBorde("#14532D", "#22C55E", 10, 1));
            }
        } else {
            b.setText("DESACTIVADO");
            if (temaActual == 2) {
                b.setTextColor(Color.parseColor("#64748B"));
                b.setBackground(fondoRedondeadoConBorde("#E2E8F0", "#CBD5E1", 10, 1));
            } else {
                b.setTextColor(Color.parseColor("#FCA5A5"));
                b.setBackground(fondoRedondeadoConBorde("#450A0A", "#EF4444", 10, 1));
            }
        }
    }

    private TextView crearChipResumen(String texto, String bgHex, String fgHex) {
        TextView tv = crearTexto(texto, fgHex, 11f, true);
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        tv.setBackground(fondoRedondeado(bgHex, 14));
        return tv;
    }

    private void cambiarPestanaInferior(int nuevaPestana) {
        pestanaInferiorActual = nuevaPestana;
        vistaCuentasScroll.setVisibility(nuevaPestana == 0 ? View.VISIBLE : View.GONE);
        vistaHistorialScroll.setVisibility(nuevaPestana == 1 ? View.VISIBLE : View.GONE);
        vistaAjustesScroll.setVisibility(nuevaPestana == 2 ? View.VISIBLE : View.GONE);

        if (badgeEstadoGlobal != null) {
            badgeEstadoGlobal.setVisibility(nuevaPestana == 0 ? View.VISIBLE : View.GONE);
        }

        if (nuevaPestana == 1) {
            reconstruirFiltrosHistorial();
            refrescarVistaHistorial();
            reconstruirListaComprasExitosas();
        }
        actualizarEstiloBarraFlotante();
    }

    private void actualizarEstiloBarraFlotante() {
        String bgCard = colFondoTarjeta();
        String txtInact = colTextoSecundario();
        String acento = colAcentoTema();

        boolean enCuentas = (pestanaInferiorActual == 0);
        boolean enHist = (pestanaInferiorActual == 1);
        boolean enAjustes = (pestanaInferiorActual == 2);

        navBtnCuentas.setBackground(fondoRedondeado(enCuentas ? acento : bgCard, 18));
        navIconCuentas.setImageDrawable(crearIconoVectorial("USERS", enCuentas ? "#FFFFFF" : txtInact, 16));
        navTxtCuentas.setTextColor(Color.parseColor(enCuentas ? "#FFFFFF" : txtInact));

        navBtnHistorial.setBackground(fondoRedondeado(enHist ? acento : bgCard, 18));
        navIconHistorial.setImageDrawable(crearIconoVectorial("HISTORY", enHist ? "#FFFFFF" : txtInact, 16));
        navTxtHistorial.setTextColor(Color.parseColor(enHist ? "#FFFFFF" : txtInact));

        navBtnAjustes.setBackground(fondoRedondeado(enAjustes ? acento : bgCard, 18));
        navIconAjustes.setImageDrawable(crearIconoVectorial("SETTINGS", enAjustes ? "#FFFFFF" : txtInact, 16));
        navTxtAjustes.setTextColor(Color.parseColor(enAjustes ? "#FFFFFF" : txtInact));
    }

    private void aplicarZoomWebView(final SesionBot s) {
        if (s == null || s.webViewOculto == null) return;
        final int zPct = s.zoomPorcentaje;
        runOnUiThread(() -> {
            try {
                String jsZoom =
                        "var z = '" + (zPct / 100.0) + "';" +
                        "var vp = document.querySelector('meta[name=\"viewport\"]');" +
                        "if (!vp) {" +
                        "  vp = document.createElement('meta');" +
                        "  vp.name = 'viewport';" +
                        "  if (document.head) document.head.appendChild(vp);" +
                        "}" +
                        "if (vp) vp.setAttribute('content', 'width=device-width, initial-scale=1.0, user-scalable=yes');" +
                        "if (document.documentElement) document.documentElement.style.zoom = '1';" +
                        "if (document.body) { document.body.style.zoom = z; }";
                evaluarJS(s, jsZoom, null);
            } catch (Exception ignored) {}
        });
    }

    private void ciclarZoomTarjeta(SesionBot s) {
        if (s == null) return;
        int idxSiguiente = 0;
        for (int i = 0; i < ESCALAS_ZOOM.length; i++) {
            if (ESCALAS_ZOOM[i] == s.zoomPorcentaje) {
                idxSiguiente = (i + 1) % ESCALAS_ZOOM.length;
                break;
            }
        }
        s.zoomPorcentaje = ESCALAS_ZOOM[idxSiguiente];
        prefs.edit().putInt("zoom_web_v2_slot_" + s.slotIndex, s.zoomPorcentaje).apply();
        actualizarBarraUrlTarjeta(s);
        aplicarZoomWebView(s);
    }

    private void sincronizarVistaPanelCuenta(SesionBot s) {
        if (s == null || s.contenedorDesplegableCard == null) return;

        s.contenedorDesplegableCard.setVisibility(s.panelDesplegado ? View.VISIBLE : View.GONE);

        if (s.frameConsolaInterna != null) {
            s.frameConsolaInterna.setVisibility(s.mostrarConsolaEnPanel ? View.VISIBLE : View.GONE);
        }

        if (s.btnToggleConsolaMini != null) {
            s.btnToggleConsolaMini.setText(s.mostrarConsolaEnPanel ? "▲ Ocultar consola" : "▼ Mostrar consola");
        }

        if (s.webViewOculto != null) {
            ViewGroup parentActual = (ViewGroup) s.webViewOculto.getParent();
            FrameLayout destino = s.panelDesplegado ? s.marcoWebViewCard : slotsFondoWebView[s.slotIndex];
            if (destino != null && parentActual != destino) {
                if (parentActual != null) {
                    parentActual.removeView(s.webViewOculto);
                }
                destino.addView(s.webViewOculto, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            if (s.panelDesplegado) {
                aplicarZoomWebView(s);
            }
        }

        if (s.panelDesplegado && s.mostrarConsolaEnPanel && s.scrollConsolaCard != null) {
            s.scrollConsolaCard.post(() -> s.scrollConsolaCard.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void actualizarBarraUrlTarjeta(SesionBot s) {
        if (s == null) return;
        runOnUiThread(() -> {
            if (s.txtUrlCard != null) {
                String u = (s.urlActualTexto == null || s.urlActualTexto.isEmpty()) ? URL_LOGIN : s.urlActualTexto;
                ponerIconoYTexto(s.txtUrlCard, crearIconoVectorial("LOCK", colTextoSecundario(), 11), u);
            }
            if (s.btnZoomUrlCard != null) {
                s.btnZoomUrlCard.setText(s.zoomPorcentaje + "%");
                if (s.cargandoRed && s.progresoWeb < 100) {
                    s.btnZoomUrlCard.setTextColor(Color.parseColor("#38BDF8"));
                    s.btnZoomUrlCard.setBackground(fondoRedondeadoConBorde("#1E293B", "#38BDF8", 8, 1));
                } else {
                    s.btnZoomUrlCard.setTextColor(Color.parseColor("#4ADE80"));
                    s.btnZoomUrlCard.setBackground(fondoRedondeadoConBorde("#14532D", "#22C55E", 8, 1));
                }
            }
        });
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void inicializarMotoresOcultos() {
        for (int i = 0; i < MAX_CUENTAS; i++) {
            final SesionBot sesion = sesiones[i];

            FrameLayout slotFondo = new FrameLayout(this);
            slotsFondoWebView[i] = slotFondo;
            LinearLayout.LayoutParams lpSlotFondo = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i < 2) {
                filaMotoresSup.addView(slotFondo, lpSlotFondo);
            } else {
                filaMotoresInf.addView(slotFondo, lpSlotFondo);
            }

            // 1. Construir Tarjeta de la Cuenta
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 14, 1));

            card.setOnClickListener(v -> {
                sesion.panelDesplegado = !sesion.panelDesplegado;
                sincronizarVistaPanelCuenta(sesion);
            });

            // Fila 1: Usuario + Estado con Ping en ms
            LinearLayout filaTopCard = new LinearLayout(this);
            filaTopCard.setOrientation(LinearLayout.HORIZONTAL);
            filaTopCard.setGravity(Gravity.CENTER_VERTICAL);

            TextView lblUsr = crearTexto("Sin cuenta", "#9CA3AF", 14.5f, true);
            TextView badgeEst = crearTexto("DETENIDO", "#9CA3AF", 9.5f, true);
            badgeEst.setPadding(dp(9), dp(3), dp(9), dp(3));
            badgeEst.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 10, 1));

            filaTopCard.addView(lblUsr, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            filaTopCard.addView(badgeEst);
            card.addView(filaTopCard);

            // Fila 2: Saldos en vivo (Bs y USD) + Monto configurado + Tasa BCV
            TextView lblSaldos = crearTexto("Bs: --   •   USD: --   •   Orden: 300$", "#38BDF8", 11f, true);
            lblSaldos.setPadding(0, dp(4), 0, dp(1));
            card.addView(lblSaldos);

            // Fila 2.5: Indicador de IP Actual y Estado del Proxy en cada tarjeta
            TextView lblPrx = crearTexto(sesion.textoEstadoProxyMostrado(), "#34D399", 10f, true);
            lblPrx.setPadding(0, dp(1), 0, dp(2));
            card.addView(lblPrx);

            // Fila 3: Paso actual en vivo (Siempre visible)
            TextView lblPaso = crearTexto("Sin actividad todavía", "#A1A1AA", 11.5f, false);
            lblPaso.setPadding(0, dp(1), 0, dp(6));
            card.addView(lblPaso);

            // Fila 4: Los 5 botones de acción compactos (30dp x 26dp)
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
                    pausarSesionSinCerrar(sesion, true);
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
                    reconstruirListaComprasExitosas();
                }
            });

            ImageView bEdit = crearBotonAccionTarjeta("EDIT", "#27272A", "#E4E4E7", () -> {
                mostrarPopupEditorCuenta(sesion.slotIndex);
            });

            LinearLayout.LayoutParams lpAct = new LinearLayout.LayoutParams(dp(30), dp(26));
            lpAct.setMargins(dp(4), 0, 0, 0);
            filaAcciones.addView(bPlay, lpAct);
            filaAcciones.addView(bStop, lpAct);
            filaAcciones.addView(bLogoutStop, lpAct);
            filaAcciones.addView(bEye, lpAct);
            filaAcciones.addView(bEdit, lpAct);
            card.addView(filaAcciones);

            // Fila 5: CONTENEDOR DESPLEGABLE (Botoncito ocultar consola + Consola compacta + Navegador con Zoom)
            LinearLayout boxDesplegable = new LinearLayout(this);
            boxDesplegable.setOrientation(LinearLayout.VERTICAL);
            boxDesplegable.setVisibility(View.GONE);
            boxDesplegable.setPadding(0, dp(8), 0, 0);
            boxDesplegable.setClickable(true);

            LinearLayout filaCabeceraPanel = new LinearLayout(this);
            filaCabeceraPanel.setOrientation(LinearLayout.HORIZONTAL);
            filaCabeceraPanel.setGravity(Gravity.CENTER_VERTICAL);
            filaCabeceraPanel.setPadding(0, 0, 0, dp(5));

            TextView lblMiniTit = crearTexto("Vista en vivo", "#94A3B8", 10.5f, true);
            filaCabeceraPanel.addView(lblMiniTit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView btnToggleCon = new TextView(this);
            btnToggleCon.setText(sesion.mostrarConsolaEnPanel ? "▲ Ocultar consola" : "▼ Mostrar consola");
            btnToggleCon.setTextColor(Color.parseColor("#38BDF8"));
            btnToggleCon.setTextSize(10f);
            btnToggleCon.setTypeface(null, Typeface.BOLD);
            btnToggleCon.setPadding(dp(9), dp(3), dp(9), dp(3));
            btnToggleCon.setBackground(fondoRedondeadoConBorde("#1E293B", "#334155", 8, 1));
            btnToggleCon.setOnClickListener(v -> {
                sesion.mostrarConsolaEnPanel = !sesion.mostrarConsolaEnPanel;
                prefs.edit().putBoolean("mostrar_consola_slot_" + sesion.slotIndex, sesion.mostrarConsolaEnPanel).apply();
                sincronizarVistaPanelCuenta(sesion);
            });
            filaCabeceraPanel.addView(btnToggleCon);
            boxDesplegable.addView(filaCabeceraPanel);

            // Consola compacta (85dp de alto)
            FrameLayout frameCon = new FrameLayout(this);
            LinearLayout.LayoutParams lpFrameCon = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(85));
            lpFrameCon.setMargins(0, 0, 0, dp(7));

            ScrollView scrollCon = new ScrollView(this);
            scrollCon.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 10, 1));
            scrollCon.setPadding(dp(10), dp(6), dp(10), dp(6));

            scrollCon.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });

            TextView txtCon = new TextView(this);
            txtCon.setTextColor(Color.parseColor("#E4E4E7"));
            txtCon.setTextSize(9.5f);
            txtCon.setTypeface(Typeface.MONOSPACE);
            scrollCon.addView(txtCon);

            frameCon.addView(scrollCon, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView btnLimpiarMini = new TextView(this);
            btnLimpiarMini.setText("Limpiar");
            btnLimpiarMini.setTextColor(Color.parseColor("#A1A1AA"));
            btnLimpiarMini.setTextSize(8f);
            btnLimpiarMini.setTypeface(null, Typeface.BOLD);
            btnLimpiarMini.setPadding(dp(6), dp(2), dp(6), dp(2));
            btnLimpiarMini.setBackground(fondoRedondeadoConBorde("#18181B", "#3F3F46", 5, 1));
            btnLimpiarMini.setOnClickListener(v -> txtCon.setText(""));

            FrameLayout.LayoutParams lpLimp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            lpLimp.setMargins(0, dp(4), dp(6), 0);
            frameCon.addView(btnLimpiarMini, lpLimp);

            boxDesplegable.addView(frameCon, lpFrameCon);

            // Barra de navegación (Botón Atrás ← | Botón Reiniciar ↻ | Recuadro de URL | Botón Zoom)
            LinearLayout barraNav = new LinearLayout(this);
            barraNav.setOrientation(LinearLayout.HORIZONTAL);
            barraNav.setGravity(Gravity.CENTER_VERTICAL);
            barraNav.setPadding(dp(6), dp(5), dp(6), dp(5));
            barraNav.setBackground(fondoRedondeadoConBorde("#14161A", "#27272A", 10, 1));
            LinearLayout.LayoutParams lpBarraNav = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpBarraNav.setMargins(0, 0, 0, dp(6));

            ImageView btnBack = new ImageView(this);
            btnBack.setScaleType(ImageView.ScaleType.CENTER);
            btnBack.setImageDrawable(crearIconoVectorial("BACK", "#E4E4E7", 15));
            btnBack.setBackground(fondoRedondeado("#202227", 8));
            btnBack.setOnClickListener(v -> {
                if (sesion.webViewOculto != null) {
                    ultimoSlotActivoRed = sesion.slotIndex;
                    if (sesion.webViewOculto.canGoBack()) {
                        sesion.webViewOculto.goBack();
                    } else {
                        sesion.webViewOculto.loadUrl(URL_LOGIN);
                    }
                }
            });

            ImageView btnReload = new ImageView(this);
            btnReload.setScaleType(ImageView.ScaleType.CENTER);
            btnReload.setImageDrawable(crearIconoVectorial("RELOAD", "#E4E4E7", 15));
            btnReload.setBackground(fondoRedondeado("#202227", 8));
            btnReload.setOnClickListener(v -> {
                if (sesion.webViewOculto != null) {
                    ultimoSlotActivoRed = sesion.slotIndex;
                    String u = sesion.webViewOculto.getUrl();
                    if (u == null || u.isEmpty()) {
                        sesion.webViewOculto.loadUrl(URL_LOGIN);
                    } else {
                        sesion.webViewOculto.reload();
                    }
                }
            });

            LinearLayout.LayoutParams lpBtnNav = new LinearLayout.LayoutParams(dp(30), dp(28));
            lpBtnNav.setMargins(0, 0, dp(5), 0);
            barraNav.addView(btnBack, lpBtnNav);
            barraNav.addView(btnReload, lpBtnNav);

            LinearLayout cajaUrl = new LinearLayout(this);
            cajaUrl.setOrientation(LinearLayout.HORIZONTAL);
            cajaUrl.setGravity(Gravity.CENTER_VERTICAL);
            cajaUrl.setPadding(dp(8), dp(4), dp(6), dp(4));
            cajaUrl.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 14, 1));

            TextView txtUrl = crearTexto("", "#E4E4E7", 10.5f, false);
            txtUrl.setSingleLine(true);
            txtUrl.setEllipsize(TextUtils.TruncateAt.END);
            ponerIconoYTexto(txtUrl, crearIconoVectorial("LOCK", "#94A3B8", 11), URL_LOGIN);

            TextView btnZoom = crearTexto(sesion.zoomPorcentaje + "%", "#4ADE80", 9.5f, true);
            btnZoom.setPadding(dp(8), dp(2), dp(8), dp(2));
            btnZoom.setBackground(fondoRedondeadoConBorde("#14532D", "#22C55E", 8, 1));
            btnZoom.setClickable(true);
            btnZoom.setOnClickListener(v -> ciclarZoomTarjeta(sesion));

            cajaUrl.addView(txtUrl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cajaUrl.addView(btnZoom);
            barraNav.addView(cajaUrl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            boxDesplegable.addView(barraNav, lpBarraNav);

            // Recuadro del Navegador Web en vivo (320dp de alto)
            FrameLayout marcoWv = new FrameLayout(this);
            marcoWv.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 10, 1));
            marcoWv.setPadding(dp(2), dp(2), dp(2), dp(2));
            boxDesplegable.addView(marcoWv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));

            card.addView(boxDesplegable);

            sesion.cardView = card;
            sesion.lblUsuarioCard = lblUsr;
            sesion.badgeEstadoCard = badgeEst;
            sesion.lblSaldosCard = lblSaldos;
            sesion.lblProxyCard = lblPrx;
            sesion.lblSubtituloPasoCard = lblPaso;
            sesion.btnPlayCard = bPlay;
            sesion.btnStopCard = bStop;
            sesion.btnLogoutCard = bLogoutStop;
            sesion.btnEyeCard = bEye;
            sesion.btnEditCard = bEdit;

            sesion.contenedorDesplegableCard = boxDesplegable;
            sesion.btnToggleConsolaMini = btnToggleCon;
            sesion.frameConsolaInterna = frameCon;
            sesion.scrollConsolaCard = scrollCon;
            sesion.txtConsolaCard = txtCon;
            sesion.btnLimpiarMiniCard = btnLimpiarMini;

            sesion.barraNavegadorCard = barraNav;
            sesion.btnNavBackCard = btnBack;
            sesion.btnNavReloadCard = btnReload;
            sesion.cajaUrlCard = cajaUrl;
            sesion.txtUrlCard = txtUrl;
            sesion.btnZoomUrlCard = btnZoom;
            sesion.marcoWebViewCard = marcoWv;

            // 2. Motor WebView conectado al Proxy de IPRoyal Venezuela
            WebView wv = new WebView(this);
            wv.setBackgroundColor(Color.parseColor("#18181B"));
            configurarPerfilAisladoWebView(wv, i);

            wv.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });

            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setLoadWithOverviewMode(false);
            ws.setUseWideViewPort(false);
            ws.setTextZoom(100);
            ws.setSupportZoom(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setLoadsImagesAutomatically(!modoTurboSinImagenes);
            ws.setBlockNetworkImage(modoTurboSinImagenes);
            ws.setBlockNetworkLoads(false);
            ws.setCacheMode(WebSettings.LOAD_DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Slot/" + (i + 1));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(wv, true);

            wv.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    sesion.progresoWeb = newProgress;
                    sesion.cargandoRed = (newProgress < 100);
                    String currentU = view.getUrl();
                    if (currentU != null && !currentU.isEmpty()) {
                        sesion.urlActualTexto = currentU;
                    }
                    if (newProgress >= 50) {
                        aplicarZoomWebView(sesion);
                    }
                    actualizarBarraUrlTarjeta(sesion);
                }
            });

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    ultimoSlotActivoRed = sesion.slotIndex;
                    sesion.cargandoRed = true;
                    sesion.inicioCargaRedMs = System.currentTimeMillis();
                    if (url != null && !url.isEmpty()) {
                        sesion.urlActualTexto = url;
                    }
                    actualizarBarraUrlTarjeta(sesion);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    sesion.cargandoRed = false;
                    sesion.progresoWeb = 100;
                    if (sesion.inicioCargaRedMs > 0) {
                        sesion.latenciaMs = Math.max(15, System.currentTimeMillis() - sesion.inicioCargaRedMs);
                    }
                    if (url != null && !url.isEmpty()) {
                        sesion.urlActualTexto = url;
                    }
                    aplicarZoomWebView(sesion);
                    if (modoTurboSinImagenes) {
                        evaluarJS(sesion,
                                "var st = document.getElementById('__vbt_turbo_css');" +
                                "if(!st){ st = document.createElement('style'); st.id='__vbt_turbo_css'; if(document.head) document.head.appendChild(st); }" +
                                "if(st) st.innerHTML = 'img, svg:not(#fb), .carousel, .banner { opacity:0 !important; pointer-events:none !important; }';", null);
                    }
                    actualizarBarraUrlTarjeta(sesion);
                }

                @Override
                public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handlerAuth, String host, String realm) {
                    if (handlerAuth != null) {
                        handlerAuth.proceed(PRX_USER_RAW, construirPasswordProxyVenezuela(sesion));
                    }
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handlerSsl, SslError error) {
                    if (handlerSsl != null) {
                        handlerSsl.proceed();
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        sesion.cargandoRed = false;
                        sesion.progresoWeb = 100;
                        actualizarBarraUrlTarjeta(sesion);
                    }
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    return true;
                }
            });

            sesion.webViewOculto = wv;
            slotFondo.addView(wv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            sincronizarVistaPanelCuenta(sesion);
            wv.loadUrl(URL_LOGIN);
        }
    }

    private ImageView crearBotonAccionTarjeta(String icono, String bgHex, String fgHex, Runnable accion) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setClickable(true);
        iv.setFocusable(false);
        iv.setImageDrawable(crearIconoVectorial(icono, fgHex, 12));
        iv.setBackground(fondoRedondeadoConBorde(bgHex, fgHex, 7, 1));
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
    // ACTUALIZACIÓN VISUAL DE TARJETAS Y LECTOR EXACTO DE SALDOS (.saldoDisp EN /pg)
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
            lpCard.setMargins(0, 0, 0, dp(9));
            contenedorTarjetasCuentas.addView(s.cardView, lpCard);
            sincronizarVistaPanelCuenta(s);
            actualizarVisualTarjeta(s);
        }

        actualizarResumenYBotonesGlobales();
    }

    private void actualizarVisualTarjeta(SesionBot s) {
        if (s == null || s.cardView == null) return;
        boolean corriendo = s.estaCorriendo();
        String bgCard = colFondoTarjeta();
        String bgSub = colFondoSubPanel();
        String borderCard = colBordeTarjeta();
        String txtPri = colTextoPrimario();
        String txtSec = colTextoSecundario();

        Drawable dot = crearCirculoNeon(corriendo || s.compraCompletada, corriendo ? pulsoNeon : true, 11);
        ponerIconoYTexto(s.lblUsuarioCard, dot, s.nombreMostrado());
        if (s.compraCompletada || corriendo) {
            s.lblUsuarioCard.setTextColor(Color.parseColor(temaActual == 2 ? "#15803D" : "#4ADE80"));
        } else {
            s.lblUsuarioCard.setTextColor(Color.parseColor(txtSec));
        }

        if (s.compraCompletada) {
            s.cardView.setBackground(fondoRedondeadoConBorde(temaActual == 2 ? "#DCFCE7" : "#0D2818", "#00FF66", 14, 2));
        } else if (corriendo) {
            s.cardView.setBackground(fondoRedondeadoConBorde(bgCard, temaActual == 1 ? "#38BDF8" : "#22C55E", 14, 1));
        } else {
            s.cardView.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 14, 1));
        }

        String txtBadge = nombreCortoEstado(s);
        s.badgeEstadoCard.setText(txtBadge);
        if (s.compraCompletada) {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#00FF66"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#064E3B", "#00FF66", 10, 1));
        } else if (corriendo) {
            boolean pingAlto = (s.latenciaMs > 1800);
            String colTxtBadge = pingAlto ? "#FBBF24" : (temaActual == 1 ? "#38BDF8" : "#4ADE80");
            String colBgBadge = pingAlto ? "#451A03" : (temaActual == 1 ? "#1E3A8A" : "#14532D");
            String colBorderBadge = pingAlto ? "#F59E0B" : (temaActual == 1 ? "#38BDF8" : "#22C55E");
            s.badgeEstadoCard.setTextColor(Color.parseColor(colTxtBadge));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde(colBgBadge, colBorderBadge, 10, 1));
        } else {
            s.badgeEstadoCard.setTextColor(Color.parseColor(txtSec));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 10, 1));
        }

        if (s.lblSaldosCard != null) {
            s.lblSaldosCard.setText(s.textoSaldosMostrado());
            s.lblSaldosCard.setTextColor(Color.parseColor(temaActual == 2 ? "#0284C7" : "#38BDF8"));
        }

        if (s.lblProxyCard != null) {
            s.lblProxyCard.setText(s.textoEstadoProxyMostrado());
            s.lblProxyCard.setTextColor(Color.parseColor(proxyRotativoActivo
                    ? (temaActual == 2 ? "#0D9488" : "#34D399")
                    : txtSec));
        }

        s.lblSubtituloPasoCard.setText(s.ultimoPasoTexto);
        s.lblSubtituloPasoCard.setTextColor(Color.parseColor(
                s.compraCompletada ? (temaActual == 2 ? "#15803D" : "#00FF66")
                        : (corriendo ? txtPri : txtSec)
        ));

        if (s.scrollConsolaCard != null) {
            s.scrollConsolaCard.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 10, 1));
        }
        if (s.txtConsolaCard != null) {
            s.txtConsolaCard.setTextColor(Color.parseColor(colTextoConsola()));
        }

        if (s.btnToggleConsolaMini != null) {
            s.btnToggleConsolaMini.setText(s.mostrarConsolaEnPanel ? "▲ Ocultar consola" : "▼ Mostrar consola");
            s.btnToggleConsolaMini.setTextColor(Color.parseColor(temaActual == 2 ? "#0284C7" : "#38BDF8"));
            s.btnToggleConsolaMini.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 8, 1));
        }

        if (s.barraNavegadorCard != null) {
            s.barraNavegadorCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 10, 1));
        }
        if (s.btnNavBackCard != null) {
            s.btnNavBackCard.setBackground(fondoRedondeado(bgCard, 8));
            s.btnNavBackCard.setImageDrawable(crearIconoVectorial("BACK", txtPri, 15));
        }
        if (s.btnNavReloadCard != null) {
            s.btnNavReloadCard.setBackground(fondoRedondeado(bgCard, 8));
            s.btnNavReloadCard.setImageDrawable(crearIconoVectorial("RELOAD", txtPri, 15));
        }
        if (s.cajaUrlCard != null) {
            s.cajaUrlCard.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 14, 1));
        }
        if (s.txtUrlCard != null) {
            s.txtUrlCard.setTextColor(Color.parseColor(txtPri));
            ponerIconoYTexto(s.txtUrlCard, crearIconoVectorial("LOCK", txtSec, 11), s.urlActualTexto);
        }
        if (s.btnZoomUrlCard != null) {
            s.btnZoomUrlCard.setText(s.zoomPorcentaje + "%");
        }
        if (s.marcoWebViewCard != null) {
            s.marcoWebViewCard.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 10, 1));
        }

        String bgBtnPlay = temaActual == 1 ? "#134E4A" : (temaActual == 2 ? "#DCFCE7" : "#14532D");
        String fgBtnPlay = temaActual == 1 ? "#2DD4BF" : (temaActual == 2 ? "#15803D" : "#4ADE80");
        s.btnPlayCard.setBackground(fondoRedondeadoConBorde(bgBtnPlay, fgBtnPlay, 7, 1));
        s.btnPlayCard.setImageDrawable(crearIconoVectorial("PLAY", fgBtnPlay, 12));
        s.btnPlayCard.setAlpha(corriendo ? 0.35f : 1.0f);
        s.btnPlayCard.setEnabled(!corriendo);

        String bgBtnStop = temaActual == 1 ? "#4C0519" : (temaActual == 2 ? "#FEE2E2" : "#450A0A");
        String fgBtnStop = temaActual == 1 ? "#FB7185" : (temaActual == 2 ? "#DC2626" : "#F87171");
        s.btnStopCard.setBackground(fondoRedondeadoConBorde(bgBtnStop, fgBtnStop, 7, 1));
        s.btnStopCard.setImageDrawable(crearIconoVectorial("STOP", fgBtnStop, 12));
        s.btnStopCard.setAlpha(corriendo ? 1.0f : 0.35f);
        s.btnStopCard.setEnabled(corriendo);

        String bgBtnOut = temaActual == 2 ? "#FFEDD5" : "#431407";
        String fgBtnOut = temaActual == 2 ? "#EA580C" : "#FB923C";
        s.btnLogoutCard.setBackground(fondoRedondeadoConBorde(bgBtnOut, fgBtnOut, 7, 1));
        s.btnLogoutCard.setImageDrawable(crearIconoVectorial("LOGOUT", fgBtnOut, 12));

        String bgBtnEye = temaActual == 1 ? "#1E3A8A" : (temaActual == 2 ? "#E0F2FE" : "#1E293B");
        String fgBtnEye = temaActual == 2 ? "#0284C7" : "#38BDF8";
        s.btnEyeCard.setBackground(fondoRedondeadoConBorde(bgBtnEye, fgBtnEye, 7, 1));
        s.btnEyeCard.setImageDrawable(crearIconoVectorial(s.nombreOculto ? "EYE_OFF" : "EYE", fgBtnEye, 12));

        String bgBtnEdit = bgSub;
        String fgBtnEdit = txtPri;
        s.btnEditCard.setBackground(fondoRedondeadoConBorde(bgBtnEdit, borderCard, 7, 1));
        s.btnEditCard.setImageDrawable(crearIconoVectorial("EDIT", fgBtnEdit, 12));
    }

    private void actualizarResumenYBotonesGlobales() {
        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        int total = reg.size();
        int activas = contarCuentasCorriendo();
        int detenidas = Math.max(0, total - activas);

        chipRegistradas.setText(total == 1 ? "1 registrada" : total + " registradas");
        chipRegistradas.setBackground(fondoRedondeado(temaActual == 2 ? "#E0F2FE" : "#1E293B", 14));
        chipRegistradas.setTextColor(Color.parseColor(temaActual == 2 ? "#0369A1" : "#38BDF8"));

        chipActivas.setText(activas == 1 ? "1 activa" : activas + " activas");
        chipActivas.setBackground(fondoRedondeado(temaActual == 2 ? "#DCFCE7" : "#14532D", 14));
        chipActivas.setTextColor(Color.parseColor(temaActual == 2 ? "#15803D" : "#4ADE80"));

        chipDetenidas.setText(detenidas == 1 ? "1 detenida" : detenidas + " detenidas");
        chipDetenidas.setBackground(fondoRedondeado(colFondoSubPanel(), 14));
        chipDetenidas.setTextColor(Color.parseColor(colTextoSecundario()));

        if (activas > 0) {
            ponerIconoYTexto(badgeEstadoGlobal, crearCirculoNeon(true, pulsoNeon, 10), "VIGILANDO (" + activas + ")");
            badgeEstadoGlobal.setTextColor(Color.parseColor("#4ADE80"));
            badgeEstadoGlobal.setBackground(fondoRedondeado("#14532D", 12));
        } else {
            ponerIconoYTexto(badgeEstadoGlobal, crearCirculoNeon(false, false, 10), "APAGADO");
            badgeEstadoGlobal.setTextColor(Color.parseColor("#FCA5A5"));
            badgeEstadoGlobal.setBackground(fondoRedondeado("#450A0A", 12));
        }
        badgeEstadoGlobal.setVisibility(pestanaInferiorActual == 0 ? View.VISIBLE : View.GONE);

        boolean hayParaIniciar = (total == 0 || activas < total);
        boolean hayParaDetener = (activas > 0);

        String bgStartOn = colBotonIniciarActivo();
        String bgStartOff = colBotonIniciarInactivo();
        String txtStartOff = (temaActual == 2) ? "#15803D" : "#86EFAC";

        btnIniciarTodas.setEnabled(hayParaIniciar);
        btnIniciarTodas.setBackground(fondoRedondeado(hayParaIniciar ? bgStartOn : bgStartOff, 10));
        ponerIconoYTexto(btnIniciarTodas, crearIconoVectorial("PLAY", hayParaIniciar ? "#FFFFFF" : txtStartOff, 14), "Iniciar todas");
        btnIniciarTodas.setTextColor(Color.parseColor(hayParaIniciar ? "#FFFFFF" : txtStartOff));

        String bgStopOn = colBotonDetenerActivo();
        String bgStopOff = colBotonDetenerInactivo();
        String txtStopOff = (temaActual == 2) ? "#B91C1C" : "#FCA5A5";

        btnDetenerTodas.setEnabled(hayParaDetener);
        btnDetenerTodas.setBackground(fondoRedondeado(hayParaDetener ? bgStopOn : bgStopOff, 10));
        ponerIconoYTexto(btnDetenerTodas, crearIconoVectorial("STOP", hayParaDetener ? "#FFFFFF" : txtStopOff, 14), "Detener todas");
        btnDetenerTodas.setTextColor(Color.parseColor(hayParaDetener ? "#FFFFFF" : txtStopOff));
    }

    private void actualizarSaldosEnSegundoPlano(final SesionBot s, boolean forzarAhora) {
        if (s == null || s.webViewOculto == null) return;
        long ahora = System.currentTimeMillis();
        if (!forzarAhora && (ahora - s.ultimaActualizacionSaldoMs < 3500)) return;
        s.ultimaActualizacionSaldoMs = ahora;

        String jsBalanceExacto =
                "(async function(){" +
                "  try {" +
                "    var t0 = Date.now();" +
                "    function parseMontoVE(str) {" +
                "      if (!str) return NaN;" +
                "      var s = String(str).trim();" +
                "      if (s.indexOf(',') !== -1) {" +
                "        s = s.replace(/\\./g, '').replace(',', '.');" +
                "      }" +
                "      return parseFloat(s);" +
                "    }" +
                "    function fmtVE(num) {" +
                "      return Number(num).toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                "    }" +
                "    function extraerDesdeRaiz(root) {" +
                "      var bsList = root.querySelectorAll('.saldoDisp[data-moneda=\"Bs\"]');" +
                "      if (bsList.length > 0) {" +
                "        var sumBs = 0, okBs = false;" +
                "        bsList.forEach(function(el){" +
                "          var v = parseMontoVE(el.getAttribute('data-saldo'));" +
                "          if (!isNaN(v)) { sumBs += v; okBs = true; }" +
                "        });" +
                "        if (okBs) { window.__vbt_bs = fmtVE(sumBs); window.__vbt_bs_num = sumBs; }" +
                "      }" +
                "      var usdList = root.querySelectorAll('.saldoDisp[data-moneda=\"USD\"]');" +
                "      if (usdList.length > 0) {" +
                "        var sumUsd = 0, okUsd = false;" +
                "        usdList.forEach(function(el){" +
                "          var v = parseMontoVE(el.getAttribute('data-saldo'));" +
                "          if (!isNaN(v)) { sumUsd += v; okUsd = true; }" +
                "        });" +
                "        if (okUsd) window.__vbt_usd = fmtVE(sumUsd);" +
                "      }" +
                "      var optD = root.querySelector('#desde option[data-disponible]');" +
                "      var optH = root.querySelector('#hasta option[data-disponible]');" +
                "      if (optD) {" +
                "        var vd = parseMontoVE(optD.getAttribute('data-disponible'));" +
                "        if (!isNaN(vd)) { window.__vbt_bs = fmtVE(vd); window.__vbt_bs_num = vd; }" +
                "      }" +
                "      if (optH) {" +
                "        var vh = parseMontoVE(optH.getAttribute('data-disponible'));" +
                "        if (!isNaN(vh)) window.__vbt_usd = fmtVE(vh);" +
                "      }" +
                "    }" +
                "    extraerDesdeRaiz(document);" +
                "    var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "    var tok = meta ? meta.getAttribute('content') : '';" +
                "    var rPg = await fetch('https://tesoroenlinea.bt.com.ve/pg', {" +
                "      method: 'GET'," +
                "      headers: {'X-Requested-With': 'XMLHttpRequest', 'X-CSRF-TOKEN': tok}," +
                "      credentials: 'same-origin'" +
                "    });" +
                "    window.__vbt_ping = Math.max(10, Date.now() - t0);" +
                "    if (rPg.status === 200) {" +
                "      var htmlPg = await rPg.text();" +
                "      var parser = new DOMParser();" +
                "      var docPg = parser.parseFromString(htmlPg, 'text/html');" +
                "      extraerDesdeRaiz(docPg);" +
                "    }" +
                "  } catch(e){}" +
                "})();";

        evaluarJS(s, jsBalanceExacto, null);
    }

    // =========================================================================================
    // HISTORIAL DE LA CONSOLA + REGISTRO DE COMPRAS EXITOSAS CON TASA BCV Y CÓDIGO
    // =========================================================================================
    private void cargarComprasExitosasGuardadas() {
        listaComprasExitosas.clear();
        try {
            String raw = prefs.getString("compras_exitosas_json", "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    listaComprasExitosas.add(new RegistroCompraExitosa(
                            o.optString("usuario", ""),
                            o.optString("cedula", ""),
                            o.optString("fechaHora", ""),
                            o.optString("montoUsd", "300$"),
                            o.optString("tasaBcv", "--"),
                            o.optString("codigoOp", "--")
                    ));
                }
            }
        } catch (Exception ignored) {}
        reconstruirListaComprasExitosas();
    }

    private void guardarComprasExitosasPrefs() {
        JSONArray arr = new JSONArray();
        for (RegistroCompraExitosa r : listaComprasExitosas) {
            try {
                JSONObject o = new JSONObject();
                o.put("usuario", r.usuario);
                o.put("cedula", r.cedula);
                o.put("fechaHora", r.fechaHora);
                o.put("montoUsd", r.montoUsd);
                o.put("tasaBcv", r.tasaBcv);
                o.put("codigoOp", r.codigoOperacion);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString("compras_exitosas_json", arr.toString()).apply();
    }

    private void registrarNuevaCompraExitosa(final SesionBot s, final String montoFinalStr, final String tasaStr, final String codigoStr) {
        final String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        runOnUiThread(() -> {
            listaComprasExitosas.add(0, new RegistroCompraExitosa(
                    s.usuario,
                    s.tipoDoc + "-" + s.cedula,
                    fechaHora,
                    montoFinalStr,
                    (tasaStr == null || tasaStr.isEmpty()) ? s.ultimaTasaLeida : tasaStr,
                    (codigoStr == null || codigoStr.isEmpty()) ? s.ultimoCodigoOpLeido : codigoStr
            ));
            if (listaComprasExitosas.size() > 60) {
                listaComprasExitosas.remove(listaComprasExitosas.size() - 1);
            }
            guardarComprasExitosasPrefs();
            reconstruirListaComprasExitosas();
        });
    }

    private void reconstruirListaComprasExitosas() {
        if (contenedorComprasExitosas == null) return;
        contenedorComprasExitosas.removeAllViews();

        String bgCard = colFondoTarjeta();
        String bgSub = colFondoSubPanel();
        String borderCard = colBordeTarjeta();
        String txtPri = colTextoPrimario();
        String txtSec = colTextoSecundario();

        if (listaComprasExitosas.isEmpty()) {
            TextView tvVacio = crearTexto("Aún no hay compras registradas en este dispositivo.", txtSec, 11.5f, false);
            tvVacio.setPadding(dp(14), dp(14), dp(14), dp(14));
            tvVacio.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 12, 1));
            contenedorComprasExitosas.addView(tvVacio);
            return;
        }

        for (final RegistroCompraExitosa item : listaComprasExitosas) {
            LinearLayout cardCompra = new LinearLayout(this);
            cardCompra.setOrientation(LinearLayout.VERTICAL);
            cardCompra.setPadding(dp(12), dp(10), dp(12), dp(10));
            cardCompra.setBackground(fondoRedondeadoConBorde(bgCard, "#22C55E", 12, 1));
            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpC.setMargins(0, 0, 0, dp(8));

            LinearLayout fila1 = new LinearLayout(this);
            fila1.setOrientation(LinearLayout.HORIZONTAL);
            fila1.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvUser = crearTexto("✅ " + item.usuario + " (" + item.cedula + ")", "#4ADE80", 13f, true);
            TextView tvMonto = crearTexto(item.montoUsd, "#00FF66", 13.5f, true);
            fila1.addView(tvUser, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            fila1.addView(tvMonto);
            cardCompra.addView(fila1);

            TextView tvDetalles = crearTexto(
                    "Código Op: #" + item.codigoOperacion + "   •   Tasa BCV: " + item.tasaBcv + " Bs/$",
                    txtPri, 11.5f, true);
            tvDetalles.setPadding(0, dp(4), 0, dp(2));
            cardCompra.addView(tvDetalles);

            LinearLayout filaInf = new LinearLayout(this);
            filaInf.setOrientation(LinearLayout.HORIZONTAL);
            filaInf.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvFecha = crearTexto("Fecha: " + item.fechaHora + "  •  Estado: APROBADO", txtSec, 10.5f, false);
            Button btnCopiarComprobante = crearBoton("Copiar", bgSub, "#38BDF8", 10, () -> {
                String resumen = "✅ Compra Ordinaria Aprobada - Banco del Tesoro\n"
                        + "👤 Cuenta: " + item.usuario + " (" + item.cedula + ")\n"
                        + "💵 Monto Pactado: " + item.montoUsd + "\n"
                        + "📊 Tasa BCV: " + item.tasaBcv + "\n"
                        + "🔢 Código Operación: #" + item.codigoOperacion + "\n"
                        + "🕒 Emisión: " + item.fechaHora;
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("Comprobante BT", resumen));
                    Toast.makeText(this, "Comprobante copiado al portapapeles", Toast.LENGTH_SHORT).show();
                }
            });
            filaInf.addView(tvFecha, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            filaInf.addView(btnCopiarComprobante, new LinearLayout.LayoutParams(dp(68), dp(26)));
            cardCompra.addView(filaInf);

            contenedorComprasExitosas.addView(cardCompra, lpC);
        }
    }

    private void reconstruirFiltrosHistorial() {
        barraFiltrosHistorial.removeAllViews();
        String acento = colAcentoTema();
        String bgInact = colFondoSubPanel();
        String txtInact = colTextoSecundario();

        Button btnAll = crearBoton("TODAS", filtroHistorialSlot == -1 ? acento : bgInact,
                filtroHistorialSlot == -1 ? "#FFFFFF" : txtInact, 10, () -> {
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
            Button bUsr = crearBoton(s.nombreMostrado(), sel ? acento : bgInact,
                    sel ? "#FFFFFF" : txtInact, 10, () -> {
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
                if (s.panelDesplegado && s.mostrarConsolaEnPanel) {
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
            sesiones[i].ultimoMontoAjustado = 0;
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
        edtMontoCustom.setHintTextColor(Color.parseColor("#71717A"));
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

        Button btnGuardar = crearBotonConIcono("CHECK", "Guardar Cuenta", colAcentoTema(), "#FFFFFF", 13, () -> {
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
                sesionEdit.ultimoMontoAjustado = 0;
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
                nueva.ultimoMontoAjustado = 0;
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
                pausarSesionSinCerrar(sesionEdit, false);
                sesionEdit.panelDesplegado = false;
                sincronizarVistaPanelCuenta(sesionEdit);
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

    private void cambiarEstado(SesionBot s, EstadoBot nuevoEstado) {
        s.estadoActual = nuevoEstado;
        s.tiempoEstadoMs = System.currentTimeMillis();
        s.accionEnCurso = false;
        actualizarVisualTarjeta(s);
        actualizarResumenYBotonesGlobales();
    }

    private String nombreCortoEstado(SesionBot s) {
        if (s.compraCompletada) return "COMPLETADA";
        String base;
        switch (s.estadoActual) {
            case DETENIDO: return "DETENIDO";
            case ENFRIANDO_CAMBIO_IP: base = "CAMBIANDO IP (2s)"; break;
            case CARGANDO_LOGIN:
            case ESPERANDO_ESTABILIZAR_LOGIN:
            case ENVIANDO_FASE1: base = "INICIANDO"; break;
            case ESPERANDO_CARGA_PASS:
            case ESPERANDO_ESTABILIZAR_PASS:
            case ENVIANDO_FASE2: base = "CONTRASEÑA"; break;
            case ESPERANDO_REINTENTO_SESION_ACTIVA: base = "SESIÓN ACTIVA"; break;
            case ESPERANDO_CARGA_INICIO:
            case ESPERANDO_ESTABILIZAR_INICIO: base = "POSICIÓN GLOBAL"; break;
            case NAVEGANDO_MENU: base = "INTERVENCIÓN"; break;
            case VIGILANDO: base = "VIGILANDO"; break;
            case ESPERANDO_FORM_INTENTO1:
            case LIMPIANDO_PANTALLA:
            case ESPERANDO_FORM_INTENTO2:
            case ESPERANDO_ESTABILIZAR_FORM:
            case LLENANDO_COMPRA:
            case COMPRANDO: base = "COMPRANDO"; break;
            case CERRANDO_SESION:
            case REINICIANDO_RED: base = "CERRANDO SESIÓN"; break;
            default: base = "ACTIVO"; break;
        }
        if (s.latenciaMs > 0) {
            return base + " • " + s.latenciaMs + "ms";
        }
        return base;
    }

    // =========================================================================================
    // ENCENDIDO FIABLE (CON 1, 2, 3 O 4 CUENTAS), PAUSA SIN CERRAR SESIÓN Y BOTÓN SALIR
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
                pausarSesionSinCerrar(s, true);
            }
        }
        lblReloginVal.setText(minutosRelogin + " min (--:--)");
    }

    private void iniciarSesionIndividual(final SesionBot s, int delayMs) {
        if (s == null || !s.registrada) return;
        ultimoSlotActivoRed = s.slotIndex;
        s.corriendo = true;
        s.detenerTrasCerrarSesion = false;
        s.rotarIpTrasCerrarSesion = false;
        s.compraCompletada = false;
        s.ultimoMontoAjustado = 0;
        s.intentoLogout = 0;
        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);

        if (!s.sesionAbiertaEnServidor) {
            registrarPasoCuenta(s, "Iniciando sesión con Proxy VE (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
        } else {
            registrarPasoCuenta(s, "Reanudando cuenta...");
        }

        handler.postDelayed(() -> {
            if (!s.estaCorriendo() || s.webViewOculto == null) return;
            String urlActual = s.webViewOculto.getUrl();
            if (urlActual == null || urlActual.isEmpty() || !urlActual.contains("tesoroenlinea.bt.com.ve")) {
                s.webViewOculto.loadUrl(URL_LOGIN);
            }
            ejecutarPasoMaquinaEstados(s);
        }, Math.max(0, delayMs));
    }

    private void pausarSesionSinCerrar(SesionBot s, boolean registrarEnLog) {
        s.corriendo = false;
        s.detenerTrasCerrarSesion = false;
        s.rotarIpTrasCerrarSesion = false;
        s.accionEnCurso = false;
        if (registrarEnLog) {
            registrarPasoCuenta(s, "Cuenta detenida");
        }
        cambiarEstado(s, EstadoBot.DETENIDO);
    }

    private void cerrarSesionYDetenerCuenta(SesionBot s) {
        if (s == null || !s.registrada) return;
        s.corriendo = true;
        s.detenerTrasCerrarSesion = true;
        s.rotarIpTrasCerrarSesion = false;
        s.intentoLogout = 0;
        cambiarEstado(s, EstadoBot.CERRANDO_SESION);
        registrarPasoCuenta(s, "Cerrando sesión...");
        ejecutarPasoMaquinaEstados(s);
    }

    // =========================================================================================
    // CONTROL DE RED INTERNA (QUITAR Y PONER INTERNET AL NAVEGADOR DE LA CUENTA)
    // =========================================================================================
    @SuppressWarnings("deprecation")
    private boolean hayInternetDisponible() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        }, 1200);
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

                // Verificación periódica silenciosa en segundo plano por si desactivas al usuario desde tu PC
                long ahoraCiclo = System.currentTimeMillis();
                if (ahoraCiclo - ultimaVerificacionNubeMs > 30000) {
                    ultimaVerificacionNubeMs = ahoraCiclo;
                    if (loginClienteOverlay != null && loginClienteOverlay.getVisibility() != View.VISIBLE) {
                        String uAct = prefs.getString("usr_licencia_activa_v3", "");
                        String cAct = prefs.getString("clv_licencia_activa_v3", "");
                        if (!uAct.isEmpty() && !cAct.isEmpty()) {
                            validarCredencialesEnNubeOLocal(uAct, cAct, true, true);
                        }
                    }
                }

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
                handler.postDelayed(this, 380);
            }
        }, 380);
    }

    private void verificarTiemposDeSesionIndependientes(ArrayList<SesionBot> reg) {
        long ahora = System.currentTimeMillis();
        long totalSeg = minutosRelogin * 60L;
        long menorRestante = -1;

        boolean hayAperturaOCompraActiva = yaAvisoApertura;
        for (SesionBot s : reg) {
            if (s.estaCorriendo() && (s.estadoActual == EstadoBot.ESPERANDO_FORM_INTENTO1
                    || s.estadoActual == EstadoBot.LIMPIANDO_PANTALLA
                    || s.estadoActual == EstadoBot.ESPERANDO_FORM_INTENTO2
                    || s.estadoActual == EstadoBot.ESPERANDO_ESTABILIZAR_FORM
                    || s.estadoActual == EstadoBot.LLENANDO_COMPRA
                    || s.estadoActual == EstadoBot.COMPRANDO)) {
                hayAperturaOCompraActiva = true;
                break;
            }
        }

        if (hayAperturaOCompraActiva) {
            lblReloginVal.setText(minutosRelogin + " min (ESCUDO)");
            return;
        }

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
                    s.rotarIpTrasCerrarSesion = false;
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
    // MÁQUINA DE ESTADOS CON LECTURA DE TASA BCV / CÓDIGO DE TABLA Y ROTACIÓN DE IP EN /lg
    // =========================================================================================
    private void ejecutarPasoMaquinaEstados(final SesionBot sesion) {
        if (sesion == null || sesion.webViewOculto == null) return;
        final long ahora = System.currentTimeMillis();

        String jsInspector =
                "var ready = (document.readyState === 'complete' || document.readyState === 'interactive');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var cargandoTxt = (bodyTxt.indexOf('cargando, por favor espera') !== -1 || bodyTxt.indexOf('procesando') !== -1);" +
                "var modalLogout = (bodyTxt.indexOf('deseas cerrar tu sesión') !== -1 || bodyTxt.indexOf('deseas cerrar tu sesion') !== -1);" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1 || bodyTxt.indexOf('posee una sesion') !== -1);" +
                "var metaTok = document.querySelector('meta[name=\"csrf-token\"]');" +
                "var inputTok = document.querySelector('input[name=\"_token\"]');" +
                "var csrfActual = metaTok ? metaTok.getAttribute('content') : (inputTok ? inputTok.value : '');" +
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
                "var perfNav = (window.performance && performance.getEntriesByType) ? performance.getEntriesByType('navigation')[0] : null;" +
                "var pingCalc = window.__vbt_ping || (perfNav && perfNav.responseEnd > 0 ? Math.round(perfNav.responseEnd - perfNav.requestStart) : 0);" +
                "var codTabla = ''; var tasaTabla = '';" +
                "try {" +
                "  var primeraFila = document.querySelector('table tbody tr');" +
                "  if (primeraFila) {" +
                "    var celdas = primeraFila.querySelectorAll('td');" +
                "    if (celdas.length >= 6) {" +
                "      codTabla = (celdas[0].innerText || '').trim();" +
                "      tasaTabla = (celdas[5].innerText || '').trim();" +
                "    }" +
                "  }" +
                "} catch(e){}" +
                "function parseMontoVE(str) {" +
                "  if (!str) return NaN;" +
                "  var s = String(str).trim();" +
                "  if (s.indexOf(',') !== -1) s = s.replace(/\\./g, '').replace(',', '.');" +
                "  return parseFloat(s);" +
                "}" +
                "function fmtVE(num) {" +
                "  return Number(num).toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2});" +
                "}" +
                "var bsNodes = document.querySelectorAll('.saldoDisp[data-moneda=\"Bs\"]');" +
                "if (bsNodes.length > 0) {" +
                "  var sumBs = 0, okBs = false;" +
                "  bsNodes.forEach(function(el){ var v = parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){ sumBs += v; okBs = true; } });" +
                "  if (okBs) { window.__vbt_bs = fmtVE(sumBs); window.__vbt_bs_num = sumBs; }" +
                "}" +
                "var usdNodes = document.querySelectorAll('.saldoDisp[data-moneda=\"USD\"]');" +
                "if (usdNodes.length > 0) {" +
                "  var sumUsd = 0, okUsd = false;" +
                "  usdNodes.forEach(function(el){ var v = parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){ sumUsd += v; okUsd = true; } });" +
                "  if (okUsd) window.__vbt_usd = fmtVE(sumUsd);" +
                "}" +
                "var optD = document.querySelector('#desde option[data-disponible]');" +
                "var optH = document.querySelector('#hasta option[data-disponible]');" +
                "if (optD) { var vd = parseMontoVE(optD.getAttribute('data-disponible')); if (!isNaN(vd)) { window.__vbt_bs = fmtVE(vd); window.__vbt_bs_num = vd; } }" +
                "if (optH) { var vh = parseMontoVE(optH.getAttribute('data-disponible')); if (!isNaN(vh)) window.__vbt_usd = fmtVE(vh); }" +
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
                "  logoutOk: !!window.__vbt_logout_ok," +
                "  csrf: csrfActual || ''," +
                "  ping: pingCalc || 0," +
                "  codOp: codTabla || ''," +
                "  tasaBcv: tasaTabla || ''," +
                "  montoAjustado: window.__vbt_monto_ajustado || 0," +
                "  bs: window.__vbt_bs || ''," +
                "  usd: window.__vbt_usd || ''" +
                "});";

        final long tInicioEval = System.currentTimeMillis();
        evaluarJS(sesion, jsInspector, raw -> {
            if (!sesion.estaCorriendo() || raw == null || raw.equals("null")) return;
            try {
                long rttEval = Math.max(12, System.currentTimeMillis() - tInicioEval);
                String clean = raw;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.substring(1, clean.length() - 1).replace("\\\"", "\"");
                }
                JSONObject st = new JSONObject(clean);
                String nuevoBs = st.optString("bs", "");
                String nuevoUsd = st.optString("usd", "");
                String csrfDetectado = st.optString("csrf", "");
                String codOpLeido = st.optString("codOp", "");
                String tasaBcvLeida = st.optString("tasaBcv", "");
                long pingWeb = st.optLong("ping", 0);
                int mAjustado = st.optInt("montoAjustado", 0);

                if (pingWeb > 0) {
                    sesion.latenciaMs = pingWeb;
                } else if (sesion.latenciaMs == 0) {
                    sesion.latenciaMs = rttEval;
                }

                if (!csrfDetectado.isEmpty()) {
                    sesion.csrfTokenGuardado = csrfDetectado;
                }

                boolean cambioInfoTabla = false;
                if (!codOpLeido.isEmpty() && !codOpLeido.equals(sesion.ultimoCodigoOpLeido)) {
                    sesion.ultimoCodigoOpLeido = codOpLeido;
                    cambioInfoTabla = true;
                }
                if (!tasaBcvLeida.isEmpty() && !tasaBcvLeida.equals(sesion.ultimaTasaLeida)) {
                    sesion.ultimaTasaLeida = tasaBcvLeida;
                    cambioInfoTabla = true;
                }

                if (mAjustado > 0 && mAjustado != sesion.ultimoMontoAjustado) {
                    sesion.ultimoMontoAjustado = mAjustado;
                    registrarPasoCuenta(sesion, "Auto-Ajuste por saldo en Bs: orden ajustada a " + mAjustado + "$");
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
                if (cambioSaldo || cambioInfoTabla) {
                    actualizarVisualTarjeta(sesion);
                }

                boolean visDoc = st.optBoolean("doc");
                boolean visPass = st.optBoolean("pass");
                boolean visMonto = st.optBoolean("monto");
                boolean visOrd = st.optBoolean("ord");
                boolean visInicio = st.optBoolean("inicio");
                boolean visTabla = st.optBoolean("tablaInt");
                boolean cargando = st.optBoolean("cargando");

                boolean elementoCriticoListo = (visDoc || visPass || visMonto || visOrd || visInicio || visTabla) && !cargando;
                boolean navCompleta = (!sesion.cargandoRed && sesion.progresoWeb >= 100) || elementoCriticoListo;
                boolean ready = st.optBoolean("ready") && navCompleta;

                procesarEstadoConDatos(
                        sesion,
                        ahora,
                        ready && !cargando,
                        visDoc,
                        visPass,
                        visInicio,
                        visTabla,
                        visMonto,
                        visOrd,
                        st.optBoolean("modalLogout"),
                        st.optBoolean("inconv"),
                        st.optBoolean("exito"),
                        st.optBoolean("sinCupo"),
                        st.optBoolean("sesionActivaErr"),
                        st.optBoolean("logoutOk")
                );
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            SesionBot s, long ahora, boolean paginaCompleta, boolean visDoc, boolean visPass,
            boolean visInicioCompleto, boolean visTablaInt, boolean visMonto,
            boolean visOrd, boolean visModalLogout, boolean hayInconv, boolean hayExito,
            boolean haySinCupo, boolean haySesionActivaErr, boolean logoutOk
    ) {
        if (!s.estaCorriendo()) return;

        if (visInicioCompleto || visTablaInt || visMonto) {
            s.sesionAbiertaEnServidor = true;
            actualizarSaldosEnSegundoPlano(s, false);
        }

        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");
        String strEntero = String.valueOf(s.montoCompra);
        String strCompleto = strEntero + ",00";

        switch (s.estadoActual) {

            case ENFRIANDO_CAMBIO_IP:
                if (ahora - s.tiempoEstadoMs >= 2000) {
                    ultimoSlotActivoRed = s.slotIndex;
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    registrarPasoCuenta(s, "Enfriamiento 2s completado - Iniciando sesión con nueva IP VE");
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case CARGANDO_LOGIN:
                if (paginaCompleta && visMonto && !hayInconv) {
                    if (s.tiempoInicioSesionMs == 0) s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visTablaInt) {
                    if (s.tiempoInicioSesionMs == 0) s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    actualizarSaldosEnSegundoPlano(s, true);
                    cambiarEstado(s, EstadoBot.VIGILANDO);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    if (s.tiempoInicioSesionMs == 0) s.tiempoInicioSesionMs = ahora;
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
                if (ahora - s.tiempoEstadoMs > 12000) {
                    s.tiempoEstadoMs = ahora;
                    ultimoSlotActivoRed = s.slotIndex;
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_LOGIN:
                if (!visDoc) {
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 200) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE1);
                }
                break;

            case ENVIANDO_FASE1:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    ultimoSlotActivoRed = s.slotIndex;
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
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 8);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(d, '" + cedSafe + "', 0, function() {" +
                            "    setTimeout(function() {" +
                            "      u.focus();" +
                            "      typeStr(u, '" + usrSafe + "', 0, function() {" +
                            "        setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 90);" +
                            "      });" +
                            "    }, 60);" +
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
                if (ahora - s.tiempoEstadoMs >= 200) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                }
                break;

            case ENVIANDO_FASE2:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    ultimoSlotActivoRed = s.slotIndex;
                    registrarPasoCuenta(s, "Colocando contraseña");
                    String jsFase2 =
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "var p = document.querySelector('#password');" +
                            "if(p) {" +
                            "  p.focus(); setter.call(p, '');" +
                            "  function typeStr(el, str, i, cb) {" +
                            "    if(i < str.length) {" +
                            "      setter.call(el, str.substring(0, i + 1)); el.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      setTimeout(function(){ typeStr(el, str, i+1, cb); }, 8);" +
                            "    } else { el.dispatchEvent(new Event('change', {bubbles:true})); if(cb) cb(); }" +
                            "  }" +
                            "  typeStr(p, '" + clvSafe + "', 0, function() {" +
                            "    setTimeout(function() { var btn = document.querySelector(\"button[type='submit'], .btn-primary\"); if(btn) btn.click(); }, 90);" +
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
                if (ahora - s.tiempoEstadoMs >= 220) {
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
                } else if (ahora - s.tiempoEstadoMs > 5500 && paginaCompleta) {
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
                    registrarPasoCuenta(s, "Disparo directo en Ordinaria (mod.add)");
                    if (!yaAvisoApertura) {
                        yaAvisoApertura = true;
                        sonarAlertaOrdinaria();
                    }
                    clicarOrdinariaJS(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                long esperaConJitter = (intervaloRefresco * 1000L) + (s.slotIndex * 220L);
                if (paginaCompleta && (ahora - s.tiempoEstadoMs >= esperaConJitter)) {
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
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 4200)) {
                    irAIntervencionJS(s);
                    cambiarEstado(s, EstadoBot.LIMPIANDO_PANTALLA);
                }
                break;

            case LIMPIANDO_PANTALLA:
                if (paginaCompleta && visTablaInt && (ahora - s.tiempoEstadoMs >= 1400)) {
                    if (visOrd) {
                        registrarPasoCuenta(s, "Segundo intento en Ordinaria (mod.add)");
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
                    sonarAlertaOrdinaria();
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv || (paginaCompleta && ahora - s.tiempoEstadoMs > 4500)) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    s.rotarIpTrasCerrarSesion = true;
                    registrarPasoCuenta(s, "Inconvenientes detectado - Cerrando sesión para cambiar IP");
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;

            case ESPERANDO_ESTABILIZAR_FORM:
                if (ahora - s.tiempoEstadoMs >= 150) {
                    cambiarEstado(s, EstadoBot.LLENANDO_COMPRA);
                }
                break;

            case LLENANDO_COMPRA:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    registrarPasoCuenta(s, "Rellenando formulario y verificando saldo Bs");
                    String jsCompraDirecta =
                            "window.__vbt_monto_ajustado = 0;" +
                            "function parseNumVE(str) {" +
                            "  if (!str) return NaN;" +
                            "  var s = String(str).trim();" +
                            "  if (s.indexOf(',') !== -1) s = s.replace(/\\./g, '').replace(',', '.');" +
                            "  return parseFloat(s);" +
                            "}" +
                            "var d = document.querySelector('#desde'); if(d && d.options.length > 1) { d.selectedIndex = 1; d.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var h = document.querySelector('#hasta'); if(h && h.options.length > 1) { h.selectedIndex = 1; h.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var o = document.querySelector('#origen'); if(o) { for(var i=0;i<o.options.length;i++){ if(o.options[i].text.includes('" + ORIGEN_FONDOS + "')){ o.selectedIndex=i; break; } } o.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var dst = document.querySelector('#destino'); if(dst) { for(var j=0;j<dst.options.length;j++){ if(dst.options[j].text.includes('" + DESTINO_FONDOS + "')){ dst.selectedIndex=j; break; } } dst.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var m = document.querySelector('#monto');" +
                            "var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "if(m) {" +
                            "  m.focus();" +
                            "  var montoMeta = " + s.montoCompra + ";" +
                            "  var pasos = ['" + strEntero.substring(0, 1) + "', '" + strEntero + "', '" + strCompleto + "'];" +
                            "  function dispararGuardadoOrden() {" +
                            "    var confirmarInstantaneo = function() {" +
                            "      var btnSi = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                            "      if(btnSi && btnSi.offsetWidth > 0) { btnSi.click(); return true; }" +
                            "      return false;" +
                            "    };" +
                            "    var obs = new MutationObserver(function(){ if(confirmarInstantaneo()) obs.disconnect(); });" +
                            "    if(document.body) obs.observe(document.body, {childList:true, subtree:true});" +
                            "    if (typeof mod !== 'undefined' && typeof mod.save === 'function') {" +
                            "      try { mod.save(); } catch(e){}" +
                            "    }" +
                            "    var col = document.querySelector(\"a[href='javascript:mod.save()']\") || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.toLowerCase().includes('colocar orden'));" +
                            "    if(col) { try { col.click(); } catch(e){} }" +
                            "    setTimeout(confirmarInstantaneo, 150);" +
                            "    setTimeout(confirmarInstantaneo, 350);" +
                            "    setTimeout(function(){ try{obs.disconnect();}catch(e){} }, 3500);" +
                            "  }" +
                            "  function verificarSaldoYAjustar() {" +
                            "    try {" +
                            "      var optD = document.querySelector('#desde option:checked') || document.querySelector('#desde option[data-disponible]');" +
                            "      var dispBs = optD ? parseNumVE(optD.getAttribute('data-disponible')) : (window.__vbt_bs_num || NaN);" +
                            "      var totEl = document.querySelector('#total-03');" +
                            "      var totalReqBs = totEl ? parseNumVE(totEl.value || totEl.innerText) : NaN;" +
                            "      if (!isNaN(dispBs) && dispBs > 0 && !isNaN(totalReqBs) && totalReqBs > dispBs && montoMeta > 10) {" +
                            "        var costoPorDolar = totalReqBs / montoMeta;" +
                            "        var maxUsd = Math.floor(((dispBs * 0.998) / costoPorDolar) / 10) * 10;" +
                            "        if (maxUsd >= 10 && maxUsd < montoMeta) {" +
                            "          window.__vbt_monto_ajustado = maxUsd;" +
                            "          var strNuevo = maxUsd + ',00';" +
                            "          setter.call(m, strNuevo);" +
                            "          m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "          m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "          m.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "          m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "        }" +
                            "      }" +
                            "    } catch(e){}" +
                            "    setTimeout(dispararGuardadoOrden, 100);" +
                            "  }" +
                            "  function escribirPaso(idx) {" +
                            "    if(idx < pasos.length) {" +
                            "      setter.call(m, pasos[idx]);" +
                            "      m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "      setTimeout(function(){ escribirPaso(idx + 1); }, 25);" +
                            "    } else {" +
                            "      if(m.value.indexOf('" + strEntero + "') === -1) {" +
                            "        setter.call(m, '" + strCompleto + "');" +
                            "        m.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "      }" +
                            "      m.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "      m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "      setTimeout(verificarSaldoYAjustar, 110);" +
                            "    }" +
                            "  }" +
                            "  setTimeout(function(){ escribirPaso(0); }, 50);" +
                            "}";
                    evaluarJS(s, jsCompraDirecta, null);
                    cambiarEstado(s, EstadoBot.COMPRANDO);
                }
                break;

            case COMPRANDO:
                if (hayExito) {
                    s.compraCompletada = true;
                    s.corriendo = false;
                    int montoFinalCompra = (s.ultimoMontoAjustado > 0) ? s.ultimoMontoAjustado : s.montoCompra;
                    registrarNuevaCompraExitosa(s, montoFinalCompra + ".00$", s.ultimaTasaLeida, s.ultimoCodigoOpLeido);
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
                    registrarPasoCuenta(s, "Orden completada exitosamente (" + montoFinalCompra + "$)");
                    sonarAlertaCompraExitosa();
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

            case CERRANDO_SESION:
                if (logoutOk || (paginaCompleta && (visDoc || visPass))) {
                    evaluarJS(s, "window.__vbt_logout_ok = false;", null);
                    s.sesionAbiertaEnServidor = false;
                    if (!visDoc) {
                        s.webViewOculto.loadUrl(URL_LOGIN);
                    }
                    if (s.detenerTrasCerrarSesion) {
                        s.corriendo = false;
                        s.detenerTrasCerrarSesion = false;
                        s.rotarIpTrasCerrarSesion = false;
                        s.tiempoInicioSesionMs = 0;
                        registrarPasoCuenta(s, "Sesión cerrada y cuenta detenida");
                        cambiarEstado(s, EstadoBot.DETENIDO);
                    } else if (s.rotarIpTrasCerrarSesion && proxyRotativoActivo) {
                        s.rotarIpTrasCerrarSesion = false;
                        rotarIpProxyParaCuenta(s, "Restricción en Ordinaria");
                        cambiarEstado(s, EstadoBot.ENFRIANDO_CAMBIO_IP);
                    } else {
                        s.rotarIpTrasCerrarSesion = false;
                        registrarPasoCuenta(s, "Iniciando sesión (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    }
                    return;
                }
                if (visModalLogout) {
                    evaluarJS(s,
                            "var bSi = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                            "if (bSi && bSi.offsetWidth > 0 && !window.__vbt_si_clicked) {" +
                            "  window.__vbt_si_clicked = true;" +
                            "  bSi.click();" +
                            "  setTimeout(function(){ window.__vbt_si_clicked = false; }, 1800);" +
                            "}", null);
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout++;
                    s.tiempoEstadoMs = ahora;
                    registrarPasoCuenta(s, "Cerrando sesión intento " + s.intentoLogout);
                    ejecutarScriptLogoutDirecto(s);
                } else if (ahora - s.tiempoEstadoMs > 6500) {
                    cambiarEstado(s, EstadoBot.REINICIANDO_RED);
                    reiniciarConexionIndividual(s);
                }
                break;

            case REINICIANDO_RED:
                if (!s.microCortePropio && (ahora - s.tiempoEstadoMs >= 1250)) {
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;
        }
    }

    private void ejecutarScriptLogoutDirecto(SesionBot s) {
        String csrfRespaldo = (s.csrfTokenGuardado != null) ? s.csrfTokenGuardado.replace("'", "") : "";

        String jsLogout =
                "window.__vbt_logout_ok = false;" +
                "window.__vbt_si_clicked = false;" +
                "(function(){" +
                "  try {" +
                "    var cerrarModalesPrevios = document.querySelectorAll(\"button.close, .btn-close, [data-dismiss='modal']\");" +
                "    for (var i = 0; i < cerrarModalesPrevios.length; i++) {" +
                "      if (cerrarModalesPrevios[i].offsetWidth > 0) { try { cerrarModalesPrevios[i].click(); } catch(e){} }" +
                "    }" +
                "  } catch(e){}" +
                "  function clicarBotonSi() {" +
                "    var bSi = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('.swal2-actions button, button, a')).find(function(el){" +
                "      if (!el || !el.innerText || el.offsetWidth <= 0) return false;" +
                "      var t = el.innerText.trim().toLowerCase();" +
                "      return t === 'si' || t === 'sí';" +
                "    });" +
                "    if (bSi && bSi.offsetWidth > 0) {" +
                "      window.__vbt_si_clicked = true;" +
                "      bSi.click();" +
                "      return true;" +
                "    }" +
                "    return false;" +
                "  }" +
                "  if (clicarBotonSi()) return;" +
                "  var disparado = false;" +
                "  if (typeof Logout === 'function') {" +
                "    try { Logout(); disparado = true; } catch(e){}" +
                "  }" +
                "  if (!disparado) {" +
                "    var btnSalir = document.querySelector(\"a[href='javascript:Logout()'], a[href*='Logout()'], a[onclick*='Logout']\");" +
                "    if (btnSalir) { try { btnSalir.click(); disparado = true; } catch(e){} }" +
                "  }" +
                "  var intentos = 0;" +
                "  var timerSi = setInterval(function(){" +
                "    intentos++;" +
                "    if (clicarBotonSi() || intentos > 25) {" +
                "      clearInterval(timerSi);" +
                "    }" +
                "  }, 60);" +
                "  setTimeout(function(){" +
                "    if (window.location.pathname.indexOf('/lg') === -1) {" +
                "      var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "      var inTok = document.querySelector('input[name=\"_token\"]');" +
                "      var tok = (meta && meta.getAttribute('content')) ? meta.getAttribute('content') : ((inTok && inTok.value) ? inTok.value : '" + csrfRespaldo + "');" +
                "      if (tok) {" +
                "        fetch('https://tesoroenlinea.bt.com.ve/logout', {" +
                "          method: 'POST'," +
                "          headers: {" +
                "            'Accept': 'application/json, text/javascript, */*; q=0.01'," +
                "            'X-Requested-With': 'XMLHttpRequest'," +
                "            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'," +
                "            'X-CSRF-TOKEN': tok" +
                "          }," +
                "          body: '_token=' + encodeURIComponent(tok)," +
                "          credentials: 'same-origin'" +
                "        }).then(function(r){" +
                "          if (r.status === 200 || r.status === 302 || r.redirected) {" +
                "            window.__vbt_logout_ok = true;" +
                "            window.location.replace('https://tesoroenlinea.bt.com.ve/lg');" +
                "          }" +
                "        }).catch(function(){});" +
                "      }" +
                "    }" +
                "  }, 2400);" +
                "})();";
        evaluarJS(s, jsLogout, null);
    }

    private void clicarOrdinariaJS(SesionBot s) {
        ultimoSlotActivoRed = s.slotIndex;
        String jsOrd =
                "var ejecutado = false;" +
                "if (typeof mod !== 'undefined' && typeof mod.add === 'function') {" +
                "  try { mod.add(1); ejecutado = true; } catch(e){}" +
                "}" +
                "if (!ejecutado) {" +
                "  var b = document.querySelector(\"a.btn-tesoro-green, a[href*='mod.add(1)']\") || Array.from(document.querySelectorAll('.card-header a, .card-header button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "  if(b) b.click();" +
                "}";
        evaluarJS(s, jsOrd, null);
    }

    private void irAIntervencionJS(SesionBot s) {
        ultimoSlotActivoRed = s.slotIndex;
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
                "  }, 400);" +
                "}";
        evaluarJS(s, jsMenu, null);
    }
}
