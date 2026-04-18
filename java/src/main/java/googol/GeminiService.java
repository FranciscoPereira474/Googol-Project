package googol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private String mainAPI_KEY;
    private String FINAL_ENDPOINT = null;

    @Autowired
    private RestTemplate restTemplate;

    public String gerarTexto(String prompt) {
        

        if (API_KEY == null || API_KEY.isEmpty() && mainAPI_KEY == null && FINAL_ENDPOINT == null) {
            mainAPI_KEY = setAPIkey();
            System.out.println("API_KEY: " + mainAPI_KEY);

            FINAL_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + mainAPI_KEY;
            
        }
        else if (API_KEY != null && FINAL_ENDPOINT == null) {
            FINAL_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
        }

        
        String fullPrompt = "Faz uma análise rápida e objetiva sobre a seguinte pesquisa: " + prompt + 
        ". Resume o que o utilizador provavelmente quer saber e apresenta uma explicação clara, concisa e informativa. " +
        "Responde na linguagem usada na pesquisa (o que vem a seguir ao ':') como se fosses uma IA que está a servir de assistencia (mas num tom profissional).";
            

        JSONObject requestBody = new JSONObject()
            .put("contents", new JSONArray()
                .put(new JSONObject()
                    .put("parts", new JSONArray()
                        .put(new JSONObject().put("text", fullPrompt)))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(FINAL_ENDPOINT, entity, String.class);

        JSONObject jsonResponse = new JSONObject(response.getBody());
        JSONArray candidates = jsonResponse.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text");
            }
        }

        return "Erro: resposta vazia";
    }

    private String setAPIkey(){
        String filePath = ".property_file"; // Path to the property file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("GEMINI_API_KEY:")) {
                    return line.substring("GEMINI_API_KEY:".length());
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler o ficheiro de propriedades");
       }
       return null;
    }
}
