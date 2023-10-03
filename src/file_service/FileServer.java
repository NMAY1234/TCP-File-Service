package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileServer {
    private static final String SERVER_FILES = "ServerFiles/";

    public static void main(String[] args) throws Exception{
        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind(new InetSocketAddress(port));

        while(true){
            SocketChannel serveChannel = welcomeChannel.accept();
            ByteBuffer request = ByteBuffer.allocate(2500);
            int numBytes = 0;
            do {
                numBytes = serveChannel.read(request);
            }

            while(numBytes >= 0);
            request.flip();
            char command = (char)request.get();
            System.out.println("received command: "+command);

            switch (command) {
                case 'D' -> { // Delete
                    // Get the file name from the remaining message
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    System.out.println("file to delete: " + fileName);

                    // Assess if there is a file to delete and set the status code
                    File file = new File(SERVER_FILES + fileName);
                    ByteBuffer statusCode;
                    boolean success = false;
                    if (file.exists()) {
                        success = file.delete();
                    }
                    if (success) {
                        statusCode = ByteBuffer.wrap("S".getBytes());
                    } else {
                        statusCode = ByteBuffer.wrap("F".getBytes());
                    }
                    // Send to Client
                    serveChannel.write(statusCode);
                    serveChannel.close();
                }

                case 'L' -> { //List
                    File folder = new File(SERVER_FILES);
                    File[] files = folder.listFiles();

                    if (files != null) {
                        ByteBuffer fileList = ByteBuffer.wrap(
                                ("S" +(Arrays.toString(files))).getBytes());
                        serveChannel.write(fileList);
                    } else {
                        ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                        serveChannel.write(code);
                    }
                    serveChannel.close();
                }
                case 'R' -> { // Rename
                }
                case 'G' -> { // Download
                }
                case 'U' -> { // Upload
                    byte[] a = new byte[request.remaining()];
                    String fileToCopy = String.valueOf(request.get(a));
                    Files.copy(Path.of(fileToCopy), Path.of(SERVER_FILES));
                    ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                    serveChannel.write(code);

                }
                default -> {
                    ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                    serveChannel.write(code);

                }
            }
        }
    }
}
