package Consola;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class VentanaConsola extends JFrame implements Salida {

    private static final Color BG = new Color(12, 12, 12);
    private static final Color FG = new Color(0, 255, 100);

    private final JTextArea output = new JTextArea();
    private final JTextField input = new JTextField();
    private final JLabel promptLabel = new JLabel();

    private final Interprete interprete;

    // Historial de comandos (flechas arriba/abajo)
    private final List<String> history = new ArrayList<>();
    private int historyPos = 0;

    public VentanaConsola() {
        super("Consola Simulada");
        construirInterfaz();

        interprete = new Interprete(this);
        interprete.mostrarBienvenida();
        actualizarPrompt();
    }

    private void construirInterfaz() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 520);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);

        Font fuente = new Font("Consolas", Font.PLAIN, 15);
        if (!fuente.getFamily().equals("Consolas")) {
            fuente = new Font(Font.MONOSPACED, Font.PLAIN, 15);
        }

        output.setEditable(false);
        output.setBackground(BG);
        output.setForeground(FG);
        output.setCaretColor(BG);
        output.setFont(fuente);
        output.setLineWrap(true);
        output.setWrapStyleWord(false);
        output.setMargin(new Insets(8, 8, 4, 8));
        ((DefaultCaret) output.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);

        promptLabel.setOpaque(true);
        promptLabel.setBackground(BG);
        promptLabel.setForeground(FG);
        promptLabel.setFont(fuente);
        promptLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        input.setBackground(BG);
        input.setForeground(FG);
        input.setCaretColor(FG);
        input.setFont(fuente);
        input.setBorder(BorderFactory.createEmptyBorder(4, 2, 6, 8));

        JPanel panelEntrada = new JPanel(new BorderLayout());
        panelEntrada.setBackground(BG);
        panelEntrada.add(promptLabel, BorderLayout.WEST);
        panelEntrada.add(input, BorderLayout.CENTER);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(panelEntrada, BorderLayout.SOUTH);

        input.addActionListener(e -> alPresionarEnter());

        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (history.isEmpty()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (historyPos > 0) {
                        historyPos--;
                    }
                    input.setText(history.get(historyPos));
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (historyPos < history.size() - 1) {
                        historyPos++;
                        input.setText(history.get(historyPos));
                    } else {
                        historyPos = history.size();
                        input.setText("");
                    }
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                input.requestFocusInWindow();
            }
        });

        output.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (output.getSelectedText() == null) {
                    input.requestFocusInWindow();
                }
            }
        });
    }

    private void alPresionarEnter() {
        String linea = input.getText();
        input.setText("");

        if (!interprete.estaEnModoEscritura() && !linea.trim().isEmpty()) {
            history.add(linea.trim());
            historyPos = history.size();
        }

        interprete.procesar(linea);

        actualizarPrompt();
        output.setCaretPosition(output.getDocument().getLength());
        input.requestFocusInWindow();
    }

    private void actualizarPrompt() {
        promptLabel.setText(interprete.getPrompt());
    }

    public void enfocarEntrada() {
        input.requestFocusInWindow();
    }

    @Override
    public void println(String texto) {
        output.append(texto + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    @Override
    public void limpiar() {
        output.setText("");
    }

    @Override
    public void cerrar() {
        dispose();
        System.exit(0);
    }
}
