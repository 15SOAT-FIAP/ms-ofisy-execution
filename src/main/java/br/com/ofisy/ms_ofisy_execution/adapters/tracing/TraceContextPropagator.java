package br.com.ofisy.ms_ofisy_execution.adapters.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// Guarda e restaura o contexto do trace entre a requisicao que grava o evento no outbox e o job que publica depois.
// Com o dd-java-agent e DD_TRACE_OTEL_ENABLED=true a API do OpenTelemetry enxerga os spans do Datadog;
// sem o agent (local e testes) ela é no-op: nada é capturado e a publicação segue sem span.
@Component
public class TraceContextPropagator {

    private static final String INSTRUMENTATION_NAME = "ms-ofisy-execution.outbox";

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    public Map<String, String> captureCurrent() {
        Map<String, String> carrier = new HashMap<>();
        propagator().inject(Context.current(), carrier, Map::put);
        return carrier;
    }

    public void runWithParent(Map<String, String> parentContext, String spanName, Map<String, String> attributes, Runnable action) {
        if (parentContext == null || parentContext.isEmpty()) {
            action.run();
            return;
        }

        Context parent = propagator().extract(Context.root(), parentContext, MAP_GETTER);
        SpanBuilder builder = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
                .spanBuilder(spanName)
                .setParent(parent)
                .setSpanKind(SpanKind.PRODUCER);
        attributes.forEach(builder::setAttribute);
        Span span = builder.startSpan();

        try (Scope ignored = span.makeCurrent()) {
            action.run();
        } catch (RuntimeException e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private TextMapPropagator propagator() {
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }
}
