package com.jahc.auto.service;

/**
 * FIFO + mutex: solo un mensaje controla WhatsApp a la vez.
 * Los receivers encolan; la máquina consume de a uno.
 * Dedup por id: evita doble envío entre AlarmReceiver y WorkManager.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\bJ(\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u0015\u001a\u00020\b2\u0006\u0010\u001a\u001a\u00020\u00142\b\u0010\u001b\u001a\u0004\u0018\u00010\u0006J\u000e\u0010\u001c\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019J\u0006\u0010\u001d\u001a\u00020\u001eJ\u000e\u0010\u001f\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\fR\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\b0\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/jahc/auto/service/SendQueue;", "", "()V", "FINISH_MEMORY_MS", "", "TAG", "", "current", "Lcom/jahc/auto/data/Schedule;", "getCurrent", "()Lcom/jahc/auto/data/Schedule;", "setCurrent", "(Lcom/jahc/auto/data/Schedule;)V", "finishedAt", "Ljava/util/concurrent/ConcurrentHashMap;", "processing", "Ljava/util/concurrent/atomic/AtomicBoolean;", "queue", "Ljava/util/concurrent/ConcurrentLinkedQueue;", "enqueue", "", "sch", "finish", "", "svc", "Lcom/jahc/auto/service/AutoAccessibilityService;", "success", "reason", "openWhatsApp", "pendingCount", "", "pumpIfIdle", "app_debug"})
public final class SendQueue {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "SendQueue";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.concurrent.ConcurrentLinkedQueue<com.jahc.auto.data.Schedule> queue = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.concurrent.atomic.AtomicBoolean processing = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.jahc.auto.data.Schedule current;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.concurrent.ConcurrentHashMap<java.lang.Long, java.lang.Long> finishedAt = null;
    private static final long FINISH_MEMORY_MS = 600000L;
    @org.jetbrains.annotations.NotNull()
    public static final com.jahc.auto.service.SendQueue INSTANCE = null;
    
    private SendQueue() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jahc.auto.data.Schedule getCurrent() {
        return null;
    }
    
    public final void setCurrent(@org.jetbrains.annotations.Nullable()
    com.jahc.auto.data.Schedule p0) {
    }
    
    public final boolean enqueue(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule sch) {
        return false;
    }
    
    public final int pendingCount() {
        return 0;
    }
    
    public final void pumpIfIdle(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService svc) {
    }
    
    public final void finish(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService svc, @org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule sch, boolean success, @org.jetbrains.annotations.Nullable()
    java.lang.String reason) {
    }
    
    /**
     * Lleva WhatsApp al frente. Bloqueado: setAlarmClock (whitelisted BAL). Desbloqueado: directo.
     */
    public final void openWhatsApp(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService svc) {
    }
}