package com.zensyra.ccollector.core.dto.response;

/**
 * Envoltorio uniforme para todas las respuestas de la API.
 * En éxito: success=true, data con el payload, error=null.
 * En error: success=false, data=null, error con el mensaje.
 */
public record ResponseDTO<T>(boolean success, T data, String error) {

    public static <T> ResponseDTO<T> ok(T data) {
        return new ResponseDTO<>(true, data, null);
    }

    public static <T> ResponseDTO<T> fail(String error) {
        return new ResponseDTO<>(false, null, error);
    }
}
