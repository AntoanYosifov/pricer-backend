package com.antdevrealm.pricer_backend;

import com.antdevrealm.pricer_backend.spike.SpikePriceObservationRepository;
import com.antdevrealm.pricer_backend.spike.dto.SparPriceResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;


@Component
public class AppRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppRunner.class);

    private static final String USER_AGENT =
            "pricer-backend/0.1 (+https://github.com/AntoanYosifov/pricer-backend) portfolio project";

    private static final String ARTICLE_ID = "2824994";
    private static final String OUTLET = "8999";
    private final String priceUrl;
    private final String productUrl;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    private final SpikePriceObservationRepository repository;

    @Autowired
    public AppRunner(
            @Value("${spar.api.price-url}") String priceUrl,
            @Value("${spar.api.product-url}") String productUrl,
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            SpikePriceObservationRepository repository
    ) {
        this.client = builder.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build();
        this.priceUrl = priceUrl;
        this.productUrl = productUrl;
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (priceUrl.isBlank() || productUrl.isBlank()) {
            System.out.println("Spar API URLs not configured — skipping probe. "
                    + "Set SPAR_PRICE_URL and SPAR_PRODUCT_URL to enable.");
            return;
        }

        try {
            ResponseEntity<String> response = client.get()
                    .uri(priceUrl + "?articleId={articleId}&outlet={outlet}", ARTICLE_ID, OUTLET)
                    .retrieve()
                    .toEntity(String.class);

            LocalDate observedOn = LocalDate.now(ZoneId.of("Europe/Vienna"));
            Instant fetchedAt = Instant.now();

            log.info("HTTP {} — x-remaining-requests: {}",
                    response.getStatusCode(),
                    response.getHeaders().getFirst("x-remaining-requests"));

            String rawJson = response.getBody();

            archive("price", ARTICLE_ID, observedOn, rawJson);

            SparPriceResponse body = objectMapper.readValue(rawJson, SparPriceResponse.class);

            log.info("outlet={} (from the request, not the response)", OUTLET);
            log.info("parsed: {}", body);

            if (body != null && body.comparisonPrice() != null) {
                log.info("price={} unit_price={} per {} {}",
                        body.calculatedPrice(),
                        body.comparisonPrice().price(),
                        body.comparisonPrice().quantity(),
                        body.comparisonPrice().unit());

                int affectedRows = this.repository.upsert(body.articleId(), OUTLET, observedOn,
                        fetchedAt, body.calculatedPrice(),
                        body.comparisonPrice().price());

                log.info("Affected rows: {}", affectedRows);
            }
        } catch (RestClientResponseException e) {
            log.error("Spar returned {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    private void archive(String kind, String articleId, LocalDate date, String body) {
        if (body == null) {
            log.warn("nothing to archive for {} {} on {} - empty response body", kind, articleId, date);
            return;
        }

        Path path = Path.of("var", "raw", "spar", kind, date.toString(),
                articleId + "-" + OUTLET + ".json");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, body);
            log.info("archived {} -> {}", kind, path);
        } catch (IOException e) {
            log.warn("could not archive {} for {} on {}: {}", kind, articleId, date, e.toString());
        }
    }
}
