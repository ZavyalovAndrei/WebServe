import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public class Main {
    protected static final int PORT = 9999;
    protected static final int THREAD_QUANTITY = 64;
    protected static final int REQUEST_PARTS = 3;
    protected static final Path FILES_FOLDER_PATH = Path.of(".", "public");

    public static void main(String[] args) {
        final var server = new Server();
        Runnable task = () -> server.run();
        Thread thread = new Thread(task);
        thread.start();
        System.out.println("Server running");
        Server.addHandler(RequestType.GET, "/messages.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(String.valueOf(Main.FILES_FOLDER_PATH), "/getmessages.html");
                Server.successfulResponse(responseStream, Files.probeContentType(filePath), Files.size(filePath));
                Files.copy(filePath, responseStream);
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Server.addHandler(RequestType.POST, "/messages.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(String.valueOf(Main.FILES_FOLDER_PATH), "/postmessages.html");
                Server.successfulResponse(responseStream, Files.probeContentType(filePath), Files.size(filePath));
                Files.copy(filePath, responseStream);
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}