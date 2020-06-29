package org.mokesoft.art.shortlink.common;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class HttpUtils {

//    public static void fireJsonResponse(HttpServerResponse response, int statusCode, String payload) {
//        response.putHeader("content-type", "application/json; charset=utf-8").setStatusCode(statusCode).end(payload);
//    }

    public static void fireResponse(HttpServerResponse response, int statusCode, ResponseResult data) {
        response.putHeader("content-type", "application/json; charset=utf-8").setStatusCode(statusCode)
                .end(JsonSerializeUtils.toString(data));
    }

    public static <T> void fireSuccessResponse(HttpServerResponse response, int statusCode, T data) {
        response.putHeader("content-type", "application/json; charset=utf-8").setStatusCode(statusCode)
                .end(JsonSerializeUtils.toString(ResponseResult.success(data)));
    }

    public static void fireTextResponse(RoutingContext routingContext, String text) {
        routingContext.response().putHeader("content-type", "text/html; charset=utf-8").end(text);
    }
}



