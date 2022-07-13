public class Request {

    private final String path;
    private String protocol;
    private final RequestType requestType;

    @Override
    public String toString() {
        return  "path = " + path + "\n" +
                "protocol = " + protocol + "\n" +
                "requestType = " + requestType;
    }

    public Request(RequestType requestType, String path, String protocol) {
        this.requestType = requestType;
        this.path = path;
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public RequestType getRequestType() {
        return requestType;
    }
}


