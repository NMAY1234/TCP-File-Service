package file_service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileServer {
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
                        statusCode = ByteBuffer.wrap("S".getBytes());
                        serveChannel.write(statusCode);

                    } else {
                        statusCode = ByteBuffer.wrap("F".getBytes());
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
                    ByteBuffer statusCode;
                    if (success) {
                        statusCode = ByteBuffer.wrap("S".getBytes());
                    } else {
                        statusCode = ByteBuffer.wrap("F".getBytes());
                    }
                    serveChannel.write(statusCode);
                    serveChannel.close();
                }

                case 'G' -> { // Download
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String requestedFileName = new String(a).replace(SERVER_FILES, "");
                    System.out.println("Request for download received for file: " + requestedFileName);

                    File serverFile = new File(SERVER_FILES + requestedFileName);
                    File clientFile = new File(CLIENT_FILES + requestedFileName);

                    if (serverFile.exists() && !clientFile.exists()) {
                        Path filePath = serverFile.toPath();
                        try {
                            Files.copy(filePath, clientFile.toPath());
                            ByteBuffer statusCode = ByteBuffer.wrap("S".getBytes());
                            serveChannel.write(statusCode);
                        } catch (IOException ex) {
                            System.out.println("Error copying file: " + ex.getMessage());
                            ByteBuffer statusCode = ByteBuffer.wrap("S".getBytes());
                            serveChannel.write(statusCode);
                        }
                    } else {
                        System.out.println("Download failed. File may not exist or already exists on client.");
                        ByteBuffer statusCode = ByteBuffer.wrap("F".getBytes());
                        serveChannel.write(statusCode);                    }
                    serveChannel.close();
                }

                case 'U' -> { // Upload
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String requestedFileName = new String(a).replace(CLIENT_FILES, "");
                    System.out.println("Request for upload received for file: " + requestedFileName);

                    File clientFile = new File(CLIENT_FILES + requestedFileName);
                    File serverFile = new File(SERVER_FILES + requestedFileName);

                    if (clientFile.exists() && !serverFile.exists()) {
                        Path filePath = clientFile.toPath();
                        try {
                            Files.copy(filePath, serverFile.toPath());
                            ByteBuffer statusCode = ByteBuffer.wrap("S".getBytes());
                            serveChannel.write(statusCode);
                        } catch (IOException ex) {
                            System.out.println("Error copying file: " + ex.getMessage());
                            ByteBuffer statusCode = ByteBuffer.wrap("F".getBytes());
                            serveChannel.write(statusCode);
                        }
                    } else {
                        System.out.println("Upload failed. File may not exist or already exists on server.");
                        ByteBuffer statusCode = ByteBuffer.wrap("F".getBytes());
                        serveChannel.write(statusCode);
                    }
                    serveChannel.close();
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
