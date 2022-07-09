import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Server {

    private static int threadQuantity;
    protected static final Path FILES_FOLDER_PATH = Path.of(".", "public");
    private Server server;
    private int port;
    protected ConcurrentHashMap<RequestType, ConcurrentHashMap<String, Handlers>> handlers = new ConcurrentHashMap<>() {{
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

    protected void processingConnection(Request request, BufferedOutputStream out, Socket socket) {
       String filteredHandlerPath = handlers.get(request.getRequestType()).entrySet()
                .stream().filter(x -> x.getKey().contains(request.getPath()))
               .map(Map.Entry :: getKey).collect(Collectors.joining(""));




        System.out.println(filteredHandlerPath);
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

    protected void notFoundResponse(BufferedOutputStream out) {
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

    protected void successfulResponse(BufferedOutputStream out, String mimeType, long length) {
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

    protected void addHandler(RequestType requestType, String path, Handlers handler) {
        (handlers.get(requestType)).put(path, handler);
    }

    private void getFile(Request request, BufferedOutputStream out) {
        try {
            final var filePath = Path.of(String.valueOf(getFolderPath()), request.getPath());
            successfulResponse(out, Files.probeContentType(filePath), Files.size(filePath));
            Files.copy(filePath, out);
            out.flush();
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
            out.flush();
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


}