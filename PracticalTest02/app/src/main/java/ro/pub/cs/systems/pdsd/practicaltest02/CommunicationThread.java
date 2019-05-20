package ro.pub.cs.systems.pdsd.practicaltest02;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;

import cz.msebera.android.httpclient.conn.scheme.Scheme;
import cz.msebera.android.httpclient.conn.scheme.SchemeRegistry;
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;

import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.conn.SingleClientConnManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.protocol.HTTP;
import ro.pub.cs.systems.pdsd.practicaltest02.Constants;
import ro.pub.cs.systems.pdsd.practicaltest02.Utilities;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CommunicationThread extends Thread {

    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    private class XmlPullParserHandler {

        String text;
        StringBuilder builder = new StringBuilder();

        public String parse(InputStream is) {

            try {

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();

                parser.setInput(is, null);

                int eventType = parser.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String tagname = parser.getName();
                    switch (eventType) {

                        case XmlPullParser.TEXT:
                            text = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (tagname.equalsIgnoreCase("YOUR_TAG1")) {
                                builder.append("TAG1 = " + text);
                            } else if (tagname.equalsIgnoreCase("YOUR_TAG2")) {
                                builder.append("\nTAG2 = " + text);
                            }
                            break;

                        default:
                            break;
                    }
                    eventType = parser.next();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            return builder.toString();
        }


    }

    public String parseXml(String input) throws ParserConfigurationException, IOException, SAXException {
       /* try {
            String tag = "";
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput( new StringReader(input) ); // pass input whatever xml you have
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
              if(eventType == XmlPullParser.START_TAG) {
                  Log.e(Constants.TAG, "[COMMUNICATION THREAD] tag xml:" + xpp.getName());

                  if (xpp.getName().equals(Constants.SEARCH_KEY)) {
                      Log.e(Constants.TAG, "[COMMUNICATION THREAD] text: " + xpp.getText());
                      return xpp.getText();
                  }
              }
                eventType = xpp.next();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new ByteArrayInputStream(input.getBytes()));
        Element element = doc.getDocumentElement();

        NodeList list = ((org.w3c.dom.Document) doc).getElementsByTagName(Constants.SEARCH_KEY);
        return list.toString();
        // getTextContent
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (word)!");
            String word = bufferedReader.readLine();

            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Word " + word);
            if (word == null || word.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (word)");
                return;
            }


            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");

            HttpClient httpClient = new DefaultHttpClient();

            HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + Constants.QUERY_ATTRIBUTE + word);

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String pageSourceCode = httpClient.execute(httpGet, responseHandler);
            if (pageSourceCode == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                return;
            }

        try {
            String result = parseXml(pageSourceCode);

            Log.e(Constants.TAG, "[COMMUNICATION THREAD] definition:" + result);
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] pageSource:" + pageSourceCode);

            printWriter.println(result);
            printWriter.flush();
        } catch (ParserConfigurationException exp ) {

        } catch (IOException e1) {
        }
        catch (SAXException e2)
        {

        }
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An IO exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        }
        finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}
