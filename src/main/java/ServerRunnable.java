import java.io.*;
import java.net.*;
import java.util.*;

public class ServerRunnable implements Runnable {

    private static final int LIMIT = 4096;
    private static final int REQUEST_PARTS = 3;
    private final Socket socket;
    private final Server server;


    public ServerRunnable(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedInputStream(socket.getInputStream());
            final var out = new BufferedOutputStream(socket.getOutputStream());
            server.processingConnection(parsRequest(in, out), out, socket);
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

    private Request parsRequest(BufferedInputStream in, BufferedOutputStream out) {
        Request request = null;
        try {
            in.mark(LIMIT);
            final var buffer = new byte[LIMIT];
            final var read = in.read(buffer);
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLineEnd == -1 || !checkRequestLine(requestLine)) {
                server.notFoundResponse(out);
            } else {
                request = new Request(RequestType.valueOf(requestLine[0]), requestLine[1], requestLine[2]);
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
        return request;
    }

    private boolean checkRequestLine(String[] requestLine) {
        if (requestLine.length != REQUEST_PARTS) {
            return false;
        }
        if (!server.handlers.containsKey(RequestType.valueOf(requestLine[0]))) {
            return false;
        }
        if (!requestLine[1].startsWith("/")) {
            return false;
        }
        return true;
    }

    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}