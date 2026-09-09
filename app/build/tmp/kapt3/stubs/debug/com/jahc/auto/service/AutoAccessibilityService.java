package com.jahc.auto.service;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u000b\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0003\u0018\u0000 M2\u00020\u0001:\u0001MB\u0005\u00a2\u0006\u0002\u0010\u0002J\r\u0010\u0012\u001a\u00020\u0013H\u0000\u00a2\u0006\u0002\b\u0014J\u0010\u0010\u0015\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J#\u0010\u0018\u001a\u00020\u00132\u0006\u0010\u0019\u001a\u00020\u00172\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00170\u001bH\u0000\u00a2\u0006\u0002\b\u001cJ\u001e\u0010\u001d\u001a\u00020\u00132\u0006\u0010\u0019\u001a\u00020\u00172\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00170\u001bH\u0002J\u0010\u0010\u001e\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u0017H\u0002J\u001d\u0010\u001f\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u00172\u0006\u0010 \u001a\u00020!H\u0000\u00a2\u0006\u0002\b\"J\u001f\u0010#\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u00172\u0006\u0010 \u001a\u00020!H\u0000\u00a2\u0006\u0002\b$J\u001a\u0010%\u001a\u0004\u0018\u00010&2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\'\u001a\u00020!H\u0002J\u0012\u0010(\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u0017H\u0002J\u0017\u0010)\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u0017H\u0000\u00a2\u0006\u0002\b*J\u0017\u0010+\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u0017H\u0000\u00a2\u0006\u0002\b,J\n\u0010-\u001a\u0004\u0018\u00010\u0017H\u0002J\u001a\u0010.\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u00172\u0006\u0010/\u001a\u00020!H\u0002J\u001f\u00100\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u00172\u0006\u0010 \u001a\u00020!H\u0000\u00a2\u0006\u0002\b1J\u001f\u00102\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u00172\u0006\u0010 \u001a\u00020!H\u0000\u00a2\u0006\u0002\b3J\u0017\u00104\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u0017H\u0000\u00a2\u0006\u0002\b5J\u0017\u00106\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0019\u001a\u00020\u0017H\u0000\u00a2\u0006\u0002\b7J\u0010\u00108\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0010\u00109\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0010\u0010:\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u0017H\u0002J\u0012\u0010;\u001a\u00020\u00132\b\u0010<\u001a\u0004\u0018\u00010=H\u0016J\b\u0010>\u001a\u00020\u0013H\u0016J\b\u0010?\u001a\u00020\u0013H\u0016J\b\u0010@\u001a\u00020\u0013H\u0014J\u001d\u0010A\u001a\u00020\u00132\u0006\u0010B\u001a\u00020\u00042\u0006\u0010C\u001a\u00020DH\u0000\u00a2\u0006\u0002\bEJ\u0010\u0010F\u001a\u00020\u00132\u0006\u0010G\u001a\u00020!H\u0002J\u0018\u0010H\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010G\u001a\u00020!H\u0002J\b\u0010I\u001a\u00020\u0004H\u0002J\f\u0010J\u001a\u00020K*\u00020\u0017H\u0002J\f\u0010L\u001a\u00020K*\u00020\u0017H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u000f\"\u0004\b\u0010\u0010\u0011\u00a8\u0006N"}, d2 = {"Lcom/jahc/auto/service/AutoAccessibilityService;", "Landroid/accessibilityservice/AccessibilityService;", "()V", "dualPickerSearched", "", "isPinRunning", "lastDirectAction", "", "lastDualAction", "lastLockAction", "lastPickerAction", "lastPinSuccess", "lastPinTap", "waitingForDualPicker", "getWaitingForDualPicker", "()Z", "setWaitingForDualPicker", "(Z)V", "clearPendingPrefs", "", "clearPendingPrefs$app_debug", "clickRightWhatsApp", "root", "Landroid/view/accessibility/AccessibilityNodeInfo;", "collectAllClickable", "node", "out", "", "collectAllClickable$app_debug", "collectContactCandidates", "containsDigitGrid", "containsText", "text", "", "containsText$app_debug", "findButtonByText", "findButtonByText$app_debug", "findDigitBounds", "Landroid/graphics/PointF;", "digit", "findDualByChildDesc", "findEditText", "findEditText$app_debug", "findFirstContact", "findFirstContact$app_debug", "findLockScreenRoot", "findNodeByResId", "resId", "findNodeContainingText", "findNodeContainingText$app_debug", "findNodeWithText", "findNodeWithText$app_debug", "findSendButton", "findSendButton$app_debug", "findSendButtonById", "findSendButtonById$app_debug", "handleDualChooser", "handleLockScreen", "hasPinPad", "onAccessibilityEvent", "event", "Landroid/view/accessibility/AccessibilityEvent;", "onDestroy", "onInterrupt", "onServiceConnected", "showResultNotification", "success", "sch", "Lcom/jahc/auto/data/Schedule;", "showResultNotification$app_debug", "tapDigitsAsync", "code", "tapDigitsByNode", "trySwipeUp", "boundsInScreenRect", "", "boundsInScreenRectY", "Companion", "app_debug"})
public final class AutoAccessibilityService extends android.accessibilityservice.AccessibilityService {
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private static volatile com.jahc.auto.data.Schedule pendingSchedule;
    @kotlin.jvm.Volatile()
    private static volatile boolean enabled = false;
    private long lastLockAction = 0L;
    private long lastPinSuccess = 0L;
    private long lastPickerAction = 0L;
    private long lastDirectAction = 0L;
    private long lastDualAction = 0L;
    @kotlin.jvm.Volatile()
    private volatile boolean waitingForDualPicker = false;
    @kotlin.jvm.Volatile()
    private volatile boolean dualPickerSearched = false;
    private long lastPinTap = 0L;
    @kotlin.jvm.Volatile()
    private volatile boolean isPinRunning = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.jahc.auto.service.AutoAccessibilityService.Companion Companion = null;
    
