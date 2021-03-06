/*
 * Copyright (C) 2016-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.lagom.javadsl.client;

import com.lightbend.lagom.internal.client.CircuitBreakers;
import com.lightbend.lagom.internal.javadsl.client.CircuitBreakersPanelImpl;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import play.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A service locator that uses static configuration and provides circuit breaker
 */
@Singleton
public class ConfigurationServiceLocator extends CircuitBreakingServiceLocator {

    private static final String LAGOM_SERVICES_KEY = "lagom.services";
    private final PMap<String, URI> services;


    /**
     * @deprecated Use constructor accepting {@link CircuitBreakersPanel} instead
     * @param circuitBreakers
     */
    @Deprecated
    public ConfigurationServiceLocator(Configuration configuration, CircuitBreakers circuitBreakers) {
        this(configuration.underlying(), new CircuitBreakersPanelImpl(circuitBreakers));
    }

    @Inject
    public ConfigurationServiceLocator(Config config, CircuitBreakersPanel circuitBreakersPanel) {
        super(circuitBreakersPanel);

        Map<String, URI> services = new HashMap<>();

        if (config.hasPath(LAGOM_SERVICES_KEY)) {
            Config configServices = config.getConfig(LAGOM_SERVICES_KEY);
            for (String key: configServices.root().keySet()) {
                try {
                    String value = configServices.getString(key);
                    URI uri = new URI(value);
                    services.put(key, uri);
                } catch (ConfigException.WrongType e) {
                    throw new IllegalStateException("Error loading configuration for " + getClass().getSimpleName() + ". Expected lagom.services." + key + " to be a String, but was " + configServices.getValue(key).valueType(), e);
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Error loading configuration for  " + getClass().getSimpleName() + ". Expected lagom.services." + key + " to be a URI, but it failed to parse", e);
                }
            }
        }
        this.services = HashTreePMap.from(services);
    }



    @Override
    public CompletionStage<Optional<URI>> locate(String name, Descriptor.Call<?, ?> serviceCall) {
        return CompletableFuture.completedFuture(Optional.ofNullable(services.get(name)));
    }

}
