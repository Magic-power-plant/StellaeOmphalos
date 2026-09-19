package com.mpp.stellaeomphalos.data.loader;

import java.util.ArrayList;
import java.util.List;

/** One report belongs to one reload transaction. */
public final class DataLoadReport {
    public enum Severity { WARN, ERROR }
    public record Issue(Severity severity, String file, String pointer, String message) {}
    private final List<Issue> issues = new ArrayList<>();
    public void error(String file, String pointer, String message) { issues.add(new Issue(Severity.ERROR, file, pointer, message)); }
    public void warn(String file, String pointer, String message) { issues.add(new Issue(Severity.WARN, file, pointer, message)); }
    public boolean hasErrors() { return issues.stream().anyMatch(issue -> issue.severity() == Severity.ERROR); }
    public List<Issue> issues() { return List.copyOf(issues); }
}
