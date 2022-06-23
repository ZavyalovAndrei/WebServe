import java.io.*;
import java.net.*;

public class ServerRunnable implements Runnable {
    private static Socket socket;
    private static final int REQUEST_PARTS = 3;

    public ServerRunnable(Socket socket) {
        ServerRunnable.socket = socket;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());
            Server.connectionProcessing(parsRequest(in).getPath(), out);
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

    private Request parsRequest(BufferedReader in) {
        Request request = null;
        try {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            if (parts.length != REQUEST_PARTS) {
                socket.close();
            } else {
                request = new Request(RequestType.valueOf(parts[0]), parts[1], parts[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }
}