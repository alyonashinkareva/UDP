package info.kgeorgiy.ja.shinkareva.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Class implements {@link HelloClient} interface.
 * @author Shinkareva Alyona (alyona.i.shinkareva@gmail.com)
 */


public class HelloUDPClient implements HelloClient {

    /**
     * Main function for starting {@link HelloUDPClient}.
     * @param args input arguments: <host> <port> <prefix> <number of threads> <number of requests>.
     */
    public static void main(String[] args) {
        if (!(Stream.of(args).anyMatch(Objects::isNull) || args.length != 5 )) {
            try {
                String host = args[0];
                String prefix = args[2];
                int port = Integer.parseInt(args[1]);
                int threads = Integer.parseInt(args[3]);
                int requests = Integer.parseInt(args[4]);
                new HelloUDPClient().run(host, port, prefix, threads, requests);
            } catch (NumberFormatException e) {
                System.out.println("Invalid arguments: <port>, <number of threads> and <number of requests> must be numbers.");
            }
        }
    }

    /**
     * Runs {@link HelloUDPClient}.
     * This method should return when all requests are completed.
     *
     * @param host server host
     * @param port server port
     * @param prefix request prefix
     * @param threads number of request threads
     * @param requests number of requests per thread.
     */

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        var socketAddress = new InetSocketAddress(host, port);
        var workers = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int threadNum = i;
            workers.submit(() -> {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    byte[] receivingDataBuffer = new byte[datagramSocket.getReceiveBufferSize()];
                    DatagramPacket response = new DatagramPacket(receivingDataBuffer, datagramSocket.getReceiveBufferSize());
                    datagramSocket.setSoTimeout(300);
                    for (int requestNum = 0; requestNum < requests; requestNum++) {
                        String request = prefix + (threadNum + 1) + "_" + (requestNum + 1);
                        DatagramPacket packet = new DatagramPacket(request.getBytes(StandardCharsets.UTF_8), request.length(), socketAddress);
                        while (true) {
                            try {
                                datagramSocket.send(packet);
                                datagramSocket.receive(response);
                            } catch (IOException e) {
                                System.out.println("IO exception: " + e.getMessage());
                            }
                            String receivedData = new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                            StringBuilder sb = new StringBuilder(receivedData);
                            for (int j = 0; j < sb.length(); j++) {
                                if (Character.isDigit(sb.charAt(j))) {
                                    sb.setCharAt(j, (char) (Character.getNumericValue(sb.charAt(j)) + 48));
                                }
                            }
                            if (sb.toString().contains(request)) {
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("error with socket: " + e.getMessage());
                }
            });
        }
        workers.close();
    }
}