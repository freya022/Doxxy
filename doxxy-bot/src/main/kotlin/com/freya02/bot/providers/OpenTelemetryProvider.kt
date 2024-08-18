package com.freya02.bot.providers

import com.freya02.bot.config.OpenTelemetryConfig
import io.github.freya022.botcommands.api.core.service.annotations.BConfiguration
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

@BConfiguration
object OpenTelemetryProvider {
    @BService
    fun openTelemetry(openTelemetryConfig: OpenTelemetryConfig): OpenTelemetry {
        if (!openTelemetryConfig.enable) return OpenTelemetry.noop()

        val resource: Resource = Resource.getDefault().toBuilder()
            .put("service.name", "Doxxy")
            .put("service.version", "1.0.0").build()

        val sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().setEndpoint(openTelemetryConfig.spanProcessor.endpoint).build()).build())
            .setResource(resource)
            .build()

        val sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(PrometheusHttpServer.builder().setPort(openTelemetryConfig.metricReader.port).build())
            .setResource(resource)
            .build()

        val sdkLoggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(SystemOutLogRecordExporter.create()).build())
            .setResource(resource)
            .build()

        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setMeterProvider(sdkMeterProvider)
            .setLoggerProvider(sdkLoggerProvider)
            .setPropagators(
                ContextPropagators.create(
                    TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance()
                    )
                )
            )
            .buildAndRegisterGlobal()
    }
}