package edu.ucla.util;
public class Assert {
    public static final void condition(boolean value) {
        if (!value) {
            throw new IllegalStateException();
        }
    }
    public static final void condition(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
    public static final void notNull(Object obj) {
        condition(obj != null, " is null");
    }
    public static final void notNull(Object obj, String message) {
        condition(obj != null, message);
    }
    public static final void noNullElements(Object[] obj) {
        notNull(obj);
        for (int i = 0; i < obj.length; i++) {
            notNull(obj[i], ""+i + "of"+obj.length);
        }
    }
}
