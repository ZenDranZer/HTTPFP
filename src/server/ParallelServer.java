package server;

import model.HttpModel;
import java.io.*;
import java.net.Socket;

public class ParallelServer implements Runnable {

    private Socket clientSocket;

    //Boolean checks
    private boolean isCT = false;       //content Type
    private boolean isD = false;        //Disposition
    private boolean isHC = false;       //Http Client
    private boolean isHFC = false;     // Http File Client
    private int port;

    //Data related
    private int count;
    private String pathToDir;
    private String clientRequest; // client input
    private String httpcRequest;
    private String content;

    private BufferedWriter out = null; // output stream send response to client
    private HttpModel model;

    ParallelServer(int counter, Socket clientSocket, String dir,int port) {
        this.clientSocket = clientSocket;
        this.count = counter;
        this.pathToDir = dir;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            model = new HttpModel();
            // input stream to get request from Client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String request;
            while((request = in.readLine()) != null){
                if(request.endsWith("HTTP/1.1")) {
                    httpcRequest = request;
                    isHC = true;
                }
                else if(request.matches("(GET|POST)/(.*)")) {
                    isHFC = true;
                    clientRequest = request;
                }

                if(isHFC) {
                    model.addfileHeaders(request);
                    if(request.startsWith("Content-type:"))
                        isCT = true;
                    if(request.startsWith("Content-Disposition:")) {
                        isD = true;
                    }
                    if(request.startsWith("-d")) {
                        content = request.substring(2);
                    }
                }

                if(isHFC && request.isEmpty())
                    break;

                if(isHC) {
                    System.out.println(request);
                    if(request.matches("(.*):(.*)")&&count==0){
                        String[] headers = request.split(":");
                        model.addHeaders(headers[0], headers[1]);
                    }

                    if(count==1) {
                        model.setData(request);
                        break;
                    }
                    if(request.isEmpty())
                        count++;
                }
            }

            if(isHC) {
                if(httpcRequest.matches("(GET|POST) /(.*)")) {
                    this.httpcRequest();
                }
            }

            if(isHFC) {
                System.out.println("Client requested command..."+clientRequest);

                if(clientRequest.startsWith("GET")) {
                    this.getServerRequest(clientRequest.substring(4));
                }else if(clientRequest.startsWith("POST")) {
                    System.out.println(clientRequest.substring(5));
                    String fileName = clientRequest.substring(5);
                    postServerRequest(fileName, content);
                }
            }
            


        }catch (Exception e){e.printStackTrace();}
    }



    synchronized void postServerRequest(String fileName, String content) throws IOException{
        File filePath;
        BufferedWriter postWriter;
        if(isCT)
            filePath = new File(pathToDir+"/"+fileName+model.getExtension());
        else
            filePath = new File(pathToDir+"/"+fileName);

        if(!fileName.contains("/")) {
            try {
                out.write("CODE 202 OK \r\n");
                postWriter = new BufferedWriter(new FileWriter(filePath));
                postWriter.write(content);
                postWriter.flush();
                out.write("POST : Done...");
                postWriter.close();
                model.setFiles(fileName);
                out.write("\r\n");
                out.write("\r\n");
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                out.write("ERROR 404 FILE NOT FOUND");
                out.close();
            }
        }else {
            System.out.println("Access Denied");
            out.write("ERROR 502 Access Denied");
            out.close();
        }
    }

    synchronized void getServerRequest(String fileNam) throws IOException{

        File filePath;
        String fileName = fileNam;
        if(isCT) {
            fileName = fileName+model.getExtension();
            filePath = new File(pathToDir+"/"+fileName);
        }else {
            filePath = new File(pathToDir+"/"+fileName);
        }

        if(!fileName.contains("/")) {

            if(filePath.exists()) {
                if(filePath.isDirectory()) {
                    File[] listOfFiles = filePath.listFiles();
                    out.write("CODE 202 OK \r\n");
                    for(File file : listOfFiles) {
                        if(file.isFile()) {
                            System.out.println("File  : "+file.getName());
                            out.write("File  : "+file.getName()+"\r\n");
                        }else if(file.isDirectory()) {
                            System.out.println("Directory >> "+file.getName());
                            out.write("Directory >> "+file.getName()+"\r\n");
                        }
                    }
                }else if(filePath.isFile()) {
                    System.out.println("Path: "+pathToDir+"/"+fileName);
                    FileReader fileReader;
                    PrintWriter fileWriter = null;
                    File downloadPath = new File("src/client/Download");
                    String fileDownloadName = "";
                    if(isD) {
                        fileDownloadName = model.getFileName();
                        System.out.println(fileDownloadName);
                        if(model.dispAttachment) {
                            if(!downloadPath.exists())
                                downloadPath.mkdir();
                        }
                    }

                    try {

                        if(model.dispAttachment) {
                            if(model.dispWithFile)
                                fileWriter = new PrintWriter(downloadPath+"/"+fileDownloadName);
                            else
                                fileWriter = new PrintWriter(downloadPath+"/"+fileName);
                        }
                        fileReader = new FileReader(filePath);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String currentLine;
                        String fileData = null;
                        out.write("CODE 202 OK \r\n");
                        while ((currentLine = bufferedReader.readLine()) != null) {
                            fileData = fileData + currentLine;
                            if(isD) {
                                if(model.dispInline) {
                                    out.write(currentLine);
                                }else if(model.dispAttachment) {
                                    fileWriter.println(currentLine);
                                }
                            }else
                                out.write(currentLine+"\r\n");
                        }
                        if(model.dispAttachment)
                            fileWriter.close();
                        out.write("Operation Successful"+"\r\n");
                        System.out.println("Operation Successful");
                    } catch (FileNotFoundException e) {
                        System.out.println("ERROR HTTP 404");
                        out.write("ERROR HTTP 404 : File Not Found"+"\r\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            } else {
                System.out.println("ERROR HTTP 404");
                out.write("ERROR HTTP 404");
            }

        }else {
            System.out.println("Access Denied");
            out.write("Error: access denied");
        }
        out.write("\r\n");
        out.write("\r\n");
        out.flush();
        out.close();
    }

    public synchronized void httpcRequest(){
        try {
            httpcRequest = httpcRequest.replace("GET /", "").replace("POST /", "").replace("HTTP/1.1", "");
            model.setStatus("200");
            model.setUrl("http://localhost:"+port+"/"+httpcRequest);
            out.write(model.getHeader());
            if(httpcRequest.startsWith("get?")) {
                System.out.println("httpc GET request...");
                //args
                httpcRequest = httpcRequest.replace("get?", "");
                extractDetails();
                System.out.println(model.getGETBodyPart());
                out.write(model.getGETBodyPart());

            }else if(httpcRequest.startsWith("post?")) {
                System.out.println("httpc POST request...");
                httpcRequest = httpcRequest.replace("post?", "");
                if(!httpcRequest.isEmpty() && httpcRequest.matches("(.*)=(.*)")) {
                    extractDetails();
                }
                out.write(model.getPOSTBodyPart());
            }
        }catch (Exception e){e.printStackTrace();}
    }

    void extractDetails() {
        if(httpcRequest.matches("(.*)&(.*)")) {
            String[] temp = httpcRequest.split("&");
            for(int i = 0;i<temp.length;i++) {
                String[] args = temp[i].split("=");
                model.setArgs(args[0], args[1]);
            }
        }else {
            String[] args = httpcRequest.split("=");
            model.setArgs(args[0], args[1]);
        }
    }

}
