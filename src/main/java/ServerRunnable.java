import java.io.*;
import java.net.*;

public class ServerRunnable implements Runnable {
    private final Socket socket;
    private final Server server;


    public ServerRunnable(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());
            server.processingConnection(parsRequest(in), out, socket);
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
            int requestParts = 3;
            if (parts.length != requestParts) {
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