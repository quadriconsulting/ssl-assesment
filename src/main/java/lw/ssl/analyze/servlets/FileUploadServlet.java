package lw.ssl.analyze.servlets;

import api.lw.ssl.analyze.responce.WebResourceStatus;
import lw.ssl.analyze.pojo.WebResourceDescription;
import lw.ssl.analyze.utils.InputStreamConverter;
import lw.ssl.analyze.utils.ResourceContainer;
import lw.ssl.analyze.utils.SSLResponseAnalys;
import lw.ssl.analyze.utils.SSLTest;
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
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by zmushko_m on 21.04.2016.
 */

@WebServlet(name = "FileUploadServlet", urlPatterns = {"/uploadCSV"})
@MultipartConfig
public class FileUploadServlet extends HttpServlet {
    public static final String FILE_PART_NAME = "fileCSV";

    private static final String WRONG_URLs_LETTER_SUBJECT = "Wrong URLs list";
    private static final String WRONG_URLs_GITHUB_SUBJECT = "Wrong URLs list";
    public static final String TASK = "task";
    TreeMap<String, WeakReference<Task>> results = new TreeMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userName = (String) request.getSession(false).getAttribute("user");
        Task currentTask = results.get(userName)==null?null:results.get(userName).get();
        if (currentTask == null || !currentTask.isAlive()) {
            Part filePart = request.getPart(FILE_PART_NAME);
            InputStream fileInputStream = filePart.getInputStream();
            currentTask = new Task(InputStreamConverter.convertToWebResourceDescriptions(fileInputStream));
            currentTask.start();
            results.put(userName, new WeakReference<Task>(currentTask));
            request.setAttribute("wrongWebResourceStatusList", new ArrayList<>());
            request.setAttribute("percent", 0);
            request.setAttribute("currentUrl", "");
        } else {
            request.setAttribute("wrongWebResourceStatusList", currentTask.getWrongWebResourceStatusList());
            request.setAttribute("percent", currentTask.getPercent());
            request.setAttribute("currentUrl", currentTask.getCurrentUrl());
        }

        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userName = (String) request.getSession(false).getAttribute("user");
        Task currentTask = results.get(userName)==null?null:results.get(userName).get();
        if (currentTask == null) {
            request.setAttribute("wrongWebResourceStatusList", new ArrayList<>());
            request.setAttribute("percent", 0);
            request.setAttribute("currentUrl", "");
        } else {
            request.setAttribute("wrongWebResourceStatusList", currentTask.getWrongWebResourceStatusList());
            request.setAttribute("percent", currentTask.getPercent());
            request.setAttribute("currentUrl", currentTask.getCurrentUrl());
        }

    request.getRequestDispatcher("/result.jsp").forward(request, response);
    }

    class Task extends Thread {
        private List<WebResourceDescription> webResourceDescriptionlist;
        private List<WebResourceStatus> wrongWebResourceStatusList;
        private int percent;
        private String currentUrl;

        public Task(List<WebResourceDescription> webResourceDescriptionlist) {
            this.webResourceDescriptionlist = webResourceDescriptionlist;
        }

        @Override
        public void run() {
            wrongWebResourceStatusList = new ArrayList<>();
            percent = 0;
            currentUrl = "";
            List<WebResourceStatus> allWebResourceStatusList = new ArrayList<>();

            SSLResponseAnalys analysysContext = new SSLResponseAnalys();
            //for each url with port (default 443)
            for (int index = 0; index < webResourceDescriptionlist.size(); index++) {
                WebResourceDescription webResourceDescription = webResourceDescriptionlist.get(index);
                currentUrl = webResourceDescription.getHost() + (webResourceDescription.getPort()==null?"":":"+webResourceDescription.getPort());
                percent = Math.round(100 * index / webResourceDescriptionlist.size());
                System.out.println("Analize url:" + webResourceDescription.getHost());

                JSONObject analysisResponseJSON = SSLTest.getStatistic(webResourceDescription.getHost(), webResourceDescription.getPort(), true);

                SSLResponseAnalys.HostAnalysysResponse hostAnalysysResponse = analysysContext.analysysResult(analysisResponseJSON);
                if (!hostAnalysysResponse.isSuccessfull()) {
                    wrongWebResourceStatusList.add(hostAnalysysResponse.getWebResourceStatus());
                }
                allWebResourceStatusList.add(hostAnalysysResponse.getWebResourceStatus());
            }

            if (wrongWebResourceStatusList != null && wrongWebResourceStatusList.size() > 0) {
                //Email-notification,
                EmailNotificator.notificate(wrongWebResourceStatusList, WRONG_URLs_LETTER_SUBJECT, getServletContext());
                //Github-notification,
                GitHubNotificator.notificate(wrongWebResourceStatusList, WRONG_URLs_GITHUB_SUBJECT, getServletContext());
            }

            percent = 100;
        }

        public List<WebResourceStatus> getWrongWebResourceStatusList() {
            return wrongWebResourceStatusList;
        }

        public int getPercent() {
            return percent;
        }

        public String getCurrentUrl() {
            return currentUrl;
        }
    }
}
