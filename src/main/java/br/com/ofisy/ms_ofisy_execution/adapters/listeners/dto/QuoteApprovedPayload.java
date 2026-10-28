package br.com.ofisy.ms_ofisy_execution.adapters.listeners.dto;

import java.util.List;
import java.util.UUID;

public record QuoteApprovedPayload(UUID quoteId, List<ServiceItem> serviceItems) {

    public record ServiceItem(UUID serviceCatalogId) {}
}