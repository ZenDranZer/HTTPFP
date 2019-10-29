package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

//httpfs -p PORT -d DIR -v
public class ServerDriver {
    public static void main(String[] args) {
        try{
            int port = 8080,count = 0;
            String dir = "src/server/Files";
            boolean isV = false;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String[] input = br.readLine().split(" ");
            for (int i=0; i < input.length;i++) {
                if(input[i].equals("-p"))
                    port = Integer.parseInt(input[++i]);

                if(input[i].equals("-d"))
                    dir = input[++i];

                if(!isV)
                    isV = input[i].equals("-v");
            }

            ServerSocket socket = new ServerSocket(port);
            if(isV)
                System.out.println("Server ON! \nListening to Port # : " + port);

            while (true){
                count++;
                Socket clientSocket = socket.accept();
                if(isV)
                    System.out.println("Client # :" + count + " || Client Port: " + clientSocket.getPort() + " || Processing Request...");

                ParallelServer p = new ParallelServer(count,clientSocket,dir,port);
                Thread thread = new Thread(p);
                thread.start();
            }
        }catch (Exception e){e.printStackTrace();}
    }
}