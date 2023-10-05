package file_service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class FileClient {
    private final static int STATUS_CODE_LENGTH = 1;
    private static final String CLIENT_FILES = "ClientFiles/";

    private static final String SERVER_FILES = "ServerFiles/";

    public static void main(String[] args) throws Exception{
        if (args.length !=2){
            System.out.println("Syntax: FileClient <ServerIP> <ServerPort>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String command;
        do {
            Scanner keyboard = new Scanner(System.in);
            System.out.println("""
                    Please type a command.\s
                    Commands:
                    D: Delete
                    U: Upload
                    G: Download
                    L: List
                    R: Rename""");

            command = keyboard.nextLine().toUpperCase();
            switch (command) {
                case "D" -> { // Delete
                    System.out.println("Please enter the name of the file to be deleted:");
                    String fileName = keyboard.nextLine();

                    // Converts command and file name to byte buffer array using wrap()
                    ByteBuffer request = ByteBuffer.wrap(
                            (command + fileName).getBytes());
                    ByteBuffer code;
                    try (SocketChannel deleteChannel = SocketChannel.open()) {
                        deleteChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Sends Command to Server
                        deleteChannel.write(request);
                        deleteChannel.shutdownOutput();

                        // Receives Status Code from Server
                        code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                        deleteChannel.read(code);
                        code.flip();
                        byte[] a = new byte[code.remaining()];
                        code.get(a);
                        System.out.println(new String(a));
                }
                    catch (IOException e){
                        System.out.println("Failed to communicate. " + e.getMessage());
                    }
                }

                // TOD0: path names for files to change the source address to a new one
                case "U" -> { // Upload
                    System.out.println("Please enter the name of the file to be uploaded:");
                    String fileName = CLIENT_FILES + keyboard.nextLine();

                    File file = new File(fileName);
                    if(file.exists()) {
                        ByteBuffer code;
                        try (SocketChannel uploadChannel = SocketChannel.open()) {
                            uploadChannel.connect(new InetSocketAddress(args[0], serverPort));
                            ByteBuffer request = ByteBuffer.wrap(
                                            (command +  fileName).getBytes());

                            uploadChannel.write(request);
                            uploadChannel.shutdownOutput();

                            code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                            uploadChannel.read(code);
                            code.flip();
                            byte[] a = new byte[STATUS_CODE_LENGTH];
                            code.get(a);
                            System.out.println(new String(a));
                        }
                    }
                    else{
                        System.out.println("No file with that name.");
                    }
                }

                case "G" -> { // Download
                    System.out.println("Please enter the name of the file to be downloaded:");
                    String fileName = SERVER_FILES + keyboard.nextLine();

                    File file = new File(fileName);
                    if(file.exists()) {
                        ByteBuffer code;
                        try (SocketChannel downloadChannel = SocketChannel.open()) {
                            downloadChannel.connect(new InetSocketAddress(args[0], serverPort));
                            ByteBuffer request = ByteBuffer.wrap(
                                    (command +  fileName).getBytes());

                            downloadChannel.write(request);
                            downloadChannel.shutdownOutput();

                            code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                            downloadChannel.read(code);
                            code.flip();
                            byte[] a = new byte[STATUS_CODE_LENGTH];
                            code.get(a);
                            System.out.println(new String(a));
                        }
                    }
                    else {
                        System.out.println("No file with that name.");
                    }

                }

                case "L" -> { // List
                    // Converts command and file name to byte buffer array using wrap()
                    ByteBuffer request = ByteBuffer.wrap((command).getBytes());
                    ByteBuffer code;
                    try (SocketChannel listChannel = SocketChannel.open()) {
                        listChannel.connect(new InetSocketAddress(args[0], serverPort));

                        // Sends Command to Server
                        listChannel.write(request);
                        listChannel.shutdownOutput();

                        // Receives Status Code and List from Server
                        code = ByteBuffer.allocate(2500);
                        listChannel.read(code);
                        code.flip();

                        char statusCode = (char)code.get();
                        if("S".equals(String.valueOf(statusCode))) {
                            System.out.println(statusCode);
                            byte[] a = new byte[code.remaining()];
                            code.get(a);
                            System.out.println(new String(a));
                        }
                        else{
                         System.out.println(
                                 "Error. List not available. \n" + statusCode);
                        }
                    }
                    catch (IOException e){
                        System.out.println("Failed to communicate. " + e.getMessage());
                    }
                }

                case "R" -> { //Rename
                    System.out.println("Please enter the name of the file to be renamed: ");
                    String oldName = keyboard.nextLine();
                    System.out.println("Please enter the new name for the file: ");
                    String newName = keyboard.nextLine();

                    ByteBuffer request = ByteBuffer.wrap((command + oldName + ":" + newName).getBytes());
                    ByteBuffer code;
                    try (SocketChannel renameChannel = SocketChannel.open()) {
                        renameChannel.connect(new InetSocketAddress(args[0], serverPort));

                        renameChannel.write(request);
                        renameChannel.shutdownOutput();

                        code = ByteBuffer.allocate(STATUS_CODE_LENGTH);
                        renameChannel.read(code);
                        code.flip();
                        byte[] a = new byte[code.remaining()];
                        code.get(a);
                        System.out.println(new String(a));
                    } catch (IOException e) {
                        System.out.println("Failed to communicate.");
                    }
                }
                default -> {
                    if (!command.equals("Q")) {
                        System.out.println("Unknown command");
                    }
                }

            }
        }
        while (!command.equals("Q")) ;
    }
}
