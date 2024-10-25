package com.arextest.schedule.beans;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author wyc
 * @date 2020-09-20
 */
@WebFilter("/*")
public class CORSFilter implements Filter {

  public CORSFilter() {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;
    String originHeads = request.getHeader("Origin");
    response.setHeader("Access-Control-Allow-Origin", originHeads);
    response.setHeader("Access-Control-Allow-Methods", "POST,GET,OPTIONS,DELETE,HEAD,PUT,PATCH");
    response.setHeader("Access-Control-Max-Age", "36000");
    response.setHeader("Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept,Authorization,authorization");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    chain.doFilter(req, response);
  }

  @Override
  public void init(FilterConfig fConfig) throws ServletException {
  }
}