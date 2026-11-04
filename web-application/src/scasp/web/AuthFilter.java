package scasp.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Sets UTF-8 encoding and blocks unauthenticated access to everything except /login. */
@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        HttpServletRequest r = (HttpServletRequest) req;
        HttpSession s = r.getSession(false);
        boolean loggedIn = s != null && s.getAttribute("user") != null;
        if (loggedIn || r.getServletPath().equals("/login")) chain.doFilter(req, res);
        else ((HttpServletResponse) res).sendRedirect(r.getContextPath() + "/login");
    }
}
