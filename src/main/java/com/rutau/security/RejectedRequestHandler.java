package com.rutau.security;

import com.rutau.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Spring Security rechaza por seguridad algunas URL sospechosas, por ejemplo con doble barra
 * ("/api/trips//complete", típico cuando una variable de Postman está vacía).
 * Este manejador responde un 400 claro en vez de dejar que termine como un 401 engañoso.
 */
@Component
@RequiredArgsConstructor
public class RejectedRequestHandler implements RequestRejectedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       RequestRejectedException ex) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = ApiErrorResponse.of(400, "Bad Request",
                "La URL de la petición no es válida (revisa que no tenga variables vacías, por ejemplo '//')",
                request.getRequestURI());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
