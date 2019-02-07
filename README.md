## jenkins-prometheus-exporter


```
$ jenkinsJobs=a/job/master,b,c jenkinsBaseUrl=https://example.com/jenkins \
  jenkinsUsername=admin  jenkinsToken=... mvn clean package exec:java
```

The build status has been set as the following:

```java
    private final int JENKINS_UNKNOWN = -1;
    private final int JENKINS_IDLE = 0;
    private final int JENKINS_BUILDING = 1;
    private final int JENKINS_FAILED = 2;
```

Here is a sample output:

```
# curl http://localhost:9103
jenkins_build_status{name="cigalsace"} -1
jenkins_build_status{name="geopicardie"} 0
jenkins_build_status{name="georhena"} 1
jenkins_build_status{name="pigma"} 0
jenkins_build_status{name="ppige"} 2
```
