package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

public class ClientDriver {
    public static void main(String[] args) throws Exception {
        boolean isHeader=false,isContent=false;
        String url="",data="",query="";
        ArrayList<String> headers = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String httpfsClient = br.readLine();
        String[] commandClient = httpfsClient.split(" ");
        if(commandClient[0].equals("httpfs")) {
            for(int i =0; i<commandClient.length; i++) {
                if(commandClient[i].equals("-h")) {
                    isHeader = true;
                    headers.add(commandClient[++i]);
                }
                if(commandClient[i].startsWith("http://")){
                    url = commandClient[i];
                }
                if(commandClient[i].startsWith("-d")) {
                    isContent = true;
                    data = commandClient[++i];
                }
            }
        }
        URI uri = new URI(url);
        String host = uri.getHost();
        int port = uri.getPort();
        query = uri.getPath();
        System.out.println(query.substring(1));
        Client client = new Client(host,port,query.substring(1),data, headers,isHeader,isContent,url);
        client.sendRequest();
    }
}
