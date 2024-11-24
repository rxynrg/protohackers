import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

class PrimeTime {
  private static final Set<Integer> primes = new HashSet<>() {{
    final int num = 10_000_000;
    final boolean[] bool = new boolean[num];
    Arrays.fill(bool, true);
    for (int i = 2; i < Math.sqrt(num); i++) {
      if (bool[i]) {
        for (int j = (i * i); j < num; j = j + i) {
          bool[j] = false;
        }
      }
    }
    for (int i = 2; i < bool.length; i++) {
      if (bool[i]) add(i);
    }
  }};

  public static void main(String[] args) throws IOException {
    try (var ss = new ServerSocket(8080)) {
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
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            System.out.println("read line: " + line);
            var prime = isPrime(line);
            outputStream.write(String.format(responseTemplate, prime).getBytes());
          }
        } catch (MalformedPayloadException mpe) {
          outputStream.write(responseTemplate.getBytes());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private boolean isPrime(String requestBody) throws MalformedPayloadException {
      final JSONObject json;
      try {
        json = new JSONObject(requestBody);
      } catch (JSONException e) {
        throw new MalformedPayloadException(e);
      }
      try {
        if (!json.getString("method").equals("isPrime")) {
          throw new MalformedPayloadException("incorrect method name");
        }
      } catch (JSONException e) {
        throw new MalformedPayloadException(e);
      }
      final double num;
      try {
        num = json.getDouble("number");
      } catch (JSONException e) {
        throw new MalformedPayloadException(e);
      }
      if (num % 1 != 0) return false;
      return primes.contains((int) num);
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
