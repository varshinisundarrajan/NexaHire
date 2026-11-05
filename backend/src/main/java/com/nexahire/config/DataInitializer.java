package com.nexahire.config;

import com.nexahire.entity.*;
import com.nexahire.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CandidateProfileRepository candidateProfileRepository,
                           RecruiterProfileRepository recruiterProfileRepository,
                           JobRepository jobRepository,
                           ApplicationRepository applicationRepository,
                           InterviewRepository interviewRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking NexaHire database initialization and seed data...");

        // 1. Initialize Roles
        Role adminRole = getOrCreateRole("ROLE_ADMIN", "Administrator with full system access and audit control");
        Role hrRole = getOrCreateRole("ROLE_HR_MANAGER", "HR Manager with recruitment oversight and analytics");
        Role recruiterRole = getOrCreateRole("ROLE_RECRUITER", "Recruiter with job posting, ATS pipeline, and interview rights");
        Role candidateRole = getOrCreateRole("ROLE_CANDIDATE", "Candidate with job application and profile management rights");

        // 2. Initialize Seed Users
        User adminUser = getOrCreateUser("admin@nexahire.com", "admin", "Admin@123", "Alex Vance", "+1-555-0100", adminRole);
        User hrUser = getOrCreateUser("hr@nexahire.com", "hrmanager", "Hr@123", "Sarah Jenkins", "+1-555-0101", hrRole);
        User recruiterUser = getOrCreateUser("recruiter@nexahire.com", "recruiter", "Recruiter@123", "Marcus Brody", "+1-555-0102", recruiterRole);
        User candidateUser = getOrCreateUser("candidate@nexahire.com", "candidate", "Candidate@123", "Varshini Sundar", "+1-555-0103", candidateRole);

        // Additional Candidate for rich pipeline demonstration
        User candidateUser2 = getOrCreateUser("david.chen@example.com", "davidchen", "David@123", "David Chen", "+1-555-0104", candidateRole);
        User candidateUser3 = getOrCreateUser("elena.rostova@example.com", "elena", "Elena@123", "Elena Rostova", "+1-555-0105", candidateRole);

        // 3. Profiles
        CandidateProfile candidateProfile1 = getOrCreateCandidateProfile(
                candidateUser,
                "Senior Full Stack Software Engineer | Java, Spring Boot & React",
                "Results-oriented Full Stack Developer with 5+ years building scalable microservices, REST APIs, and modern responsive SPAs.",
                "Java, Spring Boot, MySQL, REST API, React, JavaScript, HTML5, CSS3, Docker, Git, AWS",
                new BigDecimal("5.5"),
                "B.Tech in Computer Science & Engineering",
                "Bengaluru, India / Remote",
                new BigDecimal("95000"),
                new BigDecimal("120000")
        );

        CandidateProfile candidateProfile2 = getOrCreateCandidateProfile(
                candidateUser2,
                "Cloud DevOps & SRE Specialist",
                "DevOps engineer specializing in CI/CD pipelines, Kubernetes orchestration, Terraform, and cloud infrastructure reliability.",
                "Docker, Kubernetes, AWS, Terraform, CI/CD, Linux, Python, Git",
                new BigDecimal("4.0"),
                "B.S. in Software Engineering",
                "San Francisco, CA / Hybrid",
                new BigDecimal("110000"),
                new BigDecimal("135000")
        );

        CandidateProfile candidateProfile3 = getOrCreateCandidateProfile(
                candidateUser3,
                "Lead UI/UX Designer & Frontend Architect",
                "Creative frontend architect passionate about human-centered design systems, responsive performance, and accessible web experiences.",
                "HTML5, CSS3, JavaScript, TypeScript, React, UI/UX, Figma, Tailwind CSS",
                new BigDecimal("6.0"),
                "M.S. in Human-Computer Interaction",
                "New York, NY / Remote",
                new BigDecimal("105000"),
                new BigDecimal("130000")
        );

        getOrCreateRecruiterProfile(hrUser, "Human Resources", "Director of Talent Acquisition", "NexaHire Global Inc.");
        getOrCreateRecruiterProfile(recruiterUser, "Engineering Recruitment", "Senior Technical Recruiter", "NexaHire Global Inc.");

        // 4. Initialize Jobs if table is empty
        if (jobRepository.count() == 0) {
            Job job1 = createJob(
                    "Senior Full Stack Java Developer",
                    "Engineering",
                    "FULL_TIME",
                    "San Francisco, CA (Remote Allowed)",
                    4, 8,
                    new BigDecimal("130000"), new BigDecimal("165000"),
                    "We are seeking an experienced Full Stack Java Developer to architect and scale our enterprise ATS microservices and customer portal. You will design robust RESTful APIs, optimize MySQL schemas, and build intuitive responsive dashboards.",
                    "Java, Spring Boot, MySQL, REST API, HTML5, JavaScript, Docker",
                    "React, AWS, Kubernetes, Redis, Microservices",
                    "ACTIVE",
                    recruiterUser,
                    LocalDate.now().plusMonths(2)
            );

            Job job2 = createJob(
                    "Lead UI/UX Frontend Engineer",
                    "Product & Design",
                    "FULL_TIME",
                    "New York, NY (Hybrid)",
                    5, 10,
                    new BigDecimal("140000"), new BigDecimal("175000"),
                    "Lead our frontend design system and client experiences. You will collaborate closely with product management and backend engineers to craft responsive, accessible SaaS interfaces.",
                    "JavaScript, HTML5, CSS3, React, UI/UX, Figma",
                    "TypeScript, Webpack, Responsive Design, Jest",
                    "ACTIVE",
                    hrUser,
                    LocalDate.now().plusMonths(1)
            );

            Job job3 = createJob(
                    "Cloud Infrastructure & DevOps Engineer",
                    "Infrastructure",
                    "FULL_TIME",
                    "Austin, TX (Remote)",
                    3, 7,
                    new BigDecimal("125000"), new BigDecimal("155000"),
                    "Join our platform infrastructure team to manage Kubernetes clusters, automate deployment pipelines with GitHub Actions, and enforce enterprise cloud security benchmarks.",
                    "AWS, Docker, Kubernetes, CI/CD, Linux, Terraform",
                    "Python, Prometheus, Grafana, Ansible",
                    "ACTIVE",
                    recruiterUser,
                    LocalDate.now().plusMonths(3)
            );

            Job job4 = createJob(
                    "Technical Product Manager - Talent Solutions",
                    "Product Management",
                    "FULL_TIME",
                    "Seattle, WA",
                    4, 9,
                    new BigDecimal("135000"), new BigDecimal("170000"),
                    "Drive the roadmap for NexaHire's recruitment funnel and ATS workflows. Define user stories, evaluate feedback, and guide engineering sprints to deliver high-impact recruitment solutions.",
                    "Product Management, Agile, Scrum, Wireframing, Data Analytics",
                    "SQL, JIRA, SaaS Strategy, User Research",
                    "ACTIVE",
                    hrUser,
                    LocalDate.now().plusMonths(2)
            );

            Job job5 = createJob(
                    "Junior Backend Developer (Java & Spring)",
                    "Engineering",
                    "FULL_TIME",
                    "Bengaluru, India / Hybrid",
                    1, 3,
                    new BigDecimal("45000"), new BigDecimal("65000"),
                    "Exciting opportunity for an enthusiastic backend engineer to contribute to core Spring Boot services, database persistence layers, and automated API integration testing.",
                    "Java, Spring Boot, SQL, Git",
                    "REST API, Postman, JUnit, Docker",
                    "ACTIVE",
                    recruiterUser,
                    LocalDate.now().plusMonths(2)
            );

            // 5. Initialize Sample Applications across pipeline stages
            Application app1 = createApplication(
                    job1,
                    candidateProfile1,
                    "SHORTLISTED",
                    88,
                    "Java, Spring Boot, MySQL, REST API, HTML5, JavaScript, Docker",
                    "",
                    "I am thrilled to apply for the Senior Full Stack Java Developer position. With over 5 years developing scalable Spring Boot services and rich frontend interfaces, my technical background aligns seamlessly with your tech stack.",
                    "Strong background in enterprise Java and microservices architecture. Shortlisted for technical screening round."
            );

            Application app2 = createApplication(
                    job2,
                    candidateProfile3,
                    "INTERVIEW",
                    92,
                    "JavaScript, HTML5, CSS3, React, UI/UX, Figma",
                    "",
                    "Having designed enterprise SaaS interfaces and design systems for 6+ years, I would love to bring my expertise in responsive web design and UI architecture to NexaHire.",
                    "Impressive portfolio and design systems leadership. Initial interview scheduled."
            );

            Application app3 = createApplication(
                    job3,
                    candidateProfile2,
                    "SCREENING",
                    83,
                    "AWS, Docker, Kubernetes, CI/CD, Linux, Terraform",
                    "",
                    "Hello team! I am excited to apply for the DevOps role. I have extensive hands-on experience orchestrating production Kubernetes and architecting resilient AWS clouds.",
                    "Candidate has relevant certifications in AWS and Kubernetes. Under screening review."
            );

            Application app4 = createApplication(
                    job5,
                    candidateProfile1,
                    "APPLIED",
                    100,
                    "Java, Spring Boot, SQL, Git",
                    "",
                    "Submitting my profile for your engineering team considerations.",
                    "New applicant from referral network."
            );

            // 6. Initialize Scheduled Interview
            Interview interview1 = new Interview();
            interview1.setApplication(app2);
            interview1.setInterviewer(recruiterUser);
            interview1.setInterviewDate(LocalDate.now().plusDays(2));
            interview1.setInterviewTime(LocalTime.of(14, 30));
            interview1.setInterviewType("VIDEO");
            interview1.setMeetingLink("https://meet.google.com/nex-hire-tech");
            interview1.setStatus("SCHEDULED");
            interview1.setFeedback("Preliminary screening completed with excellent communication and UI architecture fundamentals.");
            interview1.setRating(5);
            interview1.setRecommendation("HIRE");
            interviewRepository.save(interview1);

            Interview interview2 = new Interview();
            interview2.setApplication(app1);
            interview2.setInterviewer(hrUser);
            interview2.setInterviewDate(LocalDate.now().plusDays(4));
            interview2.setInterviewTime(LocalTime.of(11, 0));
            interview2.setInterviewType("VIDEO");
            interview2.setMeetingLink("https://meet.google.com/nex-hire-eng");
            interview2.setStatus("SCHEDULED");
            interview2.setRating(4);
            interview2.setRecommendation("HIRE");
            interviewRepository.save(interview2);

            log.info("NexaHire sample database seed completed successfully with active jobs, candidates, pipeline applications, and scheduled interviews.");
        }
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private User getOrCreateUser(String email, String username, String rawPassword, String fullName, String phone, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User(email, username, passwordEncoder.encode(rawPassword), fullName, phone, role);
            return userRepository.save(user);
        });
    }

    private CandidateProfile getOrCreateCandidateProfile(User user, String headline, String summary, String skills,
                                                         BigDecimal expYears, String education, String location,
                                                         BigDecimal currentSal, BigDecimal expectedSal) {
        return candidateProfileRepository.findByUser(user).orElseGet(() -> {
            CandidateProfile cp = new CandidateProfile(user);
            cp.setHeadline(headline);
            cp.setSummary(summary);
            cp.setSkills(skills);
            cp.setExperienceYears(expYears);
            cp.setEducation(education);
            cp.setLocation(location);
            cp.setCurrentSalary(currentSal);
            cp.setExpectedSalary(expectedSal);
            return candidateProfileRepository.save(cp);
        });
    }

    private RecruiterProfile getOrCreateRecruiterProfile(User user, String dept, String designation, String company) {
        return recruiterProfileRepository.findByUser(user).orElseGet(() -> {
            RecruiterProfile rp = new RecruiterProfile(user, dept, designation, company);
            return recruiterProfileRepository.save(rp);
        });
    }

    private Job createJob(String title, String dept, String jobType, String location, int minExp, int maxExp,
                          BigDecimal minSal, BigDecimal maxSal, String desc, String reqSkills, String niceSkills,
                          String status, User createdBy, LocalDate deadline) {
        Job job = new Job();
        job.setTitle(title);
        job.setDepartment(dept);
        job.setJobType(jobType);
        job.setLocation(location);
        job.setExperienceMin(minExp);
        job.setExperienceMax(maxExp);
        job.setSalaryMin(minSal);
        job.setSalaryMax(maxSal);
        job.setDescription(desc);
        job.setRequiredSkills(reqSkills);
        job.setNiceToHaveSkills(niceSkills);
        job.setStatus(status);
        job.setCreatedBy(createdBy);
        job.setDeadline(deadline);
        return jobRepository.save(job);
    }

    private Application createApplication(Job job, CandidateProfile candidate, String status, int matchScore,
                                          String matchedSkills, String missingSkills, String coverLetter, String notes) {
        Application app = new Application(job, candidate, null, coverLetter);
        app.setStatus(status);
        app.setMatchScore(matchScore);
        app.setMatchedSkills(matchedSkills);
        app.setMissingSkills(missingSkills);
        app.setRecruiterNotes(notes);
        return applicationRepository.save(app);
    }
}
