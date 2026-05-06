package tech.ydb.retry.integration;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.query.YdbQuery;

public class DeterministicErrorChannel implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {

    private record ErrorRule(String methodName, int callNumber, StatusCode code) {
        boolean matches(String method, int callNum) {
            return methodName.equals(method) && (callNumber == 0 || callNumber == callNum);
        }
    }

    private static final List<ErrorRule> rules = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private static final DeterministicErrorChannel INSTANCE = new DeterministicErrorChannel();

    private static final Map<String, Function<StatusCodesProtos.StatusIds.StatusCode, ?>> RESPONSE_BUILDERS = Map.of(
            "ExecuteQuery", code -> YdbQuery.ExecuteQueryResponsePart.newBuilder().setStatus(code).build(),
            "BeginTransaction", code -> YdbQuery.BeginTransactionResponse.newBuilder().setStatus(code).build(),
            "CommitTransaction", code -> YdbQuery.CommitTransactionResponse.newBuilder().setStatus(code).build()
            );

    public DeterministicErrorChannel() {
        loadFromSystemProperty();
    }

    public static DeterministicErrorChannel configure() {
        rules.clear();
        counters.clear();
        return INSTANCE;
    }

    public static void resetCounters() {
        counters.clear();
    }

    public static int getCallCount(String method) {
        String pascalName = Character.toUpperCase(method.charAt(0)) + method.substring(1);
        AtomicInteger counter = counters.get(pascalName);
        return counter != null ? counter.get() : 0;
    }

    public DeterministicErrorChannel onError(String method, int callNumber, StatusCode code) {
        addRule(method, callNumber, code);
        return this;
    }

    private static void addRule(String method, int callNumber, StatusCode code) {
        String pascalName = Character.toUpperCase(method.charAt(0)) + method.substring(1);
        toProto(code);
        rules.add(new ErrorRule(pascalName, callNumber, code));
    }

    @Override
    public void accept(ManagedChannelBuilder<?> builder) {
        builder.intercept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        String fullMethodName = method.getFullMethodName();
        String shortName = fullMethodName.substring(fullMethodName.lastIndexOf('/') + 1);

        AtomicInteger counter = counters.computeIfAbsent(shortName, k -> new AtomicInteger());
        int callNum = counter.incrementAndGet();

        for (ErrorRule rule : rules) {
            if (rule.matches(shortName, callNum)) {
                Function<StatusCodesProtos.StatusIds.StatusCode, ?> builderFn = RESPONSE_BUILDERS.get(shortName);
                if (builderFn != null) {
                    StatusCodesProtos.StatusIds.StatusCode protoCode = toProto(rule.code());
                    RespT errorMsg = (RespT) builderFn.apply(protoCode);
                    return new ErrorCall<>(errorMsg);
                }
            }
        }

        return next.newCall(method, callOptions);
    }

    private static StatusCodesProtos.StatusIds.StatusCode toProto(StatusCode code) {
        try {
            return StatusCodesProtos.StatusIds.StatusCode.valueOf(code.name());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Status " + code + " is not a YDB protobuf response status. ",
                    ex
            );
        }
    }

    private class ErrorCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final RespT errorMsg;

        ErrorCall(RespT errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
            ForkJoinPool.commonPool().execute(() -> {
                listener.onMessage(errorMsg);
                listener.onClose(Status.OK, new Metadata());
            });
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }

        @Override
        public void halfClose() {
        }

        @Override
        public void sendMessage(ReqT message) {
        }
    }

    private static void loadFromSystemProperty() {
        String config = System.getProperty("deterministic.error.channel.rules");
        if (config == null || config.isBlank()) {
            return;
        }
        rules.clear();
        counters.clear();
        for (String ruleStr : config.split(";")) {
            String[] parts = ruleStr.trim().split(":");
            if (parts.length == 3) {
                String method = parts[0].trim();
                int callNumber = Integer.parseInt(parts[1].trim());
                StatusCode code = StatusCode.valueOf(parts[2].trim());
                addRule(method, callNumber, code);
            }
        }
    }
}
