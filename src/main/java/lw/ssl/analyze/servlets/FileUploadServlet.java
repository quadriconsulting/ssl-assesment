package lw.ssl.analyze.servlets;

import api.lw.ssl.analyze.responce.WebResourceStatus;
import lw.ssl.analyze.pojo.WebResourceDescription;
import lw.ssl.analyze.utils.InputStreamConverter;
import lw.ssl.analyze.utils.ResourceContainer;
import lw.ssl.analyze.utils.SSLResponseAnalys;
import lw.ssl.analyze.utils.notificators.EmailNotificator;
import lw.ssl.analyze.utils.notificators.GitHubNotificator;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zmushko_m on 21.04.2016.
 */

@WebServlet(name = "FileUploadServlet", urlPatterns = {"/uploadCSV"})
@MultipartConfig
public class FileUploadServlet extends HttpServlet {
    public static final String FILE_PART_NAME  = "fileCSV";
    private static final int CONNECT_TIMEOUT_MS = 30 * 1000;
    private static final int CONTENT_READ_TIMEOUT_MS = 30 * 1000;
    private static final int DEFAULT_NEW_ASSESSMENT_COOL_OFF = 1000;

    private static final String WRONG_URLs_LETTER_SUBJECT = "Wrong URLs list";
    private static final String WRONG_URLs_GITHUB_SUBJECT = "Wrong URLs list";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part filePart = request.getPart(FILE_PART_NAME);
        InputStream fileInputStream = filePart.getInputStream();
        List<WebResourceDescription> webResourceDescriptionlist = InputStreamConverter.convertToWebResourceDescriptions(fileInputStream);

        List<WebResourceStatus> wrongWebResourceStatusList = new ArrayList<>();
        StringBuilder wrongUrlsJSONStringInfo = new StringBuilder();

        final URL getInfoUrl = new URL(ResourceContainer.getSSLLabsInfoUrl());

//      the cool-off period after each new assessment, in milliseconds;
//      you're not allowed to submit a new assessment before the cool-off expires, otherwise you'll get a 429.
        int newAssessmentCoolOff = DEFAULT_NEW_ASSESSMENT_COOL_OFF;

        SSLResponseAnalys analysysContext = new SSLResponseAnalys();
        //for each url with port (default 443)
        for (WebResourceDescription webResourceDescription : webResourceDescriptionlist) {
            System.out.println("Analize url:" + webResourceDescription.getHost());
            // set url and port
            final URL getStatisticUrl = new URL(ResourceContainer.getSSLLabsAnalysisUrl(webResourceDescription.getHost(), webResourceDescription.getPort()));

            final URLConnection connection = getStatisticUrl.openConnection();

            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(CONTENT_READ_TIMEOUT_MS);

            // if result code is 429 do 3 attempts
            int tryStep = 3;
            while(tryStep>0) {
                try (InputStream inputAnalysisStream = connection.getInputStream()) {
                    String analysisResponseString = InputStreamConverter.convertInputStreamToString(inputAnalysisStream);

                    // if result is not A
                    SSLResponseAnalys.HostAnalysysResponse hostAnalysysResponse= analysysContext.analysysResult(analysisResponseString);

                    if (!hostAnalysysResponse.isSuccessfull()) {
                        wrongUrlsJSONStringInfo.append(analysisResponseString);
                        wrongUrlsJSONStringInfo.append(System.getProperty("line.separator"));
                    }
                    wrongWebResourceStatusList.add(hostAnalysysResponse.getWebResourceStatus());
                    // stop iterations with the same url
                    tryStep = 0;
                } catch (IOException ex) {
                    System.out.println("HTTP response code: 429");
                    tryStep--;
                    try {
                        if(tryStep>0) {
                            // sleep 15 minutes
                            Thread.sleep(900000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            //get sleep time for next cycle
            final URLConnection infoConnection = getInfoUrl.openConnection();
            infoConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            infoConnection.setReadTimeout(CONTENT_READ_TIMEOUT_MS);
            try (InputStream inputInfoStream = infoConnection.getInputStream()) {
                String infoResponseString = InputStreamConverter.convertInputStreamToString(inputInfoStream);
                JSONObject infoResponseJSON = new JSONObject(infoResponseString);

                if (infoResponseJSON != null) {
                    try {
                        newAssessmentCoolOff = infoResponseJSON.getInt("newAssessmentCoolOff");
                    } catch (JSONException e) {
                    }
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
            System.out.println("Sleep time:" + newAssessmentCoolOff);
            try {
                Thread.sleep(newAssessmentCoolOff);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        if (wrongUrlsJSONStringInfo != null && wrongUrlsJSONStringInfo.length() > 0) {
            //Email-notification,
            EmailNotificator.notificate(wrongUrlsJSONStringInfo.toString(), WRONG_URLs_LETTER_SUBJECT, getServletContext());
            //Github-notification,
            GitHubNotificator.notificate(wrongUrlsJSONStringInfo.toString(), WRONG_URLs_GITHUB_SUBJECT, getServletContext());
        }

        request.setAttribute("wrongWebResourceStatusList", wrongWebResourceStatusList);
        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }
}
