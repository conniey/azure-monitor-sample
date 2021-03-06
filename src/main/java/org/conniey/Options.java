package org.conniey;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Command line options
 */
public class Options {
    @Parameter(names = {"--workspace", "-w"}, description = "Workspace Id.", required = true)
    private String workspaceId;

    @Parameter(names ={"--query", "-q"}, description = "Query to pass into Azure Monitor. Please sort results by date ascending.", required = true)
    private String query;

    @Parameter(names = {"--start", "-s"}, description = "Start date and time in UTC for query (ie. 2011-12-03T10:15:30Z). If not set, last 24 hours is used.",
            converter = StringToDateConverter.class)
    private OffsetDateTime startDate;

    @Parameter(names = {"--end", "-e"}, description = "End date and time in UTC for query (ie. 2011-12-03T10:15:30Z).  If not set, last 24 hours is used.",
            converter = StringToDateConverter.class)
    private OffsetDateTime endDate;

    @Parameter(names = {"--dateColumn", "-dc"}, description = "Name of the date column in query. (ie. TimeGenerated)")
    private String dateColumnName;

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getDateColumnName() {
        return dateColumnName;
    }

    public void setDateColumnName(String dateColumnName) {
        this.dateColumnName = dateColumnName;
    }

    /**
     * Converts a string to its Instant representation.
     */
    static class StringToDateConverter implements IStringConverter<OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String s) {
            final Instant instant = DateTimeFormatter.ISO_INSTANT.parse(s, Instant::from);

            return instant.atOffset(ZoneOffset.UTC);
        }
    }
}
