package com.jahc.auto.service;

/**
 * Máquina de estados para UN mensaje de WhatsApp Business.
 * Avanza solo por verificación real de UI (poll 400ms + eventos),
 * nunca por delays fijos. Reintentos 3x con recovery, sin duplicar envíos.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010!\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0018\n\u0002\u0018\u0002\n\u0002\b\u000e\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010@\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0012\u0010D\u001a\u0004\u0018\u00010E2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010F\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u000204H\u0002J\u0010\u0010H\u001a\u00020\u000b2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010I\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u000204H\u0002J\u001e\u0010J\u001a\u00020A2\u0006\u0010K\u001a\u00020E2\f\u0010L\u001a\b\u0012\u0004\u0012\u00020E0MH\u0002J\u0018\u0010N\u001a\u00020\u00042\u0006\u0010B\u001a\u00020C2\u0006\u0010O\u001a\u00020\tH\u0002J\u0018\u0010P\u001a\u00020\u00042\u0006\u0010Q\u001a\u00020E2\u0006\u0010O\u001a\u00020\tH\u0002J\u0010\u0010R\u001a\u00020S2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010T\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010U\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u000204H\u0002J\u0010\u0010V\u001a\u00020\t2\u0006\u0010B\u001a\u00020CH\u0002J\u0012\u0010W\u001a\u0004\u0018\u00010\t2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010X\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010Y\u001a\u00020\tH\u0002J\"\u0010Z\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010[\u001a\u00020\u000b2\b\u0010Y\u001a\u0004\u0018\u00010\tH\u0002J\u0012\u0010\\\u001a\u0004\u0018\u00010E2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010]\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010Q\u001a\u00020EH\u0002J\u0010\u0010^\u001a\u00020\u000b2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010_\u001a\u00020\u000b2\u0006\u0010B\u001a\u00020CH\u0002J\u000e\u0010`\u001a\u00020\u000b2\u0006\u0010a\u001a\u00020\u0006J\u0010\u0010b\u001a\u00020\u000b2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010c\u001a\u00020\u000b2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010d\u001a\u00020\u00042\u0006\u0010e\u001a\u00020\tH\u0002J\u0018\u0010f\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u00020\tH\u0002J\u0018\u0010g\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u00020\tH\u0002J\u0010\u0010h\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u000e\u0010i\u001a\u00020A2\u0006\u0010B\u001a\u00020CJ\u0010\u0010j\u001a\u00020A2\b\u0010k\u001a\u0004\u0018\u00010lJ\u0010\u0010m\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\b\u0010n\u001a\u00020AH\u0002J\u0010\u0010o\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010p\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010q\u001a\u00020\tH\u0002J\u0010\u0010r\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0016\u0010s\u001a\u00020A2\u0006\u0010B\u001a\u00020C2\u0006\u0010G\u001a\u00020*J\u0006\u0010t\u001a\u00020AJ\b\u0010u\u001a\u00020\tH\u0002J\u0010\u0010v\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010w\u001a\u00020\u00062\u0006\u0010G\u001a\u000204H\u0002J\u0010\u0010x\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002J\u0010\u0010y\u001a\u00020A2\u0006\u0010B\u001a\u00020CH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\f\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0011\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010 \u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010$\u001a\u0004\u0018\u00010\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\'\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010(\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010)\u001a\u0004\u0018\u00010*X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010-\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010.\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010/\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00100\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00101\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00103\u001a\u000204X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00105\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u00107\u001a\u0004\u0018\u000108X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00109\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010:\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010;\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010<\u001a\b\u0018\u00010=R\u00020>X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010?\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006z"}, d2 = {"Lcom/jahc/auto/service/BusinessSendMachine;", "", "()V", "MAX_RECOVERY", "", "OVERALL_TIMEOUT_MS", "", "POLL_MS", "TAG", "", "aceptarHandled", "", "active", "getActive", "()Z", "setActive", "(Z)V", "armStable", "armedRect", "Landroid/graphics/Rect;", "bubblesBeforeSend", "contactClicked", "emptyStreak", "entered", "failReason", "handler", "Landroid/os/Handler;", "jobStart", "lastEvPkg", "lastEvType", "lastEventAt", "lastTick", "lastWinSig", "openedWhatsApp", "pendingLogTick", "postReopenAt", "prevPkg", "recoveryCount", "reverifyAt", "reverifyDone", "reverifyPhase", "sch", "Lcom/jahc/auto/data/Schedule;", "searchTyped", "sendArmed", "sendArmedAt", "sendClicks", "sentClicked", "stableCount", "staleStreak", "startedLocked", "state", "Lcom/jahc/auto/service/SendState;", "stateSince", "tapAt", "tickRunnable", "Ljava/lang/Runnable;", "verifyPollCount", "verifyStirred", "verifyStuckSince", "wakeLock", "Landroid/os/PowerManager$WakeLock;", "Landroid/os/PowerManager;", "wakeStart", "acquireWakeLock", "", "svc", "Lcom/jahc/auto/service/AutoAccessibilityService;", "activeRoot", "Landroid/view/accessibility/AccessibilityNodeInfo;", "advance", "s", "attemptSendClick", "checkState", "collectSendNodes", "node", "out", "", "countBubbles", "msg", "countBubblesIn", "root", "detectScreen", "Lcom/jahc/auto/service/Screen;", "doCleanup", "enterState", "entryText", "entryTextFresh", "fail", "reason", "finish", "success", "freshRoot", "handleAceptar", "isCorrectChat", "isLocked", "isProcessing", "id", "isUnlocked", "isWhatsAppForeground", "log", "m", "mark", "navMark", "navigateBack", "onEvent", "onRawEvent", "event", "Landroid/view/accessibility/AccessibilityEvent;", "recover", "releaseWakeLock", "reopenChat", "retry", "why", "routeFromWhatsApp", "start", "stop", "targetPkg", "tick", "timeoutFor", "typeMessage", "typeSearch", "app_debug"})
public final class BusinessSendMachine {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BusinessSendMachine";
    private static final long POLL_MS = 400L;
    private static final int MAX_RECOVERY = 9;
    private static int recoveryCount = 0;
    @kotlin.jvm.Volatile()
    private static volatile boolean active = false;
    @org.jetbrains.annotations.Nullable()
    private static com.jahc.auto.data.Schedule sch;
    @org.jetbrains.annotations.NotNull()
    private static com.jahc.auto.service.SendState state = com.jahc.auto.service.SendState.IDLE;
    private static boolean entered = false;
    private static long stateSince = 0L;
    private static boolean openedWhatsApp = false;
    private static boolean sentClicked = false;
    private static int sendClicks = 0;
    private static boolean searchTyped = false;
    private static boolean contactClicked = false;
    private static int stableCount = 0;
    private static long lastTick = 0L;
    private static long lastEventAt = 0L;
    private static int lastEvType = -1;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String lastEvPkg;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String failReason;
    private static long jobStart = 0L;
    private static boolean sendArmed = false;
    private static long sendArmedAt = 0L;
    @org.jetbrains.annotations.Nullable()
    private static android.graphics.Rect armedRect;
    private static int armStable = 0;
    private static final long OVERALL_TIMEOUT_MS = 150000L;
    private static int bubblesBeforeSend = -1;
    private static boolean aceptarHandled = false;
    private static long tapAt = 0L;
    private static boolean reverifyDone = false;
    private static int reverifyPhase = 0;
    private static long reverifyAt = 0L;
    private static int emptyStreak = 0;
    private static long verifyStuckSince = 0L;
    private static boolean verifyStirred = false;
    private static int staleStreak = 0;
    private static long postReopenAt = 0L;
    private static int pendingLogTick = 0;
    private static int verifyPollCount = 0;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String lastWinSig;
    private static boolean startedLocked = false;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String prevPkg;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile android.os.PowerManager.WakeLock wakeLock;
    private static long wakeStart = 0L;
    @org.jetbrains.annotations.NotNull()
    private static final android.os.Handler handler = null;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.Runnable tickRunnable;
    @org.jetbrains.annotations.NotNull()
    public static final com.jahc.auto.service.BusinessSendMachine INSTANCE = null;
    
    private BusinessSendMachine() {
        super();
    }
    
    public final boolean getActive() {
        return false;
    }
    
    public final void setActive(boolean p0) {
    }
    
    public final boolean isProcessing(long id) {
        return false;
    }
    
    public final void start(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService svc, @org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule s) {
    }
    
    public final void onEvent(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    public final void onRawEvent(@org.jetbrains.annotations.Nullable()
    android.view.accessibility.AccessibilityEvent event) {
    }
    
    public final void stop() {
    }
    
    private final void acquireWakeLock(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void releaseWakeLock() {
    }
    
    private final void mark(com.jahc.auto.service.AutoAccessibilityService svc, java.lang.String s) {
    }
    
    private final void advance(com.jahc.auto.service.AutoAccessibilityService svc, com.jahc.auto.service.SendState s) {
    }
    
    private final void retry(com.jahc.auto.service.AutoAccessibilityService svc, java.lang.String why) {
    }
    
    private final void fail(com.jahc.auto.service.AutoAccessibilityService svc, java.lang.String reason) {
    }
    
    private final void navMark(com.jahc.auto.service.AutoAccessibilityService svc, java.lang.String s) {
    }
    
    /**
     * CASO 1 (empezó bloqueado): BACK/HOME + matar WhatsApp; la pantalla se
     * apaga sola por timeout (sin wakelock). Nunca se bloquea por código.
     * CASO 2 (usuario en otra app): volver a esa app, nunca HOME a ciegas.
     */
    private final void navigateBack(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void finish(com.jahc.auto.service.AutoAccessibilityService svc, boolean success, java.lang.String reason) {
    }
    
    /**
     * Recovery real: unlock -> foreground -> detectar pantalla -> retomar. Nunca sleep+ciego.
     */
    private final void recover(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void tick(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void enterState(com.jahc.auto.service.AutoAccessibilityService svc, com.jahc.auto.service.SendState s) {
    }
    
    private final void checkState(com.jahc.auto.service.AutoAccessibilityService svc, com.jahc.auto.service.SendState s) {
    }
    
    /**
     * Reabre la conversación del MISMO número solo para leer (nunca enviar).
     */
    private final void reopenChat(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void routeFromWhatsApp(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void typeSearch(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final void typeMessage(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    /**
     * Recolecta nodos Send por ID de vista o descripción (sin filtrar).
     */
    private final void collectSendNodes(android.view.accessibility.AccessibilityNodeInfo node, java.util.List<android.view.accessibility.AccessibilityNodeInfo> out) {
    }
    
    /**
     * Un clic real, nunca al mic con campo vacío. Devuelve true si se pulsó.
     */
    private final boolean attemptSendClick(com.jahc.auto.service.AutoAccessibilityService svc) {
        return false;
    }
    
    private final void handleAceptar(com.jahc.auto.service.AutoAccessibilityService svc, android.view.accessibility.AccessibilityNodeInfo root) {
    }
    
    private final void doCleanup(com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    private final android.view.accessibility.AccessibilityNodeInfo activeRoot(com.jahc.auto.service.AutoAccessibilityService svc) {
        return null;
    }
    
    /**
     * Árbol fresco: intenta refrescar el nodo raíz. Si el sistema devuelve
     * false, el árbol está muerto (vista vieja) y NO debe usarse para verificar.
     */
    private final android.view.accessibility.AccessibilityNodeInfo freshRoot(com.jahc.auto.service.AutoAccessibilityService svc) {
        return null;
    }
    
    /**
     * Texto del campo con árbol fresco. null = sin datos (no decidir nada).
     */
    private final java.lang.String entryTextFresh(com.jahc.auto.service.AutoAccessibilityService svc) {
        return null;
    }
    
    private final boolean isLocked(com.jahc.auto.service.AutoAccessibilityService svc) {
        return false;
    }
    
    private final boolean isUnlocked(com.jahc.auto.service.AutoAccessibilityService svc) {
        return false;
    }
    
    private final java.lang.String targetPkg() {
        return null;
    }
    
    private final boolean isWhatsAppForeground(com.jahc.auto.service.AutoAccessibilityService svc) {
        return false;
    }
    
    private final com.jahc.auto.service.Screen detectScreen(com.jahc.auto.service.AutoAccessibilityService svc) {
        return null;
    }
    
    private final boolean isCorrectChat(com.jahc.auto.service.AutoAccessibilityService svc) {
        return false;
    }
    
    private final java.lang.String entryText(com.jahc.auto.service.AutoAccessibilityService svc) {
        return null;
    }
    
    /**
     * Cuenta burbujas con el texto exacto SOLO dentro de las filas de la
     * conversación (IDs estables). Exige árbol fresco; si no hay, -1.
     */
    private final int countBubbles(com.jahc.auto.service.AutoAccessibilityService svc, java.lang.String msg) {
        return 0;
    }
    
    /**
     * Variante que reutiliza un root ya leído (un solo escaneo por poll).
     */
    private final int countBubblesIn(android.view.accessibility.AccessibilityNodeInfo root, java.lang.String msg) {
        return 0;
    }
    
    private final long timeoutFor(com.jahc.auto.service.SendState s) {
        return 0L;
    }
    
    private final int log(java.lang.String m) {
        return 0;
    }
}