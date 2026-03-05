import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class ServidorChat extends JFrame {

    static final int PUERTO = 12345;
    static final int COLA = 10;

    ServerSocket servidor;
    JTextArea pantalla;
    JLabel lblConectados;
    List<ManejadorCliente> clientes = Collections.synchronizedList(new ArrayList<>());
    int contadorClientes = 0;

    public ServidorChat() {
        // ── UI ──────────────────────────────────────────────────────────────
        setTitle("Servidor Chat");
        setSize(520, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel fondo = new JPanel(new BorderLayout(8, 8));
        fondo.setBackground(new Color(18, 18, 30));
        fondo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Cabecera
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel("⬡  SERVIDOR CHAT");
        titulo.setFont(new Font("Monospaced", Font.BOLD, 18));
        titulo.setForeground(new Color(0, 255, 180));
        lblConectados = new JLabel("Clientes: 0");
        lblConectados.setFont(new Font("Monospaced", Font.PLAIN, 13));
        lblConectados.setForeground(new Color(160, 160, 200));
        cabecera.add(titulo, BorderLayout.WEST);
        cabecera.add(lblConectados, BorderLayout.EAST);

        // Bitácora
        pantalla = new JTextArea();
        pantalla.setEditable(false);
        pantalla.setBackground(new Color(10, 10, 20));
        pantalla.setForeground(new Color(180, 220, 255));
        pantalla.setFont(new Font("Monospaced", Font.PLAIN, 13));
        pantalla.setCaretColor(Color.WHITE);
        JScrollPane scroll = new JScrollPane(pantalla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 180), 1));

        // Botón apagar
        JButton btnApagar = new JButton("■  APAGAR SERVIDOR");
        btnApagar.setBackground(new Color(200, 50, 50));
        btnApagar.setForeground(Color.WHITE);
        btnApagar.setFont(new Font("Monospaced", Font.BOLD, 13));
        btnApagar.setFocusPainted(false);
        btnApagar.setBorderPainted(false);
        btnApagar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnApagar.addActionListener(e -> System.exit(0));

        fondo.add(cabecera, BorderLayout.NORTH);
        fondo.add(scroll, BorderLayout.CENTER);
        fondo.add(btnApagar, BorderLayout.SOUTH);
        setContentPane(fondo);
        setVisible(true);

        // ── Servidor ────────────────────────────────────────────────────────
        new Thread(() -> iniciarServidor()).start();
    }

    void iniciarServidor() {
        try {
            servidor = new ServerSocket(PUERTO, COLA);
            log("✔ Escuchando en el puerto " + PUERTO);
            while (true) {
                log("⏳ Esperando conexión...");
                Socket socket = servidor.accept();
                contadorClientes++;
                ManejadorCliente mc = new ManejadorCliente(socket, contadorClientes);
                clientes.add(mc);
                actualizarContador();
                new Thread(mc).start();
            }
        } catch (Exception e) {
            log("✖ Error: " + e.getMessage());
        }
    }

    /** Envía un mensaje a TODOS los clientes conectados */
    void broadcast(String mensaje, ManejadorCliente origen) {
        synchronized (clientes) {
            for (ManejadorCliente mc : clientes) {
                mc.enviar(mensaje);
            }
        }
        log("📢 " + mensaje);
    }

    void eliminarCliente(ManejadorCliente mc) {
        clientes.remove(mc);
        actualizarContador();
        broadcast("⚠ " + mc.nombre + " ha salido del chat.", null);
    }

    void actualizarContador() {
        SwingUtilities.invokeLater(() ->
            lblConectados.setText("Clientes: " + clientes.size()));
    }

    void log(String texto) {
        SwingUtilities.invokeLater(() -> {
            pantalla.append("\n" + texto);
            pantalla.setCaretPosition(pantalla.getDocument().getLength());
        });
    }

    // ── Hilo por cliente ──────────────────────────────────────────────────
    class ManejadorCliente implements Runnable {
        Socket socket;
        DataInputStream dis;
        DataOutputStream dos;
        String nombre;
        int id;

        ManejadorCliente(Socket s, int id) {
            this.socket = s;
            this.id = id;
            this.nombre = "Cliente-" + id;
        }

        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                // El primer mensaje del cliente es su nombre/alias
                nombre = dis.readUTF().trim();
                if (nombre.isEmpty()) nombre = "Cliente-" + id;

                log("✔ Conectado: " + nombre + " (" + socket.getInetAddress().getHostName() + ")");
                broadcast("✔ " + nombre + " se unió al chat.", this);

                // Bucle de recepción
                while (true) {
                    String msg = dis.readUTF();
                    if (msg.equalsIgnoreCase("/salir")) break;
                    broadcast("[" + nombre + "]: " + msg, this);
                }
            } catch (IOException e) {
                log("⚠ " + nombre + " desconectado inesperadamente.");
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                eliminarCliente(this);
            }
        }

        void enviar(String mensaje) {
            try {
                dos.writeUTF(mensaje);
                dos.flush();
            } catch (IOException e) {
                log("✖ No se pudo enviar a " + nombre);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServidorChat::new);
    }
}
