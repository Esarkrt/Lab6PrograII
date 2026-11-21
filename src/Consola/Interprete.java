package Consola;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Interprete {

    private static final String ROOT_NAME = "ConsolaSimulada";

    private final Salida salida;
    private final File root;
    private File current;

    private boolean writeMode = false;
    private File writeTarget;
    private boolean writeAppend;
    private StringBuilder writeBuffer;

    public Interprete(Salida salida) {
        this.salida = salida;
        File r = new File(System.getProperty("user.dir"), ROOT_NAME);
        if (!r.exists()) {
            r.mkdirs();
        }
        this.root = Utilidades.canon(r);
        this.current = root;
    }

    public boolean estaEnModoEscritura() {
        return writeMode;
    }

    public String getPrompt() {
        if (writeMode) {
            return ">";
        }
        return Utilidades.rutaMostrada(root, current) + ">";
    }

    public void mostrarBienvenida() {
        salida.println("Consola Simulada [Version 1.0]");
        salida.println("Escriba Help para ver la lista de comandos disponibles.");
        salida.println("");
    }

    public void procesar(String linea) {
        if (writeMode) {
            procesarLineaEscritura(linea);
        } else {
            salida.println(getPrompt() + linea);
            String t = linea.trim();
            if (!t.isEmpty()) {
                ejecutar(t);
            }
        }
    }

    private void ejecutar(String linea) {
        String cmd;
        String resto;
        int idx = -1;
        for (int i = 0; i < linea.length(); i++) {
            if (Character.isWhitespace(linea.charAt(i))) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            cmd = linea;
            resto = "";
        } else {
            cmd = linea.substring(0, idx);
            resto = linea.substring(idx).trim();
        }

        switch (cmd) {
            case "Mkdir":
                cmdMkdir(resto);
                break;
            case "Mfile":
                cmdMfile(resto);
                break;
            case "Rm":
                cmdRm(resto);
                break;
            case "Cd":
                cmdCd(resto);
                break;
            case "..":
                irAtras();
                break;
            case "Dir":
                cmdDir();
                break;
            case "Date":
                salida.println("La fecha actual es: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
                break;
            case "Time":
                salida.println("La hora actual es: " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                break;
            case "Wr":
                cmdEscribir(resto, false);
                break;
            case "Ap":
                cmdEscribir(resto, true);
                break;
            case "Rd":
                cmdRd(resto);
                break;
            case "Ren":
                cmdRen(resto);
                break;
            case "Copy":
                cmdCopy(resto);
                break;
            case "Find":
                cmdFind(resto);
                break;
            case "Info":
                cmdInfo(resto);
                break;
            case "Tree":
                cmdTree();
                break;
            case "Cls":
                salida.limpiar();
                return;
            case "Help":
                cmdHelp();
                break;
            case "Exit":
                salida.cerrar();
                return;
            default:
                salida.println("'" + cmd + "' no se reconoce como un comando interno o externo,");
                salida.println("programa o archivo por lotes ejecutable.");
        }
        salida.println("");
    }

    private void cmdMkdir(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Mkdir <nombre>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (f.exists()) {
            salida.println("Ya existe un archivo o carpeta con el nombre '" + nombre + "'.");
            return;
        }
        if (f.mkdir()) {
            salida.println("Carpeta creada: " + nombre);
        } else {
            salida.println("No se pudo crear la carpeta '" + nombre + "'.");
        }
    }

    private void cmdMfile(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Mfile <nombre.ext>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (f.exists()) {
            salida.println("Ya existe un archivo o carpeta con el nombre '" + nombre + "'.");
            return;
        }
        try {
            if (f.createNewFile()) {
                salida.println("Archivo creado: " + nombre);
            } else {
                salida.println("No se pudo crear el archivo '" + nombre + "'.");
            }
        } catch (IOException e) {
            salida.println("Error al crear el archivo: " + e.getMessage());
        }
    }

    private void cmdRm(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Rm <nombre>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (!f.exists()) {
            salida.println("No se encuentra '" + nombre + "' en la carpeta actual.");
            return;
        }
        if (Utilidades.borrarRecursivo(f)) {
            salida.println("Eliminado: " + nombre);
        } else {
            salida.println("No se pudo eliminar '" + nombre + "' por completo.");
        }
    }

    private void cmdCd(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println(Utilidades.rutaMostrada(root, current));
            return;
        }
        if (nombre.equals("..")) {
            irAtras();
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (!f.exists()) {
            salida.println("El sistema no puede encontrar la ruta especificada: " + nombre);
            return;
        }
        if (!f.isDirectory()) {
            salida.println("'" + nombre + "' no es una carpeta.");
            return;
        }
        current = Utilidades.canon(f);
    }

    private void irAtras() {
        if (Utilidades.canon(current).equals(root)) {
            salida.println("Ya se encuentra en la carpeta raiz.");
            return;
        }
        File padre = current.getParentFile();
        if (padre == null) {
            salida.println("Ya se encuentra en la carpeta raiz.");
            return;
        }
        current = Utilidades.canon(padre);
    }

    private void cmdDir() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm");
        salida.println(" Directorio de " + Utilidades.rutaMostrada(root, current));
        salida.println("");
        int archivos = 0, carpetas = 0;
        long bytes = 0;
        for (File f : Utilidades.listarOrdenado(current)) {
            String fecha = sdf.format(new Date(f.lastModified()));
            if (f.isDirectory()) {
                carpetas++;
                salida.println(fecha + "    <DIR>          " + f.getName());
            } else {
                archivos++;
                bytes += f.length();
                salida.println(fecha + "    " + String.format("%14d", f.length()) + " " + f.getName());
            }
        }
        salida.println(String.format("%15d archivo(s) %14d bytes", archivos, bytes));
        salida.println(String.format("%15d carpeta(s)", carpetas));
    }

    private void cmdEscribir(String arg, boolean append) {
        String nombre = Utilidades.quitarComillas(arg);
        String sintaxis = append ? "Ap" : "Wr";
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: " + sintaxis + " <archivo.ext>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (f.isDirectory()) {
            salida.println("'" + nombre + "' es una carpeta, no un archivo.");
            return;
        }
        if (append && !f.exists()) {
            salida.println("No se encuentra el archivo '" + nombre + "'.");
            return;
        }

        writeMode = true;
        writeTarget = f;
        writeAppend = append;
        writeBuffer = new StringBuilder();
        salida.println((append ? "Agregando texto a " : "Escribiendo en ") + nombre
                + ". Escriba EXIT (mayusculas) en una linea para terminar.");
    }

    private void procesarLineaEscritura(String linea) {
        salida.println(linea);
        if (linea.equals("EXIT")) {
            writeMode = false;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(writeTarget, writeAppend))) {
                bw.write(writeBuffer.toString());
                salida.println(writeAppend
                        ? "Texto agregado correctamente a " + writeTarget.getName() + "."
                        : "Archivo guardado correctamente: " + writeTarget.getName() + ".");
            } catch (IOException e) {
                salida.println("Error al escribir en el archivo: " + e.getMessage());
            }
            salida.println("");
            writeBuffer = null;
            writeTarget = null;
        } else {
            writeBuffer.append(linea).append(System.lineSeparator());
        }
    }

    private void cmdRd(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Rd <archivo.ext>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (!f.exists()) {
            salida.println("No se encuentra el archivo '" + nombre + "'.");
            return;
        }
        if (!f.isFile()) {
            salida.println("'" + nombre + "' no es un archivo.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            boolean vacio = true;
            while ((l = br.readLine()) != null) {
                salida.println(l);
                vacio = false;
            }
            if (vacio) {
                salida.println("(el archivo esta vacio)");
            }
        } catch (IOException e) {
            salida.println("Error al leer el archivo: " + e.getMessage());
        }
    }

    private void cmdRen(String arg) {
        List<String> t = Utilidades.tokenizar(arg);
        if (t.size() != 2) {
            salida.println("La sintaxis del comando no es correcta. Uso: Ren <actual> <nuevo>");
            return;
        }
        String actual = t.get(0), nuevo = t.get(1);
        if (!Utilidades.nombreValido(actual) || !Utilidades.nombreValido(nuevo)) {
            salida.println("Nombre no valido.");
            return;
        }
        File f = new File(current, actual);
        File n = new File(current, nuevo);
        if (!f.exists()) {
            salida.println("No se encuentra '" + actual + "'.");
            return;
        }
        if (n.exists()) {
            salida.println("Ya existe un archivo o carpeta con el nombre '" + nuevo + "'.");
            return;
        }
        if (f.renameTo(n)) {
            salida.println("'" + actual + "' renombrado a '" + nuevo + "'.");
        } else {
            salida.println("No se pudo renombrar '" + actual + "'.");
        }
    }

    private void cmdCopy(String arg) {
        List<String> t = Utilidades.tokenizar(arg);
        if (t.size() != 2) {
            salida.println("La sintaxis del comando no es correcta. Uso: Copy <origen> <destino>");
            return;
        }
        String origen = t.get(0), destino = t.get(1);
        if (!Utilidades.nombreValido(origen) || !Utilidades.nombreValido(destino)) {
            salida.println("Nombre no valido.");
            return;
        }
        File o = new File(current, origen);
        File d = new File(current, destino);
        if (!o.exists()) {
            salida.println("No se encuentra el archivo '" + origen + "'.");
            return;
        }
        if (!o.isFile()) {
            salida.println("'" + origen + "' no es un archivo (Copy solo copia archivos).");
            return;
        }
        if (d.exists()) {
            salida.println("Ya existe un archivo o carpeta con el nombre '" + destino + "'.");
            return;
        }
        try (FileReader fr = new FileReader(o); FileWriter fw = new FileWriter(d)) {
            char[] buffer = new char[4096];
            int n;
            while ((n = fr.read(buffer)) != -1) {
                fw.write(buffer, 0, n);
            }
            salida.println("Se copio '" + origen + "' a '" + destino + "'. 1 archivo(s) copiado(s).");
        } catch (IOException e) {
            salida.println("Error al copiar: " + e.getMessage());
        }
    }

    private void cmdFind(String arg) {
        String texto = Utilidades.quitarComillas(arg);
        if (texto.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Find <nombre>");
            return;
        }
        int[] cont = {0};
        buscar(current, texto.toLowerCase(), "", cont);
        if (cont[0] == 0) {
            salida.println("No se encontraron archivos ni carpetas que contengan '" + texto + "'.");
        } else {
            salida.println(cont[0] + " resultado(s) encontrado(s).");
        }
    }

    private void buscar(File dir, String texto, String relativo, int[] cont) {
        for (File f : Utilidades.listarOrdenado(dir)) {
            String rel = relativo.isEmpty() ? f.getName() : relativo + "\\" + f.getName();
            if (f.getName().toLowerCase().contains(texto)) {
                salida.println((f.isDirectory() ? "[DIR]  " : "[FILE] ") + rel);
                cont[0]++;
            }
            if (f.isDirectory()) {
                buscar(f, texto, rel, cont);
            }
        }
    }

    private void cmdInfo(String arg) {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println("La sintaxis del comando no es correcta. Uso: Info <nombre>");
            return;
        }
        if (!Utilidades.nombreValido(nombre)) {
            salida.println("Nombre no valido: " + nombre);
            return;
        }
        File f = new File(current, nombre);
        if (!f.exists()) {
            salida.println("No se encuentra '" + nombre + "'.");
            return;
        }
        salida.println("Nombre:                " + f.getName());
        salida.println("Tipo:                  " + (f.isDirectory() ? "Carpeta" : "Archivo"));
        salida.println("Ruta:                  " + Utilidades.rutaMostrada(root, f));
        salida.println("Tamano:                " + Utilidades.tamanoTotal(f) + " bytes");
        salida.println("Ultima modificacion:   "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(f.lastModified())));
    }

    private void cmdTree() {
        salida.println(Utilidades.rutaMostrada(root, current));
        arbol(current, "");
    }

    private void arbol(File dir, String prefijo) {
        File[] lista = Utilidades.listarOrdenado(dir);
        for (int i = 0; i < lista.length; i++) {
            boolean ultimo = (i == lista.length - 1);
            salida.println(prefijo + (ultimo ? "\\---" : "+---") + lista[i].getName());
            if (lista[i].isDirectory()) {
                arbol(lista[i], prefijo + (ultimo ? "    " : "|   "));
            }
        }
    }

    private void cmdHelp() {
        salida.println("Comandos disponibles (respete mayusculas/minusculas):");
        salida.println("");
        salida.println("  Mkdir <nombre>            Crea una nueva carpeta.");
        salida.println("  Mfile <nombre.ext>        Crea un nuevo archivo.");
        salida.println("  Rm <nombre>               Elimina un archivo o carpeta (con su contenido).");
        salida.println("  Cd <carpeta>              Cambia a la carpeta indicada.");
        salida.println("  ..                        Regresa a la carpeta anterior.");
        salida.println("  Dir                       Lista archivos y carpetas de la carpeta actual.");
        salida.println("  Date                      Muestra la fecha actual.");
        salida.println("  Time                      Muestra la hora actual.");
        salida.println("  Wr <archivo.ext>          Escribe texto en el archivo (termina con EXIT).");
        salida.println("  Rd <archivo.ext>          Lee y muestra el contenido del archivo.");
        salida.println("  Ap <archivo.ext>          Agrega texto al final del archivo (termina con EXIT).");
        salida.println("  Ren <actual> <nuevo>      Renombra un archivo o carpeta.");
        salida.println("  Copy <origen> <destino>   Copia el contenido de un archivo a uno nuevo.");
        salida.println("  Find <nombre>             Busca archivos/carpetas por nombre (incluye subcarpetas).");
        salida.println("  Info <nombre>             Muestra tipo, ruta, tamano y fecha de modificacion.");
        salida.println("  Tree                      Muestra la estructura en forma de arbol.");
        salida.println("  Grep <texto> <archivo>    Busca texto dentro del contenido de un archivo.");
        salida.println("  Cls                       Limpia la pantalla.");
        salida.println("  Help                      Muestra esta ayuda.");
        salida.println("  Exit                      Cierra la aplicacion.");
        salida.println("");
        salida.println("Nota: use comillas para nombres con espacios, por ejemplo: Mkdir \"Mi carpeta\"");
    }

}
