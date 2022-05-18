package org.conniey;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.LogsQueryAsyncClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.monitor.query.models.QueryTimeInterval;
import java.time.Duration;

public class Program {
    public static void main(String[] args) {
        LogsQueryAsyncClient logsQueryAsyncClient = new LogsQueryClientBuilder()
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildAsyncClient();
        Duration duration = Duration.ofMinutes(10);
    }
}
