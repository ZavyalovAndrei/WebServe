import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ServerRunnable implements Runnable {

    private static final int LIMIT = 4096;
    private static final int REQUEST_PARTS = 3;
    private final Socket socket;
    private final Server server;
    private static final String QUERY_FILTER = "key";
    private static final String PARAM_FILTER = "value";

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
                System.out.println("Values for param " + "\"" + QUERY_FILTER + "\" :");
                for (NameValuePair paramValue : request.getQueryParam(QUERY_FILTER)) {
                    System.out.println("\t" + paramValue.getValue());
                }
                System.out.println();
            }
            if (request.getRequestType() != RequestType.GET) {
               System.out.println("Parts: ");
                for (String PartsValue : request.getParts()) {
                    System.out.println("\t" + PartsValue);
                }
                System.out.println();
                System.out.println("Filtered by "  + "\"" + PARAM_FILTER + "\" :");
                for (String PartsValue : request.getPart(PARAM_FILTER)) {
                    System.out.println("\t" + PartsValue);
                }
                System.out.println();
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
        List<String> body = null;
        String path;
        String fullPath;
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
                final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final var headersStart = requestLineEnd + requestLineDelimiter.length;
                final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    server.notFoundResponse(out);
                }
                in.reset();
                in.skip(headersStart);
                final var headersBytes = in.readNBytes(headersEnd - headersStart);
                final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                System.out.println(headers);
                if (!requestLine[0].equals("GET")) {
                    in.skip(headersDelimiter.length);
                    final var contentLength = extractHeader(headers, "Content-Length");
                    if (contentLength.isPresent()) {
                        final var length = Integer.parseInt(contentLength.get());
                        final var bodyBytes = in.readNBytes(length);

                        body = new ArrayList<>(Arrays.asList(new String(bodyBytes).split("&")));
                        }
                }
                request = new Request(RequestType.valueOf(requestLine[0]), path, protocol, queryParams, body);
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}