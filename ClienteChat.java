import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ClienteChat extends JFrame {

    static final String HOST = "localhost";
    static final int PUERTO = 12345;

    Socket socket;
    DataInputStream dis;
    DataOutputStream dos;

    JTextArea pantalla;
    JTextField campoMensaje;
    JTextField campoNombre;
    JButton btnConectar;
    JButton btnEnviar;
    JLabel lblEstado;
    boolean conectado = false;

    public ClienteChat() {
        // ── UI ──────────────────────────────────────────────────────────────
        setTitle("Chat Cliente");
        setSize(520, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel fondo = new JPanel(new BorderLayout(8, 8));
        fondo.setBackground(new Color(18, 18, 30));
        fondo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Cabecera ──
        JPanel cabecera = new JPanel(new BorderLayout(8, 0));
        cabecera.setOpaque(false);

        JLabel titulo = new JLabel("💬  CHAT");
        titulo.setFont(new Font("Monospaced", Font.BOLD, 18));
        titulo.setForeground(new Color(0, 255, 180));

        lblEstado = new JLabel("● Desconectado");
        lblEstado.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(200, 80, 80));

        cabecera.add(titulo, BorderLayout.WEST);
        cabecera.add(lblEstado, BorderLayout.EAST);

        // ── Panel nombre + botón conectar ──
        JPanel panelConexion = new JPanel(new BorderLayout(6, 0));
        panelConexion.setOpaque(false);

        campoNombre = crearTextField("Tu alias (nombre)...");
        btnConectar = crearBoton("CONECTAR", new Color(0, 180, 120));
        btnConectar.addActionListener(e -> toggleConexion());

        panelConexion.add(campoNombre, BorderLayout.CENTER);
        panelConexion.add(btnConectar, BorderLayout.EAST);

        // ── Panel norte (cabecera + conexión) ──
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        norte.add(cabecera, BorderLayout.NORTH);
        norte.add(panelConexion, BorderLayout.SOUTH);

        // ── Área de mensajes ──
        pantalla = new JTextArea();
        pantalla.setEditable(false);
        pantalla.setLineWrap(true);
        pantalla.setWrapStyleWord(true);
        pantalla.setBackground(new Color(10, 10, 22));
        pantalla.setForeground(new Color(200, 220, 255));
        pantalla.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(pantalla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 150), 1));

        // ── Panel envío ──
        JPanel panelEnvio = new JPanel(new BorderLayout(6, 0));
        panelEnvio.setOpaque(false);

        campoMensaje = crearTextField("Escribe un mensaje...");
        campoMensaje.setEnabled(false);
        // Enviar con Enter
        campoMensaje.addActionListener(e -> enviarMensaje());

        btnEnviar = crearBoton("ENVIAR ➤", new Color(0, 120, 200));
        btnEnviar.setEnabled(false);
        btnEnviar.addActionListener(e -> enviarMensaje());

        JButton btnSalir = crearBoton("SALIR", new Color(180, 60, 60));
        btnSalir.addActionListener(e -> salir());

        JPanel botonesEnvio = new JPanel(new GridLayout(1, 2, 6, 0));
        botonesEnvio.setOpaque(false);
        botonesEnvio.add(btnEnviar);
        botonesEnvio.add(btnSalir);

        panelEnvio.add(campoMensaje, BorderLayout.CENTER);
        panelEnvio.add(botonesEnvio, BorderLayout.EAST);

        fondo.add(norte, BorderLayout.NORTH);
        fondo.add(scroll, BorderLayout.CENTER);
        fondo.add(panelEnvio, BorderLayout.SOUTH);
        setContentPane(fondo);
        setVisible(true);
    }

    // ── Helpers UI ────────────────────────────────────────────────────────
    JTextField crearTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setBackground(new Color(28, 28, 45));
        tf.setForeground(new Color(160, 180, 220));
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 100)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        // Limpiar placeholder al hacer foco
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(Color.WHITE);
                }
            }
        });
        return tf;
    }

    JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Monospaced", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return btn;
    }

    // ── Lógica ───────────────────────────────────────────────────────────
    void toggleConexion() {
        if (!conectado) {
            conectar();
        } else {
            salir();
        }
    }

    void conectar() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty() || nombre.equals("Tu alias (nombre)...")) {
            mostrarMensaje("⚠ Ingresa tu alias antes de conectar.");
            return;
        }
        try {
            socket = new Socket(HOST, PUERTO);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            // Enviar nombre al servidor
            dos.writeUTF(nombre);
            dos.flush();

            conectado = true;
            setTitle("Chat — " + nombre);
            lblEstado.setText("● Conectado como " + nombre);
            lblEstado.setForeground(new Color(0, 220, 130));
            btnConectar.setText("DESCONECTAR");
            btnConectar.setBackground(new Color(180, 60, 60));
            campoNombre.setEnabled(false);
            campoMensaje.setEnabled(true);
            btnEnviar.setEnabled(true);
            campoMensaje.requestFocus();

            // Hilo receptor
            new Thread(this::recibirMensajes).start();

        } catch (IOException e) {
            mostrarMensaje("✖ No se pudo conectar: " + e.getMessage());
        }
    }

    void recibirMensajes() {
        try {
            while (conectado) {
                String msg = dis.readUTF();
                SwingUtilities.invokeLater(() -> {
                    pantalla.append(msg + "\n");
                    pantalla.setCaretPosition(pantalla.getDocument().getLength());
                });
            }
        } catch (IOException e) {
            if (conectado) mostrarMensaje("⚠ Conexión perdida.");
            desconectarUI();
        }
    }

    void enviarMensaje() {
        if (!conectado) return;
        String texto = campoMensaje.getText().trim();
        if (texto.isEmpty()) return;
        try {
            dos.writeUTF(texto);
            dos.flush();
            campoMensaje.setText("");
        } catch (IOException e) {
            mostrarMensaje("✖ Error al enviar: " + e.getMessage());
        }
    }

    void salir() {
        if (conectado) {
            try {
                dos.writeUTF("/salir");
                dos.flush();
                socket.close();
            } catch (IOException ignored) {}
        }
        desconectarUI();
    }

    void desconectarUI() {
        conectado = false;
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("● Desconectado");
            lblEstado.setForeground(new Color(200, 80, 80));
            btnConectar.setText("CONECTAR");
            btnConectar.setBackground(new Color(0, 180, 120));
            campoNombre.setEnabled(true);
            campoMensaje.setEnabled(false);
            btnEnviar.setEnabled(false);
        });
    }

    void mostrarMensaje(String msg) {
        SwingUtilities.invokeLater(() -> {
            pantalla.append(msg + "\n");
            pantalla.setCaretPosition(pantalla.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClienteChat::new);
    }
}
