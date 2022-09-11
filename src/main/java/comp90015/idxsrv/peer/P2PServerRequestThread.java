package comp90015.idxsrv.peer;
import java.io.*;
import java.net.Socket;

import comp90015.idxsrv.filemgr.BlockUnavailableException;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingDeque;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import java.util.Base64;

public class P2PServerRequestThread extends Thread{
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private ISharerGUI tgui;

    private LinkedBlockingDeque<Socket> incomingConnections;

    private int port;
    private int timeout;

    private int blockLength;

    public P2PServerRequestThread(LinkedBlockingDeque<Socket> incomingConnections, ISharerGUI tgui,
                                  int port, int timeout, int blockLength){
        this.tgui = tgui;
        this.incomingConnections = incomingConnections;
        this.port = port;
        this.timeout = timeout;
        this.blockLength = blockLength;
    }

    @Override
    public void run(){
        while(!isInterrupted()) {
            tgui.logInfo("P2P server: ready for accepting socket.");
            try {
                while (incomingConnections.size()==0){
                }
                socket = incomingConnections.take();
                tgui.logInfo("P2P server: receive request from "+socket.getInetAddress().toString()+": "+socket.getPort());

                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                RandomAccessFile file = null;

                while (true){
                    while (!bufferedReader.ready()){ // detect if there is message sent
                    };
                    Message msg = readMsg();
                    if(msg.getClass().getName() == BlockRequest.class.getName()) {
                        BlockRequest blockRequest = (BlockRequest) msg;
                        String filename = blockRequest.filename;
                        String fileMd5 = blockRequest.fileMd5;
                        int blockIdx = (int)blockRequest.blockIdx;
                        //tgui.logInfo("P2P server: Received block request msg from client, "+filename+" "+fileMd5+" "+blockIdx);

                        try{
                            file = new RandomAccessFile(filename, "rw");
                        } catch (FileNotFoundException e){
                            tgui.logWarn(e.toString());
                        }
                        FileDescr fileDescr = new FileDescr(file, blockLength);
//                        tgui.logInfo("file MD5: "+fileDescr.getFileMd5());
//                        tgui.logInfo("block MD5: "+fileDescr.getBlockMd5(blockIdx));
//                        tgui.logInfo("place 1, "+fileDescr.getNumBlockBytes(blockIdx));
                        FileMgr fileMgr = new FileMgr(filename, fileDescr, tgui);

                        int blockNum = fileDescr.getNumBlocks();

                        if (blockIdx<blockNum){
                            byte[] sendingBuffer = fileMgr.readBlock(blockIdx);
//                            tgui.logInfo("place 11, "+sendingBuffer.length);
                            String bytes = Base64.getEncoder().encodeToString(sendingBuffer);
//                            tgui.logInfo("place 2");
                            BlockReply blockReply = new BlockReply(filename, fileMd5, blockIdx, bytes);
                            writeMsg(blockReply);
                            //tgui.logInfo("P2P server: Block "+blockIdx+" send block reply msg "+filename+" "+fileMd5+" "+blockIdx+" "+bytes);

                        }else if (blockIdx == blockNum){
                            writeMsg(new GoodBye());
                            tgui.logInfo("P2P server: Send ByeBye");
                            break;
                        }else{
                            tgui.logError("P2P server: block index larger than block number. ");
                            break;
                        }

                    }else{
                        tgui.logError("P2P server: Expect to receive blockRequest. However receive something else. ");
                    }
                }

                file.close();
                bufferedReader.close();
                bufferedWriter.close();
                socket.close();
                // break; ////////
            } catch (IOException e) {
                tgui.logWarn("P2P server: "+e.toString());
            } catch (InterruptedException e) {
                tgui.logWarn("P2P server: "+e.toString());
                // throw new RuntimeException(e);
            } catch (JsonSerializationException e) {
                tgui.logWarn("P2P server: "+e.toString());
                // throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                tgui.logWarn("P2P server: "+e.toString());
                // throw new RuntimeException(e);
            } catch (BlockUnavailableException e) {
                tgui.logWarn("P2P server: "+e.toString());
            }
        }
        tgui.logInfo("P2P server: outside while loop ");
    }

    // copyright from Prof. Aaron
    private void writeMsg(Message msg) throws IOException {
        tgui.logDebug("P2P server: "+"sending: "+msg.toString());
        bufferedWriter.write(msg.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }


    private Message readMsg() throws IOException, JsonSerializationException {
        String jsonStr = bufferedReader.readLine();
        if(jsonStr!=null) {
            Message msg = (Message) MessageFactory.deserialize(jsonStr);
            tgui.logDebug("P2P server: "+"received: "+msg.toString());
            return msg;
        } else {
            throw new IOException();
        }
    }


}
