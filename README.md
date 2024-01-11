# <img src="https://avatars.githubusercontent.com/u/103105168?s=200&v=4" alt="Arex Icon" width="27" height=""> AREX Schedule Service

## Introduction

Once you have successfully installed the **AREX's Agent** and configured it to use the [`Storage Service`](https://github.com/arextest/arex-storage), you can initiate a replay to verify the impact of new version deployments on your target host. This process helps in understanding whether the changes are as expected or not.

## How to Run a Replay

The replay process involves the following steps:

1. **Retrieve Records:**
   Load all records from the `Remote Storage Service` for each API operation, categorizing them by version.

2. **Prepare and Send Requests:**
   Organize the request messages and dispatch them to the target host.

3. **Collect Responses:**
   Upon successful request transmission, gather all response results (including entry and dependent services) using `recordId` + `replayId`.

4. **Result Comparison:**
   Compare these results as per configurations, categorized by `MockCategoryType` (e.g., HTTP servlet, Redis, DB).

5. **Report Generation:**
   Send the replay comparison results to the [`API Service`](https://github.com/arextest/arex-api) for analysis and summary creation.

## Important Notes

- **Host Availability:** Do not create a plan if the target host is unreachable.
- **Error Handling:** Interrupt the plan if more than 10% of request sending results in exceptions.

## Getting Started

### 1. Configuring Connection Strings

To set up the connection, modify the default `localhost` values in `resources/META-INF/application.properties`. Here's how you can configure for `Redis`, `MySQL`, and dependent `web services`:

```yaml
# Web API
arex.storage.service.api=http://10.3.2.42:8093/api/storage/replay/query
arex.api.service.api=http://10.3.2.42:8090/api/report
arex.config.service.api=http://10.3.2.42:8091/api/config

# MySQL
spring.datasource.url=jdbc:mysql://10.3.2.42:3306/arexdb?useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=arex_admin
spring.datasource.password=arex_admin_password

# Redis
arex.redis.uri=redis://10.3.2.42:6379/
```

### 2. Extending the Replay Sender

The `DefaultHttpReplaySender` handles HTTP requests (PUT, GET, POST, DELETE, etc.). To implement a custom sender, follow the `ReplaySender` interface pattern:

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

### 3. Extending Deployment Environments

To generate comprehensive reports, additional information such as the target image version and source code author is required. Implement the `DeploymentEnvironmentProvider` interface to provide this data:

```java
public interface DeploymentEnvironmentProvider {
   
   DeploymentVersion getVersion(String appId, String env);
   
   List<ServiceInstance> getActiveInstanceList(AppServiceDescriptor serviceDescriptor, String env);
}
```

## License

- Code: [Apache-2.0](https://github.com/arextest/arex-agent-java/blob/LICENSE)
