import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    private static int threadQuantity;
    protected static final Path FILES_FOLDER_PATH = Path.of(".", "public");
    private static final int REQUEST_LINE_LIMIT = 4096;
    private static final int REQUEST_PARTS = 3;
    private Server server;
    private int port;
    private ConcurrentHashMap<RequestType, ConcurrentHashMap<String, Handlers>> handlers = new ConcurrentHashMap<>() {{
        put(RequestType.GET, new ConcurrentHashMap<>());
        put(RequestType.POST, new ConcurrentHashMap<>());

    }};

    public Server(int threadQuantity) {
        this.threadQuantity = threadQuantity;
    }

    protected void runServer() {
        try {
            final ServerSocket servSocket = new ServerSocket(port);
            final ExecutorService threadPool = Executors.newFixedThreadPool(threadQuantity);
            while (true) {
                try {
                    final var socket = servSocket.accept();
                    final var threadServer = new ServerRunnable(socket, server);
                    threadPool.execute(threadServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processingConnection(BufferedInputStream in, BufferedOutputStream out, Socket socket) {
          try {
                in.mark(REQUEST_LINE_LIMIT);
                final byte[] buffer = new byte[REQUEST_LINE_LIMIT];
                final int read = in.read(buffer);
                final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
                final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    notFoundResponse(out);
                    out.close();
                    socket.close();
                }
                final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (!checkRequestLine(requestLine)) {
                    notFoundResponse(out);
                    out.close();
                    socket.close();
                }

                Request request = parsQuery(requestLine);
                if (((handlers.get(request.getRequestType())).get(request.getPath()) == null)) {
                    System.out.println(request);
                    notFoundResponse(out);
                    out.close();
                    socket.close();
//            } else if () {
//                in.mark(Main.REQUEST_LINE_LIMIT);
//                final byte[] buffer = new byte[Main.REQUEST_LINE_LIMIT];
//                final int read = in.read(buffer);

                } else {
                    System.out.println();
                    ((handlers.get(request.getRequestType())).get(request.getPath())).handle(request, out);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void notFoundResponse(BufferedOutputStream out) {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void successfulResponse(BufferedOutputStream out, String mimeType, long length) {
        try {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void addHandler(RequestType requestType, String path, Handlers handler) {
        (handlers.get(requestType)).put(path, handler);
    }

    private void getFile(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(String.valueOf(getFolderPath()), request.getPath());
            successfulResponse(out, Files.probeContentType(filePath), Files.size(filePath));
            Files.copy(filePath, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void getTime(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(String.valueOf(getFolderPath()), request.getPath());
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            successfulResponse(out, Files.probeContentType(filePath), content.length);
            out.write(content);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static Path getFolderPath() {
        return FILES_FOLDER_PATH;
    }

    protected void listen(int port, Server server) {
        this.server = server;
        this.port = port;
        handlers.get(RequestType.GET).put("/index.html", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/spring.svg", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/spring.png", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/resources.html", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/styles.css", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/app.js", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/links.html", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/forms.html", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/classic.html", (Handlers) server::getTime);
        handlers.get(RequestType.GET).put("/events.html", (Handlers) server::getFile);
        handlers.get(RequestType.GET).put("/events.js", (Handlers) server::getFile);
        Runnable task = () -> server.runServer();
        Thread thread = new Thread(task);
        thread.start();
    }
    private Request parsQuery(String[] requestLine) {
        Request request = null;
        String path;
        try {
            String[] protocol = requestLine[2].split("/");
            String uri = protocol[0].toLowerCase()+ ":/" + requestLine[1];
            if (requestLine[1].contains("?")) {
                path = requestLine[1].substring(0, requestLine[1].indexOf("?"));
            } else {
                path = requestLine[1];
            }
            System.out.println(path);
            List<NameValuePair> parsedRequest = URLEncodedUtils.parse(new URI(uri), Charset.forName("UTF-8"));
            for (NameValuePair value: parsedRequest) {
                System.out.println(value);
            }
                request = new Request(RequestType.valueOf(requestLine[0]), path, protocol);

//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
        } catch (URISyntaxException | RuntimeException e) {
            e.printStackTrace();
        }
        return request;
    }

    private boolean checkRequestLine (String[] requestLine) {
        if (requestLine.length != REQUEST_PARTS) {
            return false;
        }
        if (!handlers.contains(requestLine[0])) {
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