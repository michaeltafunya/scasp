package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

/** Demo accounts only (fictional). Role is stored in the session: STUDENT or STAFF. */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Map<String, String[]> USERS = Map.of(          // user -> {password, role, studentId}
            "student1", new String[]{"demo", "STUDENT", "S1"},
            "staff1", new String[]{"demo", "STAFF", ""});

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("out".equals(req.getParameter("action"))) {                 // logout
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] u = USERS.get(req.getParameter("user"));
        if (u == null || !u[0].equals(req.getParameter("password"))) {
            req.setAttribute("error", "Invalid username or password");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }
        req.changeSessionId();                                           // prevent session fixation
        HttpSession s = req.getSession();
        s.setAttribute("user", req.getParameter("user"));
        s.setAttribute("role", u[1]);
        s.setAttribute("studentId", u[2]);
        resp.sendRedirect(req.getContextPath() + "/courses");           // redirect after POST
    }
}
