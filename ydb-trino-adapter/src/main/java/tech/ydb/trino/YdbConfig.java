package tech.ydb.trino;

import io.airlift.configuration.Config;
import jakarta.validation.constraints.NotNull;

public class YdbConfig {
    private String root = "//home";

    @NotNull
    public String getRoot() {
        return root;
    }

    @Config("ydb.root")
    public YdbConfig setRoot(String root) {
        this.root = root;
        return this;
    }
}
