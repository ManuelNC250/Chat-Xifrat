package net.elpuig.dam2b;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;

public class ServidorChat {
    private static final int PUERTO = 5000;
    private static List<ClienteInfo> clientes = new ArrayList<>(); // lista sincronizada
    private static KeyPair parServidor;
    private static PublicKey clavePublicaServidor;
    private static PrivateKey clavePrivadaServidor;

    public static void main(String[] args) {
        // Generar par de claves RSA del servidor
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            parServidor = generador.generateKeyPair();
            clavePublicaServidor = parServidor.getPublic();
            clavePrivadaServidor = parServidor.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor de chat iniciado en puerto " + PUERTO);
            System.out.println("Clave pública del servidor: " + clavePublicaServidor);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socket.getInetAddress());
                HiloCliente hilo = new HiloCliente(socket);
                hilo.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase para almacenar información de cada cliente
    static class ClienteInfo {
        Socket socket;
        PublicKey clavePublica;
        PrintWriter out; // para enviar objetos (ObjectOutputStream)
        ObjectOutputStream oos;
        String nombre; // opcional

        ClienteInfo(Socket socket) {
            this.socket = socket;
        }
    }

    // Hilo que atiende a un cliente
    static class HiloCliente extends Thread {
        private Socket socket;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        private ClienteInfo info;

        public HiloCliente(Socket socket) {
            this.socket = socket;
            this.info = new ClienteInfo(socket);
        }

        @Override
        public void run() {
            try {
                // Enviar la clave pública del servidor al cliente
                oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(clavePublicaServidor);
                oos.flush();

                // Recibir la clave pública del cliente
                ois = new ObjectInputStream(socket.getInputStream());
                PublicKey clavePublicaCliente = (PublicKey) ois.readObject();
                info.clavePublica = clavePublicaCliente;
                info.oos = oos;

                // Añadir a la lista compartida
                synchronized (clientes) {
                    clientes.add(info);
                }
                System.out.println("Cliente añadido. Total conectados: " + clientes.size());

                // Notificar a todos los clientes (broadcast) que un nuevo usuario se ha unido
                String mensajeBienvenida = "** Un nuevo usuario se ha conectado **";
                broadcast(mensajeBienvenida, null); // null indica que se envía a todos

                // Bucle de recepción de mensajes cifrados
                while (true) {
                    byte[] mensajeCifrado = (byte[]) ois.readObject();
                    // Descifrar con la clave privada del servidor
                    String mensajeDescifrado = descifrar(mensajeCifrado, clavePrivadaServidor);
                    System.out.println("Mensaje recibido: " + mensajeDescifrado);

                    // Reenviar a todos los demás clientes (broadcast)
                    broadcast(mensajeDescifrado, info);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Cliente desconectado: " + socket.getInetAddress());
            } finally {
                // Eliminar de la lista
                synchronized (clientes) {
                    clientes.remove(info);
                }
                try {
                    socket.close();
                } catch (IOException e) { }
                System.out.println("Cliente eliminado. Quedan: " + clientes.size());
            }
        }

        // Método para descifrar con RSA
        private String descifrar(byte[] datosCifrados, Key clave) {
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, clave);
                byte[] datosDescifrados = cipher.doFinal(datosCifrados);
                return new String(datosDescifrados);
            } catch (Exception e) {
                e.printStackTrace();
                return "[Error al descifrar]";
            }
        }

        // Método para cifrar un mensaje con la clave pública de un destinatario
        private byte[] cifrar(String mensaje, PublicKey clavePublica) {
            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, clavePublica);
                return cipher.doFinal(mensaje.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                return new byte[0];
            }
        }

        // Envía un mensaje a todos los clientes (excepto al emisor si se especifica)
        private void broadcast(String mensaje, ClienteInfo emisor) {
            synchronized (clientes) {
                for (ClienteInfo cliente : clientes) {
                    if (emisor != null && cliente == emisor) continue; // no enviar al que envió
                    try {
                        byte[] mensajeCifrado = cifrar(mensaje, cliente.clavePublica);
                        cliente.oos.writeObject(mensajeCifrado);
                        cliente.oos.flush();
                    } catch (IOException e) {
                        System.err.println("Error al enviar a un cliente: " + e.getMessage());
                    }
                }
            }
        }
    }
}
