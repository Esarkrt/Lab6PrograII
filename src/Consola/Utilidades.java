package Consola;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class Utilidades {

    private Utilidades() {
    }

    public static File canon(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
            return f.getAbsoluteFile();
        }
    }

    public static String rutaMostrada(File root, File f) {
        String rutaRaiz = root.getPath();
        String rutaF = canon(f).getPath();
        String rel = rutaF.length() >= rutaRaiz.length() ? rutaF.substring(rutaRaiz.length()) : "";
        rel = rel.replace('/', '\\');
        return "C:\\" + root.getName() + rel;
    }

    public static boolean nombreValido(String n) {
        if (n == null || n.trim().isEmpty()) {
            return false;
        }
        if (n.equals(".") || n.equals("..")) {
            return false;
        }
        for (char c : "\\/:*?\"<>|".toCharArray()) {
            if (n.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    public static String quitarComillas(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    public static List<String> tokenizar(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false, hay = false;
        for (char c : s.toCharArray()) {
            if (c == '"') {
                enComillas = !enComillas;
                hay = true;
            } else if (Character.isWhitespace(c) && !enComillas) {
                if (hay || sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                    hay = false;
                }
            } else {
                sb.append(c);
            }
        }
        if (hay || sb.length() > 0) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    public static File[] listarOrdenado(File dir) {
        File[] lista = dir.listFiles();
        if (lista == null) {
            return new File[0];
        }
        Arrays.sort(lista, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return lista;
    }

    public static long tamanoTotal(File f) {
        if (f.isFile()) {
            return f.length();
        }
        long total = 0;
        File[] hijos = f.listFiles();
        if (hijos != null) {
            for (File h : hijos) {
                total += tamanoTotal(h);
            }
        }
        return total;
    }

    public static boolean borrarRecursivo(File f) {
        if (f.isDirectory()) {
            File[] hijos = f.listFiles();
            if (hijos != null) {
                for (File h : hijos) {
                    if (!borrarRecursivo(h)) {
                        return false;
                    }
                }
            }
        }
        return f.delete();
    }
}
