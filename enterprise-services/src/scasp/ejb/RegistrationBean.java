package scasp.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import scasp.core.AcademicService;

/** Stateless bean: a container-managed transaction wraps each business method (REQUIRED by default). */
@Stateless
@LocalBean
public class RegistrationBean {
    @Inject AppMetrics metrics;                       // dependency injection of the singleton below
    @Inject AcademicService svc;                      // produce via a CDI @Produces method or use JdbcRepository

    @PostConstruct void init() { /* lifecycle callback */ }
    @PreDestroy void done() { }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String register(String studentId, String code) {
        String r = svc.register(studentId, code);     // an unchecked exception here rolls the transaction back
        metrics.count();
        return r;
    }
}
