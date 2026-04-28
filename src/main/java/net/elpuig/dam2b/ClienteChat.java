package net.elpuig.dam2b;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.Cipher;

public class ClienteChat {
    private static final String HOST = "localhost";
    private static final int PUERTO = 5000;

    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private PublicKey clavePublicaServidor;
    private KeyPair parCliente;
    private PrivateKey clavePrivadaCliente;
    private PublicKey clavePublicaCliente;

    public void iniciar() {
        try {
            // Generar par de claves del cliente
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            parCliente = generador.generateKeyPair();
            clavePublicaCliente = parCliente.getPublic();
            clavePrivadaCliente = parCliente.getPrivate();

            // Conectar al servidor
            socket = new Socket(HOST, PUERTO);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            // Recibir la clave pública del servidor
            clavePublicaServidor = (PublicKey) ois.readObject();

            // Enviar nuestra clave pública al servidor
            oos.writeObject(clavePublicaCliente);
            oos.flush();

            // Hilo para recibir mensajes del servidor
            Thread hiloReceptor = new Thread(() -> {
                try {
                    while (true) {
                        byte[] mensajeCifrado = (byte[]) ois.readObject();
                        String mensaje = descifrar(mensajeCifrado, clavePrivadaCliente);
                        System.out.println("\n[Servidor] " + mensaje);
                        System.out.print("Tú > ");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Desconectado del servidor.");
                }
            });
            hiloReceptor.start();

            // Bucle para leer mensajes del usuario y enviarlos
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Conectado al chat. Escribe mensajes (salir para terminar):");
            String mensaje;
            while (true) {
                System.out.print("Tú > ");
                mensaje = teclado.readLine();
                if (mensaje.equalsIgnoreCase("salir")) {
                    break;
                }
                // Cifrar mensaje con la clave pública del servidor
                byte[] mensajeCifrado = cifrar(mensaje, clavePublicaServidor);
                oos.writeObject(mensajeCifrado);
                oos.flush();
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] cifrar(String texto, PublicKey clave) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, clave);
            return cipher.doFinal(texto.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private String descifrar(byte[] datos, Key clave) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, clave);
            byte[] original = cipher.doFinal(datos);
            return new String(original);
        } catch (Exception e) {
            e.printStackTrace();
            return "[Error al descifrar]";
        }
    }

    public static void main(String[] args) {
        new ClienteChat().iniciar();
    }
}