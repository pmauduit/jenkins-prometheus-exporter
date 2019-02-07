package fr.spironet.jenkins.prometheus.exporter;

import static spark.Spark.get;
import static spark.Spark.port;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class App {

    private final static Logger logger = LoggerFactory.getLogger(App.class);

    private final static OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String jenkinsBaseUrl = System.getenv("jenkinsBaseUrl") == null ?
            "https://ci.camptocamp.com/job/geospatial/job" : System.getenv("jenkinsBaseUrl");

    // Note: in case of multibranch, projects should be qualified with the branch name,
    // like "georchestra-cigalsace-ci/job/master"

    private final List<String> projectsToQuery;

    // See in Jenkins UI how to get these infos (my user / configure / show api token...)
    private final String username = System.getenv("jenkinsUsername") != null ?
            System.getenv("jenkinsUsername") : null;
    private final String token = System.getenv("jenkinsToken") != null ?
            System.getenv("jenkinsToken") : null;

    private final int JENKINS_UNKNOWN = -1;
    private final int JENKINS_IDLE = 0;
    private final int JENKINS_BUILDING = 1;
    private final int JENKINS_FAILED = 2;

    public App() {
        if (System.getenv("jenkinsJobs") == null) {
            projectsToQuery = Arrays.asList("cigalsace,geopicardie,georhena,pigma,ppige".split(","));
        } else {
            projectsToQuery = Pattern.compile(",").splitAsStream(System.getenv("jenkinsJobs"))
                .map(s -> s.trim())
                .collect(Collectors.toList());
        }

        int port = Integer.getInteger("spark.port", 9103);

        port(port);

        get("/*", (req, res) -> {
            StringBuilder ret = new StringBuilder();
            for (String project : projectsToQuery) {
                ret.append(String.format("jenkins_build_status{name=\"%s\"} %d\n", project, getBuildStatus(project)));
            }
            return ret.toString();
        });
        
        logger.info("initialized with the following parameters:");
        logger.info("jenkins base url: " + jenkinsBaseUrl);
        logger.info("jenkins jobs to monitor:");
        for (String p : projectsToQuery) {
            logger.info(" * " + p);
        }

    }
    /**
     * Returns the status of a Jenkins build.
     *
     * @param proj the project name in Jenkins
     * @return an integer describing the build status as following:
     * -1 if unknown
     * 0 if idle (i.e. not building and last build OK)
     * 1 if currently building
     * 2 if idle and last build failed or unstable
     */
    private int getBuildStatus(String proj) {
        try {
            String lastBuildUrl = getLastBuildUrl(proj);
            JsonObject json = gson.fromJson(getUrl(lastBuildUrl), JsonObject.class);

            Boolean buildStatus =  json.get("building").getAsBoolean();
            if (buildStatus == true) {
                return JENKINS_BUILDING;
            }
            String result = json.get("result").getAsString();
            if (result.equals("SUCCESS")) {
                return JENKINS_IDLE;
            }
            return JENKINS_FAILED;
        } catch (Exception e) {
          logger.error("Unable to get status for project " + proj + ", returning unknown status");
          return JENKINS_UNKNOWN;
        }
    }
    
    /**
     * Given a project name, returns the last build url by querying the Jenkins JSON API.
     *
     * @param proj the project codename in Jenkins.
     *
     * @return a string representing the last build URL.
     *
     */
    private String getLastBuildUrl(String proj) {
        String projectUrl = String.format(
                "%s/%s/api/json",
                jenkinsBaseUrl, proj);

        JsonObject json = gson.fromJson(getUrl(projectUrl), JsonObject.class);

        return String.format("%s/api/json",
                 json.getAsJsonObject("lastBuild").get("url").getAsString());
    }
    
    /**
     * Gets the raw content pointed by the URL given as argument using OkHttp.
     *
     * @param url the url to GET.
     * @return the content body as a string.
     */
    private String getUrl(String url) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        if ((username != null) && (token != null)) {
            requestBuilder.header("Authorization",
                    Credentials.basic(username, token));
        }

        Request request = requestBuilder.build();
        Response response;
        try {
            response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.error("Error getting url " + url, e);
        }
        return null;
    }

    
    /**
     * Webapp entry point. You can customize the configuration using the JAVA_TOOL_OPTIONS variable, e.g.:
     *
     * $ JAVA_TOOL_OPTIONS="-DjenkinsBaseUrl=http://myjenkins:8080/job -DjenkinsJobs=pigma,cigalsace -Dspark.port=8081" \
     *     java -jar myjar-with-dependencies.jar
     *
     * @param args
     */
    public static void main(String[] args) {
        new App();
        return;
    }

}
