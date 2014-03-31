package com.nokiaappcrawler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class Main {

  private static final String MIDLET_NAME = "MIDlet-Name: ";
  private static final String MIDLET_URL = "MIDlet-Jar-URL: ";
  
  private static void usage() {
    System.out.println("java -jar crawler.jar <start id> <end id> <sleep millisecond>");
  }
  /**
   * @param args
   * @throws IOException 
   * @throws HttpException 
   * @throws InterruptedException 
   */
  public static void main(String[] args) throws HttpException, IOException, InterruptedException {
    if (args.length != 3) {
      usage();
      return;
    }
    int startId = Integer.parseInt(args[0]);
    int endId = Integer.parseInt(args[1]);
    int sleepMillisecond = Integer.parseInt(args[2]);
    
    HttpClient client = new HttpClient();
    PostMethod post = new PostMethod("https://account.nokia.com/acct/AuthenticationServlet?doneURL=%2Facct%2FAuthenticationServlet");
    NameValuePair name = new NameValuePair("username", "laoyaosniper@outlook.com");     
    NameValuePair pass = new NameValuePair("password", "laozikannizaitou");     
    post.setRequestBody(new NameValuePair[]{name,pass});
    int loginStatusCode = client.executeMethod(post);
    System.out.println(post.getStatusLine());
    post.releaseConnection();  
    if ( loginStatusCode == HttpStatus.SC_MOVED_TEMPORARILY ) {
      for ( int appId = startId; appId < endId; appId++ ) {
        HttpMethod method = new GetMethod("http://store.ovi.com/content/" + appId + "/download"); 
        int jadStatusCode = client.executeMethod(method);
        //打印服务器返回的状态
        System.out.println(appId + " " + method.getStatusLine());
        if (jadStatusCode == HttpStatus.SC_OK) {
          String fileName = getFileName(method);
          String appName = appId + "";
          if ( fileName != null ) {
            if (fileName.endsWith("jad")) {
              String jadString = method.getResponseBodyAsString();
              if ( jadString.contains(MIDLET_NAME) && jadString.contains(MIDLET_URL)) {
                int appNameStart = jadString.indexOf(MIDLET_NAME);
                int appNameEnd = jadString.indexOf("\n", appNameStart);
                appName = jadString.substring(appNameStart + MIDLET_NAME.length(), appNameEnd) + "." + appId;
                int jarURLStart = jadString.indexOf(MIDLET_URL);
                int jarURLEnd = jadString.indexOf("\n", jarURLStart);
                String jarURL = jadString.substring(jarURLStart + MIDLET_URL.length(), jarURLEnd);
                System.out.println(MIDLET_NAME + appName);
//                System.out.println(MIDLET_URL + jarURL);

                HttpMethod downloadJarMethod = new GetMethod(jarURL);
                client.executeMethod(downloadJarMethod);     
                // store jar and jad
                saveToFile(method, appName + ".jad");
                saveToFile(downloadJarMethod, appName + ".jar");
                // release connection
                downloadJarMethod.releaseConnection();
              }         
            }
            else {
              System.out.println(fileName);
            }     
          }
        }
        method.releaseConnection();
        if ( sleepMillisecond != 0 ) {
          Thread.sleep(sleepMillisecond);
        }
      }
    }
  }

  private static void saveToFile(HttpMethod method, String fileName) throws IOException {
    InputStream input = method.getResponseBodyAsStream();
    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
    int inByte;
    while((inByte = input.read()) != -1) output.write(inByte);
    input.close();
    output.close();
  }
  
  private static String getFileName(HttpMethod method) {
    Header contentHeader = method.getResponseHeader("Content-Disposition");
    String fileName = null;
    if (contentHeader != null) {
      HeaderElement[] values = contentHeader.getElements();
      if (values != null && values.length == 1) {
        NameValuePair param = values[0].getParameterByName("filename");
        if (param != null) {
          fileName = param.getValue();
        }
      }
    }
//    System.out.println("File Name:" + fileName);
    return fileName;
  }
}
