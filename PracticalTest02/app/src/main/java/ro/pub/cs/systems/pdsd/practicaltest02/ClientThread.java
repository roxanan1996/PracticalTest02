package ro.pub.cs.systems.pdsd.practicaltest02;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import ro.pub.cs.systems.pdsd.practicaltest02.Constants;
import ro.pub.cs.systems.pdsd.practicaltest02.Utilities;

public class ClientThread extends Thread {

    private String address;
    private int port;
    private String word;
    private TextView definitionTextView;

    private Socket socket;

    public ClientThread(String address, int port, String word, TextView definitionTextView) {
        this.address = address;
        this.port = port;
        this.word = word;
        this.definitionTextView = definitionTextView;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(address, port);
            if (socket == null) {
                Log.e(Constants.TAG, "[CLIENT THREAD] Could not create socket!");
                return;
            }
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[CLIENT THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            printWriter.println(word);
            printWriter.flush();

            String definition;
            while ((definition = bufferedReader.readLine()) != null) {
                final String finalizedDefinition = definition;
                definitionTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        definitionTextView.setText(finalizedDefinition);
                    }
                });
            }
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[CLIENT THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}

