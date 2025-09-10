package server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Represents a client session on the server side.
 */
public class ClientSession {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String sessionId;
    private String username;
    private String deviceId;
    private volatile boolean running = false;
    
    public interface MessageHandler {
        void onMessage(ClientSession session, String message);
        void onDisconnected(ClientSession session, Exception error);
    }
    
    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        InputStream sin = new BufferedInputStream(socket.getInputStream());
        OutputStream sout = new BufferedOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(sin);
        this.out = new DataOutputStream(sout);
        this.sessionId = java.util.UUID.randomUUID().toString();
    }
    
    public void start(MessageHandler handler) {
        running = true;
        new Thread(() -> {
            try {
                while (running) {
                    int len = in.readInt();
                    if (len <= 0 || len > (10 * 1024 * 1024)) {
                        throw new IOException("Invalid frame length: " + len);
                    }
                    byte[] buf = new byte[len];
                    in.readFully(buf);
                    String json = new String(buf, StandardCharsets.UTF_8);
                    handler.onMessage(this, json);
                }
            } catch (IOException e) {
                if (running) {
                    handler.onDisconnected(this, e);
                }
            } finally {
                close();
            }
        }, "ClientSession-" + sessionId).start();
    }
    
    public void send(Object message) {
        try {
            String json = mapper.writeValueAsString(message);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            synchronized (out) {
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();
            }
        } catch (Exception e) {
            // swallow to avoid cascading failures in demo server
        }
    }
    
    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
        try { in.close(); } catch (IOException ignored) {}
        try { out.close(); } catch (IOException ignored) {}
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public boolean isRunning() {
        return running;
    }
}

