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
        if (writeMode) return ">";
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

   
    private String nombreDe(String arg, String uso) throws ComandoException {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            throw new ComandoException("La sintaxis del comando no es correcta. Uso: " + uso);
        }
        if (!Utilidades.nombreValido(nombre)) {
            throw new ComandoException("Nombre no valido: " + nombre);
        }
        return nombre;
    }

    
    private File existente(String nombre) throws ComandoException {
        File f = new File(current, nombre);
        if (!f.exists()) {
            throw new ComandoException("No se encuentra '" + nombre + "' en la carpeta actual.");
        }
        return f;
    }

    
    private File libre(String nombre) throws ComandoException {
        File f = new File(current, nombre);
        if (f.exists()) {
            throw new ComandoException("Ya existe un archivo o carpeta con el nombre '" + nombre + "'.");
        }
        return f;
    }

    private File archivoExistente(String nombre) throws ComandoException {
        File f = existente(nombre);
        if (!f.isFile()) {
            throw new ComandoException("'" + nombre + "' no es un archivo.");
        }
        return f;
    }

    
    private List<String> dosArgumentos(String arg, String uso) throws ComandoException {
        List<String> t = Utilidades.tokenizar(arg);
        if (t.size() != 2) {
            throw new ComandoException("La sintaxis del comando no es correcta. Uso: " + uso);
        }
        for (String s : t) {
            if (!Utilidades.nombreValido(s)) {
                throw new ComandoException("Nombre no valido: " + s);
            }
        }
        return t;
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

        try {
            switch (cmd) {
                case "Mkdir": cmdMkdir(resto); break;
                case "Mfile": cmdMfile(resto); break;
                case "Rm":    cmdRm(resto); break;
                case "Cd":    cmdCd(resto); break;
                case "..":    irAtras(); break;
                case "Dir":   cmdDir(); break;
                case "Date":  salida.println("La fecha actual es: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date())); break;
                case "Time":  salida.println("La hora actual es: " + new SimpleDateFormat("HH:mm:ss").format(new Date())); break;
                case "Wr":    cmdEscribir(resto, false); break;
                case "Ap":    cmdEscribir(resto, true); break;
                case "Rd":    cmdRd(resto); break;
                case "Ren":   cmdRen(resto); break;
                case "Copy":  cmdCopy(resto); break;
                case "Find":  cmdFind(resto); break;
                case "Info":  cmdInfo(resto); break;
                case "Tree":  cmdTree(); break;
                case "Cls":   salida.limpiar(); return;
                case "Help":  cmdHelp(); break;
                case "Exit":  salida.cerrar(); return;
                default:
                    throw new ComandoException("'" + cmd + "' no se reconoce como un comando interno o externo,\n"
                            + "programa o archivo por lotes ejecutable.");
            }
        } catch (ComandoException e) {
            salida.println(e.getMessage());
        }
        salida.println("");
    }

    // ===================== COMANDOS =====================

    private void cmdMkdir(String arg) throws ComandoException {
        String nombre = nombreDe(arg, "Mkdir <nombre>");
        File f = libre(nombre);
        if (!f.mkdir()) {
            throw new ComandoException("No se pudo crear la carpeta '" + nombre + "'.");
        }
        salida.println("Carpeta creada: " + nombre);
    }

    private void cmdMfile(String arg) throws ComandoException {
        String nombre = nombreDe(arg, "Mfile <nombre.ext>");
        File f = libre(nombre);
        try {
            if (!f.createNewFile()) {
                throw new ComandoException("No se pudo crear el archivo '" + nombre + "'.");
            }
        } catch (IOException e) {
            throw new ComandoException("Error al crear el archivo: " + e.getMessage());
        }
        salida.println("Archivo creado: " + nombre);
    }

    private void cmdRm(String arg) throws ComandoException {
        String nombre = nombreDe(arg, "Rm <nombre>");
        File f = existente(nombre);
        if (!Utilidades.borrarRecursivo(f)) {
            throw new ComandoException("No se pudo eliminar '" + nombre + "' por completo.");
        }
        salida.println("Eliminado: " + nombre);
    }

    private void cmdCd(String arg) throws ComandoException {
        String nombre = Utilidades.quitarComillas(arg);
        if (nombre.isEmpty()) {
            salida.println(Utilidades.rutaMostrada(root, current));
            return;
        }
        if (nombre.equals("..")) {
            irAtras();
            return;
        }
        nombre = nombreDe(nombre, "Cd <carpeta>");
        File f = new File(current, nombre);
        if (!f.exists()) {
            throw new ComandoException("El sistema no puede encontrar la ruta especificada: " + nombre);
        }
        if (!f.isDirectory()) {
            throw new ComandoException("'" + nombre + "' no es una carpeta.");
        }
        current = Utilidades.canon(f);
    }

    private void irAtras() throws ComandoException {
        File padre = current.getParentFile();
        if (Utilidades.canon(current).equals(root) || padre == null) {
            throw new ComandoException("Ya se encuentra en la carpeta raiz.");
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

    // ---- Wr / Ap ----
    private void cmdEscribir(String arg, boolean append) throws ComandoException {
        String nombre = nombreDe(arg, (append ? "Ap" : "Wr") + " <archivo.ext>");
        File f = new File(current, nombre);
        if (f.isDirectory()) {
            throw new ComandoException("'" + nombre + "' es una carpeta, no un archivo.");
        }
        if (append && !f.exists()) {
            throw new ComandoException("No se encuentra el archivo '" + nombre + "'.");
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
            try {
                guardarBuffer();
                salida.println(writeAppend
                        ? "Texto agregado correctamente a " + writeTarget.getName() + "."
                        : "Archivo guardado correctamente: " + writeTarget.getName() + ".");
            } catch (ComandoException e) {
                salida.println(e.getMessage());
            }
            salida.println("");
            writeBuffer = null;
            writeTarget = null;
        } else {
            writeBuffer.append(linea).append(System.lineSeparator());
        }
    }

    private void guardarBuffer() throws ComandoException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(writeTarget, writeAppend))) {
            bw.write(writeBuffer.toString());
        } catch (IOException e) {
            throw new ComandoException("Error al escribir en el archivo: " + e.getMessage());
        }
    }

    private void cmdRd(String arg) throws ComandoException {
        String nombre = nombreDe(arg, "Rd <archivo.ext>");
        File f = archivoExistente(nombre);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String l;
            boolean vacio = true;
            while ((l = br.readLine()) != null) {
                salida.println(l);
                vacio = false;
            }
            if (vacio) salida.println("(el archivo esta vacio)");
        } catch (IOException e) {
            throw new ComandoException("Error al leer el archivo: " + e.getMessage());
        }
    }

    private void cmdRen(String arg) throws ComandoException {
        List<String> t = dosArgumentos(arg, "Ren <actual> <nuevo>");
        String actual = t.get(0), nuevo = t.get(1);
        File f = existente(actual);
        File n = libre(nuevo);
        if (!f.renameTo(n)) {
            throw new ComandoException("No se pudo renombrar '" + actual + "'.");
        }
        salida.println("'" + actual + "' renombrado a '" + nuevo + "'.");
    }

    private void cmdCopy(String arg) throws ComandoException {
        List<String> t = dosArgumentos(arg, "Copy <origen> <destino>");
        String origen = t.get(0), destino = t.get(1);
        File o = archivoExistente(origen);
        File d = libre(destino);
        try (FileReader fr = new FileReader(o); FileWriter fw = new FileWriter(d)) {
            char[] buffer = new char[4096];
            int n;
            while ((n = fr.read(buffer)) != -1) {
                fw.write(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new ComandoException("Error al copiar: " + e.getMessage());
        }
        salida.println("Se copio '" + origen + "' a '" + destino + "'. 1 archivo(s) copiado(s).");
    }

    private void cmdFind(String arg) throws ComandoException {
        String texto = Utilidades.quitarComillas(arg);
        if (texto.isEmpty()) {
            throw new ComandoException("La sintaxis del comando no es correcta. Uso: Find <nombre>");
        }
        int[] cont = {0};
        buscar(current, texto.toLowerCase(), "", cont);
        if (cont[0] == 0) {
            throw new ComandoException("No se encontraron archivos ni carpetas que contengan '" + texto + "'.");
        }
        salida.println(cont[0] + " resultado(s) encontrado(s).");
    }

    private void buscar(File dir, String texto, String relativo, int[] cont) {
        for (File f : Utilidades.listarOrdenado(dir)) {
            String rel = relativo.isEmpty() ? f.getName() : relativo + "\\" + f.getName();
            if (f.getName().toLowerCase().contains(texto)) {
                salida.println((f.isDirectory() ? "[DIR]  " : "[FILE] ") + rel);
                cont[0]++;
            }
            if (f.isDirectory()) buscar(f, texto, rel, cont);
        }
    }

    private void cmdInfo(String arg) throws ComandoException {
        String nombre = nombreDe(arg, "Info <nombre>");
        File f = existente(nombre);
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