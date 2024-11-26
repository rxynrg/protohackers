import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

class PrimeTime {
  private static final int sieveSize = 100_000_000;
  private static final BitSet sieve = new BitSet(sieveSize) {{
    set(2, sieveSize - 1);
    for (int p = 2; p < Math.sqrt(sieveSize); p++) {
      if (get(p)) {
        for (int i = p * p; i < sieveSize; i += p) {
          clear(i);
        }
      }
    }
  }};

  public static void main(String... args) throws IOException {
    try (var ss = new ServerSocket(8080)) {
      System.out.println("ready & listening for incoming connections");
      while (true) {
        try {
          var clientSocket = ss.accept();
          Thread.ofVirtual().start(new Handler(clientSocket));
        } catch (IOException e) {
          System.err.println(e);
        }
      }
    }
  }

  static class Handler implements Runnable {
    private static final Gson gson = new GsonBuilder()
      .setStrictness(Strictness.STRICT)
      .registerTypeAdapter(Payload.class, new PayloadAdapter())
      .create();
    private static final String responseTemplate =
      "{\"method\":\"isPrime\",\"prime\":%b}\n";

    private final Socket socket;

    Handler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try (var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           var outputStream = socket.getOutputStream()) {
        String line;
        byte[] response;
        while ((line = reader.readLine()) != null) {
          try {
            response = this.getResponse(line);
          } catch (MalformedPayloadException e) {
            System.out.println("malformed payload: " + line);
            response = responseTemplate.getBytes();
          } catch (Exception e) {
            throw new RuntimeException("unexpected error occurred", e);
          }
          outputStream.write(response);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private byte[] getResponse(String line) throws MalformedPayloadException {
      var prime = isPrime(line);
      var response = String.format(responseTemplate, prime);
      System.out.println("write " + response + " to line: " + line);
      return response.getBytes();
    }

    private boolean isPrime(String requestBody) throws MalformedPayloadException {
      final Payload p;
      try {
        p = gson.fromJson(requestBody, Payload.class);
        System.out.println("parsed payload: " + p);
      } catch (JsonParseException | ClassCastException e) {
        throw new MalformedPayloadException(e);
      }
      if (!p.method.equals("isPrime")) {
        throw new MalformedPayloadException("incorrect method name");
      }
      if (p.number < 0 || p.number % 1 != 0) return false;
      return sieve.get((int) p.number);
    }

    record Payload(String method, double number) {
    }

    static class PayloadAdapter implements JsonDeserializer<Payload> {
      @Override
      public Payload deserialize(JsonElement jsonElement,
                                 Type type,
                                 JsonDeserializationContext ctx
      ) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        if (!obj.has("method")) {
          throw new JsonParseException("method field required");
        }
        if (!obj.has("number")) {
          throw new JsonParseException("number field required");
        }
        return new Payload(
          obj.get("method").getAsJsonPrimitive().getAsString(),
          obj.get("number").getAsJsonPrimitive().getAsDouble());
      }
    }

    static class MalformedPayloadException extends Exception {
      public MalformedPayloadException(Throwable cause) {
        super(cause);
      }

      public MalformedPayloadException(String message) {
        super(message);
      }
    }
  }
}
