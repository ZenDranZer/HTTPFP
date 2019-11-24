package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
    private boolean isH;    //header
    private boolean isC;    //content
    private String data;
    private Socket socket;
    private BufferedWriter out;
    private String url;
    private String query;
    private ArrayList<String> headers;

    public Client(String host, int port, String query,String content, ArrayList<String> headers,boolean isH,boolean isC,String url) {
        try
        {
            this.url = url;
            this.data = content;
            this.isC = isC;
            this.isH = isH;
            this.query = query;
            this.headers = headers;
            socket = new Socket(host, port);
        } catch (Exception e) {
            System.out.println("\nERROR HTTP 404: Host Not Found");
        }
    }

    public void sendRequest(){
        try {
            out= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(query+"\n");
            if(isH) {
                for (String header : headers) {
                    out.write(header+"\n");
                }
            }
            if(isC) {
                out.write("-d"+data);
            }
            out.write("\r\n");
            out.write("\r\n");
            out.flush();
            this.printOutput();
        }catch (Exception e){e.printStackTrace();}
    }

    public void printOutput() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String output;
            while((output = br.readLine()) != null) {
                System.out.println(output);
            }
            socket.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
