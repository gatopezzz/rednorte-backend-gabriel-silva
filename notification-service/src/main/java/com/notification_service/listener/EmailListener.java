package com.rednorte.notification_service.listener;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailListener {

    @Autowired
    private JavaMailSender mailSender;

    @RabbitListener(queuesToDeclare = @Queue("registro.email.queue"))
    public void procesarRegistro(String emailDestino) {
        System.out.println("Atrapé un mensaje de RabbitMQ! Enviando correo a: " + emailDestino);

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("tu_correo_rednorte@gmail.com");
            mensaje.setTo(emailDestino);
            mensaje.setSubject("¡Bienvenido a RedNorte!");
            mensaje.setText("Hola,\n\nTu cuenta ha sido creada exitosamente. " +
                           "Ahora puedes unirte a nuestras listas de espera desde la plataforma.\n\n" +
                           "Saludos,\nEquipo RedNorte.");

            mailSender.send(mensaje);
            System.out.println("Correo enviado con éxito.");
        } catch (Exception e) {
            System.err.println("Error al enviar email: " + e.getMessage());
        }
    }
}