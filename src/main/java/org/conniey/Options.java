package org.conniey;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import java.time.OffsetDateTime;

/**
 * Command line options
 */
public class Options {
    @Parameter(names = {"--start", "-s"}, description = "Start date and time for query", required = true,
            converter = StringToDateConverter.class)
    private OffsetDateTime startDate;

    @Parameter(names = {"--end", "-e"}, description = "End date and time for query", required = true,
            converter = StringToDateConverter.class)
    private OffsetDateTime endDate;

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

    /**
     * Converts a string to its OffsetDateTime representation.
     */
    static class StringToDateConverter implements IStringConverter<OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String s) {
            return OffsetDateTime.parse(s);
        }
    }
}
