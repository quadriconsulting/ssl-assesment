package lw.ssl.analyze.servlets;

import api.lw.ssl.analyze.responce.WebResourceStatus;
import lw.ssl.analyze.pojo.WebResourceDescription;
import lw.ssl.analyze.utils.InputStreamConverter;
import lw.ssl.analyze.utils.SSLResponseAnalys;
import lw.ssl.analyze.utils.SSLTest;
import lw.ssl.analyze.utils.notificators.EmailNotificator;
import lw.ssl.analyze.utils.notificators.GitHubNotificator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        Task currentTask = results.get(userName) == null ? null : results.get(userName).get();
        if(currentTask!=null&&currentTask.percent<100){
            currentTask.interrupt();
        }
        Part filePart = request.getPart(FILE_PART_NAME);
        InputStream fileInputStream = filePart.getInputStream();
        currentTask = new Task(InputStreamConverter.convertToWebResourceDescriptions(fileInputStream));
        currentTask.start();
        results.put(userName, new WeakReference<Task>(currentTask));
        request.setAttribute("wrongWebResourceStatusList", new ArrayList<>());
        request.setAttribute("percent", 0);
        request.setAttribute("currentUrl", "");

        request.getRequestDispatcher("/result.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userName = (String) request.getSession(false).getAttribute("user");
        Task currentTask = results.get(userName) == null ? null : results.get(userName).get();
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
        private HashMap<VerificationThread,String> currentUrl;

        public Task(List<WebResourceDescription> webResourceDescriptionlist) {
            this.webResourceDescriptionlist = webResourceDescriptionlist;
        }

        List<WebResourceStatus> allWebResourceStatusList;
        int count;

        @Override
        public void run() {
            wrongWebResourceStatusList = new ArrayList<>();
            percent = 0;
            allWebResourceStatusList = new ArrayList<>();
            count = 0;
            currentUrl = new HashMap<>();
            ExecutorService executor = Executors.newFixedThreadPool(5);

            //for each url with port (default 443)
            for (WebResourceDescription webResourceDescription : webResourceDescriptionlist) {
                VerificationThread verificationThread = new VerificationThread(webResourceDescription);
                executor.execute(verificationThread);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                executor.shutdown();
                return;
            }

            if (wrongWebResourceStatusList != null && wrongWebResourceStatusList.size() > 0) {
                //Email-notification,
                EmailNotificator.notificate(wrongWebResourceStatusList, WRONG_URLs_LETTER_SUBJECT, getServletContext());
                //Github-notification,
                GitHubNotificator.notificate(wrongWebResourceStatusList, WRONG_URLs_GITHUB_SUBJECT, getServletContext());
            }

            percent = 100;
        }

        class VerificationThread implements Runnable {
            WebResourceDescription webResourceDescription;
            SSLResponseAnalys analysysContext = new SSLResponseAnalys();

            VerificationThread(WebResourceDescription webResourceDescription) {
                this.webResourceDescription = webResourceDescription;
            }

            @Override
            public void run() {
                System.out.println("Analize url:" + webResourceDescription.getHost());
                currentUrl.put(this,webResourceDescription.getHost() + (webResourceDescription.getPort() == null ? "" : ":" + webResourceDescription.getPort()));
                JSONObject analysisResponseJSON = SSLTest.getStatistic(webResourceDescription.getHost(), webResourceDescription.getPort(), true);

                SSLResponseAnalys.HostAnalysysResponse hostAnalysysResponse = analysysContext.analysysResult(analysisResponseJSON);
                if (!hostAnalysysResponse.isSuccessfull()) {
                    addWrongWebResourceStatus(hostAnalysysResponse.getWebResourceStatus());
                }
                addWebResourceStatus(hostAnalysysResponse.getWebResourceStatus());
                currentUrl.remove(this);
                System.out.println("Url:" + webResourceDescription.getHost() + " done");
            }
        }

        synchronized private void addWebResourceStatus(WebResourceStatus webResourceStatus) {
            allWebResourceStatusList.add(webResourceStatus);
            count++;
            percent = Math.round(100 * count / webResourceDescriptionlist.size());
        }

        synchronized private void addWrongWebResourceStatus(WebResourceStatus webResourceStatus) {
            wrongWebResourceStatusList.add(webResourceStatus);
        }

        public List<WebResourceStatus> getWrongWebResourceStatusList() {
            return wrongWebResourceStatusList;
        }

        public int getPercent() {
            return percent;
        }

        public String getCurrentUrl() {
            StringBuilder result = new StringBuilder();
            for (String url : currentUrl.values()){
                result.append(url).append(", ");
            }
            return result.toString();
        }
    }
}
