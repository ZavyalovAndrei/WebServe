import org.apache.http.NameValuePair;

import java.util.List;
import java.util.stream.Collectors;

public class Request {

    private final String path;
    private String[] protocol;
    private final RequestType requestType;
    private final List<NameValuePair> queryParams;
    private final  List <String> body;

    @Override
    public String toString() {
        return "path = " + path + "\n" +
                "protocol = " + protocol[0] + " / " + protocol[1] + "\n" +
                "requestType = " + requestType;
    }

    public Request(RequestType requestType, String path, String[] protocol, List<NameValuePair> queryParams,
                   List<String> body) {
        this.requestType = requestType;
        this.path = path;
        this.protocol = protocol;
        this.queryParams = queryParams;
        this.body = body;
    }

    public String getPath() {
        return path;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public List<NameValuePair> getQueryParam(String name) {
        List<NameValuePair> result = queryParams.stream()
                .filter(param -> param.getName().equals(name))
                .collect(Collectors.toList());
        return result;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public List<String> getPart(String name) {
        List<String> result = body.stream()
                .filter(param -> (param.substring(0, param.indexOf("=")).equals(name)))
                .collect(Collectors.toList());
        return result;
    }

    public List <String> getParts() {
    return body;
    }
}