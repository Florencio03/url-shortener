package com.example.shortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.Map;

public class ShortenHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final UrlRepository repo;

    public ShortenHandler() {
        var dynamo = DynamoDbClient.create();
        var table = System.getenv("TABLE_NAME");
        this.repo = new UrlRepository(dynamo, table);
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

        try{
            var body = event.getBody() == null ? "" : event.getBody();
            var node = MAPPER.readTree(body);

            String longUrl = node.path("url").asText(null);
            if (longUrl == null || longUrl.isBlank()) return json(400, "{\"error\":\"Missing url\"}");

            // basic validation
            URI uri = URI.create(longUrl);
            if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                return json(400, "{\"error\":\"url must start with http or https\"}");
            }

            //generate code and store retry and collision
            String code = null;
            for (int i= 0; i < 5; i++){
                code = CodeGenerator.generate(7);

                try{
                    repo.put(code, longUrl);
                    break;
                } catch (Exception collision){
                    code = null;
                }
            }
            if (code == null) return json(500, "{\"error\":\"Could not generate unique code\"}");

            String baseUrl = "https://" + event.getRequestContext().getDomainName();
            String shortUrl = baseUrl + "/" + code;

            String response = MAPPER.writeValueAsString(Map.of(
                    "code", code,
                    "shortUrl", shortUrl,
                    "longUrl", longUrl
            ));
            return json(200, response);

        } catch (Exception e){
            return json(500, "{\"error\":\"Server error\"}");
        }

    }

    private APIGatewayV2HTTPResponse json(int status, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(status)
                .withHeaders(Map.of("content-type", "application/json"))
                .withBody(body)
                .build();
    }

}
