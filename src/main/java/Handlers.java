import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handlers {
    void handle(Request request, BufferedOutputStream out);
}
