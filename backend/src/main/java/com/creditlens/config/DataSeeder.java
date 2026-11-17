package com.creditlens.config;

import com.creditlens.domain.EmploymentStatus;
import com.creditlens.domain.ProductType;
import com.creditlens.repository.CreditApplicationRepository;
import com.creditlens.service.CreditApplicationService;
import com.creditlens.web.dto.CreateApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo data for the 'dev' profile only. Goes through the real service, so every seeded
 * application gets a real engine decision and audit row. All people are fictional.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CreditApplicationService service;
    private final CreditApplicationRepository repository;

    public DataSeeder(CreditApplicationService service, CreditApplicationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<CreateApplicationRequest> samples = List.of(
                req("Avery Thompson", "avery.t@example.com", "92000", "450", 781, "15000", 36, EmploymentStatus.FULL_TIME, ProductType.CAR_LOAN),
                req("Priya Natarajan", "priya.n@example.com", "68000", "300", 722, "12000", 48, EmploymentStatus.FULL_TIME, ProductType.PERSONAL_LOAN),
                req("Marcus Bell", "marcus.b@example.com", "54000", "900", 641, "10000", 36, EmploymentStatus.FULL_TIME, ProductType.PERSONAL_LOAN),
                req("Sofia Alvarez", "sofia.a@example.com", "47000", "250", 705, "8000", 24, EmploymentStatus.SELF_EMPLOYED, ProductType.LINE_OF_CREDIT),
                req("Daniel Okafor", "daniel.o@example.com", "38000", "1400", 612, "20000", 60, EmploymentStatus.PART_TIME, ProductType.PERSONAL_LOAN),
                req("Hannah Kim", "hannah.k@example.com", "24000", "100", 688, "5000", 24, EmploymentStatus.STUDENT, ProductType.PERSONAL_LOAN),
                req("Liam O'Connor", "liam.o@example.com", "120000", "1800", 802, "40000", 72, EmploymentStatus.FULL_TIME, ProductType.CAR_LOAN),
                req("Grace Mensah", "grace.m@example.com", "18000", "200", 590, "6000", 36, EmploymentStatus.PART_TIME, ProductType.PERSONAL_LOAN),
                req("Ethan Wright", "ethan.w@example.com", "75000", "2600", 670, "25000", 60, EmploymentStatus.FULL_TIME, ProductType.LINE_OF_CREDIT),
                req("Chloe Martin", "chloe.m@example.com", "61000", "0", 745, "9000", 24, EmploymentStatus.RETIRED, ProductType.CAR_LOAN));
        samples.forEach(service::submit);
        log.info("Seeded {} demo applications", samples.size());
    }

    private static CreateApplicationRequest req(String name, String email, String income, String debt, int score,
                                                String amount, int term, EmploymentStatus emp, ProductType product) {
        return new CreateApplicationRequest(name, email, new BigDecimal(income), new BigDecimal(debt), score,
                new BigDecimal(amount), term, emp, product);
    }
}
