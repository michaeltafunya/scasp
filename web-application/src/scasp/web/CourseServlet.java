package scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import scasp.core.AcademicService;

/** GET /courses lists courses; POST /courses registers or withdraws the logged-in student (STUDENT role only). */
@WebServlet("/courses")
public class CourseServlet extends HttpServlet {
    private AcademicService svc(HttpServletRequest r) { return (AcademicService) getServletContext().getAttribute("svc"); }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = req.getParameter("q");
        req.setAttribute("courses", q == null || q.isBlank() ? svc(req).courses() : svc(req).search(q));
        req.setAttribute("report", svc(req).registrationReport());
        req.getRequestDispatcher("/WEB-INF/views/courses.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (!"STUDENT".equals(s.getAttribute("role"))) { resp.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        String code = req.getParameter("code");
        String action = req.getParameter("action");
        if (code == null || code.isBlank() || action == null) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing input"); return; }
        try {
            String id = (String) s.getAttribute("studentId");
            if ("register".equals(action)) s.setAttribute("msg", svc(req).register(id, code));
            else if ("withdraw".equals(action)) { svc(req).withdraw(id, code); s.setAttribute("msg", "Withdrawn"); }
            else { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action"); return; }
        } catch (IllegalArgumentException | IllegalStateException e) {
            s.setAttribute("msg", e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/courses");
    }
}
