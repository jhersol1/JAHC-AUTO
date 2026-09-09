package com.jahc.auto.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u0018\u0010\n\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u000b\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00050\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ\u001e\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u0010\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\b0\u0013H\'J\u0016\u0010\u0014\u001a\u00020\f2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0015"}, d2 = {"Lcom/jahc/auto/data/ScheduleDao;", "", "delete", "", "schedule", "Lcom/jahc/auto/data/Schedule;", "(Lcom/jahc/auto/data/Schedule;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllOnce", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getById", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEnabled", "markSent", "ts", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeAll", "Lkotlinx/coroutines/flow/Flow;", "upsert", "app_debug"})
@androidx.room.Dao()
public abstract interface ScheduleDao {
    
    @androidx.room.Query(value = "SELECT * FROM schedules ORDER BY hour, minute")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.jahc.auto.data.Schedule>> observeAll();
    
    @androidx.room.Query(value = "SELECT * FROM schedules")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllOnce(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.jahc.auto.data.Schedule>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM schedules WHERE enabled = 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEnabled(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.jahc.auto.data.Schedule>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM schedules WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getById(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.jahc.auto.data.Schedule> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule schedule, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(@org.jetbrains.annotations.NotNull()
    com.jahc.auto.data.Schedule schedule, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE schedules SET lastSentAt = :ts WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markSent(long id, long ts, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}