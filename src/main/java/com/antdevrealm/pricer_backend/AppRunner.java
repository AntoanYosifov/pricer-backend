package com.antdevrealm.pricer_backend;

import com.antdevrealm.pricer_backend.spike.dto.SparPriceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;


@Component
public class AppRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppRunner.class);

    private static final String USER_AGENT =
            "pricer-backend/0.1 (+https://github.com/AntoanYosifov/pricer-backend) portfolio project";

    private static final String ARTICLE_ID = "2824994";
    private static final String OUTLET = "8999";


    private final RestClient client;

    private final String priceUrl;
    private final String productUrl;

    public AppRunner(RestClient.Builder builder,
                     @Value("${spar.api.price-url}") String priceUrl,
                     @Value("${spar.api.product-url}") String productUrl) {
        this.client = builder.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build();
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

        try {
            ResponseEntity<SparPriceResponse> response = client.get()
                    .uri(priceUrl + "?articleId={articleId}&outlet={outlet}", ARTICLE_ID, OUTLET)
                    .retrieve()
                    .toEntity(SparPriceResponse.class);

            log.info("HTTP {} — x-remaining-requests: {}",
                    response.getStatusCode(),
                    response.getHeaders().getFirst("x-remaining-requests"));

            SparPriceResponse body = response.getBody();
            log.info("outlet={} (from the request, not the response)", OUTLET);
            log.info("parsed: {}", body);

            if (body != null && body.comparisonPrice() != null) {
                log.info("price={} unit_price={} per {} {}",
                        body.calculatedPrice(),
                        body.comparisonPrice().price(),
                        body.comparisonPrice().quantity(),
                        body.comparisonPrice().unit());
            }

        } catch (RestClientResponseException e) {
            log.error("Spar returned {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        }

    }
}
