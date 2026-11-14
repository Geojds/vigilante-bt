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
import android.graphics.LinearGradient;
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
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
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
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String URL_LOGIN = "https://tesoroenlinea.bt.com.ve/lg";
    private static final String URL_GIF_LOADER = "https://tesoroenlinea.bt.com.ve/img/loader.gif?1792770684";
    private static final String ORIGEN_FONDOS = "Sueldos y Salarios";
    private static final String DESTINO_FONDOS = "Ahorro";
    private static final int MAX_CUENTAS = 4;
    private static final long TIMEOUT_CARGA_INT_MS = 12000L; // 12 segundos máximo esperando carga de intervención
    private final int[] OPCIONES_MINUTOS = {1, 3, 5, 10, 15, 20, 30};
    private final int[] ESCALAS_ZOOM = {60, 75, 85, 100};

    // Modos de Configuración Avanzada de Proxy (Exclusivos: solo 1 activo a la vez)
    private static final int MODO_PROXY_NINGUNO = -1;
    private static final int MODO_PROXY_POSICION_GLOBAL = 0; // Por defecto activado
    private static final int MODO_PROXY_SIEMPRE_ACTIVO = 1;
    private static final int MODO_PROXY_EN_CONTRASENA = 2;
    private static final int MODO_PROXY_EN_INTERVENCION = 3;
    private static final int MODO_PROXY_PERSONALIZADO_CUENTA = 4;

    // =========================================================================================
    // SISTEMA DE LICENCIAS EN LA NUBE (GITHUB licencias.json)
    // =========================================================================================
    private static final String GH_REPO_OWNER = "Geojds";
    private static final String GH_REPO_NAME = "vigilante-bt";
    private static final String GH_TOKEN_B64 = "";
    private static final String LICENCIA_USUARIO_CLIENTE = "geonaldiaz1";
    private static final String LICENCIA_CLAVE_CLIENTE = "Valera25**";
    private static final String LICENCIA_DEVICE_ID_FIJO = "LIBRE";
    private static final String CLAVE_MAESTRA_VENDEDOR = "geonal11";

    // =========================================================================================
    // CREDENCIALES DE PROXY RESIDENCIAL IPROYAL VENEZUELA
    // =========================================================================================
    private static final String PRX_H_B64 = "Z2VvLmlwcm95YWwuY29t";
    private static final int PRX_PORT = 12321;
    private static final String PRX_USER_RAW = "ftNVsXfGVrFN3Pq3";
    private static final String PRX_PASS_RAW = "SPWd2zTzmz3M8KNj";

    private ServerSocket servidorPuenteLocal;
    private int puertoPuenteLocal = 0;
    private volatile int ultimoSlotActivoRed = 0;
    private volatile InetAddress ipProxyCacheada = null;
    private volatile InetAddress ipBancoCacheada = null;
    private final ExecutorService poolHilosProxy = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });
    private final ConcurrentHashMap<String, byte[]> cacheEstaticosRam = new ConcurrentHashMap<>();

    // Contenedores principales y gestor de Swipe Horizontal interactivo
    private FrameLayout frameRoot;
    private LinearLayout mainVertical;
    private FrameLayout contenedorPaginasSwipe;
    private ScrollView vistaCuentasScroll;
    private LinearLayout contenedorTarjetasCuentas;
    private LinearLayout cardEstadoVacioCuentas;
    private TextView lblEmptyTitulo, lblEmptySub;

    private ScrollView vistaHistorialScroll;
    private LinearLayout vistaHistorialContainer;
    private LinearLayout barraFiltrosHistorial;
    private ScrollView scrollHistorialConsola;
    private TextView txtHistorialConsola;
    private LinearLayout contenedorComprasExitosas;
    private TextView lblTituloComprasExitosas, lblSubComprasExitosas;
    private ScrollView vistaAjustesScroll;
    private ScrollView vistaPerfilScroll;

    // Variables del gesto de deslizar entre pestañas (Swipe en tiempo real)
    private VelocityTracker velocityTrackerSwipe;
    private float touchStartX = 0f, touchStartY = 0f;
    private boolean deslizandoPestanaHorizontal = false;
    private boolean gestoDescartado = false;
    private int pestanaVecinaSwipe = -1;

    // Elementos de la Pestaña "Perfil"
    private LinearLayout cardPerfilPrincipal, boxPerfilDevice, boxPerfilCuentas, boxPerfilClave;
    private ImageView imgAvatarPerfil;
    private TextView lblNombrePerfil, lblValorDevicePerfil, lblValorCuentasPerfil, lblValorClavePerfil;
    private TextView btnMantenerMostrarClave, lblPieLicenciaPerfil;
    private Button btnCerrarSesionPerfil;

    // Pantalla de bloqueo / Login de Licencia del Cliente
    private LinearLayout loginClienteOverlay;
    private EditText edtUserLicGlobal, edtPassLicGlobal;
    private Button btnOjoLicGlobal;
    private CheckBox chkRecordarGlobal;
    private TextView lblEstadoValidacionLic;

    // Tarjetas y textos adaptables al tema
    private TextView lblTituloApp, lblCuentasBig, lblCuentasSub;
    private TextView lblHistBig, lblHistSub, lblAjustesBig, lblAjustesSub;
    private TextView lblTRel, lblReloginVal;
    private LinearLayout cardTiempos, cardTemaAjustes, cardAlertasAjustes;
    private TextView lblSubTema, lblDescTema, lblSubAlertas, lblDescAlertas;

    // Tarjetas de vista previa de los 3 temas (Oscuro | Azul Zafiro | Claro) + Barra Glass
    private LinearLayout cardPrevOscuro, cardPrevZafiro, cardPrevClaro;
    private ImageView imgPrevOscuro, imgPrevZafiro, imgPrevClaro;
    private TextView txtPrevOscuro, txtPrevZafiro, txtPrevClaro;
    private LinearLayout panelBarraGlass;
    private TextView lblItemBarraGlass, lblDescItemBarraGlass;
    private Button btnToggleBarraGlass;

    // Sub-paneles separados de Modo Turbo, Proxy VE, Alertas y Vibración con Barrita Corta
    private LinearLayout panelItemTurbo, panelItemProxy, panelItemOrd, panelItemCompra, panelItemVib;
    private TextView lblItemTurbo, lblDescItemTurbo;
    private TextView lblItemProxy, lblDescItemProxy;
    private TextView lblItemOrd, lblDescItemOrd;
    private TextView lblItemCompra, lblDescItemCompra;
    private TextView lblItemVib, lblDescItemVib, lblNivelVibValor;
    private Button btnToggleTurbo, btnToggleProxy, btnToggleOrd, btnToggleCompra;
    private SeekBar seekBarVibracion;

    // Desplegable de "Configuración avanzada" en Ajustes (Con los 5 modos incluyendo Personalizado por cuenta)
    private TextView btnTextoConfigAvanzada;
    private LinearLayout contenedorConfigAvanzada;
    private boolean configAvanzadaExpandida = false;
    private LinearLayout panelAdvPosGlobal, panelAdvSiempre, panelAdvPass, panelAdvIntervencion, panelAdvPorCuenta;
    private TextView lblAdvPosGlobal, lblDescAdvPosGlobal;
    private TextView lblAdvSiempre, lblDescAdvSiempre;
    private TextView lblAdvPass, lblDescAdvPass;
    private TextView lblAdvIntervencion, lblDescAdvIntervencion;
    private TextView lblAdvPorCuenta, lblDescAdvPorCuenta;
    private Button btnToggleAdvPosGlobal, btnToggleAdvSiempre, btnToggleAdvPass, btnToggleAdvIntervencion, btnToggleAdvPorCuenta;

    // Contenedor de motores en segundo plano cuando la tarjeta está cerrada
    private LinearLayout filaMotoresSup, filaMotoresInf;
    private final FrameLayout[] slotsFondoWebView = new FrameLayout[MAX_CUENTAS];

    // Cabecera, chips y controles
    private TextView badgeEstadoGlobal;
    private TextView chipRegistradas, chipActivas, chipDetenidas;
    private Button btnIniciarTodas, btnDetenerTodas, btnNuevaCuenta, btnLimpiarHist;

    // Barra flotante inferior con Efecto Glass iOS Real, Cápsula Deslizante y Ocultado Progresivo
    private FrameLayout contenedorBarraFlotante;
    private ImageView imgFondoBlurGlass;
    private View vistaBrilloEspecularGlass;
    private View indicadorDeslizanteBarra;
    private LinearLayout barraFlotante;
    private LinearLayout navBtnCuentas, navBtnHistorial, navBtnAjustes, navBtnPerfil;
    private ImageView navIconCuentas, navIconHistorial, navIconAjustes, navIconPerfil;
    private TextView navTxtCuentas, navTxtHistorial, navTxtAjustes, navTxtPerfil;
    private float traduccionBarraY = 0f;
    private float acumuladorBajandoScroll = 0f;
    private long ultimoRenderGlassMs = 0;

    // 0 = Cuentas, 1 = Historial, 2 = Ajustes, 3 = Perfil
    private int pestanaInferiorActual = 0;
    private int filtroHistorialSlot = -1;

    private int temaActual = 0;
    private boolean barraGlassActiva = false;
    private boolean modoTurboSinImagenes = true;
    private boolean proxyRotativoActivo = true;
    private int modoAvanzadoProxy = MODO_PROXY_POSICION_GLOBAL;
    private boolean alertaOrdinariaActiva = true;
    private boolean alertaCompraActiva = true;
    private int intensidadVibracion = 85; // 0 = Apagado, 1..100 = Intensidad progresiva

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
        ACTIVANDO_PROXY_EN_PASS,
        ENVIANDO_FASE2,
        ESPERANDO_CARGA_INICIO,
        ESPERANDO_REINTENTO_SESION_ACTIVA,
        ESPERANDO_ESTABILIZAR_INICIO,
        ACTIVANDO_PROXY_EN_PG,
        VIGILANDO_CARGA_INT,
        ACTIVANDO_PROXY_EN_INT,
        PAUSA_REINICIO_INT,
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
        volatile boolean proxyActivoEnEstaCuenta = false;
        volatile boolean proxyVerificadoOk = false;
        volatile boolean verificandoConexionProxy = false;
        boolean yaActivoProxyEnIntervencion = false;
        boolean sesionAbiertaEnServidor = false;
        boolean yaAnuncioPaginaLoginCargada = false;
        boolean nombreOculto = false;

        int modoProxyIndividual = MODO_PROXY_POSICION_GLOBAL;
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
        long ultimoKeepAliveMs = 0;
        long tiempoCompraExitosaMs = 0;
        long inicioPeticionIntMs = 0;
        String ultimoPasoTexto = "Sin actividad todavía";
        String urlActualTexto = URL_LOGIN;

        final List<Socket> socketsAbiertos = Collections.synchronizedList(new ArrayList<>());
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
        boolean esperandoCargaNuevaInt = false;
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

        int obtenerModoProxyEfectivo() {
            if (modoAvanzadoProxy == MODO_PROXY_PERSONALIZADO_CUENTA) {
                return modoProxyIndividual;
            }
            return modoAvanzadoProxy;
        }

        String nombreCortoModoIndividual() {
            switch (modoProxyIndividual) {
                case MODO_PROXY_SIEMPRE_ACTIVO: return "Siempre activo";
                case MODO_PROXY_EN_CONTRASENA: return "En contraseña";
                case MODO_PROXY_EN_INTERVENCION: return "En intervención";
                case MODO_PROXY_POSICION_GLOBAL:
                default: return "Posición global";
            }
        }

        String codigoNumericoTipoDoc() {
            switch (tipoDoc.toUpperCase(Locale.ROOT)) {
                case "P": return "02";
                case "J": return "04";
                case "R": return "05";
                case "E": return "08";
                case "G": return "10";
                case "C": return "11";
                case "V":
                default: return "01";
            }
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
            int modoEf = obtenerModoProxyEfectivo();
            if (!proxyRotativoActivo || modoEf == MODO_PROXY_NINGUNO) {
                return "Conexión Directa Wi-Fi (Proxy desactivado)";
            }
            String idCorto = idSesionProxy.length() >= 6 ? idSesionProxy.substring(0, 6).toUpperCase() : idSesionProxy.toUpperCase();
            String etiquetaModo = (modoAvanzadoProxy == MODO_PROXY_PERSONALIZADO_CUENTA)
                    ? (" [" + nombreCortoModoIndividual() + "]")
                    : "";
            if (estaCorriendo() && !proxyActivoEnEstaCuenta && modoEf != MODO_PROXY_SIEMPRE_ACTIVO) {
                if (modoEf == MODO_PROXY_EN_CONTRASENA) {
                    return "Wi-Fi Normal (Activará Proxy #" + idCorto + " en Contraseña)" + etiquetaModo;
                } else if (modoEf == MODO_PROXY_EN_INTERVENCION) {
                    return "Wi-Fi Normal (Activará Proxy #" + idCorto + " en Intervención)" + etiquetaModo;
                }
                return "Wi-Fi Normal (Activará Proxy #" + idCorto + " en Posición Global)" + etiquetaModo;
            }
            return "Proxy VE Activo • IP #" + idCorto + " • Cambios IP: " + contadorRotacionesIp + etiquetaModo;
        }

        boolean estaEnFormularioCompra() {
            return estaCorriendo() && (estadoActual == EstadoBot.ESPERANDO_ESTABILIZAR_FORM
                    || estadoActual == EstadoBot.LLENANDO_COMPRA
                    || estadoActual == EstadoBot.COMPRANDO);
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
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        } catch (Throwable ignored) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(false);
            }
        } catch (Exception ignored) {}

        prefs = getSharedPreferences("VigilanteBTPrefs", Context.MODE_PRIVATE);

        temaActual = prefs.getInt("tema_app_v2", 0);
        barraGlassActiva = prefs.getBoolean("barra_glass_iphone_on", false);
        modoTurboSinImagenes = prefs.getBoolean("modo_turbo_default_on", true);
        proxyRotativoActivo = prefs.getBoolean("proxy_iproyal_on", true);
        modoAvanzadoProxy = prefs.getInt("modo_avanzado_proxy_v1", MODO_PROXY_POSICION_GLOBAL);
        alertaOrdinariaActiva = prefs.getBoolean("alerta_ord", true);
        alertaCompraActiva = prefs.getBoolean("alerta_compra", true);
        intensidadVibracion = prefs.getInt("intensidad_vibracion_v1", 85);

        for (int i = 0; i < MAX_CUENTAS; i++) {
            sesiones[i] = new SesionBot(i);
            sesiones[i].mostrarConsolaEnPanel = prefs.getBoolean("mostrar_consola_slot_" + i, true);
            sesiones[i].modoProxyIndividual = prefs.getInt("modo_proxy_indiv_slot_" + i, MODO_PROXY_POSICION_GLOBAL);
            int zGuardado = prefs.getInt("zoom_web_v2_slot_" + i, 75);
            if (zGuardado < 60 || zGuardado > 100) zGuardado = 75;
            sesiones[i].zoomPorcentaje = zGuardado;
        }

        iniciarPuenteProxyLocalIPRoyal();
        construirInterfazCompleta();
        inicializarMotoresOcultos();
        vincularPuenteLocalAlWebView();
        registrarMonitorPasivoRed();
        concentrarRedAlMaximoEnEstePrograma(true);
        cargarCuentasGuardadas();
        cargarComprasExitosasGuardadas();
        aplicarTemaVisual();
        iniciarCicloReloj();
    }

    @Override
    protected void onDestroy() {
        try {
            concentrarRedAlMaximoEnEstePrograma(false);
        } catch (Exception ignored) {}

        try {
            if (servidorPuenteLocal != null && !servidorPuenteLocal.isClosed()) {
                servidorPuenteLocal.close();
            }
        } catch (Exception ignored) {}

        try {
            poolHilosProxy.shutdownNow();
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
    // CONCENTRACIÓN DEL 95% DEL RENDIMIENTO DE RED EN ESTE PROGRAMA CUANDO ESTÁ PRENDIDO
    // =========================================================================================
    private void concentrarRedAlMaximoEnEstePrograma(boolean activarPrioridadMaxima) {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            if (!activarPrioridadMaxima) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cm.bindProcessToNetwork(null);
                }
                return;
            }

            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            TrafficStats.setThreadStatsTag(0xB795);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network redActiva = cm.getActiveNetwork();
                if (redActiva != null) {
                    cm.bindProcessToNetwork(redActiva);
                }
                NetworkRequest req = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .build();
                cm.requestNetwork(req, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        try {
                            if (contarCuentasCorriendo() > 0) {
                                cm.bindProcessToNetwork(network);
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            }

            for (int i = 0; i < MAX_CUENTAS; i++) {
                if (sesiones[i] != null && sesiones[i].webViewOculto != null) {
                    try {
                        sesiones[i].webViewOculto.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private void configurarSocketPrioridadMaxima(Socket s) {
        if (s == null) return;
        try {
            s.setPerformancePreferences(0, 2, 1); // Prioridad absoluta a baja latencia y ancho de banda
            s.setTcpNoDelay(true);
            s.setKeepAlive(true);
            s.setTrafficClass(0x10 | 0x08); // IPTOS_LOWDELAY | IPTOS_THROUGHPUT
            s.setReceiveBufferSize(131072);
            s.setSendBufferSize(131072);
        } catch (Throwable ignored) {}
    }

    // =========================================================================================
    // MOTOR DE RED HÍBRIDO CON MODOS GLOBALES Y MODO PERSONALIZADO POR CUENTA
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

    private void cerrarSocketsDeSesion(SesionBot s) {
        if (s == null) return;
        synchronized (s.socketsAbiertos) {
            for (Socket sock : s.socketsAbiertos) {
                try {
                    if (sock != null && !sock.isClosed()) {
                        sock.close();
                    }
                } catch (Exception ignored) {}
            }
            s.socketsAbiertos.clear();
        }
    }

    private void usarWifiNormalEnCuenta(final SesionBot s) {
        if (s == null) return;
        s.proxyActivoEnEstaCuenta = false;
        s.proxyVerificadoOk = false;
        s.verificandoConexionProxy = false;
        ultimoSlotActivoRed = s.slotIndex;
        cerrarSocketsDeSesion(s);
        runOnUiThread(() -> {
            vincularPuenteLocalAlWebView();
            actualizarVisualTarjeta(s);
        });
    }

    private void activarModoProxySiempreActivoEnCuenta(final SesionBot s) {
        if (s == null) return;
        s.proxyActivoEnEstaCuenta = true;
        s.proxyVerificadoOk = true;
        s.verificandoConexionProxy = false;
        ultimoSlotActivoRed = s.slotIndex;
        cerrarSocketsDeSesion(s);
        runOnUiThread(() -> {
            vincularPuenteLocalAlWebView();
            actualizarVisualTarjeta(s);
        });
    }

    private void activarProxyYCambiarIpConVerificacion(final SesionBot s, final String etapaDondeActiva) {
        if (s == null) return;
        int modoEf = s.obtenerModoProxyEfectivo();
        if (!proxyRotativoActivo || modoEf == MODO_PROXY_NINGUNO) {
            s.proxyActivoEnEstaCuenta = false;
            s.proxyVerificadoOk = true;
            s.verificandoConexionProxy = false;
            return;
        }
        s.idSesionProxy = generarIdSesionProxyNuevo();
        s.contadorRotacionesIp++;
        s.proxyActivoEnEstaCuenta = true;
        s.proxyVerificadoOk = false;
        s.verificandoConexionProxy = true;
        ultimoSlotActivoRed = s.slotIndex;

        cerrarSocketsDeSesion(s);
        runOnUiThread(() -> {
            vincularPuenteLocalAlWebView();
            actualizarVisualTarjeta(s);
        });

        final String idCorto = s.idSesionProxy.substring(0, 6).toUpperCase();
        registrarPasoCuenta(s, "Activando Proxy en " + etapaDondeActiva + " y cambiando IP (#" + idCorto + ")...");

        poolHilosProxy.execute(() -> {
            boolean conectado = false;
            for (int intento = 1; intento <= 2; intento++) {
                Socket testSock = null;
                try {
                    testSock = new Socket();
                    configurarSocketPrioridadMaxima(testSock);
                    InetSocketAddress ep = (ipProxyCacheada != null)
                            ? new InetSocketAddress(ipProxyCacheada, PRX_PORT)
                            : new InetSocketAddress(obtenerHostProxyIPRoyal(), PRX_PORT);
                    testSock.connect(ep, 4200);
                    testSock.setSoTimeout(4500);

                    String authRaw = PRX_USER_RAW + ":" + construirPasswordProxyVenezuela(s);
                    String authB64 = Base64.encodeToString(authRaw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    String connectHandshake = "CONNECT tesoroenlinea.bt.com.ve:443 HTTP/1.1\r\n"
                            + "Host: tesoroenlinea.bt.com.ve:443\r\n"
                            + "Proxy-Authorization: Basic " + authB64 + "\r\n"
                            + "Proxy-Connection: Keep-Alive\r\n\r\n";

                    OutputStream os = testSock.getOutputStream();
                    os.write(connectHandshake.getBytes(StandardCharsets.ISO_8859_1));
                    os.flush();

                    BufferedReader br = new BufferedReader(new InputStreamReader(testSock.getInputStream(), StandardCharsets.ISO_8859_1));
                    String statusLine = br.readLine();
                    if (statusLine != null && statusLine.contains("200")) {
                        conectado = true;
                        testSock.close();
                        break;
                    }
                } catch (Exception ignored) {
                } finally {
                    try { if (testSock != null) testSock.close(); } catch (Exception ignored) {}
                }
                s.idSesionProxy = generarIdSesionProxyNuevo();
            }

            s.verificandoConexionProxy = false;
            s.proxyVerificadoOk = true;
            cerrarSocketsDeSesion(s);
            final String idFinal = s.idSesionProxy.substring(0, 6).toUpperCase();
            final boolean okFinal = conectado;
            runOnUiThread(() -> {
                vincularPuenteLocalAlWebView();
                actualizarVisualTarjeta(s);
                if (okFinal) {
                    registrarPasoCuenta(s, "Proxy activo e IP cambiada (#" + idFinal + ") con conexión verificada");
                } else {
                    registrarPasoCuenta(s, "IP cambiada (#" + idFinal + ") lista para continuar");
                }
            });
        });
    }

    private void limpiarTodosLosDatosDeSesionAnterior(final SesionBot s, String motivo, boolean rotarIpSiSiempreActivo) {
        if (s == null) return;
        int modoEf = s.obtenerModoProxyEfectivo();
        if (rotarIpSiSiempreActivo && proxyRotativoActivo && modoEf == MODO_PROXY_SIEMPRE_ACTIVO) {
            s.idSesionProxy = generarIdSesionProxyNuevo();
            s.contadorRotacionesIp++;
            s.proxyActivoEnEstaCuenta = true;
        } else {
            s.proxyActivoEnEstaCuenta = false;
        }
        s.proxyVerificadoOk = false;
        s.verificandoConexionProxy = false;
        s.yaActivoProxyEnIntervencion = false;
        ultimoSlotActivoRed = s.slotIndex;
        cerrarSocketsDeSesion(s);

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
                    s.webViewOculto.clearHistory();
                    s.webViewOculto.clearCache(false);
                    evaluarJS(s,
                            "try { window.sessionStorage.clear(); } catch(e){}" +
                            "try { window.localStorage.clear(); } catch(e){}" +
                            "try { document.cookie.split(';').forEach(function(c) { document.cookie = c.replace(/^ +/, '').replace(/=.*/, '=;expires=' + new Date().toUTCString() + ';path=/'); }); } catch(e){}",
                            null);
                } catch (Exception ignored) {}
            }
            vincularPuenteLocalAlWebView();
            actualizarVisualTarjeta(s);
        });

        registrarPasoCuenta(s, "Datos y cookies de la sesión anterior borrados (" + motivo + ")");
    }

    private WebResourceResponse obtenerArchivoEstaticoDesdeRamODirecto(String urlStr) {
        try {
            String cleanKey = urlStr;
            String low = urlStr.toLowerCase(Locale.ROOT);
            String pathOnly = low;
            int qIdx = pathOnly.indexOf('?');
            if (qIdx != -1) pathOnly = pathOnly.substring(0, qIdx);
            String mime = pathOnly.endsWith(".css") ? "text/css" : (pathOnly.endsWith(".json") ? "application/json" : "application/javascript");

            byte[] enCache = cacheEstaticosRam.get(cleanKey);
            if (enCache != null && enCache.length > 0) {
                return new WebResourceResponse(mime, "UTF-8", new ByteArrayInputStream(enCache));
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(Proxy.NO_PROXY);
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(4500);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36");

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                is.close();
                byte[] bytesDescargados = bos.toByteArray();
                if (bytesDescargados.length > 0) {
                    cacheEstaticosRam.put(cleanKey, bytesDescargados);
                    return new WebResourceResponse(mime, "UTF-8", new ByteArrayInputStream(bytesDescargados));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void iniciarPuenteProxyLocalIPRoyal() {
        poolHilosProxy.execute(() -> {
            try {
                ipProxyCacheada = InetAddress.getByName(obtenerHostProxyIPRoyal());
            } catch (Exception ignored) {}
            try {
                ipBancoCacheada = InetAddress.getByName("tesoroenlinea.bt.com.ve");
            } catch (Exception ignored) {}

            try {
                servidorPuenteLocal = new ServerSocket(0, 256);
                puertoPuenteLocal = servidorPuenteLocal.getLocalPort();
                runOnUiThread(this::vincularPuenteLocalAlWebView);

                while (!servidorPuenteLocal.isClosed()) {
                    final Socket cliente = servidorPuenteLocal.accept();
                    poolHilosProxy.execute(() -> manejarConexionPuenteHibrido(cliente));
                }
            } catch (Exception ignored) {}
        });
    }

    private void manejarConexionPuenteHibrido(Socket clienteLocal) {
        Socket socketRemoto = null;
        SesionBot sesionRef = null;
        try {
            TrafficStats.setThreadStatsTag(0xB795);
            configurarSocketPrioridadMaxima(clienteLocal);
            clienteLocal.setSoTimeout(15000);

            InputStream inCliente = clienteLocal.getInputStream();
            OutputStream outCliente = clienteLocal.getOutputStream();

            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream(1024);
            byte[] chunk = new byte[4096];
            int finHeadersIdx = -1;

            while (headerBuf.size() < 16384) {
                int leidos = inCliente.read(chunk);
                if (leidos == -1) break;
                headerBuf.write(chunk, 0, leidos);
                byte[] acumulado = headerBuf.toByteArray();
                for (int i = 0; i <= acumulado.length - 4; i++) {
                    if (acumulado[i] == '\r' && acumulado[i + 1] == '\n'
                            && acumulado[i + 2] == '\r' && acumulado[i + 3] == '\n') {
                        finHeadersIdx = i + 4;
                        break;
                    }
                }
                if (finHeadersIdx != -1) break;
            }

            byte[] totalLeido = headerBuf.toByteArray();
            if (totalLeido.length == 0) {
                clienteLocal.close();
                return;
            }

            int idxSlot = Math.max(0, Math.min(MAX_CUENTAS - 1, ultimoSlotActivoRed));
            sesionRef = sesiones[idxSlot];
            if (sesionRef != null) {
                sesionRef.socketsAbiertos.add(clienteLocal);
            }

            int modoEf = (sesionRef != null) ? sesionRef.obtenerModoProxyEfectivo() : modoAvanzadoProxy;
            boolean usarTunelProxy = proxyRotativoActivo
                    && (modoEf != MODO_PROXY_NINGUNO)
                    && (modoEf == MODO_PROXY_SIEMPRE_ACTIVO
                        || (sesionRef != null && sesionRef.proxyActivoEnEstaCuenta));

            int corteHeader = (finHeadersIdx != -1) ? finHeadersIdx : totalLeido.length;
            String rawStr = new String(totalLeido, 0, corteHeader, StandardCharsets.ISO_8859_1);
            int finPrimeraLinea = rawStr.indexOf("\r\n");
            String primeraLinea = (finPrimeraLinea != -1) ? rawStr.substring(0, finPrimeraLinea) : rawStr;

            // MODO 1: CONEXIÓN CON WI-FI / INTERNET NORMAL DE MÁXIMO RENDIMIENTO
            if (!usarTunelProxy && primeraLinea.toUpperCase(Locale.ROOT).startsWith("CONNECT ")) {
                String[] partes = primeraLinea.split(" ");
                String hostPort = (partes.length > 1) ? partes[1].trim() : "tesoroenlinea.bt.com.ve:443";
                String hostDestino = "tesoroenlinea.bt.com.ve";
                int portDestino = 443;
                int idxDosPuntos = hostPort.indexOf(':');
                if (idxDosPuntos != -1) {
                    hostDestino = hostPort.substring(0, idxDosPuntos).trim();
                    try {
                        portDestino = Integer.parseInt(hostPort.substring(idxDosPuntos + 1).trim());
                    } catch (Exception ignored) {
                        portDestino = 443;
                    }
                } else if (!hostPort.isEmpty()) {
                    hostDestino = hostPort;
                }

                socketRemoto = new Socket();
                configurarSocketPrioridadMaxima(socketRemoto);

                InetSocketAddress epDirecto;
                if (hostDestino.equalsIgnoreCase("tesoroenlinea.bt.com.ve") && ipBancoCacheada != null) {
                    epDirecto = new InetSocketAddress(ipBancoCacheada, portDestino);
                } else {
                    epDirecto = new InetSocketAddress(hostDestino, portDestino);
                }
                socketRemoto.connect(epDirecto, 6500);
                socketRemoto.setSoTimeout(20000);
                if (sesionRef != null) {
                    sesionRef.socketsAbiertos.add(socketRemoto);
                }

                outCliente.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                outCliente.flush();

                final InputStream inRemoto = socketRemoto.getInputStream();
                final OutputStream outRemoto = socketRemoto.getOutputStream();
                final Socket sockCliFinal = clienteLocal;
                final Socket sockRemFinal = socketRemoto;

                poolHilosProxy.execute(() -> copiarFlujoSocketTurbo(inCliente, outRemoto, sockCliFinal, sockRemFinal));
                copiarFlujoSocketTurbo(inRemoto, outCliente, sockCliFinal, sockRemFinal);
                return;
            }

            // MODO 2: TÚNEL PROXY IPROYAL VENEZUELA CON PRIORIDAD DE TRÁFICO
            String authRaw = PRX_USER_RAW + ":" + construirPasswordProxyVenezuela(sesionRef);
            String authB64 = Base64.encodeToString(authRaw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

            String reqModificada;
            if (finPrimeraLinea != -1) {
                reqModificada = rawStr.substring(0, finPrimeraLinea + 2)
                        + "Proxy-Authorization: Basic " + authB64 + "\r\n"
                        + "Proxy-Connection: Keep-Alive\r\n"
                        + rawStr.substring(finPrimeraLinea + 2);
            } else {
                reqModificada = rawStr;
            }

            socketRemoto = new Socket();
            configurarSocketPrioridadMaxima(socketRemoto);

            InetSocketAddress endpointRemoto;
            if (ipProxyCacheada != null) {
                endpointRemoto = new InetSocketAddress(ipProxyCacheada, PRX_PORT);
            } else {
                ipProxyCacheada = InetAddress.getByName(obtenerHostProxyIPRoyal());
                endpointRemoto = new InetSocketAddress(ipProxyCacheada, PRX_PORT);
            }
            socketRemoto.connect(endpointRemoto, 7000);
            socketRemoto.setSoTimeout(18000);
            if (sesionRef != null) {
                sesionRef.socketsAbiertos.add(socketRemoto);
            }

            final InputStream inRemoto = socketRemoto.getInputStream();
            final OutputStream outRemoto = socketRemoto.getOutputStream();

            outRemoto.write(reqModificada.getBytes(StandardCharsets.ISO_8859_1));
            if (totalLeido.length > corteHeader) {
                outRemoto.write(totalLeido, corteHeader, totalLeido.length - corteHeader);
            }
            outRemoto.flush();

            final Socket sockCliFinal = clienteLocal;
            final Socket sockRemFinal = socketRemoto;

            poolHilosProxy.execute(() -> copiarFlujoSocketTurbo(inCliente, outRemoto, sockCliFinal, sockRemFinal));
            copiarFlujoSocketTurbo(inRemoto, outCliente, sockCliFinal, sockRemFinal);
        } catch (Exception e) {
            try { if (clienteLocal != null) clienteLocal.close(); } catch (Exception ignored) {}
            try { if (socketRemoto != null) socketRemoto.close(); } catch (Exception ignored) {}
        } finally {
            if (sesionRef != null) {
                sesionRef.socketsAbiertos.remove(clienteLocal);
                if (socketRemoto != null) {
                    sesionRef.socketsAbiertos.remove(socketRemoto);
                }
            }
        }
    }

    private void copiarFlujoSocketTurbo(InputStream in, OutputStream out, Socket s1, Socket s2) {
        try {
            byte[] buf = new byte[65536];
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

    private void vincularPuenteLocalAlWebView() {
        if (puertoPuenteLocal <= 0) return;
        String hostFinal = "127.0.0.1";
        int portFinal = puertoPuenteLocal;

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

    // =========================================================================================
    // VALIDACIÓN EN TIEMPO REAL EN LA NUBE (GITHUB licencias.json) + CANDADO Y PERFIL
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
            if (GH_TOKEN_B64.matches("^[0-9a-fA-F]+$") && GH_TOKEN_B64.length() % 2 == 0) {
                int len = GH_TOKEN_B64.length();
                byte[] out = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    out[i / 2] = (byte) (Integer.parseInt(GH_TOKEN_B64.substring(i, i + 2), 16) ^ 0x5A);
                }
                return new String(out, StandardCharsets.UTF_8).trim();
            }
            return new String(Base64.decode(GH_TOKEN_B64, Base64.DEFAULT), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void mostrarAlertaCuentaDesactivada(String detalleExtra) {
        runOnUiThread(() -> {
            detenerTodasLasCuentas();
            if (loginClienteOverlay != null) {
                loginClienteOverlay.setVisibility(View.VISIBLE);
            }
            if (contenedorBarraFlotante != null) {
                contenedorBarraFlotante.setVisibility(View.GONE);
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

    private void guardarDatosExitoLoginLicencia(String usr, String clv, boolean recordar, String deviceId, String fechaCreacionLic) {
        String ahoraStr = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date());
        String fechaCreadaPrev = prefs.getString("fecha_creacion_lic_v3", "");
        String fechaCreadaFinal = (fechaCreacionLic != null && !fechaCreacionLic.isEmpty())
                ? fechaCreacionLic
                : (!fechaCreadaPrev.isEmpty() ? fechaCreadaPrev : ahoraStr);

        prefs.edit()
                .putBoolean("sesion_cliente_recordada_v3", recordar)
                .putString("usr_licencia_activa_v3", usr)
                .putString("clv_licencia_activa_v3", clv)
                .putString("device_id_vinculado_v3", deviceId)
                .putString("fecha_creacion_lic_v3", fechaCreadaFinal)
                .putString("ultima_conexion_lic_v3", ahoraStr)
                .apply();

        refrescarDatosVistaPerfil();
    }

    private void validarCredencialesEnNubeOLocal(final String usrInput, final String clvInput, final boolean recordarChecked, final boolean esChequeoSilencioso) {
        final String miDeviceId = obtenerDeviceIdUnico();

        if (clvInput.equals(CLAVE_MAESTRA_VENDEDOR)) {
            guardarDatosExitoLoginLicencia(usrInput, clvInput, recordarChecked, miDeviceId, "");
            if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
            if (contenedorBarraFlotante != null) contenedorBarraFlotante.setVisibility(View.VISIBLE);
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
                    HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection(Proxy.NO_PROXY);
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

                        final String fechaCreacionNube = usuarioEncontrado.optString("fecha", "");

                        if (deviceRegistrado.equalsIgnoreCase("LIBRE") && !tokenGh.isEmpty() && !shaArchivo.isEmpty()) {
                            try {
                                usuarioEncontrado.put("device", miDeviceId);
                                String nuevoContenidoB64 = Base64.encodeToString(
                                        listaUsuarios.toString(2).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                                JSONObject bodyPut = new JSONObject();
                                bodyPut.put("message", "Vincular dispositivo " + usrInput + " [skip ci]");
                                bodyPut.put("content", nuevoContenidoB64);
                                bodyPut.put("sha", shaArchivo);

                                HttpURLConnection putConn = (HttpURLConnection) new URL(apiUrl).openConnection(Proxy.NO_PROXY);
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
                            if (!esChequeoSilencioso) {
                                guardarDatosExitoLoginLicencia(usrInput, clvInput, recordarChecked, miDeviceId, fechaCreacionNube);
                            } else if (!fechaCreacionNube.isEmpty()) {
                                prefs.edit().putString("fecha_creacion_lic_v3", fechaCreacionNube).apply();
                                refrescarDatosVistaPerfil();
                            }
                            if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
                            if (contenedorBarraFlotante != null) contenedorBarraFlotante.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                } catch (Exception ignored) {}
            }

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

                    guardarDatosExitoLoginLicencia(usrInput, clvInput, recordarChecked, miDeviceId, "");
                    if (loginClienteOverlay != null) loginClienteOverlay.setVisibility(View.GONE);
                    if (contenedorBarraFlotante != null) contenedorBarraFlotante.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private LinearLayout construirOverlayLoginCliente() {
        final String miDeviceId = obtenerDeviceIdUnico();
        boolean yaRecordado = prefs.getBoolean("sesion_cliente_recordada_v3", false);
        String usrGuardado = prefs.getString("usr_licencia_activa_v3", "");
        String clvGuardada = prefs.getString("clv_licencia_activa_v3", "");

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(Color.parseColor("#0B0F19"));
        overlay.setPadding(dp(24), dp(24), dp(24), dp(24));
        overlay.setClickable(true);
        overlay.setElevation(dp(90));
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        TextView tit = crearTexto("Acceso Vigilante BT", "#F8FAFC", 19f, true);
        tit.setGravity(Gravity.CENTER);
        cardLogin.addView(tit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sub = crearTexto("Ingresa tu usuario y contraseña para continuar.", "#93C5FD", 11.5f, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(14));
        cardLogin.addView(sub, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

        edtUserLicGlobal = new EditText(this);
        edtUserLicGlobal.setHint("Usuario");
        edtUserLicGlobal.setTextSize(13.5f);
        edtUserLicGlobal.setHintTextColor(Color.parseColor("#64748B"));
        edtUserLicGlobal.setTextColor(Color.parseColor("#F8FAFC"));
        edtUserLicGlobal.setSingleLine(true);
        edtUserLicGlobal.setBackground(fondoRedondeadoConBorde("#0B0F19", "#334155", 10, 1));
        edtUserLicGlobal.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (yaRecordado && !usrGuardado.isEmpty()) {
            edtUserLicGlobal.setText(usrGuardado);
        }
        LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        lpInput.setMargins(0, 0, 0, dp(10));
        cardLogin.addView(edtUserLicGlobal, lpInput);

        LinearLayout filaPassLic = new LinearLayout(this);
        filaPassLic.setOrientation(LinearLayout.HORIZONTAL);
        filaPassLic.setGravity(Gravity.CENTER_VERTICAL);

        edtPassLicGlobal = new EditText(this);
        edtPassLicGlobal.setHint("Contraseña");
        edtPassLicGlobal.setTextSize(13.5f);
        edtPassLicGlobal.setHintTextColor(Color.parseColor("#64748B"));
        edtPassLicGlobal.setTextColor(Color.parseColor("#F8FAFC"));
        edtPassLicGlobal.setSingleLine(true);
        edtPassLicGlobal.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtPassLicGlobal.setTransformationMethod(PasswordTransformationMethod.getInstance());
        edtPassLicGlobal.setBackground(fondoRedondeadoConBorde("#0B0F19", "#334155", 10, 1));
        edtPassLicGlobal.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (yaRecordado && !clvGuardada.isEmpty()) {
            edtPassLicGlobal.setText(clvGuardada);
        }

        btnOjoLicGlobal = crearBotonConIcono("EYE_OFF", "", "#1E293B", "#F8FAFC", 12, null);
        btnOjoLicGlobal.setOnClickListener(v -> toggleCampoOjo(edtPassLicGlobal, btnOjoLicGlobal));

        LinearLayout.LayoutParams lpEdtPass = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lpEdtPass.setMargins(0, 0, dp(6), 0);
        filaPassLic.addView(edtPassLicGlobal, lpEdtPass);
        filaPassLic.addView(btnOjoLicGlobal, new LinearLayout.LayoutParams(dp(40), dp(40)));
        cardLogin.addView(filaPassLic, lpInput);

        chkRecordarGlobal = new CheckBox(this);
        chkRecordarGlobal.setText("Recordar siempre");
        chkRecordarGlobal.setTextColor(Color.parseColor("#E2E8F0"));
        chkRecordarGlobal.setTextSize(12f);
        chkRecordarGlobal.setChecked(yaRecordado || usrGuardado.isEmpty());
        LinearLayout.LayoutParams lpChk = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpChk.setMargins(0, dp(2), 0, dp(10));
        cardLogin.addView(chkRecordarGlobal, lpChk);

        lblEstadoValidacionLic = crearTexto("", "#38BDF8", 11f, true);
        lblEstadoValidacionLic.setGravity(Gravity.CENTER);
        lblEstadoValidacionLic.setVisibility(View.GONE);
        lblEstadoValidacionLic.setPadding(0, 0, 0, dp(8));
        cardLogin.addView(lblEstadoValidacionLic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button btnEntrarLic = crearBotonConIcono("CHECK", "Iniciar sesión", "#2563EB", "#FFFFFF", 13, () -> {
            String uIngresado = edtUserLicGlobal.getText().toString().trim();
            String pIngresada = edtPassLicGlobal.getText().toString().trim();
            if (uIngresado.isEmpty() || pIngresada.isEmpty()) {
                Toast.makeText(this, "Ingresa tu usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            validarCredencialesEnNubeOLocal(uIngresado, pIngresada, chkRecordarGlobal.isChecked(), false);
        });
        cardLogin.addView(btnEntrarLic, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        overlay.addView(cardLogin, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return overlay;
    }

    private void cerrarSesionDeLicenciaActual() {
        detenerTodasLasCuentas();
        boolean recordar = prefs.getBoolean("sesion_cliente_recordada_v3", true);
        String uGuardado = prefs.getString("usr_licencia_activa_v3", "");
        String cGuardada = prefs.getString("clv_licencia_activa_v3", "");

        if (edtUserLicGlobal != null) {
            edtUserLicGlobal.setText(recordar ? uGuardado : "");
        }
        if (edtPassLicGlobal != null) {
            edtPassLicGlobal.setText(recordar ? cGuardada : "");
            edtPassLicGlobal.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            edtPassLicGlobal.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        if (btnOjoLicGlobal != null) {
            btnOjoLicGlobal.setBackground(fondoRedondeado("#1E293B", 6));
            ponerIconoYTexto(btnOjoLicGlobal, crearIconoVectorial("EYE_OFF", "#F8FAFC", 14), "");
        }
        if (contenedorBarraFlotante != null) {
            contenedorBarraFlotante.setVisibility(View.GONE);
        }
        if (loginClienteOverlay != null) {
            loginClienteOverlay.setVisibility(View.VISIBLE);
        }
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

    private GradientDrawable fondoBordePunteado(String colorHex, String bordeHex, int radioDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(colorHex));
        gd.setCornerRadius(dp(radioDp));
        gd.setStroke(dp(1), Color.parseColor(bordeHex), dp(6), dp(4));
        return gd;
    }

    /**
     * Acabado Cristal Esmerilado (iOS Frosted Blur) idéntico a las carpetas de iPhone:
     * Superficie lechosa translúcida con borde especular brillante arriba.
     */
    private GradientDrawable fondoCristalIphone(boolean esTemaClaro) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                esTemaClaro
                        ? new int[]{Color.parseColor("#78FFFFFF"), Color.parseColor("#48E2E8F0")}
                        : new int[]{Color.parseColor("#54FFFFFF"), Color.parseColor("#2494A3B8")}
        );
        gd.setCornerRadius(dp(24));
        gd.setStroke(dp(1), Color.parseColor(esTemaClaro ? "#CCFFFFFF" : "#80FFFFFF"));
        return gd;
    }

    private GradientDrawable fondoCapsulaActivaGlass(boolean esTemaClaro) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                esTemaClaro
                        ? new int[]{Color.parseColor("#AA0284C7"), Color.parseColor("#880369A1")}
                        : new int[]{Color.parseColor("#66FFFFFF"), Color.parseColor("#33CBD5E1")}
        );
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), Color.parseColor("#99FFFFFF"));
        return gd;
    }

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
    // VIBRACIÓN REGULABLE CON BARRITA CORTA (0% APAGADO A 100% MÁXIMO) + SONIDOS
    // =========================================================================================
    private String textoNivelVibracion(int nivel) {
        if (nivel <= 0) return "Apagado (0%)";
        if (nivel <= 30) return "Bajo (" + nivel + "%)";
        if (nivel <= 70) return "Medio (" + nivel + "%)";
        if (nivel < 100) return "Alto (" + nivel + "%)";
        return "Máximo (100%)";
    }

    private void vibrarDispositivo(boolean esCompraExitosa) {
        if (intensidadVibracion <= 0) return;
        runOnUiThread(() -> {
            int amp = Math.max(1, Math.min(255, (int) ((intensidadVibracion / 100f) * 255f)));
            long duracionBase = esCompraExitosa
                    ? (60L + (long) (intensidadVibracion * 2.2f))
                    : (25L + (long) (intensidadVibracion * 1.3f));

            try {
                if (frameRoot != null && intensidadVibracion >= 20) {
                    frameRoot.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
            } catch (Exception ignored) {}
            try {
                Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (esCompraExitosa) {
                            vib.vibrate(VibrationEffect.createWaveform(
                                    new long[]{0, duracionBase / 2, 55, duracionBase},
                                    new int[]{0, amp, 0, amp}, -1));
                        } else {
                            vib.vibrate(VibrationEffect.createOneShot(duracionBase, amp));
                        }
                    } else {
                        vib.vibrate(duracionBase);
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

    private Bitmap crearAvatarCircularPerfil() {
        int s = dp(92);
        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = s / 2f;
        float cy = s / 2f;
        float r = (s / 2f) - dp(3);

        RadialGradient bgGrad = new RadialGradient(
                cx, cy - dp(8), r,
                new int[]{Color.parseColor("#1E3A8A"), Color.parseColor("#0F172A")},
                null, Shader.TileMode.CLAMP
        );
        p.setStyle(Paint.Style.FILL);
        p.setShader(bgGrad);
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);

        Path clipCircle = new Path();
        clipCircle.addCircle(cx, cy, r - dp(2), Path.Direction.CW);
        c.save();
        c.clipPath(clipCircle);

        p.setColor(Color.parseColor("#38BDF8"));
        c.drawCircle(cx, s * 0.36f, s * 0.16f, p);
        c.drawOval(new RectF(s * 0.20f, s * 0.58f, s * 0.80f, s * 1.06f), p);
        c.restore();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2));
        p.setColor(Color.parseColor("#38BDF8"));
        c.drawCircle(cx, cy, r, p);

        return bmp;
    }

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

    /**
     * Dibujo miniatura de vista previa estilo iOS Glass (con manchas de color difuminadas
     * detrás del cristal esmerilado y borde especular luminoso).
     */
    private Bitmap crearImagenPreviewBarraGlass(boolean activa) {
        int w = dp(78);
        int h = dp(52);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#0D1017"));
        c.drawRoundRect(new RectF(0, 0, w, h), dp(12), dp(12), p);

        // Esferas de color suaves detrás del cristal (como en la foto de iOS)
        RadialGradient gRojo = new RadialGradient(w * 0.35f, h * 0.62f, dp(22),
                new int[]{Color.parseColor("#CCEF4444"), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        p.setShader(gRojo);
        c.drawCircle(w * 0.35f, h * 0.62f, dp(22), p);

        RadialGradient gAzul = new RadialGradient(w * 0.68f, h * 0.45f, dp(20),
                new int[]{Color.parseColor("#CC38BDF8"), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        p.setShader(gAzul);
        c.drawCircle(w * 0.68f, h * 0.45f, dp(20), p);
        p.setShader(null);

        RectF rBar = new RectF(dp(6), dp(16), w - dp(6), h - dp(6));
        LinearGradient frostGrad = new LinearGradient(
                0, rBar.top, 0, rBar.bottom,
                new int[]{Color.parseColor("#90FFFFFF"), Color.parseColor("#45CBD5E1")},
                null, Shader.TileMode.CLAMP
        );
        p.setShader(frostGrad);
        c.drawRoundRect(rBar, dp(14), dp(14), p);
        p.setShader(null);

        p.setColor(Color.parseColor("#66FFFFFF"));
        c.drawRoundRect(new RectF(dp(9), dp(19), dp(26), h - dp(9)), dp(10), dp(10), p);

        float[] posX = {dp(17.5f), dp(32), dp(46), dp(60)};
        for (int i = 0; i < 4; i++) {
            p.setColor(i == 0 ? Color.WHITE : Color.parseColor("#F1F5F9"));
            p.setAlpha(i == 0 ? 255 : 190);
            c.drawCircle(posX[i], rBar.centerY(), dp(3), p);
        }
        p.setAlpha(255);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(1));
        p.setColor(Color.parseColor(activa ? "#FFFFFF" : "#AAFFFFFF"));
        c.drawRoundRect(rBar, dp(14), dp(14), p);

        return bmp;
    }

    /**
     * Motor de desenfoque esmerilado profundo tipo iOS (Frosted Glass):
     * Captura lo que pasa por detrás de la barra, aplica desenfoque de radio amplio,
     * mezcla una capa lechosa translúcida y dibuja el reflejo especular en el borde.
     */
    private void actualizarEfectoBlurBarraGlass() {
        if (!barraGlassActiva || imgFondoBlurGlass == null || contenedorBarraFlotante == null || mainVertical == null) {
            if (imgFondoBlurGlass != null) imgFondoBlurGlass.setVisibility(View.GONE);
            if (vistaBrilloEspecularGlass != null) vistaBrilloEspecularGlass.setVisibility(View.GONE);
            return;
        }
        if (contenedorBarraFlotante.getVisibility() != View.VISIBLE || traduccionBarraY >= dp(70)) return;

        long ahora = System.currentTimeMillis();
        if (ahora - ultimoRenderGlassMs < 65) return;
        ultimoRenderGlassMs = ahora;

        int barW = contenedorBarraFlotante.getWidth();
        int barH = contenedorBarraFlotante.getHeight();
        if (barW <= 0 || barH <= 0) return;

        try {
            int scale = 8; // Mayor factor de reducción para lograr el difuminado cremoso de iPhone
            int wSmall = Math.max(18, barW / scale);
            int hSmall = Math.max(10, barH / scale);

            Bitmap bmpSmall = Bitmap.createBitmap(wSmall, hSmall, Bitmap.Config.ARGB_8888);
            Canvas cSmall = new Canvas(bmpSmall);
            cSmall.scale(1f / scale, 1f / scale);
            float realTop = contenedorBarraFlotante.getTop() + traduccionBarraY;
            cSmall.translate(-contenedorBarraFlotante.getLeft(), -realTop);
            mainVertical.draw(cSmall);

            Bitmap blurred = desenfocarBitmapRapido(bmpSmall, 5);
            Canvas cFrost = new Canvas(blurred);
            // Capa esmerilada estilo iOS (lechosa suave que deja pasar los tonos de luz de atrás)
            cFrost.drawColor(Color.parseColor(temaActual == 2 ? "#88F8FAFC" : "#68242936"));
            Paint pSheen = new Paint(Paint.ANTI_ALIAS_FLAG);
            pSheen.setShader(new LinearGradient(0, 0, wSmall, hSmall,
                    new int[]{Color.parseColor("#42FFFFFF"), Color.parseColor("#12FFFFFF"), Color.parseColor("#28FFFFFF")},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            cFrost.drawRect(0, 0, wSmall, hSmall, pSheen);

            Bitmap finalGlass = crearBitmapRedondeado(blurred, barW, barH, dp(24));
            imgFondoBlurGlass.setVisibility(View.VISIBLE);
            imgFondoBlurGlass.setImageBitmap(finalGlass);
            if (vistaBrilloEspecularGlass != null) {
                vistaBrilloEspecularGlass.setVisibility(View.VISIBLE);
            }
        } catch (Throwable ignored) {}
    }

    private Bitmap desenfocarBitmapRapido(Bitmap src, int pasadas) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pix = new int[w * h];
        int[] tmp = new int[w * h];
        src.getPixels(pix, 0, w, 0, 0, w, h);

        for (int p = 0; p < pasadas; p++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = 0, g = 0, b = 0, count = 0;
                    for (int dx = -3; dx <= 3; dx++) {
                        int nx = Math.min(w - 1, Math.max(0, x + dx));
                        int c = pix[y * w + nx];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        count++;
                    }
                    tmp[y * w + x] = 0xFF000000 | ((r / count) << 16) | ((g / count) << 8) | (b / count);
                }
            }
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = 0, g = 0, b = 0, count = 0;
                    for (int dy = -3; dy <= 3; dy++) {
                        int ny = Math.min(h - 1, Math.max(0, y + dy));
                        int c = tmp[ny * w + x];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        count++;
                    }
                    pix[y * w + x] = 0xFF000000 | ((r / count) << 16) | ((g / count) << 8) | (b / count);
                }
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pix, 0, w, 0, 0, w, h);
        return out;
    }

    private Bitmap crearBitmapRedondeado(Bitmap srcSmall, int targetW, int targetH, float radioPx) {
        Bitmap scaled = Bitmap.createScaledBitmap(srcSmall, targetW, targetH, true);
        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path path = new Path();
        path.addRoundRect(new RectF(0, 0, targetW, targetH), radioPx, radioPx, Path.Direction.CW);
        c.clipPath(path);
        c.drawBitmap(scaled, 0, 0, p);
        return out;
    }

    /**
     * Ocultado progresivo al bajar con el dedo y reaparición inmediata al subir:
     * Funciona tanto con la Barra Glass activada como desactivada.
     */
    private void conectarAutoOcultadoBarraEnScroll(final ScrollView sv) {
        if (sv == null) return;
        sv.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (contenedorBarraFlotante == null || loginClienteOverlay.getVisibility() == View.VISIBLE) return;
            Object tagObj = sv.getTag();
            int oldY = (tagObj instanceof Integer) ? (Integer) tagObj : sv.getScrollY();
            int newY = sv.getScrollY();
            int dy = newY - oldY;
            sv.setTag(newY);

            if (Math.abs(dy) > dp(120)) return; // Ignora saltos bruscos de layout

            float maxHidePx = dp(76);
            if (newY <= dp(12)) {
                acumuladorBajandoScroll = 0f;
                aplicarTraduccionProgresivaBarra(0f);
            } else if (dy > 0) {
                acumuladorBajandoScroll += dy;
                // Espera un pequeño margen de 20dp antes de empezar a ocultarla poco a poco
                if (acumuladorBajandoScroll > dp(20)) {
                    float nuevaT = Math.min(maxHidePx, traduccionBarraY + (dy * 0.85f));
                    aplicarTraduccionProgresivaBarra(nuevaT);
                }
            } else if (dy < 0) {
                acumuladorBajandoScroll = 0f;
                float nuevaT = Math.max(0f, traduccionBarraY + (dy * 1.35f));
                aplicarTraduccionProgresivaBarra(nuevaT);
            }

            if (barraGlassActiva && traduccionBarraY < maxHidePx) {
                actualizarEfectoBlurBarraGlass();
            }
        });
    }

    private void aplicarTraduccionProgresivaBarra(float nuevaTraduccionY) {
        if (contenedorBarraFlotante == null) return;
        traduccionBarraY = nuevaTraduccionY;
        float maxHidePx = dp(76);
        float progresoOculto = Math.min(1f, Math.max(0f, traduccionBarraY / maxHidePx));
        contenedorBarraFlotante.setTranslationY(traduccionBarraY);
        contenedorBarraFlotante.setAlpha(1f - (progresoOculto * 0.95f));
    }

    private ScrollView obtenerVistaPestana(int idx) {
        switch (idx) {
            case 0: return vistaCuentasScroll;
            case 1: return vistaHistorialScroll;
            case 2: return vistaAjustesScroll;
            case 3: return vistaPerfilScroll;
            default: return null;
        }
    }

    private void actualizarPosicionIndicadorBarra(float posicionContinua) {
        if (indicadorDeslizanteBarra == null) return;
        float clamped = Math.max(0f, Math.min(3f, posicionContinua));
        float pasoPx = dp(71); // 68dp de botón + 3dp de separación
        indicadorDeslizanteBarra.setTranslationX(clamped * pasoPx);
    }

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
            case "PROFILE": {
                p.setStyle(Paint.Style.STROKE);
                c.drawCircle(s * 0.50f, s * 0.33f, s * 0.18f, p);
                Path shoulders = new Path();
                shoulders.addArc(new RectF(s * 0.18f, s * 0.56f, s * 0.82f, s * 0.96f), 180, 180);
                c.drawPath(shoulders, p);
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
                c.drawArc(arcR, 35, 285, false, p);
                c.drawLine(s * 0.78f, s * 0.22f, s * 0.78f, s * 0.44f, p);
                c.drawLine(s * 0.56f, s * 0.44f, s * 0.78f, s * 0.44f, p);
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
    // CONSTRUCCIÓN DE LA INTERFAZ CON GESTOS SWIPE ENTRE PESTAÑAS Y BARRA GLASS iOS
    // =========================================================================================
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        contenedorMotores2x2.addView(filaMotoresInf, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        frameRoot.addView(contenedorMotores2x2, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(440)));

        mainVertical = new LinearLayout(this);
        mainVertical.setOrientation(LinearLayout.VERTICAL);
        mainVertical.setBackgroundColor(Color.parseColor("#0F1115"));
        mainVertical.setClickable(true);
        mainVertical.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        // Contenedor de páginas con soporte de arrastre horizontal en vivo (Swipe izquierda/derecha)
        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        contenedorPaginasSwipe = new FrameLayout(this) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                int action = ev.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    touchStartX = ev.getRawX();
                    touchStartY = ev.getRawY();
                    deslizandoPestanaHorizontal = false;
                    gestoDescartado = false;
                    pestanaVecinaSwipe = -1;
                    if (velocityTrackerSwipe == null) velocityTrackerSwipe = VelocityTracker.obtain();
                    else velocityTrackerSwipe.clear();
                    velocityTrackerSwipe.addMovement(ev);
                } else if (action == MotionEvent.ACTION_MOVE && !gestoDescartado) {
                    float dx = ev.getRawX() - touchStartX;
                    float dy = ev.getRawY() - touchStartY;
                    if (Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                        gestoDescartado = true;
                        return false;
                    }
                    if (Math.abs(dx) > touchSlop * 1.4f && Math.abs(dx) > Math.abs(dy) * 1.35f) {
                        deslizandoPestanaHorizontal = true;
                        return true;
                    }
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                if (velocityTrackerSwipe == null) velocityTrackerSwipe = VelocityTracker.obtain();
                velocityTrackerSwipe.addMovement(ev);
                int action = ev.getActionMasked();
                int w = Math.max(1, getWidth());
                float dx = ev.getRawX() - touchStartX;

                if (action == MotionEvent.ACTION_MOVE) {
                    if ((pestanaInferiorActual == 0 && dx > 0) || (pestanaInferiorActual == 3 && dx < 0)) {
                        dx = dx * 0.25f;
                    }
                    int nuevoVecino = (dx < 0) ? (pestanaInferiorActual + 1) : (dx > 0 ? (pestanaInferiorActual - 1) : -1);
                    if (nuevoVecino < 0 || nuevoVecino > 3) nuevoVecino = -1;

                    if (pestanaVecinaSwipe != nuevoVecino) {
                        ScrollView viejoVec = obtenerVistaPestana(pestanaVecinaSwipe);
                        if (viejoVec != null && pestanaVecinaSwipe != pestanaInferiorActual) {
                            viejoVec.setVisibility(View.GONE);
                            viejoVec.setTranslationX(0f);
                        }
                        pestanaVecinaSwipe = nuevoVecino;
                        ScrollView actVec = obtenerVistaPestana(pestanaVecinaSwipe);
                        if (actVec != null) {
                            actVec.setVisibility(View.VISIBLE);
                            if (pestanaVecinaSwipe == 1) {
                                reconstruirFiltrosHistorial();
                                refrescarVistaHistorial();
                                reconstruirListaComprasExitosas();
                            } else if (pestanaVecinaSwipe == 3) {
                                refrescarDatosVistaPerfil();
                            }
                        }
                    }

                    ScrollView vistaActual = obtenerVistaPestana(pestanaInferiorActual);
                    if (vistaActual != null) vistaActual.setTranslationX(dx);

                    ScrollView vistaVecina = obtenerVistaPestana(pestanaVecinaSwipe);
                    if (vistaVecina != null) {
                        float offsetBase = (pestanaVecinaSwipe > pestanaInferiorActual) ? w : -w;
                        vistaVecina.setTranslationX(offsetBase + dx);
                    }

                    actualizarPosicionIndicadorBarra(pestanaInferiorActual - (dx / (float) w));
                    if (barraGlassActiva) actualizarEfectoBlurBarraGlass();
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    velocityTrackerSwipe.computeCurrentVelocity(1000);
                    float vx = velocityTrackerSwipe.getXVelocity();
                    boolean cambiar = false;
                    int destino = pestanaInferiorActual;

                    if (pestanaVecinaSwipe >= 0 && pestanaVecinaSwipe <= 3) {
                        if (Math.abs(dx) > w * 0.32f || Math.abs(vx) > 950f) {
                            if ((dx < 0 && vx <= 200f && pestanaVecinaSwipe > pestanaInferiorActual)
                                    || (dx > 0 && vx >= -200f && pestanaVecinaSwipe < pestanaInferiorActual)) {
                                cambiar = true;
                                destino = pestanaVecinaSwipe;
                            }
                        }
                    }

                    final ScrollView vAct = obtenerVistaPestana(pestanaInferiorActual);
                    final ScrollView vVec = obtenerVistaPestana(pestanaVecinaSwipe);
                    final int destFinal = destino;
                    final boolean cambiarFinal = cambiar;
                    float finActX = cambiarFinal ? ((destFinal > pestanaInferiorActual) ? -w : w) : 0f;
                    float finVecX = cambiarFinal ? 0f : ((pestanaVecinaSwipe > pestanaInferiorActual) ? w : -w);

                    if (vAct != null) {
                        vAct.animate().translationX(finActX).setDuration(210)
                                .setInterpolator(new DecelerateInterpolator()).start();
                    }
                    if (vVec != null) {
                        vVec.animate().translationX(finVecX).setDuration(210)
                                .setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> {
                                    if (vAct != null) vAct.setTranslationX(0f);
                                    if (vVec != null) vVec.setTranslationX(0f);
                                    cambiarPestanaInferior(destFinal);
                                }).start();
                    } else {
                        cambiarPestanaInferior(destFinal);
                    }
                    if (indicadorDeslizanteBarra != null) {
                        indicadorDeslizanteBarra.animate()
                                .translationX(destFinal * dp(71))
                                .setDuration(210)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                    deslizandoPestanaHorizontal = false;
                    pestanaVecinaSwipe = -1;
                    return true;
                }
                return super.onTouchEvent(ev);
            }
        };

        vistaCuentasScroll = new ScrollView(this);
        vistaCuentasScroll.setFillViewport(true);
        vistaCuentasScroll.setVerticalScrollBarEnabled(false);
        conectarAutoOcultadoBarraEnScroll(vistaCuentasScroll);

        LinearLayout contenidoScrollCuentas = new LinearLayout(this);
        contenidoScrollCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.setPadding(dp(14), dp(4), dp(14), dp(88));

        LinearLayout filaHeaderCuentas = new LinearLayout(this);
        filaHeaderCuentas.setOrientation(LinearLayout.HORIZONTAL);
        filaHeaderCuentas.setGravity(Gravity.CENTER_VERTICAL);
        filaHeaderCuentas.setPadding(0, dp(4), 0, dp(6));

        LinearLayout colTitCuentas = new LinearLayout(this);
        colTitCuentas.setOrientation(LinearLayout.VERTICAL);
        lblCuentasBig = crearTexto("Cuentas", "#F4F4F5", 21f, true);
        lblCuentasSub = crearTexto("Toca cualquier cuenta para ver la página del banco y su consola en vivo.", "#A1A1AA", 11f, false);
        colTitCuentas.addView(lblCuentasBig);
        colTitCuentas.addView(lblCuentasSub);
        filaHeaderCuentas.addView(colTitCuentas, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNuevaCuenta = crearBotonConIcono("PLUS", "Nueva", "#0284C7", "#FFFFFF", 12, () -> mostrarPopupEditorCuenta(-1));
        btnNuevaCuenta.setPadding(dp(10), 0, dp(10), 0);
        filaHeaderCuentas.addView(btnNuevaCuenta, new LinearLayout.LayoutParams(dp(88), dp(34)));
        contenidoScrollCuentas.addView(filaHeaderCuentas);

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

        cardTiempos = new LinearLayout(this);
        cardTiempos.setOrientation(LinearLayout.VERTICAL);
        cardTiempos.setPadding(dp(12), dp(10), dp(12), dp(10));
        cardTiempos.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 12, 1));
        LinearLayout.LayoutParams lpCardT = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCardT.setMargins(0, 0, 0, dp(10));

        LinearLayout filaRel = new LinearLayout(this);
        filaRel.setGravity(Gravity.CENTER_VERTICAL);
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
        filaRel.addView(lblTRel, new LinearLayout.LayoutParams(dp(115), ViewGroup.LayoutParams.WRAP_CONTENT));
        filaRel.addView(sldRel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaRel.addView(lblReloginVal, new LinearLayout.LayoutParams(dp(85), ViewGroup.LayoutParams.WRAP_CONTENT));
        cardTiempos.addView(filaRel);
        contenidoScrollCuentas.addView(cardTiempos, lpCardT);

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

        cardEstadoVacioCuentas = new LinearLayout(this);
        cardEstadoVacioCuentas.setOrientation(LinearLayout.VERTICAL);
        cardEstadoVacioCuentas.setGravity(Gravity.CENTER);
        cardEstadoVacioCuentas.setPadding(dp(22), dp(28), dp(22), dp(28));
        cardEstadoVacioCuentas.setBackground(fondoBordePunteado("#14161D", "#334155", 16));
        cardEstadoVacioCuentas.setOnClickListener(v -> mostrarPopupEditorCuenta(-1));

        ImageView imgEmpty = new ImageView(this);
        imgEmpty.setImageBitmap(crearLogoTransparenteBT());
        imgEmpty.setAlpha(0.75f);
        LinearLayout.LayoutParams lpImgEmpty = new LinearLayout.LayoutParams(dp(48), dp(48));
        lpImgEmpty.setMargins(0, 0, 0, dp(10));
        cardEstadoVacioCuentas.addView(imgEmpty, lpImgEmpty);

        lblEmptyTitulo = crearTexto("No tienes cuentas registradas aún", "#F8FAFC", 15f, true);
        lblEmptyTitulo.setGravity(Gravity.CENTER);
        cardEstadoVacioCuentas.addView(lblEmptyTitulo);

        lblEmptySub = crearTexto(
                "Toca aquí o en el botón '+ Nueva' arriba a la derecha para agregar tu primera cuenta del Banco del Tesoro (Máx. 4 simultáneas).",
                "#94A3B8", 11.5f, false);
        lblEmptySub.setGravity(Gravity.CENTER);
        lblEmptySub.setPadding(0, dp(6), 0, dp(14));
        cardEstadoVacioCuentas.addView(lblEmptySub);

        TextView badgeEmptyAdd = crearTexto("+ Agregar primera cuenta", "#38BDF8", 11.5f, true);
        badgeEmptyAdd.setPadding(dp(14), dp(6), dp(14), dp(6));
        badgeEmptyAdd.setBackground(fondoRedondeadoConBorde("#1E293B", "#38BDF8", 12, 1));
        cardEstadoVacioCuentas.addView(badgeEmptyAdd);

        LinearLayout.LayoutParams lpEmpty = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpEmpty.setMargins(0, dp(6), 0, dp(10));
        contenidoScrollCuentas.addView(cardEstadoVacioCuentas, lpEmpty);

        contenedorTarjetasCuentas = new LinearLayout(this);
        contenedorTarjetasCuentas.setOrientation(LinearLayout.VERTICAL);
        contenidoScrollCuentas.addView(contenedorTarjetasCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        vistaCuentasScroll.addView(contenidoScrollCuentas);
        contenedorPaginasSwipe.addView(vistaCuentasScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        vistaHistorialScroll = new ScrollView(this);
        vistaHistorialScroll.setFillViewport(true);
        vistaHistorialScroll.setVerticalScrollBarEnabled(false);
        vistaHistorialScroll.setVisibility(View.GONE);
        conectarAutoOcultadoBarraEnScroll(vistaHistorialScroll);

        vistaHistorialContainer = new LinearLayout(this);
        vistaHistorialContainer.setOrientation(LinearLayout.VERTICAL);
        vistaHistorialContainer.setPadding(dp(14), dp(6), dp(14), dp(88));

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
        contenedorPaginasSwipe.addView(vistaHistorialScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            vistaAjustesScroll = new ScrollView(this);
        vistaAjustesScroll.setFillViewport(true);
        vistaAjustesScroll.setVerticalScrollBarEnabled(false);
        vistaAjustesScroll.setVisibility(View.GONE);
        conectarAutoOcultadoBarraEnScroll(vistaAjustesScroll);

        LinearLayout contenidoAjustes = new LinearLayout(this);
        contenidoAjustes.setOrientation(LinearLayout.VERTICAL);
        contenidoAjustes.setPadding(dp(14), dp(6), dp(14), dp(88));

        lblAjustesBig = crearTexto("Ajustes", "#F4F4F5", 21f, true);
        lblAjustesSub = crearTexto("Personaliza la apariencia y configuración adicional del bot.", "#A1A1AA", 11f, false);
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

        panelBarraGlass = new LinearLayout(this);
        panelBarraGlass.setOrientation(LinearLayout.HORIZONTAL);
        panelBarraGlass.setGravity(Gravity.CENTER_VERTICAL);
        panelBarraGlass.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lpGlassBox = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpGlassBox.setMargins(dp(4), dp(12), dp(4), 0);

        ImageView imgPrevGlass = new ImageView(this);
        imgPrevGlass.setTag("IMG_GLASS_PREVIEW");
        imgPrevGlass.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imgPrevGlass.setImageBitmap(crearImagenPreviewBarraGlass(barraGlassActiva));
        LinearLayout.LayoutParams lpImgGlass = new LinearLayout.LayoutParams(dp(72), dp(48));
        lpImgGlass.setMargins(0, 0, dp(10), 0);
        panelBarraGlass.addView(imgPrevGlass, lpImgGlass);

        LinearLayout colTxtGlass = new LinearLayout(this);
        colTxtGlass.setOrientation(LinearLayout.VERTICAL);
        lblItemBarraGlass = crearTexto("Barra Glass", "#F4F4F5", 13.5f, true);
        lblDescItemBarraGlass = crearTexto("Efecto cristal translúcido tipo iPhone con fondo difuminado en la barra inferior.", "#A1A1AA", 10.5f, false);
        lblDescItemBarraGlass.setPadding(0, dp(2), dp(8), 0);
        colTxtGlass.addView(lblItemBarraGlass);
        colTxtGlass.addView(lblDescItemBarraGlass);

        btnToggleBarraGlass = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            barraGlassActiva = !barraGlassActiva;
            prefs.edit().putBoolean("barra_glass_iphone_on", barraGlassActiva).apply();
            actualizarTarjetasPreviewTema();
            actualizarBotonesAjustes();
            actualizarEstiloBarraFlotante();
            actualizarEfectoBlurBarraGlass();
        });
        panelBarraGlass.addView(colTxtGlass, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelBarraGlass.addView(btnToggleBarraGlass, new LinearLayout.LayoutParams(dp(92), dp(34)));
        cardTemaAjustes.addView(panelBarraGlass, lpGlassBox);

        contenidoAjustes.addView(cardTemaAjustes, lpCardAj);

        cardAlertasAjustes = new LinearLayout(this);
        cardAlertasAjustes.setOrientation(LinearLayout.VERTICAL);
        cardAlertasAjustes.setPadding(dp(14), dp(14), dp(14), dp(14));
        cardAlertasAjustes.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 16, 1));

        lblSubAlertas = crearTexto("Configuración adicional", "#F4F4F5", 15f, true);
        lblDescAlertas = crearTexto("Opciones de velocidad, cambios de IP y avisos del bot.", "#A1A1AA", 11f, false);
        lblDescAlertas.setPadding(0, dp(2), 0, dp(12));
        cardAlertasAjustes.addView(lblSubAlertas);
        cardAlertasAjustes.addView(lblDescAlertas);

        LinearLayout.LayoutParams lpSubPanel = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSubPanel.setMargins(0, 0, 0, dp(10));

        panelItemTurbo = new LinearLayout(this);
        panelItemTurbo.setOrientation(LinearLayout.HORIZONTAL);
        panelItemTurbo.setGravity(Gravity.CENTER_VERTICAL);
        panelItemTurbo.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtTurbo = new LinearLayout(this);
        colTxtTurbo.setOrientation(LinearLayout.VERTICAL);
        lblItemTurbo = crearTexto("Modo Turbo", "#F4F4F5", 13.5f, true);
        lblDescItemTurbo = crearTexto("Oculta imágenes y recursos pesados para mayor velocidad.", "#A1A1AA", 10.5f, false);
        lblDescItemTurbo.setPadding(0, dp(2), dp(8), 0);
        colTxtTurbo.addView(lblItemTurbo);
        colTxtTurbo.addView(lblDescItemTurbo);
        btnToggleTurbo = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            if (modoTurboSinImagenes) {
                new AlertDialog.Builder(this)
                        .setTitle("¿Desactivar Modo Turbo?")
                        .setMessage("¿Estás seguro de desactivar el Modo Turbo?")
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

        panelItemProxy = new LinearLayout(this);
        panelItemProxy.setOrientation(LinearLayout.HORIZONTAL);
        panelItemProxy.setGravity(Gravity.CENTER_VERTICAL);
        panelItemProxy.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtProxy = new LinearLayout(this);
        colTxtProxy.setOrientation(LinearLayout.VERTICAL);
        lblItemProxy = crearTexto("Proxy VE", "#F4F4F5", 13.5f, true);
        lblDescItemProxy = crearTexto("Conexión residencial de Venezuela para cambios de IP.", "#A1A1AA", 10.5f, false);
        lblDescItemProxy.setPadding(0, dp(2), dp(8), 0);
        colTxtProxy.addView(lblItemProxy);
        colTxtProxy.addView(lblDescItemProxy);
        btnToggleProxy = crearBoton("", "#14532D", "#4ADE80", 10, () -> {
            proxyRotativoActivo = !proxyRotativoActivo;
            prefs.edit().putBoolean("proxy_iproyal_on", proxyRotativoActivo).apply();
            vincularPuenteLocalAlWebView();
            actualizarBotonesAjustes();
            for (SesionBot s : obtenerSesionesRegistradas()) {
                if (!proxyRotativoActivo) {
                    s.proxyActivoEnEstaCuenta = false;
                    cerrarSocketsDeSesion(s);
                }
                actualizarVisualTarjeta(s);
            }
        });
        panelItemProxy.addView(colTxtProxy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelItemProxy.addView(btnToggleProxy, new LinearLayout.LayoutParams(dp(96), dp(34)));
        cardAlertasAjustes.addView(panelItemProxy, lpSubPanel);

        panelItemOrd = new LinearLayout(this);
        panelItemOrd.setOrientation(LinearLayout.HORIZONTAL);
        panelItemOrd.setGravity(Gravity.CENTER_VERTICAL);
        panelItemOrd.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colTxtOrd = new LinearLayout(this);
        colTxtOrd.setOrientation(LinearLayout.VERTICAL);
        lblItemOrd = crearTexto("Alerta de Ordinaria", "#F4F4F5", 13.5f, true);
        lblDescItemOrd = crearTexto("Sonido de aviso cuando abre el formulario de Ordinaria.", "#A1A1AA", 10.5f, false);
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

        // Panel de Vibración del Dispositivo con Barrita Corta regulable (0% a 100%)
        panelItemVib = new LinearLayout(this);
        panelItemVib.setOrientation(LinearLayout.VERTICAL);
        panelItemVib.setPadding(dp(14), dp(12), dp(12), dp(12));
        panelItemVib.setBackground(fondoRedondeadoConBorde("#202227", "#27272A", 14, 1));

        LinearLayout filaTopVib = new LinearLayout(this);
        filaTopVib.setOrientation(LinearLayout.HORIZONTAL);
        filaTopVib.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout colTxtVib = new LinearLayout(this);
        colTxtVib.setOrientation(LinearLayout.VERTICAL);
        lblItemVib = crearTexto("Vibración del Dispositivo", "#F4F4F5", 13.5f, true);
        lblDescItemVib = crearTexto("Intensidad del motor háptico para avisos y compras.", "#A1A1AA", 10.5f, false);
        lblDescItemVib.setPadding(0, dp(2), dp(8), 0);
        colTxtVib.addView(lblItemVib);
        colTxtVib.addView(lblDescItemVib);

        lblNivelVibValor = crearTexto(textoNivelVibracion(intensidadVibracion), "#38BDF8", 11.5f, true);
        lblNivelVibValor.setGravity(Gravity.END);
        filaTopVib.addView(colTxtVib, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filaTopVib.addView(lblNivelVibValor);
        panelItemVib.addView(filaTopVib);

        seekBarVibracion = new SeekBar(this);
        seekBarVibracion.setMax(100);
        seekBarVibracion.setProgress(intensidadVibracion);
        LinearLayout.LayoutParams lpSeekVib = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSeekVib.setMargins(0, dp(8), 0, 0);
        seekBarVibracion.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intensidadVibracion = progress;
                prefs.edit().putInt("intensidad_vibracion_v1", intensidadVibracion).apply();
                if (lblNivelVibValor != null) {
                    lblNivelVibValor.setText(textoNivelVibracion(intensidadVibracion));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (intensidadVibracion > 0) vibrarDispositivo(false);
            }
        });
        panelItemVib.addView(seekBarVibracion, lpSeekVib);
        cardAlertasAjustes.addView(panelItemVib, lpSubPanel);

        // Botón desplegable "Configuración avanzada" en modo oscuro
        btnTextoConfigAvanzada = crearTexto("▼ Configuración avanzada", "#E4E4E7", 12.5f, true);
        btnTextoConfigAvanzada.setGravity(Gravity.CENTER);
        btnTextoConfigAvanzada.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnTextoConfigAvanzada.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 12, 1));
        btnTextoConfigAvanzada.setOnClickListener(v -> {
            configAvanzadaExpandida = !configAvanzadaExpandida;
            contenedorConfigAvanzada.setVisibility(configAvanzadaExpandida ? View.VISIBLE : View.GONE);
            btnTextoConfigAvanzada.setText(configAvanzadaExpandida ? "▲ Ocultar configuración avanzada" : "▼ Configuración avanzada");
        });
        cardAlertasAjustes.addView(btnTextoConfigAvanzada, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        contenedorConfigAvanzada = new LinearLayout(this);
        contenedorConfigAvanzada.setOrientation(LinearLayout.VERTICAL);
        contenedorConfigAvanzada.setVisibility(View.GONE);
        contenedorConfigAvanzada.setPadding(0, dp(10), 0, 0);

        panelAdvPosGlobal = new LinearLayout(this);
        panelAdvPosGlobal.setOrientation(LinearLayout.HORIZONTAL);
        panelAdvPosGlobal.setGravity(Gravity.CENTER_VERTICAL);
        panelAdvPosGlobal.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colAdvPG = new LinearLayout(this);
        colAdvPG.setOrientation(LinearLayout.VERTICAL);
        lblAdvPosGlobal = crearTexto("Activar proxy en posición global", "#F4F4F5", 13f, true);
        lblDescAdvPosGlobal = crearTexto("Inicia con Wi-Fi normal y activa el proxy con cambio de IP al cargar Posición Global.", "#A1A1AA", 10.5f, false);
        lblDescAdvPosGlobal.setPadding(0, dp(2), dp(8), 0);
        colAdvPG.addView(lblAdvPosGlobal);
        colAdvPG.addView(lblDescAdvPosGlobal);
        btnToggleAdvPosGlobal = crearBoton("", "#14532D", "#4ADE80", 10, () -> intentarCambiarModoAvanzadoProxy(MODO_PROXY_POSICION_GLOBAL));
        panelAdvPosGlobal.addView(colAdvPG, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelAdvPosGlobal.addView(btnToggleAdvPosGlobal, new LinearLayout.LayoutParams(dp(96), dp(34)));
        contenedorConfigAvanzada.addView(panelAdvPosGlobal, lpSubPanel);

        panelAdvSiempre = new LinearLayout(this);
        panelAdvSiempre.setOrientation(LinearLayout.HORIZONTAL);
        panelAdvSiempre.setGravity(Gravity.CENTER_VERTICAL);
        panelAdvSiempre.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colAdvSiempre = new LinearLayout(this);
        colAdvSiempre.setOrientation(LinearLayout.VERTICAL);
        lblAdvSiempre = crearTexto("Mantener proxy siempre activo", "#F4F4F5", 13f, true);
        lblDescAdvSiempre = crearTexto("Mantiene el proxy activo en todo momento desde que carga el login.", "#A1A1AA", 10.5f, false);
        lblDescAdvSiempre.setPadding(0, dp(2), dp(8), 0);
        colAdvSiempre.addView(lblAdvSiempre);
        colAdvSiempre.addView(lblDescAdvSiempre);
        btnToggleAdvSiempre = crearBoton("", "#450A0A", "#FCA5A5", 10, () -> intentarCambiarModoAvanzadoProxy(MODO_PROXY_SIEMPRE_ACTIVO));
        panelAdvSiempre.addView(colAdvSiempre, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelAdvSiempre.addView(btnToggleAdvSiempre, new LinearLayout.LayoutParams(dp(96), dp(34)));
        contenedorConfigAvanzada.addView(panelAdvSiempre, lpSubPanel);

        panelAdvPass = new LinearLayout(this);
        panelAdvPass.setOrientation(LinearLayout.HORIZONTAL);
        panelAdvPass.setGravity(Gravity.CENTER_VERTICAL);
        panelAdvPass.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colAdvPass = new LinearLayout(this);
        colAdvPass.setOrientation(LinearLayout.VERTICAL);
        lblAdvPass = crearTexto("Activar proxy en contraseña", "#F4F4F5", 13f, true);
        lblDescAdvPass = crearTexto("Inicia usuario y cédula con Wi-Fi normal; al cargar contraseña activa el proxy y cambia la IP.", "#A1A1AA", 10.5f, false);
        lblDescAdvPass.setPadding(0, dp(2), dp(8), 0);
        colAdvPass.addView(lblAdvPass);
        colAdvPass.addView(lblDescAdvPass);
        btnToggleAdvPass = crearBoton("", "#450A0A", "#FCA5A5", 10, () -> intentarCambiarModoAvanzadoProxy(MODO_PROXY_EN_CONTRASENA));
        panelAdvPass.addView(colAdvPass, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelAdvPass.addView(btnToggleAdvPass, new LinearLayout.LayoutParams(dp(96), dp(34)));
        contenedorConfigAvanzada.addView(panelAdvPass, lpSubPanel);

        panelAdvIntervencion = new LinearLayout(this);
        panelAdvIntervencion.setOrientation(LinearLayout.HORIZONTAL);
        panelAdvIntervencion.setGravity(Gravity.CENTER_VERTICAL);
        panelAdvIntervencion.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colAdvInt = new LinearLayout(this);
        colAdvInt.setOrientation(LinearLayout.VERTICAL);
        lblAdvIntervencion = crearTexto("Activar proxy en intervención", "#F4F4F5", 13f, true);
        lblDescAdvIntervencion = crearTexto("Entra hasta Intervención con Wi-Fi normal; cuando cargue bien activa el proxy, cambia la IP y reinicia Intervención.", "#A1A1AA", 10.5f, false);
        lblDescAdvIntervencion.setPadding(0, dp(2), dp(8), 0);
        colAdvInt.addView(lblAdvIntervencion);
        colAdvInt.addView(lblDescAdvIntervencion);
        btnToggleAdvIntervencion = crearBoton("", "#450A0A", "#FCA5A5", 10, () -> intentarCambiarModoAvanzadoProxy(MODO_PROXY_EN_INTERVENCION));
        panelAdvIntervencion.addView(colAdvInt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelAdvIntervencion.addView(btnToggleAdvIntervencion, new LinearLayout.LayoutParams(dp(96), dp(34)));
        contenedorConfigAvanzada.addView(panelAdvIntervencion, lpSubPanel);

        // Recuadro nuevo: Proxy personalizado por cuenta (Abre panel por cuenta)
        panelAdvPorCuenta = new LinearLayout(this);
        panelAdvPorCuenta.setOrientation(LinearLayout.HORIZONTAL);
        panelAdvPorCuenta.setGravity(Gravity.CENTER_VERTICAL);
        panelAdvPorCuenta.setPadding(dp(14), dp(12), dp(12), dp(12));
        LinearLayout colAdvCta = new LinearLayout(this);
        colAdvCta.setOrientation(LinearLayout.VERTICAL);
        lblAdvPorCuenta = crearTexto("Proxy personalizado por cuenta", "#F4F4F5", 13f, true);
        lblDescAdvPorCuenta = crearTexto("Configura un modo de proxy independiente para cada cuenta registrada.", "#A1A1AA", 10.5f, false);
        lblDescAdvPorCuenta.setPadding(0, dp(2), dp(8), 0);
        colAdvCta.addView(lblAdvPorCuenta);
        colAdvCta.addView(lblDescAdvPorCuenta);
        btnToggleAdvPorCuenta = crearBoton("", "#450A0A", "#FCA5A5", 10, () -> {
            intentarCambiarModoAvanzadoProxy(MODO_PROXY_PERSONALIZADO_CUENTA);
            if (modoAvanzadoProxy == MODO_PROXY_PERSONALIZADO_CUENTA) {
                mostrarPopupConfiguracionProxyPorCuenta();
            }
        });
        panelAdvPorCuenta.addView(colAdvCta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        panelAdvPorCuenta.addView(btnToggleAdvPorCuenta, new LinearLayout.LayoutParams(dp(96), dp(34)));
        contenedorConfigAvanzada.addView(panelAdvPorCuenta);

        cardAlertasAjustes.addView(contenedorConfigAvanzada);
        contenidoAjustes.addView(cardAlertasAjustes, lpCardAj);
        vistaAjustesScroll.addView(contenidoAjustes);
        mainVertical.addView(vistaAjustesScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        vistaPerfilScroll = new ScrollView(this);
        vistaPerfilScroll.setFillViewport(true);
        vistaPerfilScroll.setVerticalScrollBarEnabled(false);
        vistaPerfilScroll.setVisibility(View.GONE);
        conectarAutoOcultadoBarraEnScroll(vistaPerfilScroll);

        LinearLayout contenidoPerfil = new LinearLayout(this);
        contenidoPerfil.setOrientation(LinearLayout.VERTICAL);
        contenidoPerfil.setGravity(Gravity.CENTER_HORIZONTAL);
        contenidoPerfil.setPadding(dp(16), dp(12), dp(16), dp(90));

        cardPerfilPrincipal = new LinearLayout(this);
        cardPerfilPrincipal.setOrientation(LinearLayout.VERTICAL);
        cardPerfilPrincipal.setGravity(Gravity.CENTER_HORIZONTAL);
        cardPerfilPrincipal.setPadding(dp(18), dp(24), dp(18), dp(20));
        cardPerfilPrincipal.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 18, 1));

        imgAvatarPerfil = new ImageView(this);
        imgAvatarPerfil.setImageBitmap(crearAvatarCircularPerfil());
        LinearLayout.LayoutParams lpAv = new LinearLayout.LayoutParams(dp(88), dp(88));
        lpAv.setMargins(0, 0, 0, dp(10));
        cardPerfilPrincipal.addView(imgAvatarPerfil, lpAv);

        lblNombrePerfil = crearTexto("geonaldiaz1", "#F8FAFC", 20f, true);
        lblNombrePerfil.setGravity(Gravity.CENTER);
        cardPerfilPrincipal.addView(lblNombrePerfil);

        TextView badgeVerificadoPerfil = crearTexto("Licencia Verificada", "#4ADE80", 10.5f, true);
        badgeVerificadoPerfil.setPadding(dp(10), dp(3), dp(10), dp(3));
        badgeVerificadoPerfil.setBackground(fondoRedondeado("#14532D", 10));
        LinearLayout.LayoutParams lpBadgeVer = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBadgeVer.setMargins(0, dp(6), 0, dp(20));
        cardPerfilPrincipal.addView(badgeVerificadoPerfil, lpBadgeVer);

        LinearLayout.LayoutParams lpBoxPerf = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBoxPerf.setMargins(0, 0, 0, dp(10));

        boxPerfilDevice = new LinearLayout(this);
        boxPerfilDevice.setOrientation(LinearLayout.HORIZONTAL);
        boxPerfilDevice.setGravity(Gravity.CENTER_VERTICAL);
        boxPerfilDevice.setPadding(dp(14), dp(12), dp(14), dp(12));
        boxPerfilDevice.setBackground(fondoRedondeadoConBorde("#202227", "#27272A", 12, 1));
        TextView lblTitDevPerf = crearTexto("ID del dispositivo:", "#94A3B8", 12f, true);
        lblValorDevicePerfil = crearTexto(obtenerDeviceIdUnico(), "#38BDF8", 12.5f, true);
        lblValorDevicePerfil.setGravity(Gravity.END);
        boxPerfilDevice.addView(lblTitDevPerf, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        boxPerfilDevice.addView(lblValorDevicePerfil);
        cardPerfilPrincipal.addView(boxPerfilDevice, lpBoxPerf);

        boxPerfilCuentas = new LinearLayout(this);
        boxPerfilCuentas.setOrientation(LinearLayout.HORIZONTAL);
        boxPerfilCuentas.setGravity(Gravity.CENTER_VERTICAL);
        boxPerfilCuentas.setPadding(dp(14), dp(12), dp(14), dp(12));
        boxPerfilCuentas.setBackground(fondoRedondeadoConBorde("#202227", "#27272A", 12, 1));
        TextView lblTitCtasPerf = crearTexto("Cuentas agregadas:", "#94A3B8", 12f, true);
        lblValorCuentasPerfil = crearTexto("0 / 4 cuentas", "#4ADE80", 12.5f, true);
        lblValorCuentasPerfil.setGravity(Gravity.END);
        boxPerfilCuentas.addView(lblTitCtasPerf, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        boxPerfilCuentas.addView(lblValorCuentasPerfil);
        cardPerfilPrincipal.addView(boxPerfilCuentas, lpBoxPerf);

        boxPerfilClave = new LinearLayout(this);
        boxPerfilClave.setOrientation(LinearLayout.HORIZONTAL);
        boxPerfilClave.setGravity(Gravity.CENTER_VERTICAL);
        boxPerfilClave.setPadding(dp(14), dp(10), dp(12), dp(10));
        boxPerfilClave.setBackground(fondoRedondeadoConBorde("#202227", "#27272A", 12, 1));

        TextView lblTitClvPerf = crearTexto("Clave:  ", "#94A3B8", 12f, true);
        lblValorClavePerfil = crearTexto("••••••••", "#F8FAFC", 13.5f, true);

        btnMantenerMostrarClave = crearTexto("Mostrar clave", "#38BDF8", 10.5f, true);
        btnMantenerMostrarClave.setPadding(dp(10), dp(5), dp(10), dp(5));
        btnMantenerMostrarClave.setBackground(fondoRedondeadoConBorde("#1E293B", "#334155", 8, 1));

        View.OnTouchListener listenerMantenerClave = (v, event) -> {
            String clvReal = prefs.getString("clv_licencia_activa_v3", LICENCIA_CLAVE_CLIENTE);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lblValorClavePerfil.setText(clvReal);
                lblValorClavePerfil.setTextColor(Color.parseColor("#4ADE80"));
                btnMantenerMostrarClave.setText("Mostrando...");
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                lblValorClavePerfil.setText("••••••••");
                lblValorClavePerfil.setTextColor(Color.parseColor(colTextoPrimario()));
                btnMantenerMostrarClave.setText("Mostrar clave");
                return true;
            }
            return false;
        };
        lblValorClavePerfil.setOnTouchListener(listenerMantenerClave);
        btnMantenerMostrarClave.setOnTouchListener(listenerMantenerClave);

        boxPerfilClave.addView(lblTitClvPerf);
        boxPerfilClave.addView(lblValorClavePerfil, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        boxPerfilClave.addView(btnMantenerMostrarClave);
        cardPerfilPrincipal.addView(boxPerfilClave, lpBoxPerf);

        btnCerrarSesionPerfil = crearBotonConIcono("LOGOUT", "Cerrar sesión", "#7F1D1D", "#FCA5A5", 12, this::cerrarSesionDeLicenciaActual);
        LinearLayout.LayoutParams lpBtnOutPerf = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        lpBtnOutPerf.setMargins(0, dp(8), 0, dp(14));
        cardPerfilPrincipal.addView(btnCerrarSesionPerfil, lpBtnOutPerf);

        lblPieLicenciaPerfil = crearTexto("Licencia activa", "#71717A", 10.5f, false);
        lblPieLicenciaPerfil.setGravity(Gravity.CENTER);
        lblPieLicenciaPerfil.setPadding(dp(6), dp(6), dp(6), 0);
        cardPerfilPrincipal.addView(lblPieLicenciaPerfil);

        contenidoPerfil.addView(cardPerfilPrincipal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        vistaPerfilScroll.addView(contenidoPerfil);
        mainVertical.addView(vistaPerfilScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        frameRoot.addView(mainVertical);

        // Barra flotante con soporte de Efecto Glass iPhone iOS Real
        contenedorBarraFlotante = new FrameLayout(this);
        contenedorBarraFlotante.setVisibility(View.GONE);
        contenedorBarraFlotante.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        imgFondoBlurGlass = new ImageView(this);
        imgFondoBlurGlass.setScaleType(ImageView.ScaleType.FIT_XY);
        imgFondoBlurGlass.setVisibility(View.GONE);
        contenedorBarraFlotante.addView(imgFondoBlurGlass, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        vistaBrilloEspecularGlass = new View(this);
        vistaBrilloEspecularGlass.setVisibility(View.GONE);
        contenedorBarraFlotante.addView(vistaBrilloEspecularGlass, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1.5f)));

        barraFlotante = new LinearLayout(this);
        barraFlotante.setOrientation(LinearLayout.HORIZONTAL);
        barraFlotante.setGravity(Gravity.CENTER);
        barraFlotante.setPadding(dp(4), dp(3), dp(4), dp(3));
        barraFlotante.setBackground(fondoRedondeadoConBorde("#18181B", "#2E323B", 22, 1));

        indicadorDeslizanteBarra = new View(this);
        FrameLayout.LayoutParams lpInd = new FrameLayout.LayoutParams(dp(68), dp(36));
        lpInd.gravity = Gravity.CENTER_VERTICAL;
        indicadorDeslizanteBarra.setLayoutParams(lpInd);
        contenedorBarraFlotante.addView(indicadorDeslizanteBarra);

        navBtnCuentas = new LinearLayout(this);
        navBtnCuentas.setOrientation(LinearLayout.VERTICAL);
        navBtnCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.setPadding(dp(3), dp(2), dp(3), dp(2));
        navIconCuentas = new ImageView(this);
        navTxtCuentas = crearTexto("Cuentas", "#FFFFFF", 9.5f, true);
        navTxtCuentas.setSingleLine(true);
        navTxtCuentas.setGravity(Gravity.CENTER);
        navBtnCuentas.addView(navIconCuentas, new LinearLayout.LayoutParams(dp(15), dp(15)));
        navBtnCuentas.addView(navTxtCuentas, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnCuentas.setOnClickListener(v -> cambiarPestanaInferior(0));

        navBtnHistorial = new LinearLayout(this);
        navBtnHistorial.setOrientation(LinearLayout.VERTICAL);
        navBtnHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.setPadding(dp(3), dp(2), dp(3), dp(2));
        navIconHistorial = new ImageView(this);
        navTxtHistorial = crearTexto("Historial", "#A1A1AA", 9.5f, true);
        navTxtHistorial.setSingleLine(true);
        navTxtHistorial.setGravity(Gravity.CENTER);
        navBtnHistorial.addView(navIconHistorial, new LinearLayout.LayoutParams(dp(15), dp(15)));
        navBtnHistorial.addView(navTxtHistorial, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnHistorial.setOnClickListener(v -> cambiarPestanaInferior(1));

        navBtnAjustes = new LinearLayout(this);
        navBtnAjustes.setOrientation(LinearLayout.VERTICAL);
        navBtnAjustes.setGravity(Gravity.CENTER);
        navBtnAjustes.setPadding(dp(3), dp(2), dp(3), dp(2));
        navIconAjustes = new ImageView(this);
        navTxtAjustes = crearTexto("Ajustes", "#A1A1AA", 9.5f, true);
        navTxtAjustes.setSingleLine(true);
        navTxtAjustes.setGravity(Gravity.CENTER);
        navBtnAjustes.addView(navIconAjustes, new LinearLayout.LayoutParams(dp(15), dp(15)));
        navBtnAjustes.addView(navTxtAjustes, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnAjustes.setOnClickListener(v -> cambiarPestanaInferior(2));

        navBtnPerfil = new LinearLayout(this);
        navBtnPerfil.setOrientation(LinearLayout.VERTICAL);
        navBtnPerfil.setGravity(Gravity.CENTER);
        navBtnPerfil.setPadding(dp(3), dp(2), dp(3), dp(2));
        navIconPerfil = new ImageView(this);
        navTxtPerfil = crearTexto("Perfil", "#A1A1AA", 9.5f, true);
        navTxtPerfil.setSingleLine(true);
        navTxtPerfil.setGravity(Gravity.CENTER);
        navBtnPerfil.addView(navIconPerfil, new LinearLayout.LayoutParams(dp(15), dp(15)));
        navBtnPerfil.addView(navTxtPerfil, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        navBtnPerfil.setOnClickListener(v -> cambiarPestanaInferior(3));

        barraFlotante.addView(navBtnCuentas, new LinearLayout.LayoutParams(dp(68), dp(36)));
        barraFlotante.addView(new View(this), new LinearLayout.LayoutParams(dp(3), dp(1)));
        barraFlotante.addView(navBtnHistorial, new LinearLayout.LayoutParams(dp(68), dp(36)));
        barraFlotante.addView(new View(this), new LinearLayout.LayoutParams(dp(3), dp(1)));
        barraFlotante.addView(navBtnAjustes, new LinearLayout.LayoutParams(dp(68), dp(36)));
        barraFlotante.addView(new View(this), new LinearLayout.LayoutParams(dp(3), dp(1)));
        barraFlotante.addView(navBtnPerfil, new LinearLayout.LayoutParams(dp(68), dp(36)));

        contenedorBarraFlotante.addView(barraFlotante, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lpFloating = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpFloating.setMargins(0, 0, 0, dp(10));
        frameRoot.addView(contenedorBarraFlotante, lpFloating);

        loginClienteOverlay = construirOverlayLoginCliente();
        frameRoot.addView(loginClienteOverlay);

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
                "svg{width:145px;height:145px;}" +
                "@keyframes draw{0%{stroke-dashoffset:360;}50%{stroke-dashoffset:0;}100%{stroke-dashoffset:-360;}}" +
                ".p1{fill:none;stroke:#0095DB;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out;}" +
                ".p2{fill:none;stroke:#F9B218;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out 0.2s;}" +
                ".p3{fill:none;stroke:#E41E25;stroke-width:4.5;stroke-linecap:round;stroke-dasharray:360;animation:draw 2.1s infinite ease-in-out 0.4s;}" +
                "</style></head><body>" +
                "<div id='wrap'>" +
                "<svg id='fb' viewBox='0 0 256 256'>" +
                "<rect class='p2' x='98' y='42' width='140' height='86' rx='43' transform='rotate(-6 168 85)'/>" +
                "<path class='p1' d='M58,68 L134,64 A42,42 0 0,1 176,106 L176,192 A44,44 0 0,1 88,192 L88,150 L58,152 A42,42 0 0,1 16,110 A42,42 0 0,1 58,68 Z'/>" +
                "<rect class='p3' x='98' y='64' width='78' height='64' rx='30'/>" +
                "</svg>" +
                "</div></body></html>";

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
                            contenedorBarraFlotante.setVisibility(View.VISIBLE);
                            handler.postDelayed(this::actualizarEfectoBlurBarraGlass, 100);
                        }
                    })
                    .start();
        }, 2000);
    }

    /**
     * Regla exclusiva para los 5 botones de Configuración avanzada:
     * Solo permite tener 1 opción activa a la vez. Si activas "Proxy personalizado por cuenta",
     * abre el panel oscuro con las cuentas registradas.
     */
    private void intentarCambiarModoAvanzadoProxy(int modoClickeado) {
        if (modoAvanzadoProxy == modoClickeado) {
            modoAvanzadoProxy = MODO_PROXY_NINGUNO;
            prefs.edit().putInt("modo_avanzado_proxy_v1", modoAvanzadoProxy).apply();
            actualizarBotonesAjustes();
            for (SesionBot s : obtenerSesionesRegistradas()) {
                s.proxyActivoEnEstaCuenta = false;
                cerrarSocketsDeSesion(s);
                actualizarVisualTarjeta(s);
            }
            return;
        }

        if (modoAvanzadoProxy != MODO_PROXY_NINGUNO) {
            new AlertDialog.Builder(this)
                    .setTitle("Opción en uso")
                    .setMessage("Solo puedes activar una opción a la vez en Configuración avanzada.\n\nDesactiva primero la opción activa para seleccionar otra.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        modoAvanzadoProxy = modoClickeado;
        prefs.edit().putInt("modo_avanzado_proxy_v1", modoAvanzadoProxy).apply();
        actualizarBotonesAjustes();
        for (SesionBot s : obtenerSesionesRegistradas()) {
            if (s.obtenerModoProxyEfectivo() == MODO_PROXY_SIEMPRE_ACTIVO && proxyRotativoActivo) {
                s.proxyActivoEnEstaCuenta = true;
            } else {
                s.proxyActivoEnEstaCuenta = false;
            }
            cerrarSocketsDeSesion(s);
            actualizarVisualTarjeta(s);
        }
    }

    /**
     * Panel en modo oscuro para "Proxy personalizado por cuenta":
     * Muestra únicamente las cuentas registradas actualmente con un recuadro limpio por usuario
     * para elegir su modo de proxy independiente.
     */
    private void mostrarPopupConfiguracionProxyPorCuenta() {
        ArrayList<SesionBot> cuentasReg = obtenerSesionesRegistradas();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#18181B"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.parseColor("#18181B"));
        scroll.addView(root);

        TextView titulo = crearTexto("Proxy por cuenta", "#F4F4F5", 16f, true);
        TextView sub = crearTexto("Elige cómo funcionará el proxy en cada cuenta activa.", "#A1A1AA", 11f, false);
        sub.setPadding(0, dp(2), 0, dp(12));
        root.addView(titulo);
        root.addView(sub);

        if (cuentasReg.isEmpty()) {
            TextView vacio = crearTexto("No hay cuentas agregadas todavía.", "#A1A1AA", 12f, false);
            vacio.setPadding(dp(12), dp(14), dp(12), dp(14));
            vacio.setBackground(fondoRedondeadoConBorde("#202227", "#27272A", 10, 1));
            root.addView(vacio);
        } else {
            final String[] nombresModos = {
                    "Posición global",
                    "Siempre activo",
                    "En contraseña",
                    "En intervención"
            };
            final int[] codigosModos = {
                    MODO_PROXY_POSICION_GLOBAL,
                    MODO_PROXY_SIEMPRE_ACTIVO,
                    MODO_PROXY_EN_CONTRASENA,
                    MODO_PROXY_EN_INTERVENCION
            };

            for (final SesionBot cta : cuentasReg) {
                LinearLayout boxCta = new LinearLayout(this);
                boxCta.setOrientation(LinearLayout.VERTICAL);
                boxCta.setPadding(dp(12), dp(10), dp(12), dp(10));
                boxCta.setBackground(fondoRedondeadoConBorde("#202227", "#2E323B", 12, 1));
                LinearLayout.LayoutParams lpBox = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpBox.setMargins(0, 0, 0, dp(10));

                LinearLayout filaUser = new LinearLayout(this);
                filaUser.setOrientation(LinearLayout.HORIZONTAL);
                filaUser.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvUser = crearTexto(cta.usuario, "#F4F4F5", 13.5f, true);
                final TextView tvModoAct = crearTexto(cta.nombreCortoModoIndividual(), "#4ADE80", 10.5f, true);
                tvModoAct.setPadding(dp(8), dp(2), dp(8), dp(2));
                tvModoAct.setBackground(fondoRedondeadoConBorde("#14532D", "#22C55E", 8, 1));

                filaUser.addView(tvUser, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                filaUser.addView(tvModoAct);
                boxCta.addView(filaUser);

                LinearLayout filaOpciones1 = new LinearLayout(this);
                filaOpciones1.setOrientation(LinearLayout.HORIZONTAL);
                filaOpciones1.setPadding(0, dp(8), 0, dp(4));

                LinearLayout filaOpciones2 = new LinearLayout(this);
                filaOpciones2.setOrientation(LinearLayout.HORIZONTAL);

                final Button[] botonesCta = new Button[4];
                for (int idx = 0; idx < 4; idx++) {
                    final int modoVal = codigosModos[idx];
                    final Button bModo = new Button(this);
                    bModo.setText(nombresModos[idx]);
                    bModo.setAllCaps(false);
                    bModo.setTextSize(10.5f);
                    bModo.setTypeface(null, Typeface.BOLD);
                    bModo.setPadding(dp(6), 0, dp(6), 0);
                    botonesCta[idx] = bModo;

                    bModo.setOnClickListener(v -> {
                        cta.modoProxyIndividual = modoVal;
                        prefs.edit().putInt("modo_proxy_indiv_slot_" + cta.slotIndex, modoVal).apply();
                        tvModoAct.setText(cta.nombreCortoModoIndividual());
                        for (int k = 0; k < 4; k++) {
                            boolean sel = (codigosModos[k] == cta.modoProxyIndividual);
                            botonesCta[k].setTextColor(Color.parseColor(sel ? "#4ADE80" : "#A1A1AA"));
                            botonesCta[k].setBackground(fondoRedondeadoConBorde(
                                    sel ? "#14532D" : "#18181B",
                                    sel ? "#22C55E" : "#2E323B", 8, 1));
                        }
                        if (cta.obtenerModoProxyEfectivo() == MODO_PROXY_SIEMPRE_ACTIVO && proxyRotativoActivo) {
                            cta.proxyActivoEnEstaCuenta = true;
                        } else if (!cta.estaCorriendo()) {
                            cta.proxyActivoEnEstaCuenta = false;
                        }
                        cerrarSocketsDeSesion(cta);
                        actualizarVisualTarjeta(cta);
                    });

                    boolean sel = (modoVal == cta.modoProxyIndividual);
                    bModo.setTextColor(Color.parseColor(sel ? "#4ADE80" : "#A1A1AA"));
                    bModo.setBackground(fondoRedondeadoConBorde(
                            sel ? "#14532D" : "#18181B",
                            sel ? "#22C55E" : "#2E323B", 8, 1));

                    LinearLayout.LayoutParams lpBtnM = new LinearLayout.LayoutParams(0, dp(32), 1f);
                    lpBtnM.setMargins(dp(2), 0, dp(2), 0);
                    if (idx < 2) filaOpciones1.addView(bModo, lpBtnM);
                    else filaOpciones2.addView(bModo, lpBtnM);
                }

                boxCta.addView(filaOpciones1);
                boxCta.addView(filaOpciones2);
                root.addView(boxCta, lpBox);
            }
        }

        builder.setView(scroll);
        final AlertDialog dialog = builder.create();

        Button btnListo = crearBoton("Listo", "#27272A", "#F4F4F5", 12, dialog::dismiss);
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        lpClose.setMargins(0, dp(4), 0, 0);
        root.addView(btnListo, lpClose);

        dialog.show();
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
                } catch (Exception ignored) {}
            }
        }
    }

    private void refrescarDatosVistaPerfil() {
        if (lblNombrePerfil == null) return;
        String usr = prefs.getString("usr_licencia_activa_v3", LICENCIA_USUARIO_CLIENTE);
        String dev = prefs.getString("device_id_vinculado_v3", obtenerDeviceIdUnico());
        if (dev.equals("DESBLOQUEADO_MAESTRO") || dev.isEmpty()) {
            dev = obtenerDeviceIdUnico();
        }
        String fechaCreacion = prefs.getString("fecha_creacion_lic_v3", "Reciente");
        String ultimaCon = prefs.getString("ultima_conexion_lic_v3", "Hoy");
        int numCuentas = obtenerSesionesRegistradas().size();

        lblNombrePerfil.setText(usr);
        if (lblValorDevicePerfil != null) {
            lblValorDevicePerfil.setText(dev);
        }
        if (lblValorCuentasPerfil != null) {
            lblValorCuentasPerfil.setText(numCuentas + " / " + MAX_CUENTAS + " agregadas");
        }
        if (lblPieLicenciaPerfil != null) {
            lblPieLicenciaPerfil.setText(
                    "Licencia activa  •  Creada: " + fechaCreacion + "\nÚltima conexión: " + ultimaCon
            );
        }
    }

    private void asegurarJerarquiaPaginasSwipe() {
        if (contenedorPaginasSwipe == null || mainVertical == null) return;
        if (vistaAjustesScroll != null && vistaAjustesScroll.getParent() != contenedorPaginasSwipe) {
            ViewGroup p = (ViewGroup) vistaAjustesScroll.getParent();
            if (p != null) p.removeView(vistaAjustesScroll);
            contenedorPaginasSwipe.addView(vistaAjustesScroll, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        if (vistaPerfilScroll != null && vistaPerfilScroll.getParent() != contenedorPaginasSwipe) {
            ViewGroup p = (ViewGroup) vistaPerfilScroll.getParent();
            if (p != null) p.removeView(vistaPerfilScroll);
            contenedorPaginasSwipe.addView(vistaPerfilScroll, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        if (contenedorPaginasSwipe.getParent() == null) {
            mainVertical.addView(contenedorPaginasSwipe, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
        if (panelAdvPorCuenta != null) {
            panelAdvPorCuenta.setOnClickListener(v -> {
                if (modoAvanzadoProxy == MODO_PROXY_PERSONALIZADO_CUENTA) {
                    mostrarPopupConfiguracionProxyPorCuenta();
                }
            });
        }
    }

    private void aplicarTemaVisual() {
        asegurarJerarquiaPaginasSwipe();

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

        if (cardEstadoVacioCuentas != null) {
            cardEstadoVacioCuentas.setBackground(fondoBordePunteado(bgCard, borderCard, 16));
        }
        if (lblEmptyTitulo != null) lblEmptyTitulo.setTextColor(Color.parseColor(txtPri));
        if (lblEmptySub != null) lblEmptySub.setTextColor(Color.parseColor(txtSec));

        if (cardPerfilPrincipal != null) {
            cardPerfilPrincipal.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 18, 1));
        }
        if (lblNombrePerfil != null) lblNombrePerfil.setTextColor(Color.parseColor(txtPri));
        if (boxPerfilDevice != null) boxPerfilDevice.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 12, 1));
        if (boxPerfilCuentas != null) boxPerfilCuentas.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 12, 1));
        if (boxPerfilClave != null) boxPerfilClave.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 12, 1));
        if (lblValorClavePerfil != null) lblValorClavePerfil.setTextColor(Color.parseColor(txtPri));

        if (lblTituloComprasExitosas != null) {
            lblTituloComprasExitosas.setTextColor(Color.parseColor(temaActual == 2 ? "#15803D" : "#4ADE80"));
        }
        if (lblSubComprasExitosas != null) {
            lblSubComprasExitosas.setTextColor(Color.parseColor(txtSec));
        }

        if (lblTRel != null) lblTRel.setTextColor(Color.parseColor(txtPri));
        if (lblReloginVal != null) lblReloginVal.setTextColor(Color.parseColor(txtPri));

        if (lblSubTema != null) lblSubTema.setTextColor(Color.parseColor(txtPri));
        if (lblDescTema != null) lblDescTema.setTextColor(Color.parseColor(txtSec));
        if (lblSubAlertas != null) lblSubAlertas.setTextColor(Color.parseColor(txtPri));
        if (lblDescAlertas != null) lblDescAlertas.setTextColor(Color.parseColor(txtSec));

        if (lblItemBarraGlass != null) lblItemBarraGlass.setTextColor(Color.parseColor(txtPri));
        if (lblDescItemBarraGlass != null) lblDescItemBarraGlass.setTextColor(Color.parseColor(txtSec));

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

        if (lblAdvPosGlobal != null) lblAdvPosGlobal.setTextColor(Color.parseColor(txtPri));
        if (lblDescAdvPosGlobal != null) lblDescAdvPosGlobal.setTextColor(Color.parseColor(txtSec));
        if (lblAdvSiempre != null) lblAdvSiempre.setTextColor(Color.parseColor(txtPri));
        if (lblDescAdvSiempre != null) lblDescAdvSiempre.setTextColor(Color.parseColor(txtSec));
        if (lblAdvPass != null) lblAdvPass.setTextColor(Color.parseColor(txtPri));
        if (lblDescAdvPass != null) lblDescAdvPass.setTextColor(Color.parseColor(txtSec));
        if (lblAdvIntervencion != null) lblAdvIntervencion.setTextColor(Color.parseColor(txtPri));
        if (lblDescAdvIntervencion != null) lblDescAdvIntervencion.setTextColor(Color.parseColor(txtSec));
        if (lblAdvPorCuenta != null) lblAdvPorCuenta.setTextColor(Color.parseColor(txtPri));
        if (lblDescAdvPorCuenta != null) lblDescAdvPorCuenta.setTextColor(Color.parseColor(txtSec));

        if (btnTextoConfigAvanzada != null) {
            btnTextoConfigAvanzada.setTextColor(Color.parseColor(txtPri));
            btnTextoConfigAvanzada.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 12, 1));
        }

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

        if (panelBarraGlass != null) panelBarraGlass.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemTurbo != null) panelItemTurbo.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemProxy != null) panelItemProxy.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemOrd != null) panelItemOrd.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemCompra != null) panelItemCompra.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelItemVib != null) panelItemVib.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));

        if (panelAdvPosGlobal != null) panelAdvPosGlobal.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelAdvSiempre != null) panelAdvSiempre.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelAdvPass != null) panelAdvPass.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelAdvIntervencion != null) panelAdvIntervencion.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));
        if (panelAdvPorCuenta != null) panelAdvPorCuenta.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 14, 1));

        if (scrollHistorialConsola != null) {
            scrollHistorialConsola.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 12, 1));
        }
        if (txtHistorialConsola != null) {
            txtHistorialConsola.setTextColor(Color.parseColor(colTextoConsola()));
        }

        actualizarTarjetasPreviewTema();
        actualizarBotonesAjustes();
        actualizarEstiloBarraFlotante();
        actualizarEfectoBlurBarraGlass();
        actualizarResumenYBotonesGlobales();
        reconstruirListaComprasExitosas();
        refrescarDatosVistaPerfil();
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

        if (panelBarraGlass != null) {
            View vImg = panelBarraGlass.findViewWithTag("IMG_GLASS_PREVIEW");
            if (vImg instanceof ImageView) {
                ((ImageView) vImg).setImageBitmap(crearImagenPreviewBarraGlass(barraGlassActiva));
            }
        }

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
        configurarBotonInterruptor(btnToggleBarraGlass, barraGlassActiva);
        configurarBotonInterruptor(btnToggleTurbo, modoTurboSinImagenes);
        configurarBotonInterruptor(btnToggleProxy, proxyRotativoActivo);
        configurarBotonInterruptor(btnToggleOrd, alertaOrdinariaActiva);
        configurarBotonInterruptor(btnToggleCompra, alertaCompraActiva);

        configurarBotonInterruptor(btnToggleAdvPosGlobal, modoAvanzadoProxy == MODO_PROXY_POSICION_GLOBAL);
        configurarBotonInterruptor(btnToggleAdvSiempre, modoAvanzadoProxy == MODO_PROXY_SIEMPRE_ACTIVO);
        configurarBotonInterruptor(btnToggleAdvPass, modoAvanzadoProxy == MODO_PROXY_EN_CONTRASENA);
        configurarBotonInterruptor(btnToggleAdvIntervencion, modoAvanzadoProxy == MODO_PROXY_EN_INTERVENCION);
        configurarBotonInterruptor(btnToggleAdvPorCuenta, modoAvanzadoProxy == MODO_PROXY_PERSONALIZADO_CUENTA);
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
        pestanaInferiorActual = Math.max(0, Math.min(3, nuevaPestana));
        acumuladorBajandoScroll = 0f;
        aplicarTraduccionProgresivaBarra(0f);

        if (vistaCuentasScroll != null) {
            vistaCuentasScroll.setTranslationX(0f);
            vistaCuentasScroll.setVisibility(pestanaInferiorActual == 0 ? View.VISIBLE : View.GONE);
        }
        if (vistaHistorialScroll != null) {
            vistaHistorialScroll.setTranslationX(0f);
            vistaHistorialScroll.setVisibility(pestanaInferiorActual == 1 ? View.VISIBLE : View.GONE);
        }
        if (vistaAjustesScroll != null) {
            vistaAjustesScroll.setTranslationX(0f);
            vistaAjustesScroll.setVisibility(pestanaInferiorActual == 2 ? View.VISIBLE : View.GONE);
        }
        if (vistaPerfilScroll != null) {
            vistaPerfilScroll.setTranslationX(0f);
            vistaPerfilScroll.setVisibility(pestanaInferiorActual == 3 ? View.VISIBLE : View.GONE);
        }

        if (badgeEstadoGlobal != null) {
            badgeEstadoGlobal.setVisibility(pestanaInferiorActual == 0 ? View.VISIBLE : View.GONE);
        }

        if (pestanaInferiorActual == 1) {
            reconstruirFiltrosHistorial();
            refrescarVistaHistorial();
            reconstruirListaComprasExitosas();
        } else if (pestanaInferiorActual == 3) {
            refrescarDatosVistaPerfil();
        }
        actualizarEstiloBarraFlotante();
        handler.postDelayed(this::actualizarEfectoBlurBarraGlass, 45);
    }

    private void actualizarEstiloBarraFlotante() {
        String bgCard = colFondoTarjeta();
        String borderCard = colBordeTarjeta();
        String txtInact = barraGlassActiva
                ? (temaActual == 2 ? "#1E293B" : "#E2E8F0")
                : colTextoSecundario();
        String acento = colAcentoTema();

        boolean enCuentas = (pestanaInferiorActual == 0);
        boolean enHist = (pestanaInferiorActual == 1);
        boolean enAjustes = (pestanaInferiorActual == 2);
        boolean enPerfil = (pestanaInferiorActual == 3);

        if (barraFlotante != null) {
            if (barraGlassActiva) {
                barraFlotante.setBackground(fondoCristalIphone(temaActual == 2));
                if (imgFondoBlurGlass != null) imgFondoBlurGlass.setVisibility(View.VISIBLE);
                if (vistaBrilloEspecularGlass != null) {
                    vistaBrilloEspecularGlass.setVisibility(View.VISIBLE);
                    vistaBrilloEspecularGlass.setBackground(fondoRedondeado("#AAFFFFFF", 20));
                }
            } else {
                barraFlotante.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 22, 1));
                if (imgFondoBlurGlass != null) imgFondoBlurGlass.setVisibility(View.GONE);
                if (vistaBrilloEspecularGlass != null) vistaBrilloEspecularGlass.setVisibility(View.GONE);
            }
        }

        if (indicadorDeslizanteBarra != null) {
            FrameLayout.LayoutParams lpInd = (FrameLayout.LayoutParams) indicadorDeslizanteBarra.getLayoutParams();
            lpInd.leftMargin = dp(4);
            indicadorDeslizanteBarra.setLayoutParams(lpInd);
            indicadorDeslizanteBarra.setBackground(barraGlassActiva
                    ? fondoCapsulaActivaGlass(temaActual == 2)
                    : fondoRedondeado(acento, 16));
            indicadorDeslizanteBarra.animate()
                    .translationX(pestanaInferiorActual * dp(71))
                    .setDuration(190)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        navBtnCuentas.setBackgroundColor(Color.TRANSPARENT);
        navIconCuentas.setImageDrawable(crearIconoVectorial("USERS", enCuentas ? "#FFFFFF" : txtInact, 15));
        navTxtCuentas.setTextColor(Color.parseColor(enCuentas ? "#FFFFFF" : txtInact));

        navBtnHistorial.setBackgroundColor(Color.TRANSPARENT);
        navIconHistorial.setImageDrawable(crearIconoVectorial("HISTORY", enHist ? "#FFFFFF" : txtInact, 15));
        navTxtHistorial.setTextColor(Color.parseColor(enHist ? "#FFFFFF" : txtInact));

        navBtnAjustes.setBackgroundColor(Color.TRANSPARENT);
        navIconAjustes.setImageDrawable(crearIconoVectorial("SETTINGS", enAjustes ? "#FFFFFF" : txtInact, 15));
        navTxtAjustes.setTextColor(Color.parseColor(enAjustes ? "#FFFFFF" : txtInact));

        if (navBtnPerfil != null) {
            navBtnPerfil.setBackgroundColor(Color.TRANSPARENT);
            navIconPerfil.setImageDrawable(crearIconoVectorial("PROFILE", enPerfil ? "#FFFFFF" : txtInact, 15));
            navTxtPerfil.setTextColor(Color.parseColor(enPerfil ? "#FFFFFF" : txtInact));
        }
    }

    private void aplicarEscalaWebView(SesionBot s) {
        if (s == null || s.webViewOculto == null) return;
        int z = s.zoomPorcentaje;
        s.webViewOculto.setInitialScale(z);
        try {
            s.webViewOculto.getSettings().setTextZoom(z);
        } catch (Exception ignored) {}
        if (s.btnZoomUrlCard != null) {
            s.btnZoomUrlCard.setText(z + "%");
        }
    }

    private void sincronizarVistaPanelCuenta(SesionBot s) {
        if (s == null || s.contenedorDesplegableCard == null || s.webViewOculto == null) return;
        s.contenedorDesplegableCard.setVisibility(s.panelDesplegado ? View.VISIBLE : View.GONE);

        if (s.frameConsolaInterna != null && s.btnToggleConsolaMini != null) {
            s.frameConsolaInterna.setVisibility(s.mostrarConsolaEnPanel ? View.VISIBLE : View.GONE);
            s.btnToggleConsolaMini.setText(s.mostrarConsolaEnPanel ? "▲ Ocultar consola" : "▼ Mostrar consola");
        }

        ViewGroup parentActual = (ViewGroup) s.webViewOculto.getParent();
        if (s.panelDesplegado) {
            if (parentActual != s.marcoWebViewCard) {
                if (parentActual != null) parentActual.removeView(s.webViewOculto);
                s.marcoWebViewCard.addView(s.webViewOculto, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            aplicarEscalaWebView(s);
            if (s.scrollConsolaCard != null && s.mostrarConsolaEnPanel) {
                s.scrollConsolaCard.post(() -> s.scrollConsolaCard.fullScroll(View.FOCUS_DOWN));
            }
        } else {
            FrameLayout slotFondo = slotsFondoWebView[s.slotIndex];
            if (parentActual != slotFondo) {
                if (parentActual != null) parentActual.removeView(s.webViewOculto);
                slotFondo.addView(s.webViewOculto, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
    }

    private boolean esRecursoPesadoBloqueable(String urlLower) {
        if (!modoTurboSinImagenes || urlLower == null) return false;
        String pathOnly = urlLower;
        int qIdx = pathOnly.indexOf('?');
        if (qIdx != -1) pathOnly = pathOnly.substring(0, qIdx);
        return pathOnly.endsWith(".png") || pathOnly.endsWith(".jpg") || pathOnly.endsWith(".jpeg")
                || pathOnly.endsWith(".gif") || pathOnly.endsWith(".webp") || pathOnly.endsWith(".svg")
                || pathOnly.endsWith(".ico") || pathOnly.endsWith(".woff") || pathOnly.endsWith(".woff2")
                || pathOnly.endsWith(".ttf") || pathOnly.endsWith(".otf") || pathOnly.endsWith(".eot")
                || pathOnly.endsWith(".mp3") || pathOnly.endsWith(".wav")
                || urlLower.contains("/sounds/") || urlLower.contains("/img/")
                || urlLower.contains("feather.min.js") || urlLower.contains("animate.min.css");
    }

    private boolean esLibreriaEstaticaDirecta(String urlLower) {
        if (urlLower == null || !urlLower.contains("tesoroenlinea.bt.com.ve")) return false;
        String pathOnly = urlLower;
        int qIdx = pathOnly.indexOf('?');
        if (qIdx != -1) pathOnly = pathOnly.substring(0, qIdx);
        return (pathOnly.endsWith(".js") || pathOnly.endsWith(".css") || pathOnly.endsWith(".json"))
                && !pathOnly.contains("/livewire/");
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void inicializarMotoresOcultos() {
        for (int i = 0; i < MAX_CUENTAS; i++) {
            final SesionBot sesion = sesiones[i];

            FrameLayout slotFondo = new FrameLayout(this);
            slotsFondoWebView[i] = slotFondo;
            LinearLayout.LayoutParams lpSlotFondo = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i < 2) filaMotoresSup.addView(slotFondo, lpSlotFondo);
            else filaMotoresInf.addView(slotFondo, lpSlotFondo);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(fondoRedondeadoConBorde("#18181B", "#27272A", 14, 1));
            card.setOnClickListener(v -> {
                sesion.panelDesplegado = !sesion.panelDesplegado;
                sincronizarVistaPanelCuenta(sesion);
            });

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

            TextView lblSaldos = crearTexto("Bs: --   •   USD: --   •   Orden: 300$", "#38BDF8", 11f, true);
            lblSaldos.setPadding(0, dp(4), 0, dp(1));
            card.addView(lblSaldos);

            TextView lblPrx = crearTexto(sesion.textoEstadoProxyMostrado(), "#34D399", 10f, true);
            lblPrx.setPadding(0, dp(1), 0, dp(2));
            card.addView(lblPrx);

            TextView lblPaso = crearTexto("Sin actividad todavía", "#A1A1AA", 11.5f, false);
            lblPaso.setPadding(0, dp(1), 0, dp(6));
            card.addView(lblPaso);

            LinearLayout filaAcciones = new LinearLayout(this);
            filaAcciones.setOrientation(LinearLayout.HORIZONTAL);
            filaAcciones.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            ImageView bPlay = crearBotonAccionTarjeta("PLAY", "#14532D", "#4ADE80", () -> {
                if (!sesion.estaCorriendo() || sesion.detenerTrasCerrarSesion) iniciarSesionIndividual(sesion, 0);
            });
            ImageView bStop = crearBotonAccionTarjeta("STOP", "#450A0A", "#F87171", () -> {
                if (sesion.estaCorriendo()) pausarSesionSinCerrar(sesion, true);
            });
            ImageView bLogoutStop = crearBotonAccionTarjeta("LOGOUT", "#431407", "#FB923C", () -> cerrarSesionYDetenerCuenta(sesion));
            ImageView bEye = crearBotonAccionTarjeta("EYE", "#1E293B", "#38BDF8", () -> {
                sesion.nombreOculto = !sesion.nombreOculto;
                actualizarVisualTarjeta(sesion);
                if (pestanaInferiorActual == 1) {
                    reconstruirFiltrosHistorial();
                    refrescarVistaHistorial();
                    reconstruirListaComprasExitosas();
                }
            });
            ImageView bEdit = crearBotonAccionTarjeta("EDIT", "#27272A", "#E4E4E7", () -> mostrarPopupEditorCuenta(sesion.slotIndex));

            LinearLayout.LayoutParams lpAct = new LinearLayout.LayoutParams(dp(30), dp(26));
            lpAct.setMargins(dp(4), 0, 0, 0);
            filaAcciones.addView(bPlay, lpAct);
            filaAcciones.addView(bStop, lpAct);
            filaAcciones.addView(bLogoutStop, lpAct);
            filaAcciones.addView(bEye, lpAct);
            filaAcciones.addView(bEdit, lpAct);
            card.addView(filaAcciones);

            LinearLayout boxDesplegable = new LinearLayout(this);
            boxDesplegable.setOrientation(LinearLayout.VERTICAL);
            boxDesplegable.setVisibility(View.GONE);
            boxDesplegable.setPadding(0, dp(8), 0, 0);
            boxDesplegable.setClickable(true);

            // Cabecera limpia sin el texto "Consola y Página del Banco en Vivo" y con botón en Modo Oscuro
            LinearLayout filaCabeceraConsola = new LinearLayout(this);
            filaCabeceraConsola.setOrientation(LinearLayout.HORIZONTAL);
            filaCabeceraConsola.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            filaCabeceraConsola.setPadding(0, 0, 0, dp(5));

            TextView btnToggleCon = crearTexto("▲ Ocultar consola", "#E4E4E7", 9.5f, true);
            btnToggleCon.setPadding(dp(10), dp(3), dp(10), dp(3));
            btnToggleCon.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 7, 1));
            btnToggleCon.setOnClickListener(v -> {
                sesion.mostrarConsolaEnPanel = !sesion.mostrarConsolaEnPanel;
                prefs.edit().putBoolean("mostrar_consola_slot_" + sesion.slotIndex, sesion.mostrarConsolaEnPanel).apply();
                sincronizarVistaPanelCuenta(sesion);
            });

            filaCabeceraConsola.addView(btnToggleCon);
            boxDesplegable.addView(filaCabeceraConsola);

            FrameLayout frameCon = new FrameLayout(this);
            LinearLayout.LayoutParams lpFrameCon = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(105));
            lpFrameCon.setMargins(0, 0, 0, dp(8));

            ScrollView scrollCon = new ScrollView(this);
            scrollCon.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 10, 1));
            scrollCon.setPadding(dp(10), dp(6), dp(10), dp(6));
            scrollCon.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            });

            TextView txtCon = new TextView(this);
            txtCon.setTextColor(Color.parseColor("#E4E4E7"));
            txtCon.setTextSize(9.5f);
            txtCon.setTypeface(Typeface.MONOSPACE);
            scrollCon.addView(txtCon);
            frameCon.addView(scrollCon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView btnLimpiarMini = new TextView(this);
            btnLimpiarMini.setText("Limpiar");
            btnLimpiarMini.setTextColor(Color.parseColor("#A1A1AA"));
            btnLimpiarMini.setTextSize(8f);
            btnLimpiarMini.setTypeface(null, Typeface.BOLD);
            btnLimpiarMini.setPadding(dp(6), dp(2), dp(6), dp(2));
            btnLimpiarMini.setBackground(fondoRedondeadoConBorde("#18181B", "#3F3F46", 5, 1));
            btnLimpiarMini.setOnClickListener(v -> txtCon.setText(""));

            FrameLayout.LayoutParams lpLimp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            lpLimp.setMargins(0, dp(4), dp(6), 0);
            frameCon.addView(btnLimpiarMini, lpLimp);
            boxDesplegable.addView(frameCon, lpFrameCon);

            // Barra de Navegador en Modo Oscuro (Atrás, Recargar y Zoom en el mismo tono oscuro del bot)
            LinearLayout barraNav = new LinearLayout(this);
            barraNav.setOrientation(LinearLayout.HORIZONTAL);
            barraNav.setGravity(Gravity.CENTER_VERTICAL);
            barraNav.setPadding(dp(6), dp(5), dp(6), dp(5));
            barraNav.setBackground(fondoRedondeadoConBorde("#14161A", "#27272A", 10, 1));
            LinearLayout.LayoutParams lpBarraNav = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpBarraNav.setMargins(0, 0, 0, dp(6));

            ImageView btnBack = crearBotonAccionTarjetaOscuro("BACK", "#202227", "#3F3F46", "#E4E4E7", () -> {
                if (sesion.webViewOculto != null && sesion.webViewOculto.canGoBack()) sesion.webViewOculto.goBack();
            });
            ImageView btnReload = crearBotonAccionTarjetaOscuro("RELOAD", "#202227", "#3F3F46", "#E4E4E7", () -> {
                if (sesion.webViewOculto != null) sesion.webViewOculto.reload();
            });

            LinearLayout cajaUrl = new LinearLayout(this);
            cajaUrl.setOrientation(LinearLayout.HORIZONTAL);
            cajaUrl.setGravity(Gravity.CENTER_VERTICAL);
            cajaUrl.setPadding(dp(8), dp(4), dp(6), dp(4));
            cajaUrl.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 8, 1));

            TextView txtUrl = crearTexto(URL_LOGIN, "#A1A1AA", 9.5f, false);
            txtUrl.setSingleLine(true);
            txtUrl.setEllipsize(TextUtils.TruncateAt.END);

            TextView btnZoom = crearTexto(sesion.zoomPorcentaje + "%", "#E4E4E7", 9.5f, true);
            btnZoom.setPadding(dp(8), dp(2), dp(8), dp(2));
            btnZoom.setBackground(fondoRedondeadoConBorde("#202227", "#3F3F46", 6, 1));
            btnZoom.setOnClickListener(v -> {
                int idxZ = 0;
                for (int k = 0; k < ESCALAS_ZOOM.length; k++) {
                    if (ESCALAS_ZOOM[k] == sesion.zoomPorcentaje) { idxZ = k; break; }
                }
                sesion.zoomPorcentaje = ESCALAS_ZOOM[(idxZ + 1) % ESCALAS_ZOOM.length];
                prefs.edit().putInt("zoom_web_v2_slot_" + sesion.slotIndex, sesion.zoomPorcentaje).apply();
                aplicarEscalaWebView(sesion);
            });

            cajaUrl.addView(txtUrl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            cajaUrl.addView(btnZoom);

            LinearLayout.LayoutParams lpNavBtn = new LinearLayout.LayoutParams(dp(28), dp(26));
            lpNavBtn.setMargins(0, 0, dp(4), 0);
            barraNav.addView(btnBack, lpNavBtn);
            barraNav.addView(btnReload, lpNavBtn);
            barraNav.addView(cajaUrl, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            boxDesplegable.addView(barraNav, lpBarraNav);

            FrameLayout marcoWeb = new FrameLayout(this);
            marcoWeb.setBackground(fondoRedondeadoConBorde("#0F1115", "#27272A", 10, 1));
            marcoWeb.setPadding(dp(2), dp(2), dp(2), dp(2));
            boxDesplegable.addView(marcoWeb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(370)));
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
            sesion.marcoWebViewCard = marcoWeb;

            WebView wv = new WebView(this);
            wv.setBackgroundColor(Color.WHITE);
            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            configurarPerfilAisladoWebView(wv, i);

            wv.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP) v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            });

            WebSettings ws = wv.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setDatabaseEnabled(true);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
            ws.setSupportZoom(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setRenderPriority(WebSettings.RenderPriority.HIGH);
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
                }
            });

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (request != null && request.getUrl() != null) {
                        String fullUrl = request.getUrl().toString();
                        String uLower = fullUrl.toLowerCase(Locale.ROOT);
                        if (esRecursoPesadoBloqueable(uLower)) {
                            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                        }
                        if (esLibreriaEstaticaDirecta(uLower)) {
                            WebResourceResponse respRam = obtenerArchivoEstaticoDesdeRamODirecto(fullUrl);
                            if (respRam != null) return respRam;
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    ultimoSlotActivoRed = sesion.slotIndex;
                    sesion.cargandoRed = true;
                    sesion.inicioCargaRedMs = System.currentTimeMillis();
                    if (url != null) {
                        sesion.urlActualTexto = url;
                        if (sesion.txtUrlCard != null) sesion.txtUrlCard.setText(url);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    sesion.cargandoRed = false;
                    sesion.progresoWeb = 100;
                    if (sesion.inicioCargaRedMs > 0) {
                        sesion.latenciaMs = Math.max(15, System.currentTimeMillis() - sesion.inicioCargaRedMs);
                    }
                    if (url != null) {
                        sesion.urlActualTexto = url;
                        if (sesion.txtUrlCard != null) sesion.txtUrlCard.setText(url);
                    }
                }

                @Override
                public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handlerAuth, String host, String realm) {
                    if (handlerAuth != null) handlerAuth.proceed(PRX_USER_RAW, construirPasswordProxyVenezuela(sesion));
                }

                @SuppressLint("WebViewClientOnReceivedSslError")
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handlerSsl, SslError error) {
                    if (handlerSsl != null) handlerSsl.proceed();
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        sesion.cargandoRed = false;
                        sesion.progresoWeb = 100;
                    }
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    return true;
                }
            });

            sesion.webViewOculto = wv;
            slotFondo.addView(wv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            aplicarEscalaWebView(sesion);
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
        iv.setOnClickListener(v -> { if (accion != null) accion.run(); });
        return iv;
    }

    private ImageView crearBotonAccionTarjetaOscuro(String icono, String bgHex, String borderHex, String iconHex, Runnable accion) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setClickable(true);
        iv.setFocusable(false);
        iv.setImageDrawable(crearIconoVectorial(icono, iconHex, 12));
        iv.setBackground(fondoRedondeadoConBorde(bgHex, borderHex, 7, 1));
        iv.setOnClickListener(v -> { if (accion != null) accion.run(); });
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

    private ArrayList<SesionBot> obtenerSesionesRegistradas() {
        ArrayList<SesionBot> lista = new ArrayList<>();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            if (sesiones[i].registrada && !sesiones[i].usuario.isEmpty()) lista.add(sesiones[i]);
        }
        return lista;
    }

    private int contarCuentasCorriendo() {
        int c = 0;
        for (SesionBot s : obtenerSesionesRegistradas()) if (s.estaCorriendo()) c++;
        return c;
    }

    private void reconstruirListaTarjetas() {
        contenedorTarjetasCuentas.removeAllViews();
        for (int i = 0; i < MAX_CUENTAS; i++) {
            ViewGroup p = (ViewGroup) sesiones[i].cardView.getParent();
            if (p != null) p.removeView(sesiones[i].cardView);
        }

        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        if (cardEstadoVacioCuentas != null) {
            cardEstadoVacioCuentas.setVisibility(reg.isEmpty() ? View.VISIBLE : View.GONE);
        }

        for (SesionBot s : reg) {
            LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpCard.setMargins(0, 0, 0, dp(9));
            contenedorTarjetasCuentas.addView(s.cardView, lpCard);
            sincronizarVistaPanelCuenta(s);
            actualizarVisualTarjeta(s);
        }

        actualizarResumenYBotonesGlobales();
        refrescarDatosVistaPerfil();
    }

    private void actualizarVisualTarjeta(SesionBot s) {
        if (s == null || s.cardView == null) return;
        boolean corriendo = s.estaCorriendo();
        boolean enFormularioDorado = s.estaEnFormularioCompra();
        long ahora = System.currentTimeMillis();
        boolean titilandoCompraVerde = s.compraCompletada && (ahora - s.tiempoCompraExitosaMs <= 3000);
        int modoEf = s.obtenerModoProxyEfectivo();

        String bgCard = colFondoTarjeta();
        String bgSub = colFondoSubPanel();
        String borderCard = colBordeTarjeta();
        String txtPri = colTextoPrimario();
        String txtSec = colTextoSecundario();

        Drawable dot = crearCirculoNeon(corriendo || s.compraCompletada, (corriendo || titilandoCompraVerde) ? pulsoNeon : true, 11);
        ponerIconoYTexto(s.lblUsuarioCard, dot, s.nombreMostrado());
        if (enFormularioDorado) s.lblUsuarioCard.setTextColor(Color.parseColor("#FACC15"));
        else if (s.compraCompletada || corriendo) s.lblUsuarioCard.setTextColor(Color.parseColor(temaActual == 2 ? "#15803D" : "#4ADE80"));
        else s.lblUsuarioCard.setTextColor(Color.parseColor(txtSec));

        if (s.compraCompletada) {
            if (titilandoCompraVerde) {
                s.cardView.setBackground(fondoRedondeadoConBorde(
                        temaActual == 2 ? "#DCFCE7" : "#0D2818", pulsoNeon ? "#00FF66" : "#14532D", 14, pulsoNeon ? 3 : 1));
            } else {
                s.cardView.setBackground(fondoRedondeadoConBorde(temaActual == 2 ? "#DCFCE7" : "#0D2818", "#00FF66", 14, 2));
            }
        } else if (enFormularioDorado) {
            s.cardView.setBackground(fondoRedondeadoConBorde(bgCard, pulsoNeon ? "#FACC15" : "#EAB308", 14, 2));
        } else if (corriendo) {
            s.cardView.setBackground(fondoRedondeadoConBorde(bgCard, temaActual == 1 ? "#38BDF8" : "#22C55E", 14, 1));
        } else {
            s.cardView.setBackground(fondoRedondeadoConBorde(bgCard, borderCard, 14, 1));
        }

        s.badgeEstadoCard.setText(nombreCortoEstado(s));
        if (s.compraCompletada) {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#00FF66"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#064E3B", "#00FF66", 10, 1));
        } else if (enFormularioDorado) {
            s.badgeEstadoCard.setTextColor(Color.parseColor("#FDE047"));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde("#422006", "#FACC15", 10, 1));
        } else if (corriendo) {
            boolean pingAlto = (s.latenciaMs > 1800);
            s.badgeEstadoCard.setTextColor(Color.parseColor(pingAlto ? "#FBBF24" : (temaActual == 1 ? "#38BDF8" : "#4ADE80")));
            s.badgeEstadoCard.setBackground(fondoRedondeadoConBorde(
                    pingAlto ? "#451A03" : (temaActual == 1 ? "#1E3A8A" : "#14532D"),
                    pingAlto ? "#F59E0B" : (temaActual == 1 ? "#38BDF8" : "#22C55E"), 10, 1));
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
            s.lblProxyCard.setTextColor(Color.parseColor(proxyRotativoActivo && modoEf != MODO_PROXY_NINGUNO
                    ? (s.proxyActivoEnEstaCuenta || modoEf == MODO_PROXY_SIEMPRE_ACTIVO
                        ? (temaActual == 2 ? "#0D9488" : "#34D399") : "#FBBF24")
                    : txtSec));
        }

        s.lblSubtituloPasoCard.setText(s.ultimoPasoTexto);
        s.lblSubtituloPasoCard.setTextColor(Color.parseColor(
                s.compraCompletada ? (temaActual == 2 ? "#15803D" : "#00FF66")
                        : (enFormularioDorado ? "#FACC15" : (corriendo ? txtPri : txtSec))
        ));

        if (s.scrollConsolaCard != null) s.scrollConsolaCard.setBackground(fondoRedondeadoConBorde(colFondoConsola(), borderCard, 10, 1));
        if (s.txtConsolaCard != null) s.txtConsolaCard.setTextColor(Color.parseColor(colTextoConsola()));

        if (s.btnToggleConsolaMini != null) {
            s.btnToggleConsolaMini.setTextColor(Color.parseColor(txtPri));
            s.btnToggleConsolaMini.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 7, 1));
        }
        if (s.btnNavBackCard != null) {
            s.btnNavBackCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 7, 1));
            s.btnNavBackCard.setImageDrawable(crearIconoVectorial("BACK", txtPri, 12));
        }
        if (s.btnNavReloadCard != null) {
            s.btnNavReloadCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 7, 1));
            s.btnNavReloadCard.setImageDrawable(crearIconoVectorial("RELOAD", txtPri, 12));
        }
        if (s.btnZoomUrlCard != null) {
            s.btnZoomUrlCard.setTextColor(Color.parseColor(txtPri));
            s.btnZoomUrlCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 6, 1));
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

        s.btnEditCard.setBackground(fondoRedondeadoConBorde(bgSub, borderCard, 7, 1));
        s.btnEditCard.setImageDrawable(crearIconoVectorial("EDIT", txtPri, 12));
    }

    private void actualizarResumenYBotonesGlobales() {
        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        int total = reg.size();
        int activas = contarCuentasCorriendo();
        int detenidas = Math.max(0, total - activas);

        if (cardEstadoVacioCuentas != null) cardEstadoVacioCuentas.setVisibility(total == 0 ? View.VISIBLE : View.GONE);

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
        String txtStartOff = (temaActual == 2) ? "#15803D" : "#86EFAC";
        btnIniciarTodas.setEnabled(hayParaIniciar);
        btnIniciarTodas.setBackground(fondoRedondeado(hayParaIniciar ? colBotonIniciarActivo() : colBotonIniciarInactivo(), 10));
        ponerIconoYTexto(btnIniciarTodas, crearIconoVectorial("PLAY", hayParaIniciar ? "#FFFFFF" : txtStartOff, 14), "Iniciar todas");
        btnIniciarTodas.setTextColor(Color.parseColor(hayParaIniciar ? "#FFFFFF" : txtStartOff));

        String txtStopOff = (temaActual == 2) ? "#B91C1C" : "#FCA5A5";
        btnDetenerTodas.setEnabled(hayParaDetener);
        btnDetenerTodas.setBackground(fondoRedondeado(hayParaDetener ? colBotonDetenerActivo() : colBotonDetenerInactivo(), 10));
        ponerIconoYTexto(btnDetenerTodas, crearIconoVectorial("STOP", hayParaDetener ? "#FFFFFF" : txtStopOff, 14), "Detener todas");
        btnDetenerTodas.setTextColor(Color.parseColor(hayParaDetener ? "#FFFFFF" : txtStopOff));
    }

    private void actualizarSaldosEnSegundoPlano(final SesionBot s, boolean permitirFetchRemoto) {
        if (s == null || s.webViewOculto == null) return;
        long ahora = System.currentTimeMillis();
        if (!permitirFetchRemoto && (ahora - s.ultimaActualizacionSaldoMs < 5000)) return;
        s.ultimaActualizacionSaldoMs = ahora;

        String bloqueFetchPg = permitirFetchRemoto ? (
                "var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "var tok = meta ? meta.getAttribute('content') : '';" +
                "var rPg = await fetch('https://tesoroenlinea.bt.com.ve/pg', {method:'GET', headers:{'X-Requested-With':'XMLHttpRequest','X-CSRF-TOKEN':tok}, credentials:'same-origin'});" +
                "window.__vbt_ping = Math.max(10, Date.now() - t0);" +
                "if (rPg.status === 200) { var docPg = new DOMParser().parseFromString(await rPg.text(), 'text/html'); extraerDesdeRaiz(docPg); }"
        ) : "";

        String jsBalanceExacto =
                "(async function(){ try {" +
                "  var t0 = Date.now();" +
                "  function parseMontoVE(str) { if(!str) return NaN; var s=String(str).trim(); if(s.indexOf(',')!==-1) s=s.replace(/\\./g,'').replace(',','.'); return parseFloat(s); }" +
                "  function fmtVE(num) { return Number(num).toLocaleString('es-VE', {minimumFractionDigits:2, maximumFractionDigits:2}); }" +
                "  function extraerDesdeRaiz(root) {" +
                "    var bsList = root.querySelectorAll('.saldoDisp[data-moneda=\"Bs\"]');" +
                "    if (bsList.length > 0) { var sumBs=0, okBs=false; bsList.forEach(function(el){ var v=parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){sumBs+=v; okBs=true;} }); if(okBs){window.__vbt_bs=fmtVE(sumBs); window.__vbt_bs_num=sumBs;} }" +
                "    var usdList = root.querySelectorAll('.saldoDisp[data-moneda=\"USD\"]');" +
                "    if (usdList.length > 0) { var sumUsd=0, okUsd=false; usdList.forEach(function(el){ var v=parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){sumUsd+=v; okUsd=true;} }); if(okUsd) window.__vbt_usd=fmtVE(sumUsd); }" +
                "  }" +
                "  extraerDesdeRaiz(document);" + bloqueFetchPg +
                "} catch(e){} })();";
        evaluarJS(s, jsBalanceExacto, null);
    }

    private void cargarComprasExitosasGuardadas() {
        listaComprasExitosas.clear();
        try {
            JSONArray arr = new JSONArray(prefs.getString("compras_exitosas_json", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    listaComprasExitosas.add(new RegistroCompraExitosa(
                            o.optString("usuario", ""), o.optString("cedula", ""),
                            o.optString("fechaHora", ""), o.optString("montoUsd", "300$"),
                            o.optString("tasaBcv", "--"), o.optString("codigoOp", "--")));
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
                    s.usuario, s.tipoDoc + "-" + s.cedula, fechaHora, montoFinalStr,
                    (tasaStr == null || tasaStr.isEmpty()) ? s.ultimaTasaLeida : tasaStr,
                    (codigoStr == null || codigoStr.isEmpty()) ? s.ultimoCodigoOpLeido : codigoStr));
            if (listaComprasExitosas.size() > 60) listaComprasExitosas.remove(listaComprasExitosas.size() - 1);
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
            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpC.setMargins(0, 0, 0, dp(8));

            LinearLayout fila1 = new LinearLayout(this);
            fila1.setOrientation(LinearLayout.HORIZONTAL);
            fila1.setGravity(Gravity.CENTER_VERTICAL);
            TextView tvUser = crearTexto(item.usuario + " (" + item.cedula + ")", "#4ADE80", 13f, true);
            TextView tvMonto = crearTexto(item.montoUsd, "#00FF66", 13.5f, true);
            fila1.addView(tvUser, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            fila1.addView(tvMonto);
            cardCompra.addView(fila1);

            TextView tvDetalles = crearTexto("Código Op: #" + item.codigoOperacion + "   •   Tasa BCV: " + item.tasaBcv + " Bs/$", txtPri, 11.5f, true);
            tvDetalles.setPadding(0, dp(4), 0, dp(2));
            cardCompra.addView(tvDetalles);

            LinearLayout filaInf = new LinearLayout(this);
            filaInf.setOrientation(LinearLayout.HORIZONTAL);
            filaInf.setGravity(Gravity.CENTER_VERTICAL);
            TextView tvFecha = crearTexto("Fecha: " + item.fechaHora + "  •  Estado: APROBADO", txtSec, 10.5f, false);
            Button btnCopiarComprobante = crearBoton("Copiar", bgSub, "#38BDF8", 10, () -> {
                String resumen = "Compra Ordinaria Aprobada - Banco del Tesoro\nCuenta: " + item.usuario + " (" + item.cedula + ")\nMonto Pactado: " + item.montoUsd + "\nTasa BCV: " + item.tasaBcv + "\nCódigo Operación: #" + item.codigoOperacion + "\nEmisión: " + item.fechaHora;
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
            Button bUsr = crearBoton(s.nombreMostrado(), sel ? acento : bgInact, sel ? "#FFFFFF" : txtInact, 10, () -> {
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
                String nombre = (e.slotIndex >= 0 && e.slotIndex < MAX_CUENTAS) ? sesiones[e.slotIndex].nombreMostrado() : e.usuario;
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
            if (historialGlobalLogs.size() > 500) historialGlobalLogs.remove(0);
            if (pestanaInferiorActual == 1 && (filtroHistorialSlot == -1 || filtroHistorialSlot == s.slotIndex)) {
                txtHistorialConsola.append("[" + hora + "] [" + s.nombreMostrado() + "] " + mensajeClaro + "\n");
                scrollHistorialConsola.post(() -> scrollHistorialConsola.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private JSONArray obtenerArrayCuentasPrefs() {
        try {
            String raw = prefs.getString("lista_cuentas_v2", "");
            if (raw != null && !raw.isEmpty()) return new JSONArray(raw);
            JSONObject viejo = new JSONObject(prefs.getString("cuentas_json", "{}"));
            JSONArray migrado = new JSONArray();
            Iterator<String> it = viejo.keys();
            while (it.hasNext() && migrado.length() < MAX_CUENTAS) {
                JSONObject o = viejo.optJSONObject(it.next());
                if (o != null) migrado.put(o);
            }
            if (migrado.length() > 0) prefs.edit().putString("lista_cuentas_v2", migrado.toString()).apply();
            return migrado;
        } catch (Exception e) { return new JSONArray(); }
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
        btnTipoDoc.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Tipo de Documento")
                .setItems(tiposDocArray, (d, which) -> {
                    tipoSeleccionado[0] = tiposDocArray[which];
                    btnTipoDoc.setText(tipoSeleccionado[0] + " ▼");
                }).show());

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
        LinearLayout.LayoutParams lpBtnMonto = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
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
        if (modoPersonalizado[0]) edtMontoCustom.setText(String.valueOf(montoInicial));
        edtMontoCustom.setVisibility(modoPersonalizado[0] ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams lpEdtMonto = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
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
                try { montoFinal = Integer.parseInt(edtMontoCustom.getText().toString().trim()); }
                catch (Exception e) { montoFinal = -1; }
                if (montoFinal < 10 || montoFinal > 300 || (montoFinal % 10 != 0)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Monto no válido")
                            .setMessage("Tiene que ser múltiplo de 10 (mínimo 10 y máximo 300).")
                            .setPositiveButton("Aceptar", null).show();
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
                    if (!sesiones[i].registrada) { slotLibre = i; break; }
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
                nueva.tiempoCompraExitosaMs = 0;
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
        ponerIconoYTexto(b, crearIconoVectorial(tipoIcono, fgHex, 14), texto);
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
            case ENFRIANDO_CAMBIO_IP:
            case ACTIVANDO_PROXY_EN_PASS:
            case ACTIVANDO_PROXY_EN_PG:
            case ACTIVANDO_PROXY_EN_INT: base = "CAMBIANDO IP"; break;
            case CARGANDO_LOGIN:
            case ESPERANDO_ESTABILIZAR_LOGIN:
            case ENVIANDO_FASE1: base = "INICIANDO"; break;
            case ESPERANDO_CARGA_PASS:
            case ESPERANDO_ESTABILIZAR_PASS:
            case ENVIANDO_FASE2: base = "CONTRASEÑA"; break;
            case ESPERANDO_REINTENTO_SESION_ACTIVA: base = "SESIÓN ACTIVA (6s)"; break;
            case ESPERANDO_CARGA_INICIO:
            case ESPERANDO_ESTABILIZAR_INICIO: base = "POSICIÓN GLOBAL"; break;
            case VIGILANDO_CARGA_INT:
            case PAUSA_REINICIO_INT: base = "VIGILANDO"; break;
            case ESPERANDO_FORM_INTENTO1:
            case LIMPIANDO_PANTALLA:
            case ESPERANDO_FORM_INTENTO2: base = "ABRIENDO ORD"; break;
            case ESPERANDO_ESTABILIZAR_FORM:
            case LLENANDO_COMPRA:
            case COMPRANDO: base = "FORMULARIO • COMPRANDO"; break;
            case CERRANDO_SESION:
            case REINICIANDO_RED: base = "CERRANDO SESIÓN"; break;
            default: base = "ACTIVO"; break;
        }
        return (s.latenciaMs > 0) ? (base + " • " + s.latenciaMs + "ms") : base;
    }

    private void iniciarTodasLasCuentas() {
        ArrayList<SesionBot> reg = obtenerSesionesRegistradas();
        if (reg.isEmpty()) { mostrarPopupEditorCuenta(-1); return; }
        concentrarRedAlMaximoEnEstePrograma(true);
        yaAvisoApertura = false;
        int delayEscalonado = 0;
        for (SesionBot s : reg) {
            if (!s.estaCorriendo() || s.detenerTrasCerrarSesion) {
                iniciarSesionIndividual(s, delayEscalonado);
                delayEscalonado += 280;
            }
        }
    }

    private void detenerTodasLasCuentas() {
        for (SesionBot s : obtenerSesionesRegistradas()) {
            if (s.estaCorriendo()) pausarSesionSinCerrar(s, true);
        }
        lblReloginVal.setText(minutosRelogin + " min (--:--)");
    }

    private void iniciarSesionIndividual(final SesionBot s, int delayMs) {
        if (s == null || !s.registrada) return;
        concentrarRedAlMaximoEnEstePrograma(true);
        ultimoSlotActivoRed = s.slotIndex;
        s.corriendo = true;
        s.detenerTrasCerrarSesion = false;
        s.rotarIpTrasCerrarSesion = false;
        s.compraCompletada = false;
        s.tiempoCompraExitosaMs = 0;
        s.ultimoMontoAjustado = 0;
        s.vueltaActual = 0;
        s.intentoLogout = 0;
        s.esperandoCargaNuevaInt = false;
        s.yaActivoProxyEnIntervencion = false;

        int modoEf = s.obtenerModoProxyEfectivo();
        if (!s.sesionAbiertaEnServidor) {
            s.yaAnuncioPaginaLoginCargada = false;
            if (proxyRotativoActivo && modoEf == MODO_PROXY_SIEMPRE_ACTIVO) {
                activarModoProxySiempreActivoEnCuenta(s);
                registrarPasoCuenta(s, "Cargando página de login con Proxy siempre activo (" + s.cedulaMostrada() + ")");
            } else {
                usarWifiNormalEnCuenta(s);
                registrarPasoCuenta(s, "Cargando página de login con Wi-Fi normal (" + s.cedulaMostrada() + ")");
            }
        } else {
            registrarPasoCuenta(s, "Reanudando cuenta...");
        }
        cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);

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
        s.esperandoCargaNuevaInt = false;
        if (registrarEnLog) registrarPasoCuenta(s, "Cuenta detenida");
        cambiarEstado(s, EstadoBot.DETENIDO);
    }

    private void cerrarSesionYDetenerCuenta(SesionBot s) {
        if (s == null || !s.registrada) return;
        s.corriendo = true;
        s.detenerTrasCerrarSesion = true;
        s.rotarIpTrasCerrarSesion = false;
        s.intentoLogout = 0;
        if (s.obtenerModoProxyEfectivo() != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
        cambiarEstado(s, EstadoBot.CERRANDO_SESION);
        registrarPasoCuenta(s, "Cerrando sesión en 1 intento...");
        ejecutarPasoMaquinaEstados(s);
    }

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
        } catch (Exception ignored) { return true; }
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
                            if (sesiones[i].webViewOculto != null) sesiones[i].webViewOculto.stopLoading();
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
                for (SesionBot s : reg) actualizarVisualTarjeta(s);
                actualizarResumenYBotonesGlobales();
                if (barraGlassActiva && traduccionBarraY < dp(70)) actualizarEfectoBlurBarraGlass();

                long ahoraCiclo = System.currentTimeMillis();
                if (ahoraCiclo - ultimaVerificacionNubeMs > 30000) {
                    ultimaVerificacionNubeMs = ahoraCiclo;
                    if (loginClienteOverlay != null && loginClienteOverlay.getVisibility() != View.VISIBLE) {
                        String uAct = prefs.getString("usr_licencia_activa_v3", "");
                        String cAct = prefs.getString("clv_licencia_activa_v3", "");
                        if (!uAct.isEmpty() && !cAct.isEmpty()) validarCredencialesEnNubeOLocal(uAct, cAct, true, true);
                    }
                }

                boolean internetOk = hayInternetDisponible();
                if (contarCuentasCorriendo() > 0) {
                    verificarTiemposDeSesionIndependientes(reg);
                    for (SesionBot s : reg) {
                        if (s.estaCorriendo() && internetOk && !s.microCortePropio) ejecutarPasoMaquinaEstados(s);
                        else if (!internetOk) s.tiempoEstadoMs = System.currentTimeMillis();
                    }
                } else {
                    lblReloginVal.setText(minutosRelogin + " min (--:--)");
                }
                handler.postDelayed(this, 200);
            }
        }, 200);
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
            if (s.sesionAbiertaEnServidor && (ahora - s.ultimoKeepAliveMs > 55000)) {
                s.ultimoKeepAliveMs = ahora;
                evaluarJS(s, "if(window.jQuery){ var t=$('meta[name=\"csrf-token\"]').attr('content')||''; $.post('https://tesoroenlinea.bt.com.ve/p',{'_token':t}); if(window.timer&&typeof timer.reset==='function'){try{timer.reset(150000);}catch(e){}} }", null);
            }
            if (s.tiempoInicioSesionMs > 0 && (s.estadoActual == EstadoBot.VIGILANDO_CARGA_INT
                    || s.estadoActual == EstadoBot.PAUSA_REINICIO_INT)) {
                long transcurridoSeg = (ahora - s.tiempoInicioSesionMs) / 1000;
                long restante = Math.max(0, totalSeg - transcurridoSeg);
                if (menorRestante == -1 || restante < menorRestante) menorRestante = restante;
                if (transcurridoSeg >= totalSeg) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    s.rotarIpTrasCerrarSesion = true;
                    if (s.obtenerModoProxyEfectivo() != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
                    registrarPasoCuenta(s, "Tiempo de reinicio cumplido - Cerrando sesión en 1 intento...");
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
            }
        }
        if (menorRestante >= 0) {
            lblReloginVal.setText(String.format(Locale.getDefault(), "%d min (%02d:%02d)", minutosRelogin, menorRestante / 60, menorRestante % 60));
        } else {
            lblReloginVal.setText(minutosRelogin + " min (--:--)");
        }
    }

    private void darClickOkEnCartelSesionActiva(SesionBot s) {
        String jsClickOk =
                "(function(){" +
                "  var bOk = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(function(el){" +
                "    var t = (el.innerText || '').trim().toLowerCase();" +
                "    return t === 'ok' || t === 'aceptar' || t === 'cerrar';" +
                "  });" +
                "  if (bOk) { try { bOk.click(); } catch(e){} }" +
                "  var swalCont = document.querySelector('.swal2-container');" +
                "  if (swalCont) { try { swalCont.remove(); } catch(e){} }" +
                "  if (!document.querySelector('#password') && window.__vbt_pass_form_html) {" +
                "    try {" +
                "      var f = document.querySelector('#form');" +
                "      if (f) f.innerHTML = window.__vbt_pass_form_html;" +
                "    } catch(e){}" +
                "  }" +
                "})();";
        evaluarJS(s, jsClickOk, null);
    }

    private void ejecutarPasoMaquinaEstados(final SesionBot sesion) {
        if (sesion == null || sesion.webViewOculto == null) return;
        final long ahora = System.currentTimeMillis();

        String jsInspector =
                "var ready = (document.readyState === 'complete' || document.readyState === 'interactive');" +
                "var bodyTxt = document.body ? document.body.innerText.toLowerCase() : '';" +
                "var avisoExpirado = (bodyTxt.indexOf('no hemos visto actividad') !== -1 || bodyTxt.indexOf('deberá realizar el ingreso') !== -1 || bodyTxt.indexOf('debera realizar el ingreso') !== -1 || bodyTxt.indexOf('su sesión ha expirado') !== -1 || bodyTxt.indexOf('su sesion ha expirado') !== -1);" +
                "var sesionActivaErr = (bodyTxt.indexOf('sesión activa') !== -1 || bodyTxt.indexOf('sesion activa') !== -1 || bodyTxt.indexOf('posee una sesión') !== -1 || bodyTxt.indexOf('posee una sesion') !== -1);" +
                "var metaTok = document.querySelector('meta[name=\"csrf-token\"]');" +
                "var inputTok = document.querySelector('input[name=\"_token\"]');" +
                "var csrfActual = metaTok ? metaTok.getAttribute('content') : (inputTok ? inputTok.value : '');" +
                "var btnX = document.querySelector(\"button.close, .btn-close, [aria-label='Close'], [data-bs-dismiss='modal']\");" +
                "if (btnX && btnX.offsetParent !== null) { try{ btnX.click(); }catch(e){} }" +
                "var doc = document.querySelector('#documento');" +
                "var usr = document.querySelector('#username');" +
                "var pass = document.querySelector('#password');" +
                "if (pass && document.querySelector('#form')) { window.__vbt_pass_form_html = document.querySelector('#form').innerHTML; }" +
                "var monto = document.querySelector('#monto');" +
                "var ord = document.querySelector(\"a.btn-tesoro-green, a[href*='mod.add(1)']\") || Array.from(document.querySelectorAll('a, button')).find(el => el.innerText && el.innerText.trim().toLowerCase() === 'ordinaria');" +
                "var btnLogout = document.querySelector(\"a[href='javascript:Logout()'], header ul li a\");" +
                "var menuInt = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "var inicioListo = !!(ready && !doc && !pass && (menuInt || typeof postURL === 'function' || btnLogout) && (bodyTxt.indexOf('posición global') !== -1 || bodyTxt.indexOf('productos activos') !== -1 || document.querySelectorAll('.saldoDisp').length > 0));" +
                "var inconv = (bodyTxt.indexOf('presentando inconvenientes') !== -1 || bodyTxt.indexOf('no podemos procesar su solicitud') !== -1);" +
                "var exito = bodyTxt.indexOf('registrada exitosamente') !== -1;" +
                "var sinCupo = bodyTxt.indexOf('no hay asignación disponible') !== -1 || bodyTxt.indexOf('no hay asignacion disponible') !== -1;" +
                "var perfNav = (window.performance && performance.getEntriesByType) ? performance.getEntriesByType('navigation')[0] : null;" +
                "var pingCalc = window.__vbt_ping || (perfNav && perfNav.responseEnd > 0 ? Math.round(perfNav.responseEnd - perfNav.requestStart) : 0);" +
                "var codTabla = ''; var tasaTabla = '';" +
                "try { var primeraFila = document.querySelector('table tbody tr'); if (primeraFila) { var celdas = primeraFila.querySelectorAll('td'); if (celdas.length >= 6) { codTabla = (celdas[0].innerText || '').trim(); tasaTabla = (celdas[5].innerText || '').trim(); } } } catch(e){}" +
                "function parseMontoVE(str) { if (!str) return NaN; var s = String(str).trim(); if (s.indexOf(',') !== -1) s = s.replace(/\\./g, '').replace(',', '.'); return parseFloat(s); }" +
                "function fmtVE(num) { return Number(num).toLocaleString('es-VE', {minimumFractionDigits: 2, maximumFractionDigits: 2}); }" +
                "var bsNodes = document.querySelectorAll('.saldoDisp[data-moneda=\"Bs\"]');" +
                "if (bsNodes.length > 0) { var sumBs = 0, okBs = false; bsNodes.forEach(function(el){ var v = parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){ sumBs += v; okBs = true; } }); if (okBs) { window.__vbt_bs = fmtVE(sumBs); window.__vbt_bs_num = sumBs; } }" +
                "var usdNodes = document.querySelectorAll('.saldoDisp[data-moneda=\"USD\"]');" +
                "if (usdNodes.length > 0) { var sumUsd = 0, okUsd = false; usdNodes.forEach(function(el){ var v = parseMontoVE(el.getAttribute('data-saldo')); if(!isNaN(v)){ sumUsd += v; okUsd = true; } }); if (okUsd) window.__vbt_usd = fmtVE(sumUsd); }" +
                "var optD = document.querySelector('#desde option[data-disponible]');" +
                "var optH = document.querySelector('#hasta option[data-disponible]');" +
                "if (optD) { var vd = parseMontoVE(optD.getAttribute('data-disponible')); if (!isNaN(vd)) { window.__vbt_bs = fmtVE(vd); window.__vbt_bs_num = vd; } }" +
                "if (optH) { var vh = parseMontoVE(optH.getAttribute('data-disponible')); if (!isNaN(vh)) window.__vbt_usd = fmtVE(vh); }" +
                "var dtEl = document.querySelector('#datatable:not([data-vbt-old=\"1\"])');" +
                "var intCargadoReal = !!(window.__vbt_int_done || dtEl || ord);" +
                "return JSON.stringify({ready:ready, avisoExpirado:avisoExpirado, doc:!!(doc&&usr), pass:!!(pass&&!doc), inicio:inicioListo, intCargado:intCargadoReal, monto:!!monto, ord:!!(ord||window.__vbt_ord_found), inconv:inconv, exito:exito, sinCupo:sinCupo, sesionActivaErr:sesionActivaErr, logoutOk:!!window.__vbt_logout_ok, csrf:csrfActual||'', ping:pingCalc||0, codOp:codTabla||'', tasaBcv:tasaTabla||'', montoAjustado:window.__vbt_monto_ajustado||0, bs:window.__vbt_bs||'', usd:window.__vbt_usd||''});";

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

                if (pingWeb > 0) sesion.latenciaMs = pingWeb;
                else if (sesion.latenciaMs == 0) sesion.latenciaMs = rttEval;
                if (!csrfDetectado.isEmpty()) sesion.csrfTokenGuardado = csrfDetectado;

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
                if (!nuevoBs.isEmpty() && !nuevoBs.equals(sesion.saldoBs)) { sesion.saldoBs = nuevoBs; cambioSaldo = true; }
                if (!nuevoUsd.isEmpty() && !nuevoUsd.equals(sesion.saldoUsd)) { sesion.saldoUsd = nuevoUsd; cambioSaldo = true; }
                if (cambioSaldo || cambioInfoTabla) actualizarVisualTarjeta(sesion);

                boolean visDoc = st.optBoolean("doc");
                boolean visPass = st.optBoolean("pass");
                boolean visMonto = st.optBoolean("monto");
                boolean visOrd = st.optBoolean("ord");
                boolean visInicio = st.optBoolean("inicio");
                boolean intCargado = st.optBoolean("intCargado");
                boolean avisoExpirado = st.optBoolean("avisoExpirado");

                boolean elementoCriticoListo = (visDoc || visPass || visMonto || visOrd || visInicio || intCargado);
                boolean navCompleta = (!sesion.cargandoRed && sesion.progresoWeb >= 100) || elementoCriticoListo;
                boolean ready = st.optBoolean("ready") && navCompleta;

                procesarEstadoConDatos(
                        sesion, ahora, ready, avisoExpirado, visDoc, visPass, visInicio,
                        intCargado, visMonto, visOrd, st.optBoolean("inconv"),
                        st.optBoolean("exito"), st.optBoolean("sinCupo"),
                        st.optBoolean("sesionActivaErr"), st.optBoolean("logoutOk"));
            } catch (Exception ignored) {}
        });
    }

    private void procesarEstadoConDatos(
            SesionBot s, long ahora, boolean paginaCompleta, boolean avisoExpirado,
            boolean visDoc, boolean visPass, boolean visInicioCompleto, boolean intCargado,
            boolean visMonto, boolean visOrd, boolean hayInconv, boolean hayExito,
            boolean haySinCupo, boolean haySesionActivaErr, boolean logoutOk
    ) {
        if (!s.estaCorriendo()) return;

        if (visInicioCompleto || intCargado || visMonto) {
            s.sesionAbiertaEnServidor = true;
            actualizarSaldosEnSegundoPlano(s, false);
        }

        int modoEf = s.obtenerModoProxyEfectivo();
        String codTipoDoc = s.codigoNumericoTipoDoc();
        String cedSafe = s.cedula.replace("\\", "\\\\").replace("'", "\\'");
        String usrSafe = s.usuario.replace("\\", "\\\\").replace("'", "\\'");
        String clvSafe = s.contrasena.replace("\\", "\\\\").replace("'", "\\'");
        String strEntero = String.valueOf(s.montoCompra);
        String strCompleto = strEntero + ",00";

        switch (s.estadoActual) {

            case ENFRIANDO_CAMBIO_IP:
                if (ahora - s.tiempoEstadoMs >= 1300) {
                    ultimoSlotActivoRed = s.slotIndex;
                    s.yaAnuncioPaginaLoginCargada = false;
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    registrarPasoCuenta(s, "Cargando página de login...");
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case CARGANDO_LOGIN:
                if (avisoExpirado) {
                    s.yaAnuncioPaginaLoginCargada = false;
                    s.tiempoEstadoMs = ahora;
                    registrarPasoCuenta(s, "Aviso de tiempo expirado detectado - Reiniciando página");
                    evaluarJS(s, "var bOk = document.querySelector('button.swal2-confirm'); if(bOk) bOk.click();", null);
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    return;
                }
                if (paginaCompleta && visMonto && !hayInconv) {
                    if (s.tiempoInicioSesionMs == 0) s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    if (s.tiempoInicioSesionMs == 0) s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    registrarPasoCuenta(s, "Posición Global cargada completo");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (paginaCompleta && visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (paginaCompleta && visDoc) {
                    s.sesionAbiertaEnServidor = false;
                    if (!s.yaAnuncioPaginaLoginCargada) {
                        s.yaAnuncioPaginaLoginCargada = true;
                        registrarPasoCuenta(s, "Página cargada");
                    }
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 9500) {
                    s.tiempoEstadoMs = ahora;
                    s.yaAnuncioPaginaLoginCargada = false;
                    ultimoSlotActivoRed = s.slotIndex;
                    registrarPasoCuenta(s, "Reintentando carga de página de login...");
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_LOGIN:
                if (avisoExpirado || !visDoc) {
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 60) cambiarEstado(s, EstadoBot.ENVIANDO_FASE1);
                break;

            case ENVIANDO_FASE1:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    ultimoSlotActivoRed = s.slotIndex;
                    registrarPasoCuenta(s, "Colocando usuario y cédula (" + s.cedulaMostrada() + " - " + s.nombreMostrado() + ")");
                    String jsFase1Directa =
                            "var sel = document.querySelector('#tipodocben');" +
                            "if (sel) { sel.value = '" + codTipoDoc + "'; sel.dispatchEvent(new Event('change', {bubbles:true})); }" +
                            "var d = document.querySelector('#documento');" +
                            "var u = document.querySelector('#username');" +
                            "if (d && u) {" +
                            "  try { if (window.Inputmask) Inputmask.remove(d); } catch(e){}" +
                            "  try { if (window.jQuery && $(d).inputmask) $(d).inputmask('remove'); } catch(e){}" +
                            "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "  setter.call(d, '" + cedSafe + "'); d.dispatchEvent(new Event('input', {bubbles:true})); d.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  setter.call(u, '" + usrSafe + "'); u.dispatchEvent(new Event('input', {bubbles:true})); u.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  var tokInput = document.querySelector('input[name=\"_token\"]');" +
                            "  var tok = tokInput ? tokInput.value : '';" +
                            "  if (typeof generateFormEncrypt === 'function' && window.jQuery && tok) {" +
                            "    var enc = generateFormEncrypt($('#form').serializeArray(), 'https://tesoroenlinea.bt.com.ve/lg/chk', tok);" +
                            "    enc.submit();" +
                            "  } else {" +
                            "    var btn = document.querySelector(\"button[type='submit']\");" +
                            "    if (btn) btn.click(); else document.querySelector('#form').submit();" +
                            "  }" +
                            "}";
                    evaluarJS(s, jsFase1Directa, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                }
                break;

            case ESPERANDO_CARGA_PASS:
                if (avisoExpirado) {
                    registrarPasoCuenta(s, "Aviso de tiempo expirado detectado - Reiniciando página");
                    s.yaAnuncioPaginaLoginCargada = false;
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (haySesionActivaErr) {
                    darClickOkEnCartelSesionActiva(s);
                    registrarPasoCuenta(s, "Sesión activa detectada - Dando OK y esperando 6s para reintentar contraseña...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (paginaCompleta && visPass) {
                    registrarPasoCuenta(s, "Página de contraseña cargada");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 8500) {
                    s.yaAnuncioPaginaLoginCargada = false;
                    registrarPasoCuenta(s, "Reiniciando página de login...");
                    s.webViewOculto.stopLoading();
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case ESPERANDO_ESTABILIZAR_PASS:
                if (haySesionActivaErr) {
                    darClickOkEnCartelSesionActiva(s);
                    registrarPasoCuenta(s, "Sesión activa detectada - Dando OK y esperando 6s para reintentar contraseña...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (!visPass) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_PASS);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 160) {
                    if (proxyRotativoActivo && modoEf == MODO_PROXY_EN_CONTRASENA && !s.proxyActivoEnEstaCuenta) {
                        activarProxyYCambiarIpConVerificacion(s, "Contraseña");
                        cambiarEstado(s, EstadoBot.ACTIVANDO_PROXY_EN_PASS);
                    } else {
                        cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                    }
                }
                break;

            case ACTIVANDO_PROXY_EN_PASS:
                if (!s.verificandoConexionProxy && s.proxyVerificadoOk && (ahora - s.tiempoEstadoMs >= 1000)) {
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                } else if (ahora - s.tiempoEstadoMs > 7500) {
                    s.verificandoConexionProxy = false;
                    s.proxyVerificadoOk = true;
                    cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                }
                break;

            case ENVIANDO_FASE2:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    ultimoSlotActivoRed = s.slotIndex;
                    registrarPasoCuenta(s, "Colocando contraseña...");
                    String jsFase2Directa =
                            "var bOk = document.querySelector('button.swal2-confirm'); if(bOk){ try{bOk.click();}catch(e){} }" +
                            "var p = document.querySelector('#password');" +
                            "if (p) {" +
                            "  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                            "  setter.call(p, '" + clvSafe + "');" +
                            "  p.dispatchEvent(new Event('input', {bubbles:true}));" +
                            "  p.dispatchEvent(new Event('change', {bubbles:true}));" +
                            "  var tokInput = document.querySelector('input[name=\"_token\"]');" +
                            "  var tok = tokInput ? tokInput.value : '';" +
                            "  if (typeof generateFormEncrypt === 'function' && window.jQuery && tok) {" +
                            "    var enc = generateFormEncrypt($('#form').serializeArray(), 'https://tesoroenlinea.bt.com.ve/lg/exe', tok);" +
                            "    enc.submit();" +
                            "  } else {" +
                            "    var btn = document.querySelector(\"button[type='submit']\");" +
                            "    if (btn) btn.click(); else document.querySelector('#form').submit();" +
                            "  }" +
                            "}";
                    evaluarJS(s, jsFase2Directa, null);
                    cambiarEstado(s, EstadoBot.ESPERANDO_CARGA_INICIO);
                }
                break;

            case ESPERANDO_CARGA_INICIO:
                if (haySesionActivaErr) {
                    darClickOkEnCartelSesionActiva(s);
                    registrarPasoCuenta(s, "Sesión activa detectada - Dando OK y esperando 6s para reintentar contraseña...");
                    cambiarEstado(s, EstadoBot.ESPERANDO_REINTENTO_SESION_ACTIVA);
                    return;
                }
                if (paginaCompleta && visInicioCompleto) {
                    s.tiempoInicioSesionMs = ahora;
                    s.sesionAbiertaEnServidor = true;
                    actualizarSaldosEnSegundoPlano(s, false);
                    registrarPasoCuenta(s, "Posición Global cargada completo");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_INICIO);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 9500 && paginaCompleta && visDoc) {
                    s.yaAnuncioPaginaLoginCargada = false;
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                }
                break;

            case ESPERANDO_REINTENTO_SESION_ACTIVA:
                if (haySesionActivaErr) darClickOkEnCartelSesionActiva(s);
                if (ahora - s.tiempoEstadoMs >= 6000) {
                    darClickOkEnCartelSesionActiva(s);
                    if (visPass) {
                        registrarPasoCuenta(s, "Reintentando contraseña tras 6s...");
                        cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                    } else if (visDoc) {
                        cambiarEstado(s, EstadoBot.ENVIANDO_FASE1);
                    } else {
                        registrarPasoCuenta(s, "Reintentando contraseña tras 6s...");
                        cambiarEstado(s, EstadoBot.ENVIANDO_FASE2);
                    }
                }
                break;

            case ESPERANDO_ESTABILIZAR_INICIO:
                if (ahora - s.tiempoEstadoMs >= 260) {
                    if (proxyRotativoActivo && modoEf == MODO_PROXY_POSICION_GLOBAL && !s.proxyActivoEnEstaCuenta) {
                        activarProxyYCambiarIpConVerificacion(s, "Posición Global");
                        cambiarEstado(s, EstadoBot.ACTIVANDO_PROXY_EN_PG);
                    } else {
                        s.vueltaActual = 1;
                        registrarPasoCuenta(s, "Entrando a Intervención...");
                        dispararCargaNativaIntervencion(s);
                        cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                    }
                }
                break;

            case ACTIVANDO_PROXY_EN_PG:
                if (!s.verificandoConexionProxy && s.proxyVerificadoOk && (ahora - s.tiempoEstadoMs >= 1000)) {
                    s.vueltaActual = 1;
                    registrarPasoCuenta(s, "Entrando a Intervención...");
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                } else if (ahora - s.tiempoEstadoMs > 7500) {
                    s.verificandoConexionProxy = false;
                    s.proxyVerificadoOk = true;
                    s.vueltaActual = 1;
                    registrarPasoCuenta(s, "Entrando a Intervención...");
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                }
                break;

            case VIGILANDO_CARGA_INT:
                if (avisoExpirado || (paginaCompleta && visDoc)) {
                    s.sesionAbiertaEnServidor = false;
                    s.yaAnuncioPaginaLoginCargada = false;
                    limpiarTodosLosDatosDeSesionAnterior(s, "Sesión expirada", true);
                    registrarPasoCuenta(s, "Aviso de tiempo expirado detectado - Reiniciando página");
                    s.webViewOculto.loadUrl(URL_LOGIN);
                    cambiarEstado(s, EstadoBot.CARGANDO_LOGIN);
                    return;
                }
                if (visMonto && !hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (visOrd) {
                    s.esperandoCargaNuevaInt = false;
                    if (proxyRotativoActivo && modoEf == MODO_PROXY_EN_INTERVENCION && !s.yaActivoProxyEnIntervencion) {
                        s.yaActivoProxyEnIntervencion = true;
                        activarProxyYCambiarIpConVerificacion(s, "Intervención");
                        cambiarEstado(s, EstadoBot.ACTIVANDO_PROXY_EN_INT);
                        return;
                    }
                    registrarPasoCuenta(s, "Botón Ordinaria detectado - Abriendo formulario");
                    if (!yaAvisoApertura) {
                        yaAvisoApertura = true;
                        sonarAlertaOrdinaria();
                    }
                    dispararAperturaOrdinaria(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                if (s.esperandoCargaNuevaInt && intCargado) {
                    s.esperandoCargaNuevaInt = false;
                    if (proxyRotativoActivo && modoEf == MODO_PROXY_EN_INTERVENCION && !s.yaActivoProxyEnIntervencion) {
                        s.yaActivoProxyEnIntervencion = true;
                        registrarPasoCuenta(s, "Intervención cargada - Activando Proxy y cambiando IP");
                        activarProxyYCambiarIpConVerificacion(s, "Intervención");
                        cambiarEstado(s, EstadoBot.ACTIVANDO_PROXY_EN_INT);
                        return;
                    }
                    registrarPasoCuenta(s, "Sin botón intento " + s.vueltaActual);
                    cambiarEstado(s, EstadoBot.PAUSA_REINICIO_INT);
                    return;
                }
                if (ahora - s.inicioPeticionIntMs >= TIMEOUT_CARGA_INT_MS) {
                    s.esperandoCargaNuevaInt = false;
                    registrarPasoCuenta(s, "No cargó la página en los 12 segundos - Reiniciando intervención");
                    s.vueltaActual++;
                    dispararCargaNativaIntervencion(s);
                }
                break;

            case ACTIVANDO_PROXY_EN_INT:
                if (!s.verificandoConexionProxy && s.proxyVerificadoOk && (ahora - s.tiempoEstadoMs >= 1000)) {
                    s.vueltaActual++;
                    registrarPasoCuenta(s, "Reiniciando intervención con nueva IP...");
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                } else if (ahora - s.tiempoEstadoMs > 7500) {
                    s.verificandoConexionProxy = false;
                    s.proxyVerificadoOk = true;
                    s.vueltaActual++;
                    registrarPasoCuenta(s, "Reiniciando intervención con nueva IP...");
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                }
                break;

            case PAUSA_REINICIO_INT:
                if (visOrd) {
                    registrarPasoCuenta(s, "Botón Ordinaria detectado - Abriendo formulario");
                    if (!yaAvisoApertura) {
                        yaAvisoApertura = true;
                        sonarAlertaOrdinaria();
                    }
                    dispararAperturaOrdinaria(s);
                    cambiarEstado(s, EstadoBot.ESPERANDO_FORM_INTENTO1);
                    return;
                }
                if (ahora - s.tiempoEstadoMs >= 180) {
                    s.vueltaActual++;
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                }
                break;

            case ESPERANDO_FORM_INTENTO1:
                if (visMonto && !hayInconv) {
                    registrarPasoCuenta(s, "Formulario abierto - Preparando compra");
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (hayInconv) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    s.rotarIpTrasCerrarSesion = true;
                    if (modoEf != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
                    registrarPasoCuenta(s, "Error en Ordinaria - Cerrando sesión con Wi-Fi normal y borrando datos...");
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 8000) {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    s.rotarIpTrasCerrarSesion = true;
                    if (modoEf != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
                    registrarPasoCuenta(s, "Formulario no cargó - Cerrando sesión con Wi-Fi normal...");
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;

            case LIMPIANDO_PANTALLA:
            case ESPERANDO_FORM_INTENTO2:
                if (visMonto && !hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                } else {
                    s.intentoLogout = 0;
                    s.detenerTrasCerrarSesion = false;
                    s.rotarIpTrasCerrarSesion = true;
                    if (modoEf != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;

            case ESPERANDO_ESTABILIZAR_FORM:
                if (ahora - s.tiempoEstadoMs >= 60) cambiarEstado(s, EstadoBot.LLENANDO_COMPRA);
                break;

            case LLENANDO_COMPRA:
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    registrarPasoCuenta(s, "Enviando orden de compra (" + s.montoCompra + "$)");
                    String jsCompraDirecta =
                            "window.__vbt_monto_ajustado = 0;" +
                            "function parseNumVE(str) { if (!str) return NaN; var s = String(str).trim(); if (s.indexOf(',') !== -1) s = s.replace(/\\./g, '').replace(',', '.'); return parseFloat(s); }" +
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
                            "    if (window.Swal && typeof Swal.fire === 'function' && !window.__vbt_swal_hooked) {" +
                            "      var origFire = Swal.fire.bind(Swal); window.__vbt_swal_hooked = true;" +
                            "      Swal.fire = function(opts) { if (opts && typeof opts === 'object' && opts.showCancelButton) return Promise.resolve({ value: true, isConfirmed: true }); return origFire.apply(this, arguments); };" +
                            "    }" +
                            "    var confirmarInstantaneo = function() {" +
                            "      var btnSi = document.querySelector('button.swal2-confirm') || Array.from(document.querySelectorAll('button, a')).find(el => el.innerText && (el.innerText.trim().toLowerCase() === 'si' || el.innerText.trim().toLowerCase() === 'sí'));" +
                            "      if(btnSi && btnSi.offsetWidth > 0) { btnSi.click(); return true; }" +
                            "      return false;" +
                            "    };" +
                            "    var obs = new MutationObserver(function(){ if(confirmarInstantaneo()) obs.disconnect(); });" +
                            "    if(document.body) obs.observe(document.body, {childList:true, subtree:true});" +
                            "    if (typeof mod !== 'undefined' && typeof mod.save === 'function') { try { mod.save(); } catch(e){} }" +
                            "    else { var col = document.querySelector(\"a[href='javascript:mod.save()']\"); if(col) { try { col.click(); } catch(e){} } }" +
                            "    setTimeout(confirmarInstantaneo, 90); setTimeout(confirmarInstantaneo, 220);" +
                            "    setTimeout(function(){ try{obs.disconnect();}catch(e){} }, 3000);" +
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
                            "          setter.call(m, maxUsd + ',00');" +
                            "          m.dispatchEvent(new Event('input', {bubbles:true})); m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "          m.dispatchEvent(new Event('change', {bubbles:true})); m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "        }" +
                            "      }" +
                            "    } catch(e){}" +
                            "    setTimeout(dispararGuardadoOrden, 50);" +
                            "  }" +
                            "  function escribirPaso(idx) {" +
                            "    if(idx < pasos.length) {" +
                            "      setter.call(m, pasos[idx]); m.dispatchEvent(new Event('input', {bubbles:true})); m.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));" +
                            "      setTimeout(function(){ escribirPaso(idx + 1); }, 15);" +
                            "    } else {" +
                            "      if(m.value.indexOf('" + strEntero + "') === -1) { setter.call(m, '" + strCompleto + "'); m.dispatchEvent(new Event('input', {bubbles:true})); }" +
                            "      m.dispatchEvent(new Event('change', {bubbles:true})); m.dispatchEvent(new Event('blur', {bubbles:true}));" +
                            "      setTimeout(verificarSaldoYAjustar, 65);" +
                            "    }" +
                            "  }" +
                            "  setTimeout(function(){ escribirPaso(0); }, 20);" +
                            "}";
                    evaluarJS(s, jsCompraDirecta, null);
                    cambiarEstado(s, EstadoBot.COMPRANDO);
                }
                break;

            case COMPRANDO:
                if (hayExito) {
                    s.compraCompletada = true;
                    s.tiempoCompraExitosaMs = System.currentTimeMillis();
                    s.corriendo = false;
                    int montoFinalCompra = (s.ultimoMontoAjustado > 0) ? s.ultimoMontoAjustado : s.montoCompra;
                    registrarNuevaCompraExitosa(s, montoFinalCompra + ".00$", s.ultimaTasaLeida, s.ultimoCodigoOpLeido);
                    actualizarSaldosEnSegundoPlano(s, true);
                    registrarPasoCuenta(s, "Orden completada exitosamente (" + montoFinalCompra + "$)");
                    sonarAlertaCompraExitosa();
                    cambiarEstado(s, EstadoBot.DETENIDO);
                    return;
                }
                if (haySinCupo) {
                    s.vueltaActual++;
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                    return;
                }
                if (hayInconv) {
                    cambiarEstado(s, EstadoBot.ESPERANDO_ESTABILIZAR_FORM);
                    return;
                }
                if (ahora - s.tiempoEstadoMs > 8000) {
                    actualizarSaldosEnSegundoPlano(s, true);
                    s.vueltaActual++;
                    dispararCargaNativaIntervencion(s);
                    cambiarEstado(s, EstadoBot.VIGILANDO_CARGA_INT);
                }
                break;

            case CERRANDO_SESION:
                if (logoutOk || (paginaCompleta && (visDoc || visPass))) {
                    evaluarJS(s, "window.__vbt_logout_ok = false;", null);
                    s.sesionAbiertaEnServidor = false;
                    s.yaAnuncioPaginaLoginCargada = false;
                    if (s.detenerTrasCerrarSesion) {
                        s.corriendo = false;
                        s.detenerTrasCerrarSesion = false;
                        s.rotarIpTrasCerrarSesion = false;
                        s.tiempoInicioSesionMs = 0;
                        limpiarTodosLosDatosDeSesionAnterior(s, "Cierre manual", false);
                        if (!visDoc) s.webViewOculto.loadUrl(URL_LOGIN);
                        registrarPasoCuenta(s, "Sesión cerrada en 1 intento y cuenta detenida");
                        cambiarEstado(s, EstadoBot.DETENIDO);
                    } else {
                        boolean rotarSiSiempre = s.rotarIpTrasCerrarSesion;
                        s.rotarIpTrasCerrarSesion = false;
                        limpiarTodosLosDatosDeSesionAnterior(s, "Reinicio de sesión", rotarSiSiempre);
                        if (!visDoc) s.webViewOculto.loadUrl(URL_LOGIN);
                        cambiarEstado(s, EstadoBot.ENFRIANDO_CAMBIO_IP);
                    }
                    return;
                }
                if (!s.accionEnCurso) {
                    s.accionEnCurso = true;
                    s.intentoLogout = 1;
                    s.tiempoEstadoMs = ahora;
                    if (modoEf != MODO_PROXY_SIEMPRE_ACTIVO) usarWifiNormalEnCuenta(s);
                    registrarPasoCuenta(s, "Cerrando sesión en 1 intento...");
                    ejecutarScriptLogoutDirecto(s);
                } else if (ahora - s.tiempoEstadoMs > 2200) {
                    s.sesionAbiertaEnServidor = false;
                    s.yaAnuncioPaginaLoginCargada = false;
                    limpiarTodosLosDatosDeSesionAnterior(s, "Cierre directo", s.rotarIpTrasCerrarSesion);
                    s.webViewOculto.loadUrl(URL_LOGIN);
                }
                break;

            case REINICIANDO_RED:
                if (!s.microCortePropio && (ahora - s.tiempoEstadoMs >= 900)) {
                    cambiarEstado(s, EstadoBot.CERRANDO_SESION);
                }
                break;
        }
    }

    private void ejecutarScriptLogoutDirecto(SesionBot s) {
        String csrfRespaldo = (s.csrfTokenGuardado != null) ? s.csrfTokenGuardado.replace("'", "") : "";
        String jsLogout1Intento =
                "window.__vbt_logout_ok = false;" +
                "(function(){" +
                "  try { if (window.rajax) rajax.abort(); } catch(e){}" +
                "  var meta = document.querySelector('meta[name=\"csrf-token\"]');" +
                "  var inTok = document.querySelector('input[name=\"_token\"]');" +
                "  var tok = (meta && meta.getAttribute('content')) ? meta.getAttribute('content') : ((inTok && inTok.value) ? inTok.value : '" + csrfRespaldo + "');" +
                "  if (window.jQuery && typeof $.post === 'function') {" +
                "    $.post('https://tesoroenlinea.bt.com.ve/logout', {'_token': tok}).always(function(){" +
                "      window.__vbt_logout_ok = true; window.location.replace('https://tesoroenlinea.bt.com.ve/lg');" +
                "    });" +
                "  } else {" +
                "    fetch('https://tesoroenlinea.bt.com.ve/logout', {method:'POST', headers:{'X-Requested-With':'XMLHttpRequest','Content-Type':'application/x-www-form-urlencoded','X-CSRF-TOKEN':tok}, body:'_token='+encodeURIComponent(tok), credentials:'same-origin'}).finally(function(){" +
                "      window.__vbt_logout_ok = true; window.location.replace('https://tesoroenlinea.bt.com.ve/lg');" +
                "    });" +
                "  }" +
                "  setTimeout(function(){ window.__vbt_logout_ok = true; window.location.replace('https://tesoroenlinea.bt.com.ve/lg'); }, 1100);" +
                "})();";
        evaluarJS(s, jsLogout1Intento, null);
    }

    private void dispararAperturaOrdinaria(SesionBot s) {
        ultimoSlotActivoRed = s.slotIndex;
        String jsAbrirOrd =
                "(function(){" +
                "  if (typeof mod !== 'undefined' && typeof mod.add === 'function') { try { mod.add(1); return; } catch(e){} }" +
                "  var b = document.querySelector(\"a.btn-tesoro-green, a[href*='mod.add(1)']\");" +
                "  if (b) { b.click(); return; }" +
                "  var cont = (typeof container !== 'undefined' && container && container.length) ? container : $('#content');" +
                "  if (typeof postURL === 'function' && cont && cont.length) postURL('https://tesoroenlinea.bt.com.ve/int/create', cont);" +
                "})();";
        evaluarJS(s, jsAbrirOrd, null);
    }

    private void dispararCargaNativaIntervencion(SesionBot s) {
        ultimoSlotActivoRed = s.slotIndex;
        s.esperandoCargaNuevaInt = true;
        s.inicioPeticionIntMs = System.currentTimeMillis();

        String jsCargaNativaInt =
                "window.__vbt_int_done = false;" +
                "window.__vbt_ord_found = false;" +
                "(function(){" +
                "  var t0 = Date.now();" +
                "  var oldDt = document.querySelector('#datatable');" +
                "  if (oldDt) { oldDt.setAttribute('data-vbt-old', '1'); }" +
                "  var sub = document.querySelector(\"a.sidebar-link[href='/int']\");" +
                "  if (sub) { sub.click(); } else if (typeof postURL === 'function') {" +
                "    var cont = (typeof container !== 'undefined' && container && container.length) ? container : $('#content');" +
                "    postURL('https://tesoroenlinea.bt.com.ve/int', cont, null, function(data){" +
                "      window.__vbt_ping = Math.max(10, Date.now() - t0);" +
                "      cont.html(data); window.__vbt_int_done = true;" +
                "    });" +
                "  } else { window.location.hash = '#/int'; }" +
                "})();";
        evaluarJS(s, jsCargaNativaInt, null);
    }
}
