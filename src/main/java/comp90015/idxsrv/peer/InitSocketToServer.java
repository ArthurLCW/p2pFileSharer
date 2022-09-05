package comp90015.idxsrv.peer;

import comp90015.idxsrv.textgui.ITerminalLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingDeque;

/*
* This class is used for initialize connection to server.
* */
public class InitSocketToServer {
    private Socket socket;
    private String ip;
    private int port;
    private int timeout; //TODO: timeout???

    public DataOutputStream output;
    public DataInputStream input;


    public InitSocketToServer(String ip, int port, int timeout){
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;

    }

    public Socket GenerateSocket() throws IOException {
        InetAddress addr = InetAddress.getByName(ip);
        socket = new Socket(addr, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        return socket;
    }

    //TODO: deal with request?
    //public void Send

}
