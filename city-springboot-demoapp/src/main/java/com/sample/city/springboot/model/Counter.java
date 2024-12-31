package com.sample.city.springboot.model;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record Counter(String id, String name, long count) implements CounterBuilder.With {}
