package info.kgeorgiy.ja.shinkareva.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Class implements {@link NewHelloServer} interface.
 * @author Shinkareva Alyona (alyona.i.shinkareva@gmail.com)
 */

public class HelloUDPServer implements NewHelloServer {
    private ExecutorService workers;
    private ExecutorService receivers;

    /**
     * Main function for starting {@link HelloUDPServer}.
     * @param args input arguments: <port> <number of threads>.
     */
    public static void main(String[] args) {
        if (!(Stream.of(args).anyMatch(Objects::isNull) || args.length != 2 )) {
            try (HelloUDPServer server =  new HelloUDPServer()) {
                int port = Integer.parseInt(args[0]);
                int threads = Integer.parseInt(args[1]);
                server.start(threads, port);
            } catch (NumberFormatException e) {
                System.out.println("Invalid arguments: <port>, <number of threads> and <number of requests> must be numbers.");
            }
        }
    }

    /**
     * Starts new {@link HelloUDPServer}
     * @param threads number of working threads.
     * @param ps port no to response format mapping.
     */

    @Override
    public void start(int threads, Map<Integer, String> ps) {
        if (ps.size() == 0) {
            return;
        }
        Map<Integer, String> ports = new ConcurrentHashMap<>(ps);
        receivers = Executors.newFixedThreadPool(ports.size());
        workers = Executors.newFixedThreadPool(threads);
        for (var entry : ports.entrySet()) {
            receivers.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket(entry.getKey())) {
                    socket.setSoTimeout(300);
                while (!receivers.isShutdown()  && !Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket received = new DatagramPacket(new byte[socket.getReceiveBufferSize()], socket.getReceiveBufferSize());
                        socket.receive(received);
                        workers.submit(() -> {
                            String receivedText = new String(received.getData(), received.getOffset(), received.getLength(), StandardCharsets.UTF_8);
                            String responseString = entry.getValue().replace("$", receivedText);
                            DatagramPacket response = new DatagramPacket(responseString.getBytes(StandardCharsets.UTF_8), responseString.length(), received.getSocketAddress());
                            try {
                                socket.send(response);
                            } catch (IOException e) {
                                System.out.println("IO exception: " + e.getMessage());
                            }
                        });
                    } catch (SocketException e) {
                        System.out.println("Socket error: " + e.getMessage());
                        break;
                    } catch (IOException e) {
                        System.out.println("Receive exception: " + e.getMessage());
                        break;
                    }
                }
            } catch (SocketException e) {
                    System.out.println("Socket error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Starts new {@link HelloUDPServer}
     * @param threads number of working threads.
     * @param port server port.
     */

    @Override
    public void start(final int port, final int threads) {
        start(threads, Map.of(port, "Hello, $"));
    }

    /**
     * Stops server and deallocates all resources.
     */

    @Override
    public void close() {
        if (Objects.nonNull(receivers)) {
            receivers.close();
        }
        if (Objects.nonNull(workers)) {
            workers.close();
        }
    }
}