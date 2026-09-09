package com.jahc.auto.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0019\u001a\u00020\u00162\u0006\u0010\u001a\u001a\u00020\u0016H\u0002J\b\u0010\u001b\u001a\u00020\u001cH\u0002J\u0010\u0010\u001d\u001a\u00020\u00162\u0006\u0010\u001a\u001a\u00020\u0016H\u0002J\u0012\u0010\u001e\u001a\u00020\u001c2\b\u0010\u001f\u001a\u0004\u0018\u00010 H\u0014J\b\u0010!\u001a\u00020\u001cH\u0002J\b\u0010\"\u001a\u00020\u001cH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0007\u001a\u00020\b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000b\u0010\f\u001a\u0004\b\t\u0010\nR\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0015\u001a\u0010\u0012\f\u0012\n \u0017*\u0004\u0018\u00010\u00160\u00160\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/jahc/auto/ui/EditActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/jahc/auto/databinding/ActivityEditBinding;", "day", "", "db", "Lcom/jahc/auto/data/AppDatabase;", "getDb", "()Lcom/jahc/auto/data/AppDatabase;", "db$delegate", "Lkotlin/Lazy;", "editingId", "", "hour", "minute", "month", "pickContactLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "Ljava/lang/Void;", "requestPermissionLauncher", "", "kotlin.jvm.PlatformType", "year", "formatPhone", "raw", "launchContactPicker", "", "normalizePhone", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "updateDateButton", "updateTimeButton", "app_debug"})
public final class EditActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.jahc.auto.databinding.ActivityEditBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy db$delegate = null;
    private long editingId = -1L;
    private int hour = 9;
    private int minute = 0;
    private int year = 0;
    private int month = 0;
    private int day = 0;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> requestPermissionLauncher = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.Void> pickContactLauncher = null;
    
    public EditActivity() {
        super();
    }
    
    private final com.jahc.auto.data.AppDatabase getDb() {
        return null;
    }
    
    private final void launchContactPicker() {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void updateDateButton() {
    }
    
    private final void updateTimeButton() {
    }
    
    private final java.lang.String formatPhone(java.lang.String raw) {
        return null;
    }
    
    private final java.lang.String normalizePhone(java.lang.String raw) {
        return null;
    }
}