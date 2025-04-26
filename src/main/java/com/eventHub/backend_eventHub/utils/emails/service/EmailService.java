package com.eventHub.backend_eventHub.utils.emails.service;



import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.eventHub.backend_eventHub.utils.emails.dto.EmailDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailClient emailClient;

    public EmailService(@Value("${azure.communication.connection-string}") String connectionString) {
        this.emailClient = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    public void sendEmail(EmailDto emailDto) {
        // Validar que el destinatario tenga formato de correo válido
        if (!isValidEmail(emailDto.getRecipientEmail())) {
            System.err.println("Correo inválido: " + emailDto.getRecipientEmail());
            return;
        }

        EmailAddress toAddress = new EmailAddress(emailDto.getRecipientEmail());

        // Estructura del cuerpo del correo utilizando `String.format()` para mejor legibilidad
        String emailBody = String.format("""
            <html>
                <body>
                    <h1>%s</h1>
                    <p>%s</p>
                </body>
            </html>
        """, emailDto.getSubject(), emailDto.getBody());

        EmailMessage emailMessage = new EmailMessage()
                .setSenderAddress("DoNotReply@cde1ce14-e182-4267-989e-7190980d4b40.azurecomm.net")
                .setToRecipients(toAddress)
                .setSubject(emailDto.getSubject())
                .setBodyHtml(emailBody);

        try {
            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
            PollResponse<EmailSendResult> result = poller.waitForCompletion();

            // Manejo correcto del resultado usando el Enum `EmailSendStatus`
            if (result.getValue().getStatus() == EmailSendStatus.SUCCEEDED) {
                System.out.println("✅ Correo enviado exitosamente a: " + emailDto.getRecipientEmail());
            } else {
                System.err.println("❌ Error al enviar el correo: " + result.getValue().getError().getMessage());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Excepción al enviar el correo: " + e.getMessage());
        }
    }

    // Método auxiliar para validar que el email sea válido
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
