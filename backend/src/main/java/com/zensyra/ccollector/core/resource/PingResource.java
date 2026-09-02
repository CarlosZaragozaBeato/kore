package com.zensyra.ccollector.core.resource;

import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint mínimo para verificar de extremo a extremo (frontend -> proxy ->
 * backend) que la API responde. Establece la convención {@link ResponseDTO}.
 */
@Path("/ping")
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

    @GET
    public ResponseDTO<Map<String, Object>> ping() {
        return ResponseDTO.ok(Map.of(
                "service", "ccollector-backend",
                "status", "up",
                "timestamp", Instant.now().toString()
        ));
    }
}
