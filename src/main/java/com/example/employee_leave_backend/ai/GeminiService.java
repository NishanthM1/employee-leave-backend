package com.example.employee_leave_backend.ai;

import com.example.employee_leave_backend.exception.AiServiceException;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    public static final String SYSTEM_INSTRUCTION =
            "You are an AI Leave Assistant for an Employee Leave Management System.\n\n"
                    + "You are helping the currently authenticated employee.\n\n"
                    + "Use ONLY the employee information and leave information provided in the context.\n\n"
                    + "Never invent:\n"
                    + "- leave balances\n"
                    + "- leave requests\n"
                    + "- dates\n"
                    + "- leave types\n"
                    + "- employee information\n"
                    + "- company policies\n\n"
                    + "Never reveal information about another employee.\n\n"
                    + "If the requested information is not present in the provided context, say that the information is not available.\n\n"
                    + "You are an assistant, not the final authority.\n\n"
                    + "Do not approve or reject leave requests.\n\n"
                    + "Do not modify leave balances.\n\n"
                    + "Do not create leave requests.\n\n"
                    + "Do not cancel leave requests.\n\n"
                    + "Do not approve leave requests.\n\n"
                    + "Do not reject leave requests.\n\n"
                    + "If the employee wants to apply for leave, direct them to the normal Apply Leave feature.\n\n"
                    + "Keep responses concise, clear, and professional.";

    private final String apiKey;
    private final String model;
    private volatile Client client;

    public GeminiService(
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-model:gemini-flash-latest}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateReply(String message) {
        if (apiKey.isBlank()) {
            throw new AiServiceException("Gemini API key is not configured");
        }

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                    .build();
            GenerateContentResponse response = getClient()
                    .models
                    .generateContent(model, message, config);
            String reply = response.text();
            if (reply == null || reply.isBlank()) {
                throw new AiServiceException("Gemini returned an empty response");
            }
            return reply;
        } catch (AiServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiServiceException("Gemini request failed", exception);
        }
    }

    private Client getClient() {
        Client current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = Client.builder().apiKey(apiKey).build();
                    client = current;
                }
            }
        }
        return current;
    }
}
