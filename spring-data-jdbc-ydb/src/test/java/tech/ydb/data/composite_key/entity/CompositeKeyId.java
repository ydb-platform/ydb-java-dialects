package tech.ydb.data.composite_key.entity;

import java.util.Objects;

/**
 * Composite id reproducing YDBREQUESTS-8347: an {@code @Id} property backed by a value object with several
 * fields instead of a single scalar column.
 */
public class CompositeKeyId {
    private final String workspaceId;
    private final String scopeId;

    public CompositeKeyId(String workspaceId, String scopeId) {
        this.workspaceId = workspaceId;
        this.scopeId = scopeId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getScopeId() {
        return scopeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompositeKeyId)) {
            return false;
        }
        CompositeKeyId other = (CompositeKeyId) o;
        return Objects.equals(workspaceId, other.workspaceId) && Objects.equals(scopeId, other.scopeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, scopeId);
    }

    @Override
    public String toString() {
        return "CompositeKeyId{workspaceId='" + workspaceId + "', scopeId='" + scopeId + "'}";
    }
}
