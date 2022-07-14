import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerRunnable implements Runnable {

    private static final int LIMIT = 4096;
    private static final int REQUEST_PARTS = 3;
    private final Socket socket;
    private final Server server;
    private static final String FILTERING_PARAM = "key";

    public ServerRunnable(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            final var in = new BufferedInputStream(socket.getInputStream());
            final var out = new BufferedOutputStream(socket.getOutputStream());
            Request request = parsRequest(in, out);
            if (request == null) {
                server.notFoundResponse(out);
            } else {
                server.processingConnection(request, out, socket);
            }
            if (!request.getQueryParams().isEmpty()) {
                System.out.println("All query params:");
                for (NameValuePair ParamsValue : request.getQueryParams()) {
                    System.out.println("\t" + ParamsValue);
                }
                System.out.println();
                System.out.println("Values for param " + "\"" + FILTERING_PARAM + "\" :");
                for (NameValuePair paramValue : request.getQueryParam(FILTERING_PARAM)) {
                        System.out.println("\t" + paramValue.getValue());
                }
            }
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
        String path;
        String fullPath;
        try {
            in.mark(LIMIT);
            final var buffer = new byte[LIMIT];
            final var read = in.read(buffer);
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            // System.out.println("RequestType " + requestLine[0] + " Body " + requestLine[1] + " protocol " + requestLine[2]);
            if (requestLineEnd == -1 || !checkRequestLine(requestLine)) {
                server.notFoundResponse(out);
            } else {
                String[] protocol = requestLine[2].split("/");
                String uri = protocol[0].toLowerCase() + ":/" + requestLine[1];
                if (requestLine[1].contains("?")) {
                    fullPath = requestLine[1].substring(0, requestLine[1].indexOf("?"));
                } else {
                    fullPath = requestLine[1];
                }
                if (fullPath.contains(".")) {
                    path = fullPath;
                } else {
                    path = server.handlers.get(RequestType.valueOf(requestLine[0])).entrySet()
                            .stream().filter(x -> x.getKey().contains(fullPath))
                            .map(Map.Entry::getKey).collect(Collectors.joining(""));
                }
                List<NameValuePair> queryParams = URLEncodedUtils.parse(new URI(uri), Charset.forName("UTF-8"));
                request = new Request(RequestType.valueOf(requestLine[0]), path, protocol, queryParams);
            }
        } catch (IOException | RuntimeException | URISyntaxException e) {
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
        if (requestLine[1].equals("/")) {
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