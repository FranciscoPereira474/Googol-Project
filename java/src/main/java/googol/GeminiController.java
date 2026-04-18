package googol;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/gerar")
    public String gerar(@RequestParam(defaultValue = "Diz uma curiosidade aleatório sobre o dia de hoje!") String prompt) {
        try {
            return geminiService.gerarTexto(prompt);
        } catch (Exception e) {
            return "Erro: " + e.getMessage();
        }
    }
}
