import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class Server {

    private static ConcurrentHashMap<RequestType, ConcurrentHashMap<String, Handlers>> handlers =
            new ConcurrentHashMap<>() {{
                put(RequestType.GET, new ConcurrentHashMap<>() {{
                    put("/index.html", (Handlers) Server::getFile);
                    put("/spring.svg", (Handlers) Server::getFile);
                    put("/spring.png", (Handlers) Server::getFile);
                    put("/resources.html", (Handlers) Server::getFile);
                    put("/styles.css", (Handlers) Server::getFile);
                    put("/app.js", (Handlers) Server::getFile);
                    put("/links.html", (Handlers) Server::getFile);
                    put("/forms.html", (Handlers) Server::getFile);
                    put("/classic.html", (Handlers) Server::getTime);
                    put("/events.html", (Handlers) Server::getFile);
                    put("/events.js", (Handlers) Server::getFile);
                }});
                put(RequestType.POST, new ConcurrentHashMap<>());
            }};

    protected void run() {
        try {
            final ServerSocket servSocket = new ServerSocket(Main.PORT);
            final ExecutorService threadPool = Executors.newFixedThreadPool(Main.THREAD_QUANTITY);
            while (true) {
                try {
                    final var socket = servSocket.accept();
                    final var server = new ServerRunnable(socket);
                    threadPool.execute(server);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void processingConnection(Request request, BufferedOutputStream out, Socket socket) {
        try {
            if (((handlers.get(request.getRequestType())).get(request.getPath()) == null)) {
                notFoundResponse(out);
                out.flush();
                socket.close();
            } else {
                ((handlers.get(request.getRequestType())).get(request.getPath())).handle(request, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void notFoundResponse(BufferedOutputStream out) {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void successfulResponse(BufferedOutputStream out, String mimeType, long length) {
        try {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void addHandler(RequestType requestType, String path, Handlers handler) {
        (handlers.get(requestType)).put(path, handler);
    }

    protected static void getFile(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(String.valueOf(Main.FILES_FOLDER_PATH), request.getPath());
            successfulResponse(out, Files.probeContentType(filePath), Files.size(filePath));
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void getTime(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(String.valueOf(Main.FILES_FOLDER_PATH), request.getPath());
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            successfulResponse(out, Files.probeContentType(filePath), content.length);
            out.write(content);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}