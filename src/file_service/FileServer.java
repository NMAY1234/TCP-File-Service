package file_service;

import java.io.File;
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
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    System.out.println("file to delete: " + fileName);
                    File file = new File(SERVER_FILES + fileName);
                    boolean success = false;
                    if (file.exists()) {
                        success = file.delete();
                    }
                    if (success) {
                        serveChannel.write(SUCCESS);
                    } else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();
                }

                case 'L' -> { //List
                    File folder = new File(SERVER_FILES);
                    File[] files = folder.listFiles();
                    if (files != null) {
                        ByteBuffer statusCode = ByteBuffer.wrap(("S" +(Arrays.toString(files))).getBytes());
                        serveChannel.write(statusCode);
                    } else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();

                }
                case 'R' -> { // Rename
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String[] names = new String(a).split(":");
                    File currentFilePath = new File(SERVER_FILES + names[0]);
                    File newFilePath = new File(SERVER_FILES + names[1]);
                    boolean success = false;
                    if (!newFilePath.exists()) {
                        success = currentFilePath.renameTo(newFilePath);
                    }
                    if (success){
                        serveChannel.write(SUCCESS);
                    }
                    else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();

                }
                case 'G' -> { // Download
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileToCopy = SERVER_FILES + new String(a);
                    Path filePath = Path.of(fileToCopy);
                    if(new File(fileToCopy).exists()){
                        Files.copy(filePath,
                            Paths.get(CLIENT_FILES, filePath.getFileName().toString()));
                        serveChannel.write(SUCCESS);
                    }
                    else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();
                }

                case 'U' -> { // Upload
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileToUpload = new String(a);
                    Path filePath = Path.of(CLIENT_FILES + fileToUpload);
                    if (!new File(SERVER_FILES + fileToUpload).exists()) {
                        Files.copy(filePath,
                                Paths.get(SERVER_FILES, filePath.getFileName().toString()));
                        serveChannel.write(SUCCESS);
                    }
                    else {
                        serveChannel.write(FAIL);
                    }
                    serveChannel.close();
                }
                default -> serveChannel.write(FAIL);
            }
        }
    }
}
