package Main;

import Annotation.WebRoute;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = getResponse(t);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String getResponse(HttpExchange t){
            String uri = t.getRequestURI().toString();
            String reqMethod = t.getRequestMethod();
            try {
                Method[] methods = Class.forName("Main.WebServer").getMethods();
                for(Method m: methods) {
                    if(m.isAnnotationPresent(WebRoute.class)) {
                        try {
                            Class<?> base = Class.forName("Main.WebServer");
                            String path = m.getAnnotation(WebRoute.class).path();
                            String value = m.getAnnotation(WebRoute.class).value();
                            String method = m.getAnnotation(WebRoute.class).method();

                            if ((path.equals(uri) || value.equals(uri)) && method.equals(reqMethod)){
                                return (String)m.invoke(base.newInstance(),t);
                            }

                            String routeBasePath = getParameteredRouteBase(path);
                            String routeBaseValue = getParameteredRouteBase(value);

                            if (routeBasePath != null || routeBaseValue != null){
                                String[] baseParameter = getParameteredUriBaseAndParameter(uri);
                                if (baseParameter != null){
                                    String uriBase = baseParameter[0];
                                    String uriParameter = baseParameter[1];
                                    if ((routeBasePath != null && routeBasePath.equals(uriBase)) || (routeBaseValue != null && routeBaseValue.equals(uriBase))){
                                        return (String)m.invoke(base.newInstance(),t, uriParameter);
                                    }
                                }
                            }

                        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (SecurityException | ClassNotFoundException e) {
                e.printStackTrace();
            }


            return "";
        }

        private String[] getParameteredUriBaseAndParameter(String uri){
            Integer baseEndIndex = -1;
            Integer i = uri.length() - 1;
            while (i >= 0) {
                if (uri.charAt(i) == '/'){
                    baseEndIndex = i;
                    break;
                }
                i--;
            }

            if (baseEndIndex != -1 && baseEndIndex < uri.length() - 1){
                return new String[]{
                        uri.substring(0,baseEndIndex + 1),
                        uri.substring(baseEndIndex + 1,uri.length())
                };
            }

            return null;
        }

        private String getParameteredRouteBase(String path){
            String base = null;
            Integer baseEndIndex = -1;
            Integer i = path.length() - 1;
            while (i >= 0) {
                if (path.charAt(i) == '>') {
                    Boolean found = false;
                    while (i >= 0) {
                        i--;
                        if (path.charAt(i) == '<') {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        baseEndIndex = i;
                        break;
                    }
                }
                i--;
            }

            if (baseEndIndex != -1){
                base = path.substring(0,baseEndIndex);
            }

            return base;
        }

    }



    @WebRoute("/")
    public String mainRoute(HttpExchange requestData){
        return "this is the main route";
    }

    @WebRoute("/test")
    public String testRoute(HttpExchange requestData){
        return "this is the test route";
    }

    @WebRoute(path = "/post", method = "POST")
    public String postRoute(HttpExchange requestData){
        return "this is the post route";
    }

    @WebRoute("/user/<userName>")
    public String usernameRoute(HttpExchange requestData, String userName){
        return "Hello " + userName;
    }

}
