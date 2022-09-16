/**
 * This file implements a thread that receives multiple sockets from idx server.
 * It generates new threads (P2PServerReuestThread) and distribute received sockets to newly generated threads.
 * These threads will run concurrently.
 * */

package comp90015.idxsrv.peer;

import comp90015.idxsrv.textgui.ISharerGUI;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;

public class P2PServerPoolThread extends Thread{
    private Socket socket;
    private ISharerGUI tgui;

    private LinkedBlockingDeque<Socket> incomingConnections;

    private int port;
    private int timeout;

    private int blockLength;

    public P2PServerPoolThread(LinkedBlockingDeque<Socket> incomingConnections, ISharerGUI tgui,
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
                socket.setSoTimeout(timeout);
                tgui.logInfo("P2P server: receive request from "+socket.getInetAddress().toString()+": "+socket.getPort());
                P2PServerRequestThread p2PServerRequestThread = new P2PServerRequestThread(socket, tgui, timeout, blockLength);
                p2PServerRequestThread.start();

            } catch (IOException e) {
                tgui.logError("P2P server: "+e.toString());
            } catch (InterruptedException e) {
                tgui.logError("P2P server: "+e.toString());
            }
        }
        tgui.logInfo("P2P server: outside while loop ");
    }
}
