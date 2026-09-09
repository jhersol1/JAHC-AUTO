package com.jahc.auto.service;

/**
 * WhatsApp Business flow - completely separate from Dual.
 * Handles: picker -> search contact -> tap -> type message -> send
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\b\u001a\u0004\u0018\u00010\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\tH\u0002J\u0016\u0010\r\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\u000fJ\u001e\u0010\u0010\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u000e\u001a\u00020\u000fJ\u0006\u0010\u0011\u001a\u00020\u0012J(\u0010\u0013\u001a\u00020\u00122\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0015H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/jahc/auto/service/BusinessFlowHandler;", "", "()V", "businessPickerSearched", "", "lastBusinessPickerAction", "", "searchIconTried", "findSearchIcon", "Landroid/view/accessibility/AccessibilityNodeInfo;", "service", "Lcom/jahc/auto/service/AutoAccessibilityService;", "root", "handleDirectMessage", "sch", "Lcom/jahc/auto/data/Schedule;", "handlePickerEvent", "reset", "", "tapBusinessContact", "targetContact", "", "searchText", "app_debug"})
public final class BusinessFlowHandler {
    private static long lastBusinessPickerAction = 0L;
    private static boolean businessPickerSearched = false;
    private static boolean searchIconTried = false;
    @org.jetbrains.annotations.NotNull()
    public static final com.jahc.auto.service.BusinessFlowHandler INSTANCE = null;
    
    private BusinessFlowHandler() {
        super();
    }
    
    public final void reset() {
    }
    
    private final android.view.accessibility.AccessibilityNodeInfo findSearchIcon(com.jahc.auto.service.AutoAccessibilityService service, android.view.accessibility.AccessibilityNodeInfo root) {
        return null;
    }
    
    public final boolean handlePickerEvent(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService service, @org.jetbrains.annotations.NotNull()
    android.view.accessibility.AccessibilityNodeInfo root, @org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule sch) {
        return false;
    }
    
    private final void tapBusinessContact(com.jahc.auto.service.AutoAccessibilityService service, android.view.accessibility.AccessibilityNodeInfo root, java.lang.String targetContact, java.lang.String searchText) {
    }
    
    public final boolean handleDirectMessage(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.service.AutoAccessibilityService service, @org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule sch) {
        return false;
    }
}