    public AutoAccessibilityService() {
        super();
    }
    
    public final boolean getWaitingForDualPicker() {
        return false;
    }
    
    public final void setWaitingForDualPicker(boolean p0) {
    }
    
    @java.lang.Override()
    protected void onServiceConnected() {
    }
    
    private final android.view.accessibility.AccessibilityNodeInfo findLockScreenRoot() {
        return null;
    }
    
    @java.lang.Override()
    public void onAccessibilityEvent(@org.jetbrains.annotations.Nullable()
    android.view.accessibility.AccessibilityEvent event) {
    }
    
    public final void clearPendingPrefs$app_debug() {
    }
    
    public final void showResultNotification$app_debug(boolean success, @org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule sch) {
    }
    
    public final boolean containsText$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return false;
    }
    
    private final void handleDualChooser(android.view.accessibility.AccessibilityNodeInfo root) {
    }
    
    private final void clickRightWhatsApp(android.view.accessibility.AccessibilityNodeInfo root) {
    }
    
    private final android.view.accessibility.AccessibilityNodeInfo findNodeByResId(android.view.accessibility.AccessibilityNodeInfo node, java.lang.String resId) {
        return null;
    }
    
    private final android.view.accessibility.AccessibilityNodeInfo findDualByChildDesc(android.view.accessibility.AccessibilityNodeInfo node) {
        return null;
    }
    
    public final void collectAllClickable$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node, @org.jetbrains.annotations.NotNull()
    java.util.List<android.view.accessibility.AccessibilityNodeInfo> out) {
    }
    
    private final int boundsInScreenRect(android.view.accessibility.AccessibilityNodeInfo $this$boundsInScreenRect) {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findEditText$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findSendButton$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findSendButtonById$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findButtonByText$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findNodeWithText$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findNodeContainingText$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.view.accessibility.AccessibilityNodeInfo findFirstContact$app_debug(@org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo node) {
        return null;
    }
    
    private final void collectContactCandidates(android.view.accessibility.AccessibilityNodeInfo node, java.util.List<android.view.accessibility.AccessibilityNodeInfo> out) {
    }
    
    private final int boundsInScreenRectY(android.view.accessibility.AccessibilityNodeInfo $this$boundsInScreenRectY) {
        return 0;
    }
    
    private final boolean hasPinPad(android.view.accessibility.AccessibilityNodeInfo node) {
        return false;
    }
    
    private final boolean handleLockScreen(android.view.accessibility.AccessibilityNodeInfo root) {
        return false;
    }
    
    private final boolean containsDigitGrid(android.view.accessibility.AccessibilityNodeInfo node) {
        return false;
    }
    
    private final android.graphics.PointF findDigitBounds(android.view.accessibility.AccessibilityNodeInfo root, java.lang.String digit) {
        return null;
    }
    
    private final void tapDigitsAsync(java.lang.String code) {
    }
    
    private final boolean tapDigitsByNode(android.view.accessibility.AccessibilityNodeInfo root, java.lang.String code) {
        return false;
    }
    
    private final boolean trySwipeUp() {
        return false;
    }
    
    @java.lang.Override()
    public void onInterrupt() {
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001c\u0010\t\u001a\u0004\u0018\u00010\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lcom/jahc/auto/service/AutoAccessibilityService$Companion;", "", "()V", "enabled", "", "getEnabled", "()Z", "setEnabled", "(Z)V", "pendingSchedule", "Lcom/jahc/auto/data/Schedule;", "getPendingSchedule", "()Lcom/jahc/auto/data/Schedule;", "setPendingSchedule", "(Lcom/jahc/auto/data/Schedule;)V", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.jahc.auto.data.Schedule getPendingSchedule() {
            return null;
        }
        
        public final void setPendingSchedule(@org.jetbrains.annotations.Nullable()
        com.jahc.auto.data.Schedule p0) {
        }
        
        public final boolean getEnabled() {
            return false;
        }
        
        public final void setEnabled(boolean p0) {
        }
    }
}