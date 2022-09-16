package comp90015.idxsrv.peer;

import comp90015.idxsrv.message.*;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * this class implement the client socket for P2P file transmission
* */
public class P2PClientThread extends Thread{
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private String ip;
    private int port;

    private int timeout;

    private ISharerGUI tgui;
    private String filename;
    private String fileMd5;
    private int blockIdx;

    /**
     * param:
     * ip: target ip; port: target port;
     */

    public P2PClientThread(String ip, int port, int timeout, ISharerGUI tgui, String filename, String fileMd5)
            throws IOException {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.tgui = tgui;
        this.filename = filename;
        this.fileMd5 = fileMd5;
        blockIdx = 0;
    }

    public Socket generateSocket(){
        try {
            socket = new Socket(ip, port);
            tgui.logInfo("P2P client: Connect to "+ip.toString()+": "+port);
        } catch (IOException e) {
            tgui.logWarn("P2P client: "+e.toString());
        }
        return socket;
    }

    @Override
    public void run(){
        tgui.logInfo("P2P client: thread is trying to connect to P2P Server.");
        while(!isInterrupted()) {
            try {
                socket = generateSocket();
                socket.setSoTimeout(timeout);
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                RandomAccessFile file = new RandomAccessFile(filename, "rw");

                while (true){
                    BlockRequest blockRequest = new BlockRequest(filename, fileMd5, (Integer)blockIdx);
                    writeMsg(blockRequest);

                    Message msg = readMsg();

                    if (msg.getClass().getName() == BlockReply.class.getName()) {
                        BlockReply blockReply = (BlockReply) msg;
                        String bytes = blockReply.bytes;
                        byte[] receivingBuffer = Base64.getDecoder().decode(bytes);
                        file.write(Arrays.copyOf(receivingBuffer, receivingBuffer.length));
                        blockIdx++; // TODO: MAYBE need to make write o specific place (blockIdx)
                    }else if (msg.getClass().getName() == GoodBye.class.getName()){
                        tgui.logInfo("P2P client: "+"Received ByeBye. ");
                        break;
                    }else{
                        tgui.logError("P2P client: "+"Expect BlockReply or GoodBye, but do not see them.");
                        break;
                    }
                }

                file.close();
                tgui.logInfo("P2P client: "+"File transmission finished. ");
                bufferedReader.close();
                bufferedWriter.close();
                socket.close();
                break;
            } catch (IOException e) {
                tgui.logError("P2P client: "+e.toString());
            } catch (JsonSerializationException e) {
                tgui.logError("P2P client: "+e.toString());
            }
        }
    }


    // copyright from Prof. Aaron
    private void writeMsg(Message msg) throws IOException {
        tgui.logDebug("P2P client: "+"sending: "+msg.toString());
        bufferedWriter.write(msg.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private Message readMsg() throws IOException, JsonSerializationException {
        String jsonStr = bufferedReader.readLine();
        if(jsonStr!=null) {
            Message msg = (Message) MessageFactory.deserialize(jsonStr);
            tgui.logDebug("P2P client: "+"received: "+msg.toString());
            return msg;
        } else {
            tgui.logError("P2P client: "+"jsonStr==null");
            throw new IOException();
        }
    }
}
