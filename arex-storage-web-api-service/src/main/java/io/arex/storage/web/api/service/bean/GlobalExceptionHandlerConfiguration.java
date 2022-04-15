package io.arex.storage.web.api.service.bean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * add GlobalExceptionHandler
 *
 * @author jmo
 * @since 2022/1/17
 */
@Slf4j
@ControllerAdvice
class GlobalExceptionHandlerConfiguration {
    private static final ModelAndView EMPTY_VIEW = new ModelAndView();

    /**
     * logged handle HttpMessageNotReadable
     *
     * @param ex       HttpMessageNotReadableException
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @param handler  the mapped handle
     * @return ModelAndView
     * @throws IOException IOException
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request, HttpServletResponse response,
                                                     Object handler) throws IOException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        LOGGER.warn("Failed to read HTTP message from remote [{}] ,request headers: {} ,handler:{}",
                request.getRemoteAddr(), fetchHeaders(request),
                handler,
                ex);
        return EMPTY_VIEW;
    }

    private Map<String, String> fetchHeaders(HttpServletRequest httpRequest) {
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        if (headerNames == null) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, httpRequest.getHeader(name));
        }
        return headers;
    }
}
