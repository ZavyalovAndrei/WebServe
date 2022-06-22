
public class Main {
    private static final int PORT = 9999;
    private static final int THREAD_QUANTITY = 64;

    public static void main(String[] args) {
        Server server  = new Server(THREAD_QUANTITY);
        server.listen(PORT);
    }
}