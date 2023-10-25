package com.arextest.schedule.beans;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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