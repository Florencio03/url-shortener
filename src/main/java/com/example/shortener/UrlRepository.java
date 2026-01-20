package com.example.shortener;


import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class UrlRepository {

    private final DynamoDbClient dynamo;
    private final String tableName;

    public UrlRepository(DynamoDbClient dynamo, String tableName) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    public void put(String code, String longUrl) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "code", AttributeValue.fromS(code),
                        "longUrl", AttributeValue.fromS(longUrl),
                        "createdAt", AttributeValue.fromN(Long.toString(Instant.now().getEpochSecond()))
                        )
                )
                .conditionExpression("attribute_not_exists(code)")
                .build());
    }

    public Optional<String> getLongUrl(String code) {
        var resp = dynamo.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("code", AttributeValue.fromS(code)))
                .consistentRead(true)
                .build());

        if (!resp.hasItem()) return Optional.empty();
        return Optional.ofNullable(resp.item().get("longUrl")).map(AttributeValue::s);
    }

}
