package com.zensyra.ccollector.core.resource.auth;

import com.zensyra.ccollector.core.dto.auth.CollectorUserDTO;
import com.zensyra.ccollector.core.dto.response.ResponseDTO;
import com.zensyra.ccollector.core.service.auth.AuthService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(String username) {
    }

    /** Crea o recupera la sesión para el username dado. */
    @POST
    @Path("/login")
    public ResponseDTO<CollectorUserDTO> login(LoginRequest request) {
        String username = request == null ? null : request.username();
        return ResponseDTO.ok(CollectorUserDTO.from(authService.login(username)));
    }
}
