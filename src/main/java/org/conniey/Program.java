package org.conniey;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.LogsQueryAsyncClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.monitor.query.models.LogsQueryOptions;
import com.azure.monitor.query.models.LogsQueryResult;
import com.azure.monitor.query.models.LogsTableCell;
import com.azure.monitor.query.models.LogsTableRow;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class Program {
    private static final Logger LOGGER = LoggerFactory.getLogger(Program.class);

    public static void main(String[] args) {
        final Options options = new Options();
        final JCommander builder = JCommander.newBuilder()
                .addObject(options)
                .build();

        try {
            builder.parse(args);
        } catch (Exception e) {
            System.err.println("Error occurred parsing arguments. " + e.getMessage());
            builder.usage();
            return;
        }

        final TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        final LogsQueryAsyncClient queryClient = new LogsQueryClientBuilder()
                .credential(credential)
                .buildAsyncClient();

        final QueryTimeInterval interval;

        if (options.getStartDate() != null) {
            final OffsetDateTime endDate = options.getEndDate() != null ? options.getEndDate() : OffsetDateTime.now();

            interval = new QueryTimeInterval(options.getStartDate(), endDate);
        } else {
            interval = QueryTimeInterval.LAST_2_DAYS;
        }

        System.out.println("Starting...");

        runQuery(queryClient, options.getWorkspaceId(), interval, options.getQuery()).block();

        System.out.println("Complete.");
    }

    private static Mono<Void> runQuery(LogsQueryAsyncClient client, String workspaceId, QueryTimeInterval interval,
            String query) {
        return client.queryWorkspace(workspaceId, query, interval).flatMapIterable(result -> result.getAllTables())
                .handle((log, sink) -> {

                    for (LogsTableRow row : log.getRows()) {
                        try {
                            final List<LogsTableCell> cells = row.getRow();
                            final String joined = cells.stream().map(cell -> cell.getValueAsString())
                                    .collect(Collectors.joining("\t"));

                            LOGGER.info(joined);
                        } catch (Exception e) {
                            LOGGER.error("Error occurred in row {}", row.getRowIndex(), e);
                            sink.error(e);

                            return;
                        }
                    }

                    sink.complete();
                })
                .then();
    }
}
