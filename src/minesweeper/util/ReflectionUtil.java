package minesweeper.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtil {
    public static Object accessPrivateMemberField(Object object, Class<?> cls, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setPrivateMemberField(Object object, Class<?> cls, String fieldName, Object value) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object accessPrivateMemberMethod(Object object, Class<?> cls, String methodName, Object... args) {
        try {
            Method method = cls.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(object, args);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object accessPrivateMemberFieldMethod(Object object, Class<?> cls, String memberName, String methodName, Object... args) {
        Object member = accessPrivateMemberField(object, cls, memberName);
        return accessPrivateMemberMethod(member, member.getClass(), methodName, args);
    }
}
