# 💬 ChatXifrat – Xat amb Cifrat Asimètric (RSA)

Aplicació de xat en xarxa on tots els missatges s'intercanvien **xifrats amb RSA** (2048 bits). El servidor atén múltiples clients simultàniament i reenvia els missatges a tots els participants.

## ✨ Característiques

- Servidor **multifil** (multithread) que accepta connexions de diversos clients.
- **Cifrat asimètric** RSA per a cada missatge (clau pública/privada).
- Intercanvi de claus públiques entre client i servidor en l'inici de la connexió.
- **Broadcast**: cada missatge enviat per un client es reenvia a tots els altres clients connectats.
- El servidor **descifra** els missatges entrants (amb la seva clau privada) i els **torna a xifrar** individualment amb la clau pública de cada destinatari abans d'enviar-los.

## 🧠 Com funciona

1. El servidor genera el seu parell de claus RSA (pública + privada).
2. Cada client genera el seu propi parell de claus en iniciar-se.
3. Quan un client es connecta:
   - Rep la clau pública del servidor.
   - Envia la seva clau pública al servidor.
4. Per enviar un missatge, el client el xifra amb la clau **pública del servidor**.
5. El servidor el descifra amb la seva clau **privada** i després el reenvia a **tots els altres clients** xifrant-lo amb la clau pública de cadascun.
6. Cada client descifra els missatges rebuts amb la seva **pròpia clau privada**.

## 🛠️ Tecnologies utilitzades

- **Java 23** (JDK 23)
- **Java Cryptography Extension (JCE)** – RSA/ECB/PKCS1Padding
- **Sockets TCP** per a la comunicació en xarxa
- **ObjectInputStream / ObjectOutputStream** per a l'intercanvi d'objectes (claus i missatges xifrats)
