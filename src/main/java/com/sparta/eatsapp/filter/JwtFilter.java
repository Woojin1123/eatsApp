package com.sparta.eatsapp.filter;

import com.sparta.eatsapp.config.JwtUtil;
import com.sparta.eatsapp.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter implements Filter {

  private final JwtUtil jwtUtil;
  private final Pattern authPattern = Pattern.compile("^/api/auth/(signin|signup)$");
  private final Pattern getOrderPattern = Pattern.compile("^/api/restaurant/.*/order");

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

  @Override
  public void doFilter(
      ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String url = httpRequest.getRequestURI();

    if (authPattern.matcher(url).matches()) {
      chain.doFilter(request, response);
      return;
    }

    String bearerJwt = httpRequest.getHeader("Authorization");

    if (bearerJwt == null || !bearerJwt.startsWith("Bearer ")) {
      // 토큰이 없는 경우 400을 반환합니다.
      httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "JWT 토큰이 필요합니다.");
      return;
    }
    String jwt = jwtUtil.substringToken(bearerJwt);

    try {
      // JWT 유효성 검사와 claims 추출
      Claims claims = jwtUtil.extractClaims(jwt);

      httpRequest.setAttribute("userId", Long.parseLong(claims.getSubject()));
      httpRequest.setAttribute("email", claims.get("email", String.class));
      httpRequest.setAttribute("userRole", claims.get("userRole", String.class));

      UserRole userRole = UserRole.valueOf(claims.get("userRole", String.class));

      if (isOwnerUrl(httpRequest, url)) {
        //사장님만 이용할 수 있는 api
        if (!userRole.equals(UserRole.OWNER)) {
          httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "접근 권한이 없습니다.");
          return;
        }
        chain.doFilter(request, response);
        return;
      }
      if (!isOwnerUrl(httpRequest, url)) {
        //유저만 이용할 수 있는 api
        if (!userRole.equals(UserRole.USER)) {
          httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "접근 권한이 없습니다.");
          return;
        }
        chain.doFilter(request, response);
        return;
      }

      chain.doFilter(request, response);
    } catch (SecurityException | MalformedJwtException e) {
      log.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.", e);
      httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않는 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      log.error("Expired JWT token, 만료된 JWT token 입니다.", e);
      httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 JWT 토큰입니다.");
    } catch (UnsupportedJwtException e) {
      log.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.", e);
      httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      log.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.", e);
      httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 JWT 토큰입니다.");
    } catch (Exception e) {
      log.error("JWT 토큰 검증 중 오류가 발생했습니다.");
      httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT 토큰 검증 중 오류가 발생했습니다.");
    }
  }

  private boolean isOwnerUrl(HttpServletRequest httpRequest, String url) {
    if ((url.startsWith("/api/eats") &&
        (httpRequest.getMethod().equals("POST")
            || httpRequest.getMethod().equals("PATCH")
            || httpRequest.getMethod().equals("DELETE"))) || getOrderPattern.matcher(url).matches()
        || url.startsWith("/api/owner") || url.startsWith(
        "/api/orderStatus")) {
      return true;
    }
    return false;
  }

  @Override
  public void destroy() {
    Filter.super.destroy();
  }
}