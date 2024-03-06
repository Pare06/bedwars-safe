package org.bedwars.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

// dato che è un pò goofy usare il debugger in un server che crasha se sta 10 secondi afk tocca fa così
// (questa classe rimane come prova della schizofrenia che mi ha fatto venire il rejoin)
@SuppressWarnings({"deprecation", "unused"})
public class Debug {
    private Debug() { }

    // controlla se obj (o una superclass) ha dei campi che si chiamano a vicenda sennò so cazzi
    public static <E> void printObjectInfo(E obj, String name) {
        Class<?> oClass = obj.getClass();

        Bukkit.broadcastMessage(String.format("\n\n\n\nVARIABILI %s:", name));

        printAllFields(oClass, obj, false);
    }

    private static <E> void printField(Field field, E obj) {
        try {
            field.setAccessible(true);
            String fieldName = field.getName();
            Object fieldValue = field.get(obj);
            String valueType = fieldValue == null ? "**null**" : fieldValue.getClass().getTypeName();

            Bukkit.broadcastMessage(String.format("%s (%s) - %s", fieldName, valueType, fieldValue));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <E> void printAllFields(Class<?> c, E obj, boolean isSuper) {
        if (isSuper) {
            Bukkit.broadcastMessage("\nSUPERCLASS " + c.getTypeName() + '\n');
        }
        for (Field field : Arrays.stream(c.getDeclaredFields()).sorted(Comparator.comparing(Field::getName)).toList()) {
            printField(field, obj);
        }
        if (!c.getSuperclass().getTypeName().equals("java.lang.Object")) {
            printAllFields(c.getSuperclass(), obj, true);
        }
    }
}
