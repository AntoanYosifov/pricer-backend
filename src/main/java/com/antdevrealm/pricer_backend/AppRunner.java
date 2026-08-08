package com.antdevrealm.pricer_backend;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;


@Component
public class AppRunner implements CommandLineRunner {

    private static final String USER_AGENT =
            "pricer-backend/0.1 (+https://github.com/AntoanYosifov/pricer-backend) portfolio project";

    private final RestClient client = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .build();

    private final String priceUrl;
    private final String productUrl;

    public AppRunner(@Value("${spar.api.price-url}") String priceUrl,
                     @Value("${spar.api.product-url}") String productUrl) {
        this.priceUrl = priceUrl;
        this.productUrl = productUrl;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (priceUrl.isBlank() || productUrl.isBlank()) {
            System.out.println("Spar API URLs not configured — skipping probe. "
                    + "Set SPAR_PRICE_URL and SPAR_PRODUCT_URL to enable.");
            return;
        }

        JsonNode price = client.get()
                .uri(priceUrl)
                .retrieve()
                .body(JsonNode.class);

        assert price != null;
        System.out.println("=== PRICE");
        System.out.println(price.toPrettyString());

        JsonNode product = client.get()
                .uri(productUrl)
                .retrieve()
                .body(JsonNode.class);

        assert product != null;
        System.out.println("=== PRODUCT");
        System.out.println(product.toPrettyString());
    }
}
