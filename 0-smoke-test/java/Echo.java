import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;


// TCP Echo Service https://www.rfc-editor.org/rfc/rfc862.html
class Echo {
  private static final int concurrencyLevel = 50;
  private static final Semaphore semaphore = new Semaphore(concurrencyLevel);

  public static void main(String... args) throws IOException {
    Thread.setDefaultUncaughtExceptionHandler((_, e) -> e.printStackTrace());
    try (var ss = new ServerSocket(8080, 1, InetAddress.getLocalHost())) {
      System.out.println("ready & listening");
      while (true) {
        var clientSocket = ss.accept();
        var from = clientSocket.getInetAddress() + ":" + clientSocket.getPort();
        System.out.println("connection accepted from=" + from);
        if (semaphore.tryAcquire()) {
          Thread.ofVirtual().start(new EchoHandler(clientSocket));
        } else {
          System.err.println("close socket due to insufficient number of workers");
          clientSocket.close();
        }
      }
    }
  }

  static class EchoHandler implements Runnable {
    private final Socket socket;

    public EchoHandler(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        var inputStream = socket.getInputStream();
        var outputStream = socket.getOutputStream();
        socket.setSoTimeout(5000);
        System.out.println("handler: got I/O streams, configured sock, start echo");
        outputStream.write(inputStream.readAllBytes());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        semaphore.release();
        try {
          socket.close();
        } catch (IOException e) {
          System.err.println("handler: failed closing client socket: " + e);
        }
      }
    }
  }
}
