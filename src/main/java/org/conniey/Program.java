package org.conniey;

import com.azure.core.credential.TokenCredential;
import com.azure.core.models.ResponseError;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.LogsQueryAsyncClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.monitor.query.models.LogsQueryOptions;
import com.azure.monitor.query.models.LogsQueryResult;
import com.azure.monitor.query.models.LogsQueryResultStatus;
import com.azure.monitor.query.models.LogsTableCell;
import com.azure.monitor.query.models.LogsTableRow;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Program {
    private static final Logger LOGGER = LoggerFactory.getLogger(Program.class);
    private static final Logger LOGS_LOGGER = LoggerFactory.getLogger("QueryLogger");

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

        try {
            runQuery(queryClient, options.getWorkspaceId(), interval, options.getQuery(), options.getDateColumnName())
                    .block();
        } catch (QueryException e) {
            LOGGER.error("Exception occurred querying data.", e);
        }

        System.out.println("Complete.");
    }

    private static Mono<Void> runQuery(LogsQueryAsyncClient client, String workspaceId, QueryTimeInterval interval,
            String query, String dateColumnName) {

        final AtomicReference<OffsetDateTime> nextDate = new AtomicReference<>(interval.getStartTime());

        return Mono.defer(() -> queryWorkspace(client, workspaceId, interval.getEndTime(), query, dateColumnName, nextDate))
                .repeat(() -> nextDate.get() != null)
                .then();
    }


    private static Mono<OffsetDateTime> queryWorkspace(LogsQueryAsyncClient client, String workspaceId,
            OffsetDateTime endDate, String query, String columnName, AtomicReference<OffsetDateTime> nextDate) {

        // Get the current date and set it to null, so we only populate with a value when this function is over.
        final OffsetDateTime currentStart = nextDate.getAndSet(null);
        final QueryTimeInterval queryTimeInterval = new QueryTimeInterval(currentStart, endDate);

        LOGGER.info("Querying from {} to {}", queryTimeInterval.getStartTime(), queryTimeInterval.getEndTime());

        return client.queryWorkspaceWithResponse(workspaceId, query, queryTimeInterval,
                        new LogsQueryOptions().setAllowPartialErrors(true))
                .handle((response, sink) -> {
                    // Get the current start date.
                    final LogsQueryResult queryResult = response.getValue();

                    if (LogsQueryResultStatus.FAILURE == queryResult.getQueryResultStatus()) {
                        LOGGER.warn("Query failed.");

                        final ResponseError error = queryResult.getError();

                        sink.error(new QueryException(error.getCode(), error.getMessage(), query));
                        return;
                    }

                    if (queryResult.getTable() == null) {
                        LOGGER.warn("There was no table returned for the query.");
                        sink.complete();
                        return;
                    } else if (queryResult.getTable().getRows().isEmpty()) {
                        LOGGER.warn("Query returned 0 results.");
                        sink.complete();
                        return;
                    }

                    final List<LogsTableRow> rows = queryResult.getTable().getRows();
                    for (final LogsTableRow row : rows) {
                        final List<LogsTableCell> cells = row.getRow();
                        final String joined = cells.stream().map(cell -> cell.getValueAsString())
                                .collect(Collectors.joining("\t"));
                        try {
                            LOGS_LOGGER.debug(joined);
                        } catch (Exception e) {
                            LOGGER.error("Error occurred in row {}", row.getRowIndex(), e);
                            sink.error(new QueryException("", "Error occurred in row " + row.getRowIndex(), query));
                            return;
                        }
                    }

                    if (LogsQueryResultStatus.SUCCESS == queryResult.getQueryResultStatus()) {
                        sink.complete();
                        return;
                    }

                    final LogsTableRow lastRow = rows.get(rows.size() - 1);
                    final Optional<LogsTableCell> cell = lastRow.getColumnValue(columnName);

                    if (!cell.isPresent()) {
                        sink.error(new IllegalArgumentException(String.format(
                                "Could not extract time column. Column name: %s. Row: %s", columnName, lastRow)));
                        return;
                    }

                    final OffsetDateTime valueAsDateTime = cell.get().getValueAsDateTime();

                    if (valueAsDateTime.isAfter(endDate)) {
                        LOGGER.info("Last result date {} is after the query end date {}.", valueAsDateTime, endDate);
                        sink.complete();
                    } else {
                        LOGGER.info("Next query date is {}.", valueAsDateTime);

                        nextDate.set(valueAsDateTime);
                        sink.next(valueAsDateTime);
                    }
                });
    }
}
