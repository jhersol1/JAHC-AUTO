package com.jahc.auto.ui;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u0012\u0012\u0004\u0012\u00020\u0002\u0012\b\u0012\u00060\u0003R\u00020\u00000\u0001:\u0002\u0014\u0015BG\u0012\u0018\u0010\u0004\u001a\u0014\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005\u0012\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00070\t\u0012\u0012\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\u0002\u0010\u000bJ\u001c\u0010\f\u001a\u00020\u00072\n\u0010\r\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\u001c\u0010\u0010\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u000fH\u0016R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00070\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00070\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u0004\u001a\u0014\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/jahc/auto/ui/ScheduleAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/jahc/auto/data/Schedule;", "Lcom/jahc/auto/ui/ScheduleAdapter$VH;", "onToggle", "Lkotlin/Function2;", "", "", "onEdit", "Lkotlin/Function1;", "onDelete", "(Lkotlin/jvm/functions/Function2;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "onBindViewHolder", "holder", "position", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "Diff", "VH", "app_debug"})
public final class ScheduleAdapter extends androidx.recyclerview.widget.ListAdapter<com.jahc.auto.data.Schedule, com.jahc.auto.ui.ScheduleAdapter.VH> {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function2<com.jahc.auto.data.Schedule, java.lang.Boolean, kotlin.Unit> onToggle = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.jahc.auto.data.Schedule, kotlin.Unit> onEdit = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<com.jahc.auto.data.Schedule, kotlin.Unit> onDelete = null;
    
    public ScheduleAdapter(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super com.jahc.auto.data.Schedule, ? super java.lang.Boolean, kotlin.Unit> onToggle, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.jahc.auto.data.Schedule, kotlin.Unit> onEdit, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.jahc.auto.data.Schedule, kotlin.Unit> onDelete) {
        super(null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public com.jahc.auto.ui.ScheduleAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.ui.ScheduleAdapter.VH holder, int position) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/jahc/auto/ui/ScheduleAdapter$Diff;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/jahc/auto/data/Schedule;", "()V", "areContentsTheSame", "", "a", "b", "areItemsTheSame", "app_debug"})
    public static final class Diff extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.jahc.auto.data.Schedule> {
        @org.jetbrains.annotations.NotNull()
        public static final com.jahc.auto.ui.ScheduleAdapter.Diff INSTANCE = null;
        
        private Diff() {
            super();
        }
        
        @java.lang.Override()
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull()
        com.jahc.auto.data.Schedule a, @org.jetbrains.annotations.NotNull()
        com.jahc.auto.data.Schedule b) {
            return false;
        }
        
        @java.lang.Override()
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull()
        com.jahc.auto.data.Schedule a, @org.jetbrains.annotations.NotNull()
        com.jahc.auto.data.Schedule b) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/jahc/auto/ui/ScheduleAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/jahc/auto/databinding/ItemScheduleBinding;", "(Lcom/jahc/auto/ui/ScheduleAdapter;Lcom/jahc/auto/databinding/ItemScheduleBinding;)V", "getBinding", "()Lcom/jahc/auto/databinding/ItemScheduleBinding;", "app_debug"})
    public final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final com.jahc.auto.databinding.ItemScheduleBinding binding = null;
        
        public VH(@org.jetbrains.annotations.NotNull()
        com.jahc.auto.databinding.ItemScheduleBinding binding) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.jahc.auto.databinding.ItemScheduleBinding getBinding() {
            return null;
        }
    }
}