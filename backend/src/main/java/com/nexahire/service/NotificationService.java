package com.nexahire.service;

import com.nexahire.entity.Notification;
import com.nexahire.entity.User;
import com.nexahire.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply.nexahire@gmail.com}")
    private String fromEmail;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification sendNotification(User user, String title, String message, String type, String link) {
        Notification notification = new Notification(user, title, message, type, link);
        Notification saved = notificationRepository.save(notification);

        // Send asynchronous email notification if mailSender is available
        sendEmailNotification(user.getEmail(), title, message);

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Async
    public void sendEmailNotification(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            if (mailSender != null && mailPassword != null && !mailPassword.contains("mock") && !mailPassword.isBlank()) {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(fromEmail);
                mailMessage.setTo(toEmail);
                mailMessage.setSubject("[NexaHire] " + subject);
                mailMessage.setText(body + "\n\nBest regards,\nThe NexaHire Recruitment Team");
                mailSender.send(mailMessage);
                log.info("Email sent successfully to {}", toEmail);
            } else {
                log.info("[SIMULATED EMAIL NOTIFICATION] To: {} | Subject: [NexaHire] {} | Body:\n{}", toEmail, subject, body);
            }
        } catch (Exception e) {
            log.warn("Could not dispatch live email to {} (simulated delivery logged): {}", toEmail, e.getMessage());
        }
    }
}
