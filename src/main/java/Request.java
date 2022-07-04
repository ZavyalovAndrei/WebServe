
public class Request {

    private final String path;
    private String[] protocol;
    private final RequestType requestType;

    public Request(RequestType requestType, String path, String[] protocol) {
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

    public String getQueryParam(String name) {
        return null;
    }
    public String getQueryParams() {
        return  null;
    }
    @Override
    public String toString() {
        return "Request: " +
                "\n\trequestType = " + requestType +
                "\n\tpath = " + path +
                "\n\tprotocol = " + protocol[0] + " / " + protocol[1];
    }
}
