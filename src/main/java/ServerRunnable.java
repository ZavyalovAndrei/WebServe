import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.*;

public class ServerRunnable implements Runnable {
    private static Socket socket;

    public ServerRunnable(Socket socket) {
        ServerRunnable.socket = socket;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedInputStream(socket.getInputStream());
            final var out = new BufferedOutputStream(socket.getOutputStream());
            Server.processingConnection(in, out, socket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}