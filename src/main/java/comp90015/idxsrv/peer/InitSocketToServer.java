package comp90015.idxsrv.peer;

import comp90015.idxsrv.message.*;
import comp90015.idxsrv.textgui.ITerminalLogger;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;
import comp90015.idxsrv.textgui.ISharerGUI;

/*
* This class is used for initialize connection to server.
* */
public class InitSocketToServer {
    private Socket socket;
    private String ip;
    private int port;
    private int timeout; //TODO: timeout???

    private ISharerGUI tgui;

//    public DataOutputStream output;
//    public DataInputStream input;


    public InitSocketToServer(String ip, int port, int timeout, ISharerGUI tgui){
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.tgui = tgui;
    }

    public Socket GenerateSocket() throws IOException, JsonSerializationException {
        InetAddress addr = InetAddress.getByName(ip);
        socket = new Socket(addr, port);
        buildConnection();
//        input = new DataInputStream(socket.getInputStream());
//        output = new DataOutputStream(socket.getOutputStream());
        return socket;
    }

    //TODO: deal with request?
    public void buildConnection() throws IOException, JsonSerializationException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        if (ReadingWelcomeMsg(bufferedReader)) {
            sendingAuthenticateRequest(bufferedWriter);
        }
    }

    public boolean ReadingWelcomeMsg(BufferedReader bufferedReader) throws JsonSerializationException, IOException {
        Message msg = readMsg(bufferedReader);
        if(msg.getClass().getName()== WelcomeMsg.class.getName()) {
            tgui.logInfo("Received welcome message from server. ");
            return true;
        } else {
            tgui.logError("Expect to receive welcome msg from server, but failed to do so. ");
            return false;
        }
    }

    public void sendingAuthenticateRequest(BufferedWriter bufferedWriter) throws IOException {
        AuthenticateRequest authRequest = new AuthenticateRequest("server123");//TODO: secret setting
        writeMsg(bufferedWriter, authRequest);
    }

    private void writeMsg(BufferedWriter bufferedWriter, Message msg) throws IOException {
        tgui.logDebug("sending: "+msg.toString());
        bufferedWriter.write(msg.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private Message readMsg(BufferedReader bufferedReader) throws IOException, JsonSerializationException {
        String jsonStr = bufferedReader.readLine();
        if(jsonStr!=null) {
            Message msg = (Message) MessageFactory.deserialize(jsonStr);
            tgui.logDebug("received: "+msg.toString());
            return msg;
        } else {
            throw new IOException();
        }
    }
}
