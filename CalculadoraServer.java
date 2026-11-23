import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CalculadoraServer {

    public static void main(String[] args) throws IOException {
        int puerto = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

        server.createContext("/", CalculadoraServer::manejarHtml);

        server.createContext("/calcular", CalculadoraServer::manejarCalculo);

        server.setExecutor(null);
        System.out.println("Servidor iniciado en http://localhost:" + puerto);
        server.start();
    }

    private static void manejarHtml(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            enviarRespuesta(exchange, 405, "text/plain; charset=UTF-8", "Método no permitido");
            return;
        }
        Path rutaHtml = Paths.get("index.html");
        if (!Files.exists(rutaHtml)) {
            enviarRespuesta(exchange, 404, "text/plain; charset=UTF-8", "Archivo index.html no encontrado");
            return;
        }
        byte[] contenido = Files.readAllBytes(rutaHtml);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, contenido.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(contenido);
        }
    }

    private static void manejarCalculo(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = parsearQuery(exchange.getRequestURI().getRawQuery());
            int num1 = validarEnteroCincoDigitos(params.get("num1"));
            int num2 = validarEnteroCincoDigitos(params.get("num2"));
            String operacion = params.get("operacion");

            String resultado = calcular(num1, num2, operacion);
            String json = "{\"ok\":true,\"resultado\":\"" + resultado + "\"}";
            enviarRespuesta(exchange, 200, "application/json; charset=UTF-8", json);
        } catch (IllegalArgumentException e) {
            String jsonError = "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
            enviarRespuesta(exchange, 400, "application/json; charset=UTF-8", jsonError);
        }
    }

    private static int validarEnteroCincoDigitos(String valor) {
        if (valor == null || !valor.trim().matches("^-?\\d{1,5}$")) {
            throw new IllegalArgumentException("Solo se permiten números enteros de hasta 5 dígitos [-99999...99999].");
        }
        int numero = Integer.parseInt(valor.trim());
        if (numero < -99999 || numero > 99999) {
            throw new IllegalArgumentException("El número debe estar entre -99999 y 99999.");
        }
        return numero;
    }

    private static String calcular(int a, int b, String operacion) {
        if (operacion == null) {
            throw new IllegalArgumentException("Debe indicar una operación.");
        }
        BigInteger bigA = BigInteger.valueOf(a);
        BigInteger bigB = BigInteger.valueOf(b);

        switch (operacion) {
            case "suma":
                return bigA.add(bigB).toString();
            case "resta":
                return bigA.subtract(bigB).toString();
            case "multiplicacion":
                return bigA.multiply(bigB).toString();
            case "potencia":
                if (b >= 0) {
                    return bigA.pow(b).toString();
                } else {
                    if (a == 0) {
                        throw new IllegalArgumentException("No se puede elevar 0 a una potencia negativa.");
                    }
                    return String.valueOf(Math.pow(a, b));
                }
            default:
                throw new IllegalArgumentException("Operación no válida. Solo: suma, resta, multiplicación o potencia.");
        }
    }

    private static Map<String, String> parsearQuery(String query) {
        Map<String, String> resultado = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return resultado;
        }
        for (String par : query.split("&")) {
            String[] claveValor = par.split("=", 2);
            String clave = URLDecoder.decode(claveValor[0], StandardCharsets.UTF_8);
            String valor = claveValor.length > 1 ? URLDecoder.decode(claveValor[1], StandardCharsets.UTF_8) : "";
            resultado.put(clave, valor);
        }
        return resultado;
    }

    private static void enviarRespuesta(HttpExchange exchange, int codigo, String contentType, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}