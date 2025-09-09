package net.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Low-level TCP connection with length-prefixed JSON framing.
 */
public class ClientConnection implements AutoCloseable {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Socket socket = new Socket();
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readerThread;
    private Thread writerThread;
    private final BlockingQueue<String> outbound = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    public interface MessageHandler {
        void onMessage(String json);
        void onClosed(IOException error);
    }

    public void connect(String host, int port, int timeoutMillis) throws IOException {
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        socket.setTcpNoDelay(true);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        running = true;
    }

    public void start(MessageHandler handler) {
        readerThread = new Thread(() -> readLoop(handler), "client-reader");
        writerThread = new Thread(this::writeLoop, "client-writer");
        readerThread.setDaemon(true);
        writerThread.setDaemon(true);
        readerThread.start();
        writerThread.start();
    }

    public void send(Object message) {
        try {
            String json = mapper.writeValueAsString(message);
            outbound.offer(json);
        } catch (Exception e) {
            // ignore; higher layers handle
        }
    }

    private void readLoop(MessageHandler handler) {
        try {
            while (running) {
                int len = in.readInt();
                byte[] buf = new byte[len];
                in.readFully(buf);
                String json = new String(buf, StandardCharsets.UTF_8);
                handler.onMessage(json);
            }
        } catch (IOException e) {
            handler.onClosed(e);
        } finally {
            running = false;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                String json = outbound.take();
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();
            }
        } catch (Exception ignored) {
        } finally {
            running = false;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}


