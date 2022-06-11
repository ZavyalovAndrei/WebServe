import java.io.*;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 9999;
    private static final int THREAD_QUANTITY = 64;

    public static void main(String[] args) {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
                "/events.html", "/events.js");
        try {
            final ServerSocket servSocket = new ServerSocket(PORT);
            final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_QUANTITY);
            while (true) {
                try {
                    final var socket = servSocket.accept();
                    final var server = new Server(socket, validPaths);
                    threadPool.execute(server);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
