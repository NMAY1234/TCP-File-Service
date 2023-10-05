package file_service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileServer {
    private static final ByteBuffer SUCCESS = ByteBuffer.wrap("S".getBytes());
    private static final ByteBuffer FAIL = ByteBuffer.wrap("F".getBytes());
    private static final String SERVER_FILES = "ServerFiles/";
    private static final String CLIENT_FILES = "ClientFiles/";


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
                        statusCode = ByteBuffer.wrap("S".getBytes());;
                        serveChannel.write(statusCode);

                    } else {
                        statusCode = ByteBuffer.wrap("F".getBytes());;
                        serveChannel.write(statusCode);
                    }
                    // Send to Client
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
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String[] names = new String(a).split(":");
                    String oldName = names[0];
                    String newName = names[1];

                    File oldFile = new File(SERVER_FILES + oldName);
                    File newFile = new File(SERVER_FILES + newName);
                    boolean success = oldFile.renameTo(newFile);
                    if (success) {
                        serveChannel.write(SUCCESS);
                    } else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();
                }
                case 'G' -> { // Download
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileToCopy = new String(a);
                    Path filePath = Path.of(fileToCopy);
                    Files.copy(filePath,
                            Paths.get(CLIENT_FILES, filePath.getFileName().toString()));
                    ByteBuffer code = ByteBuffer.wrap("S".getBytes());
                    serveChannel.write(code);
                    // Close the channel
                    serveChannel.close();
                }
                case 'U' -> { // Upload
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileToCopy = new String(a);
                    Path filePath = Path.of(fileToCopy);
                    Files.copy(filePath,
                            Paths.get(SERVER_FILES, filePath.getFileName().toString()));
                    ByteBuffer code = ByteBuffer.wrap("S".getBytes());
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
//u same file exception
