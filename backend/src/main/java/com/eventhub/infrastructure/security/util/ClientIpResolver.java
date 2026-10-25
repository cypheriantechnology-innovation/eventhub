package com.eventhub.infrastructure.security.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private static final String[] IP_HEADER_CANDIDATES = {
            "CF-Connecting-IP",
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private ClientIpResolver() {}

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList)) {
                // Return the first IP in the list (client address)
                return ipList.split(",")[0].trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "0.0.0.0";
    }
}
