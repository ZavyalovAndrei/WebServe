import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final int PORT = 9999;
    private static final int THREAD_QUANTITY = 64;

    public static void main(String[] args) {

        Server server = new Server(THREAD_QUANTITY);
        server.listen(PORT, server);
        System.out.println("Server running");

        server.addHandler(RequestType.GET, "/messages.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(String.valueOf(Server.getFolderPath()), "/getmessages.html");
                server.successfulResponse(responseStream, Files.probeContentType(filePath), Files.size(filePath));
                Files.copy(filePath, responseStream);
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler(RequestType.POST, "/messages.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(String.valueOf(Server.getFolderPath()), "/postmessages.html");
                server.successfulResponse(responseStream, Files.probeContentType(filePath), Files.size(filePath));
                Files.copy(filePath, responseStream);
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}