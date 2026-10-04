package com.jahc.auto.service;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0016\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016\u00a8\u0006\u0017"}, d2 = {"Lcom/jahc/auto/service/SendState;", "", "(Ljava/lang/String;I)V", "IDLE", "UNLOCKING", "WAITING_FOR_DEVICE_READY", "OPENING_WHATSAPP", "WAITING_FOR_WHATSAPP", "VERIFYING_WHATSAPP", "OPENING_SEARCH", "WAITING_FOR_SEARCH", "SEARCHING_CONTACT", "VERIFYING_CONTACT", "OPENING_CHAT", "VERIFYING_CHAT", "TYPING_MESSAGE", "VERIFYING_MESSAGE", "SENDING", "VERIFYING_SENT", "REVERIFY_SENT", "CLEANUP", "READY_FOR_NEXT_MESSAGE", "FAILED", "app_debug"})
public enum SendState {
    /*public static final*/ IDLE /* = new IDLE() */,
    /*public static final*/ UNLOCKING /* = new UNLOCKING() */,
    /*public static final*/ WAITING_FOR_DEVICE_READY /* = new WAITING_FOR_DEVICE_READY() */,
    /*public static final*/ OPENING_WHATSAPP /* = new OPENING_WHATSAPP() */,
    /*public static final*/ WAITING_FOR_WHATSAPP /* = new WAITING_FOR_WHATSAPP() */,
    /*public static final*/ VERIFYING_WHATSAPP /* = new VERIFYING_WHATSAPP() */,
    /*public static final*/ OPENING_SEARCH /* = new OPENING_SEARCH() */,
    /*public static final*/ WAITING_FOR_SEARCH /* = new WAITING_FOR_SEARCH() */,
    /*public static final*/ SEARCHING_CONTACT /* = new SEARCHING_CONTACT() */,
    /*public static final*/ VERIFYING_CONTACT /* = new VERIFYING_CONTACT() */,
    /*public static final*/ OPENING_CHAT /* = new OPENING_CHAT() */,
    /*public static final*/ VERIFYING_CHAT /* = new VERIFYING_CHAT() */,
    /*public static final*/ TYPING_MESSAGE /* = new TYPING_MESSAGE() */,
    /*public static final*/ VERIFYING_MESSAGE /* = new VERIFYING_MESSAGE() */,
    /*public static final*/ SENDING /* = new SENDING() */,
    /*public static final*/ VERIFYING_SENT /* = new VERIFYING_SENT() */,
    /*public static final*/ REVERIFY_SENT /* = new REVERIFY_SENT() */,
    /*public static final*/ CLEANUP /* = new CLEANUP() */,
    /*public static final*/ READY_FOR_NEXT_MESSAGE /* = new READY_FOR_NEXT_MESSAGE() */,
    /*public static final*/ FAILED /* = new FAILED() */;
    
    SendState() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.jahc.auto.service.SendState> getEntries() {
        return null;
    }
}