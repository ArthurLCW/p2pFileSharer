package comp90015.idxsrv.peer;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import comp90015.idxsrv.message.*;
import comp90015.idxsrv.textgui.ISharerGUI;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;

import java.security.NoSuchAlgorithmException;
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

    private int timeout; //TODO: timeout??? !!!

    private ISharerGUI tgui;
    private String filename;
    private String fileMd5;
    private int blockIdx;
    private int blockLength;

    private FileDescr fileDescr; //// delete later
    private FileMgr fileMgr; //// delete later

    /**
     * param:
     * ip: target ip; port: target port;
     */

    public P2PClientThread(String ip, int port, int timeout, ISharerGUI tgui, String filename, String fileMd5,
                           int blockIdx, int blockLength) throws IOException {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.tgui = tgui;
        this.filename = filename;
        this.fileMd5 = fileMd5;
        // this.blockIdx = blockIdx; // probably do not need this. no need for blockIdx as parameter
        this.blockIdx = 0;

    }

    public Socket generateSocket(){
        try {
            socket = new Socket(ip, port);
            tgui.logInfo("P2P client: Connect to "+ip.toString()+": "+port);
        } catch (IOException e) {
            tgui.logWarn(e.toString());
            // throw new RuntimeException(e);
        }
        return socket;
    }

    @Override
    public void run(){
        tgui.logInfo("P2P client: thread is trying to connect to P2P Server.");
        while(!isInterrupted()) {
            try {
                socket = generateSocket();
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                RandomAccessFile file = new RandomAccessFile(filename, "rw"); //todo: PATH
//                fileDescr = new FileDescr(file, blockLength);
//                fileMgr = new FileMgr(filename, fileDescr, tgui);

//                int tester = 4;
//                int testee = 0;

                while (true){
//                    if (testee>=tester) break;
//                    testee++;

                    BlockRequest blockRequest = new BlockRequest(filename, fileMd5, (Integer)blockIdx);
                    writeMsg(blockRequest);
                    // tgui.logInfo("P2P client: "+"Request block "+blockIdx);

                    while (!bufferedReader.ready()){ // detect if there is message sent
                    };
                    Message msg = readMsg();

                    if (msg.getClass().getName() == BlockReply.class.getName()) {
                        BlockReply blockReply = (BlockReply) msg;
                        String bytes = blockReply.bytes;
                        byte[] receivingBuffer = Base64.getDecoder().decode(bytes);
                        //tgui.logInfo("P2P client: "+"Block "+blockIdx+" received bytes: "+bytes);
//                        tgui.logInfo("Block "+blockIdx+" write bytes: "+bytes);
//                        tgui.logInfo("receiving buffer: "+Base64.getEncoder().encodeToString(receivingBuffer));

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



                // TODO: assume only 1 block now.
//                BlockRequest blockRequest = new BlockRequest(filename, fileMd5, blockIdx);//TODO: secret setting
//                writeMsg(blockRequest);
//                tgui.logInfo("Request block "+blockIdx);


//                Message msg = readMsg(); //TODO: multiple blocks
//                tgui.logInfo(msg.getClass().getName());
//                if (msg.getClass().getName() == BlockReply.class.getName()){
//                    BlockReply blockReply = (BlockReply) msg;
//                    String bytes = blockReply.bytes;
//                    tgui.logInfo("bytes: "+bytes);
//
//                    byte[] receivingBuffer = Base64.getDecoder().decode(bytes);
//                    tgui.logInfo("receiving buffer: "+Base64.getEncoder().encodeToString(receivingBuffer));
//
//
//
//                    file.write(Arrays.copyOf(receivingBuffer, receivingBuffer.length));
//
//
//                }else if (msg.getClass().getName() == GoodBye.class.getName()){
//
//                }else{
//                    tgui.logError("Expect BlockReply or GoodBye, but do not see them.");
//                }

//                file.close();
//                tgui.logInfo("file transmission finished. ");
//                bufferedReader.close();
//                bufferedWriter.close();
//                socket.close();
//                break;
            } catch (IOException e) {
                tgui.logWarn("P2P client: "+e.toString());
            } catch (JsonSerializationException e) {
                tgui.logWarn("P2P client: "+e.toString());
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
