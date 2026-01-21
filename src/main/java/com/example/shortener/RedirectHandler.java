package com.example.shortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class RedirectHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private final UrlRepository repo;

    public RedirectHandler() {
        var dynamo = DynamoDbClient.create();
        var table = System.getenv("TABLE_NAME");
        this.repo = new UrlRepository(dynamo, table);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String code = event.getPathParameters() == null ? null : event.getPathParameters().get("code");
        if (code == null || code.isBlank()) return text(400, "Missing code");

        return repo.getLongUrl(code)
                .map(longUrl -> APIGatewayV2HTTPResponse.builder()
                        .withStatusCode(302)
                        .withHeaders(Map.of("Location", longUrl))
                        .build())
                .orElseGet(() -> text(404, "Not found"));
    }

    private APIGatewayV2HTTPResponse text(int status, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(Map.of("content-type", "text/plain"))
                .withBody(body)
                .build();
    }
}
