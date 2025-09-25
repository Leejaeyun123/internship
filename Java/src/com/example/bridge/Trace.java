// Trace.java
package com.example.bridge;

public final class Trace {
    public static void log(String tag, String fmt, Object... args) {
        long now = System.currentTimeMillis();
        String msg = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        System.out.printf("%tT.%<tL [%s] %-10s %s%n",
                now, Thread.currentThread().getName(), tag, msg);
    }
}
