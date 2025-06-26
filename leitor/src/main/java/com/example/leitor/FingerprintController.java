package com.example.leitor;

import com.example.leitor.dto.FingerprintRequest;
import com.example.leitor.model.MatchResult;
import com.example.leitor.model.TemplateResponse;
import com.machinezoo.sourceafis.*;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FingerprintController {

    private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/ponto_eletronico_tenant_tjap";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "admin";
    private static final double MATCH_THRESHOLD = 70; // Valor ajustado para melhor detecção

    // Classe auxiliar para armazenar template com dados do usuário
    private static class UserTemplate {
        private final Long userId;
        private final String userName;
        private final String userRegistration;
        private final FingerprintTemplate template;

        public UserTemplate(Long userId, String userName, String userRegistration, FingerprintTemplate template) {
            this.userId = userId;
            this.userName = userName;
            this.userRegistration = userRegistration;
            this.template = template;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserRegistration() {
            return userRegistration;
        }

        public FingerprintTemplate getTemplate() {
            return template;
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/fingerprint/match/")
    public MatchResult matchFingerprints(@RequestBody FingerprintRequest request) {
        try {
            byte[] fmdBytes1 = Base64.getDecoder().decode(request.getTemplate1());
            FingerprintImage fp1 = new FingerprintImage(fmdBytes1);
            FingerprintTemplate ft1 = new FingerprintTemplate(fp1);

            // Obter todos os templates com informações dos usuários
            List<UserTemplate> userTemplates = getUserTemplates();

            // Comparar com cada template armazenado
            FingerprintMatcher matcher = new FingerprintMatcher(ft1);
            double highestSimilarity = 0;
            UserTemplate bestMatch = null;

            for (UserTemplate userTemplate : userTemplates) {
                double similarity = matcher.match(userTemplate.getTemplate());
                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                    bestMatch = userTemplate;
                }
            }

            // Verificar se o melhor match atinge o threshold
            if (bestMatch != null && highestSimilarity >= MATCH_THRESHOLD) {
                return new MatchResult(
                    true,
                    highestSimilarity,
                    bestMatch.getUserId(),
                    bestMatch.getUserName(),
                    bestMatch.getUserRegistration()
                );
            } else {
                return new MatchResult(false, highestSimilarity, null, null, null);
            }

        } catch (Exception e) {
            return new MatchResult(false, 0.0, "Erro: " + e.getMessage());
        }
    }

    private List<UserTemplate> getUserTemplates() throws SQLException {
        List<UserTemplate> templates = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, name, registration, biometrics FROM users WHERE biometrics IS NOT NULL";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Long userId = rs.getLong("id");
                    String userName = rs.getString("name");
                    String userRegistration = rs.getString("registration");
                    String templateBase64 = rs.getString("biometrics");
                    
                    if (templateBase64 != null && !templateBase64.isEmpty()) {
                        try {
                            byte[] templateBytes = Base64.getDecoder().decode(templateBase64);
                            FingerprintTemplate template = new FingerprintTemplate(templateBytes);
                            templates.add(new UserTemplate(userId, userName, userRegistration, template));
                        } catch (Exception e) {
                            System.err.println("Erro ao processar template para usuário " + userId + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        return templates;
    }

    // Método original mantido para compatibilidade
    private List<FingerprintTemplate> getStoredTemplates() throws SQLException {
        return getUserTemplates().stream()
                .map(UserTemplate::getTemplate)
                .collect(Collectors.toList());
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/fingerprint/create-template/")
    public TemplateResponse createTemplate(@RequestBody FingerprintRequest request) {
        try {
            byte[] fmdBytes = Base64.getDecoder().decode(request.getTemplate1());
            FingerprintImage fpImage = new FingerprintImage(fmdBytes);
            
            FingerprintTemplate template = new FingerprintTemplate(fpImage);
            String templateBase64 = Base64.getEncoder().encodeToString(template.toByteArray());
            
            return new TemplateResponse(true, "Template criado com sucesso", templateBase64);
        } catch (Exception e) {
            return new TemplateResponse(false, "Erro ao criar template: " + e.getMessage(), null);
        }
    }
}