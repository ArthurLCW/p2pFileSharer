/**
 * This file implements a thread that receives one socket and act as a P2P server (sharer).
 * */

package comp90015.idxsrv.peer;

import comp90015.idxsrv.filemgr.BlockUnavailableException;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class P2PServerRequestThread extends Thread{
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private ISharerGUI tgui;
    private int timeout;
    private int blockLength;

    public P2PServerRequestThread(Socket socket, ISharerGUI tgui,
                                   int timeout, int blockLength){
        this.socket = socket;
        this.tgui = tgui;
        this.timeout = timeout;
        this.blockLength = blockLength;
    }

    @Override
    public void run(){
        tgui.logInfo("P2P server: ready for running socket.");
        try {
            socket.setSoTimeout(timeout);
            tgui.logInfo("P2P server: receive request from "+socket.getInetAddress().toString()+": "+socket.getPort());

            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            RandomAccessFile file = null;

            while (true){
                Message msg = readMsg();
                if(msg.getClass().getName() == BlockRequest.class.getName()) {
                    BlockRequest blockRequest = (BlockRequest) msg;
                    String filename = blockRequest.filename;
                    String fileMd5 = blockRequest.fileMd5;
                    int blockIdx = (int)blockRequest.blockIdx;

                    try{
                        file = new RandomAccessFile(filename, "r");
                    } catch (FileNotFoundException e){
                        tgui.logWarn(e.toString());
                    }
                    FileDescr fileDescr = new FileDescr(file, blockLength);
                    FileMgr fileMgr = new FileMgr(filename, fileDescr);

                    int blockNum = fileDescr.getNumBlocks();

                    if (blockIdx<blockNum){
                        byte[] sendingBuffer = fileMgr.readBlock(blockIdx);
                        String bytes = Base64.getEncoder().encodeToString(sendingBuffer);
                        BlockReply blockReply = new BlockReply(filename, fileMd5, blockIdx, bytes);
                        writeMsg(blockReply);

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
            tgui.logError("P2P server: "+e.toString());
        } catch (JsonSerializationException e) {
            tgui.logError("P2P server: "+e.toString());
            // throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            tgui.logError("P2P server: "+e.toString());
            // throw new RuntimeException(e);
        } catch (BlockUnavailableException e) {
            tgui.logError("P2P server: "+e.toString());
        }
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
