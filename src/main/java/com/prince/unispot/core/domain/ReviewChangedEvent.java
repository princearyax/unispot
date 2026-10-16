package com.prince.unispot.core.domain;

//a domain event, review service can check and publish new event
public record ReviewChangedEvent(Long placeId) {}