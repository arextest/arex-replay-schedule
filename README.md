# <img src="https://avatars.githubusercontent.com/u/103105168?s=200&v=4" alt="Arex Icon" width="27" height=""> AREX's Replay Schedule Service

## Introduction

After your success installed the **AREX's Agent**, and configure it used
the [`Remote Storage Service`](https://github.com/arextest/arex-storage).

You should be run a replay from what your operations and how many recorded sources retrieved you
wants to
validate the changes of a new version deployed on the target host is expected or unexpected.

To implements the purpose, we accept a target host to create a plan by your requested and a schedule
trigger will running for :

1. Loading all records from `Remote Storage Service` by each operations(your APIs) and group it by
   dependent on version.
1. Prepare the request message and send it to the target host.
1. If send success we retrieve all the response results(include entry service and called dependency
   service) by `recordId` + `replayId`
1. Use configuration indicate that how to compare the results which grouped by `MockCategoryType`
   such as : http servlet, redis, db .....
1. send replay compared results to the [`Report Service`](https://github.com/arextest/arex-report),
   which should be able to analysis and build summary.

**Note:**

* The plan should not create if the target host is unreachable.
* The plan should interrupt if sending request too many exceptions over then 10%

## Getting Started

1. **Modify default `localhost` connection string value**

   you should be change the connection string in the file of path '
   resources/META-INF/application.properties'.

   example for `Redis`,`mysql` and dependent `web services` as following:
    ```yaml
   # web api
   arex.storage.service.api=http://10.3.2.42:8093/api/storage/replay/query
   arex.api.service.api=http://10.3.2.42:8090/api/report
   arex.config.service.api=http://10.3.2.42:8091/api/config
   # mysql
   spring.datasource.url=jdbc:mysql://10.3.2.42:3306/arexdb?&useUnicode=true&characterEncoding=UTF-8
   spring.datasource.username=arex_admin
   spring.datasource.password=arex_admin_password
   # redis
   arex.redis.uri=redis://10.3.2.42:6379/
    ```
1. **Extends the replay sender if you have a requirement**

   There is a `DefaultHttpReplaySender` implemented `ReplaySender` used to handle http request,such
   as:put,get,post,delete etc.

   You should be write another implementation which loaded by spring,the `ReplaySender` defined as
   following:

   ```java
   public interface ReplaySender {
       /**
        * Indicate the instance should be working for the message content type,
        * return true should be used,others skipped
        */
       boolean isSupported(int contentType);
   
       /**
        * Try to send the replay case to remote target host
        */
       boolean send(ReplayActionCaseItem caseItem);
   
       /**
        * Try to send the request message to remote target host
        */
       ReplaySendResult send(SenderParameters senderParameters);
   
       /**
        * Try to prepare the replay case remote dependency such as resume config files
        */
       boolean prepareRemoteDependency(ReplayActionCaseItem caseItem);
   
       /**
        * Try to warm up the remote target service before sending
        */
       default boolean activeRemoteService(ReplayActionCaseItem caseItem) {
           return true;
       }
   }
   ```
1. **Extends the deploy environment**

   To create a plan for report, we should be required more info such as target image's version and
   the source code author.

   By default,there is a empty instance implemented `DeploymentEnvironmentProvider` which defined as
   following:

   ```java
   public interface DeploymentEnvironmentProvider {
   
       DeploymentVersion getVersion(String appId, String env);
   
       List<ServiceInstance> getActiveInstanceList(AppServiceDescriptor serviceDescriptor, String env);
   }
   ```

## License

- Code: [Apache-2.0](https://github.com/arextest/arex-agent-java/blob/LICENSE)
